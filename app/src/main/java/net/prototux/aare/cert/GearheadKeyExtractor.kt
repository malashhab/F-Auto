package net.prototux.aare.cert

import android.content.Context
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.security.KeyFactory
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.zip.ZipInputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * On-device port of [extract_key.py](../../../../../../../../../extract_key.py) (repository
 * root): pulls Android Auto's CarService certificate and RSA private key out of a Gearhead
 * (`com.google.android.projection.gearhead`) build, the same way
 * github.com/iscle/android-auto-certificates does it, but without a computer, Python or a
 * third-party repository - either from the Gearhead app already installed on this phone (no
 * download at all: the app already holds `QUERY_ALL_PACKAGES`, and an installed APK's file is
 * world-readable without root) or from an APK/APKM/APKS/XAPK the user picked.
 *
 * Why this exists: [CertificateDownload] depends on that repository staying current, and
 * `extract_key.py` depends on having a computer. This depends on neither - it reuses Android's own
 * crypto (`javax.crypto`, `KeyFactory`, `CertificateFactory`, the real AOSP `android.util.Base64`)
 * instead of re-implementing AES and ASN.1 in pure Python, and finishes by running the downloaded
 * certificate's own check ([CertificateDownload.check]) so a wrong candidate can't be mistaken for
 * the real key. See docs/research/gearhead-notes.md for what the extracted fields mean and
 * extract_key.py's docstring for the layout this is scanning for (a build where it changed needs
 * that layout note updated on both sides).
 */
object GearheadKeyExtractor {
    const val GEARHEAD_PACKAGE = "com.google.android.projection.gearhead"

    /** Nothing usable was found: not installed, no key blob in what was scanned, or no candidate verified. */
    class NotFound(message: String) : Exception(message)

    data class Extracted(val certPem: String, val keyPem: String, val source: String)

    /** The installed Gearhead's version name, or null if Google's Android Auto app isn't on this phone. */
    fun installedVersion(context: Context): String? = runCatching {
        context.packageManager.getPackageInfo(GEARHEAD_PACKAGE, 0).versionName
    }.getOrNull()

    /** Extracts from the Gearhead build already installed on this phone. Blocking: call off the main thread. */
    fun extractFromInstalled(context: Context): Extracted {
        val pm = context.packageManager
        val appInfo = runCatching { pm.getApplicationInfo(GEARHEAD_PACKAGE, 0) }
            .getOrElse { throw NotFound("Google's Android Auto app isn't installed on this phone") }
        val version = installedVersion(context) ?: "?"
        val paths = listOf(appInfo.sourceDir) + appInfo.splitSourceDirs.orEmpty().toList()
        val dexes = paths.flatMap { path -> loadDexBlobs(open = { FileInputStream(path) }) }
        return extract(dexes, "the installed Android Auto $version")
    }

    /** Extracts from an APK/APKM/APKS/XAPK the user picked. Blocking: call off the main thread. */
    fun extractFromUri(context: Context, uri: Uri): Extracted {
        val dexes = loadDexBlobs(open = {
            context.contentResolver.openInputStream(uri) ?: throw NotFound("couldn't open the selected file")
        })
        return extract(dexes, "the selected file")
    }

    // ---------------------------------------------------------------- extraction

    private data class Arr(val off: Int, val size: Int, val data: ByteArray)
    private data class Candidate(val salt: ByteArray, val enc: ByteArray)
    private class Cert(val pem: String, val subject: String)

    private fun extract(dexes: List<ByteArray>, source: String): Extracted {
        if (dexes.isEmpty()) throw NotFound("no classes*.dex found in $source")

        val allCerts = dexes.flatMap { dex -> findCertStrings(dex).map { Cert(it, subjectOf(it)) } }
            .distinctBy { it.pem }
        val galCerts = allCerts.filter { it.subject.contains("Google Automotive Link", true) && !it.subject.contains("CarService", true) }
        val carCerts = allCerts - galCerts.toSet()
        if (carCerts.isEmpty()) throw NotFound("no CarService-looking certificate found in $source - is this an Android Auto app?")
        val galPem = galCerts.firstOrNull()?.pem ?: GAL_CA_PEM

        val pairs = mutableListOf<Candidate>()
        val salts256 = mutableListOf<ByteArray>()
        val ciphers512 = mutableListOf<ByteArray>()
        for (dex in dexes) {
            val arrays = findByteArrays(dex)
            pairs += findCandidatePairs(arrays)
            salts256 += arrays.filter { it.size == 256 }.map { it.data }
            ciphers512 += arrays.filter { it.size >= 512 && it.size % 16 == 0 }.map { it.data }
        }

        // The adjacent-payload heuristic first (fast, and right in every build seen so far); only
        // if that finds nothing usable, the slower full cross-product of same-dex-file candidates.
        for (cand in pairs) tryCandidate(cand.salt, cand.enc, carCerts, galPem)?.let { return it.copy(source = source) }
        for (salt in salts256) for (enc in ciphers512) tryCandidate(salt, enc, carCerts, galPem)?.let { return it.copy(source = source) }

        throw NotFound(
            "couldn't derive a key from $source - either it holds no key blob (a secondary/config " +
                "split APK carries none) or this build's layout changed (see extract_key.py's docstring)"
        )
    }

    /** Tries one (salt, ciphertext) blob against every candidate cert, both KDF orderings. Null if none verify. */
    private fun tryCandidate(salt: ByteArray, enc: ByteArray, carCerts: List<Cert>, galPem: String): Extracted? {
        for (car in carCerts) {
            for ((first, second) in listOf(car.pem to galPem, galPem to car.pem)) {
                val (key, iv) = deriveKeyMaterial(salt, first.toByteArray(), second.toByteArray())
                val plain = runCatching {
                    Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
                        init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
                    }.doFinal(enc)
                }.getOrNull() ?: continue
                if (plain.size < 55) continue
                val mid = plain.copyOfRange(28, plain.size - 26)
                val der = runCatching { android.util.Base64.decode(mid, android.util.Base64.DEFAULT) }.getOrNull() ?: continue
                val keyPem = runCatching {
                    KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(der)) // throws if not a valid RSA key
                    CertificateStore.pem("PRIVATE KEY", der)
                }.getOrNull() ?: continue
                // The cheap checks above can accept a wrong (salt, cert-order) pair by chance; only a
                // signature that verifies against the certificate, chained to the pinned Google root,
                // confirms this is really the key.
                val verified = runCatching { CertificateDownload.check(car.pem + "\n" + galPem, keyPem) }.isSuccess
                if (verified) return Extracted(car.pem, keyPem, "")
            }
        }
        return null
    }

    // ---------------------------------------------------------------- dex scanning

    /** Yields every classes*.dex reachable from one zip (an apk, or a bundle holding further apks/zips). */
    private fun loadDexBlobs(open: () -> InputStream, depth: Int = 3): List<ByteArray> {
        val out = mutableListOf<ByteArray>()
        val zis = ZipInputStream(open())
        try {
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                val base = name.substringAfterLast('/')
                when {
                    DEX_NAME.matches(base) -> {
                        val bytes = zis.readBytes()
                        val magic = if (bytes.size >= 4) String(bytes, 0, 4, Charsets.ISO_8859_1) else ""
                        if (magic == "dex\n" || magic == "cdex") out += bytes
                    }
                    depth > 0 && NESTED_ARCHIVE_NAME.matches(name) -> {
                        val bytes = zis.readBytes()
                        out += loadDexBlobs({ ByteArrayInputStream(bytes) }, depth - 1)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        } catch (_: Exception) {
            // A bundle/APK that's truncated, encrypted, or just not a zip: whatever was already
            // collected still stands a chance; the caller reports NotFound only if nothing came of it.
        } finally {
            runCatching { zis.close() }
        }
        return out
    }

    /** All PEM certificates stored as strings in a DEX, exactly as the app sees them (incl. the trailing NUL). */
    private fun findCertStrings(dex: ByteArray): List<String> {
        val text = String(dex, Charsets.ISO_8859_1) // 1 byte = 1 char: offsets into `text` are byte offsets into `dex`
        val seen = mutableSetOf<String>()
        val out = mutableListOf<String>()
        var idx = text.indexOf(PEM_BEGIN)
        while (idx >= 0) {
            val endIdx = text.indexOf(PEM_END, idx)
            if (endIdx >= 0) {
                val nul = text.indexOf('\u0000', endIdx + PEM_END.length)
                if (nul >= 0) {
                    val s = text.substring(idx, nul)
                    if (seen.add(s)) out += s
                }
            }
            idx = text.indexOf(PEM_BEGIN, idx + 1)
        }
        return out
    }

    /** Dalvik `array-payload` structures holding 1-byte elements: u16 ident(0x0300), u16 elemSize(1), u32 size, data. */
    private fun findByteArrays(dex: ByteArray, minSize: Int = 64, maxSize: Int = 1 shl 20): List<Arr> {
        val text = String(dex, Charsets.ISO_8859_1)
        val out = mutableListOf<Arr>()
        var idx = text.indexOf(ARRAY_PAYLOAD_MARKER)
        while (idx >= 0) {
            if (idx % 2 == 0 && idx + 8 <= dex.size) { // code units are 2-byte aligned
                val size = readU32LE(dex, idx + 4)
                if (size in minSize..maxSize && idx + 8 + size <= dex.size) {
                    out += Arr(idx, size, dex.copyOfRange(idx + 8, idx + 8 + size))
                }
            }
            idx = text.indexOf(ARRAY_PAYLOAD_MARKER, idx + ARRAY_PAYLOAD_MARKER.length)
        }
        return out
    }

    /**
     * The (salt, ciphertext) blob pairs: in every known build the two `byte[]` initialisers are
     * emitted back to back in one `array-payload` chain, so a salt of any size immediately followed
     * (+/- 2-byte alignment padding) by a cipher-sized blob is a pair, without knowing field names.
     */
    private fun findCandidatePairs(arrays: List<Arr>): List<Candidate> {
        val byOff = arrays.associateBy { it.off }
        val pairs = mutableListOf<Candidate>()
        for (a in arrays) {
            for (delta in intArrayOf(8 + a.size, 8 + a.size + 2)) {
                val nxt = byOff[a.off + delta]
                if (nxt != null && nxt.size >= 512 && nxt.size % 16 == 0) pairs += Candidate(a.data, nxt.data)
            }
        }
        return pairs
    }

    private fun readU32LE(data: ByteArray, off: Int): Int =
        (data[off].toInt() and 0xFF) or
            ((data[off + 1].toInt() and 0xFF) shl 8) or
            ((data[off + 2].toInt() and 0xFF) shl 16) or
            ((data[off + 3].toInt() and 0xFF) shl 24)

    private fun subjectOf(pem: String): String = runCatching {
        (CertificateFactory.getInstance("X.509").generateCertificate(pem.byteInputStream()) as X509Certificate)
            .subjectX500Principal.name
    }.getOrDefault("")

    // ---------------------------------------------------------------- the app's KDF (uga.bl / ivq / jep)

    /** tables[j][v] == ((v>>7 | v+v) + 33) ^ salt[j % salt.size], low 8 bits. */
    private fun makeTables(salt: ByteArray): Array<IntArray> {
        val n = salt.size
        return Array(48) { j ->
            IntArray(256) { v -> ((((v shr 7) or (v + v)) + 33) xor (salt[j % n].toInt() and 0xFF)) and 0xFF }
        }
    }

    /**
     * Port of the app's `uga.bl(byte[] src, byte[] buf, byte[] salt)`: every source byte rotates all
     * 48 accumulator slots, mixing in the salt and XORing with the source byte.
     *
     * NOTE: when `src` and `buf` are the very same array (the seven self-mixing rounds do exactly
     * that), `src[i]` must be re-read on every inner-loop iteration, not cached once per outer `i` -
     * JADX's decompile of the original bytecode reads it that way, and the self-mix rounds mutate
     * `buf[i]` partway through the inner loop. Verified against extract_key.py deriving a valid
     * PKCS#8 key from a real APK with this exact ordering.
     */
    private fun tweak(src: ByteArray, buf: ByteArray, tables: Array<IntArray>) {
        if (src === buf) {
            for (i in src.indices) {
                for (j in 0 until 48) {
                    val sb = buf[i].toInt() and 0xFF // live read: may already be this iteration's updated value
                    buf[j] = (tables[j][buf[j].toInt() and 0xFF] xor sb).toByte()
                }
            }
            return
        }
        for (b in src) {
            val sb = b.toInt() and 0xFF
            for (j in 0 until 48) {
                buf[j] = (tables[j][buf[j].toInt() and 0xFF] xor sb).toByte()
            }
        }
    }

    /** buf = 0; tweak(cert1); tweak(cert2); tweak(buf) x7 ; key = buf[0:32], IV = buf[32:48]. */
    private fun deriveKeyMaterial(salt: ByteArray, cert1: ByteArray, cert2: ByteArray): Pair<ByteArray, ByteArray> {
        val buf = ByteArray(48)
        val tables = makeTables(salt)
        tweak(cert1, buf, tables)
        tweak(cert2, buf, tables)
        repeat(7) { tweak(buf, buf, tables) }
        return buf.copyOfRange(0, 32) to buf.copyOfRange(32, 48)
    }

    private val DEX_NAME = Regex("""classes\d*\.dex""")
    private val NESTED_ARCHIVE_NAME = Regex(""".*\.(apk|apks|apkm|xapk|zip|jar)""", RegexOption.IGNORE_CASE)
    private const val PEM_BEGIN = "-----BEGIN CERTIFICATE-----"
    private const val PEM_END = "-----END CERTIFICATE-----"
    private const val ARRAY_PAYLOAD_MARKER = "\u0000\u0003\u0001\u0000"

    /** Fallback only: used when no "Google Automotive Link" certificate string turns up in the scanned dex. */
    private val GAL_CA_PEM = (
        "-----BEGIN CERTIFICATE-----\n" +
            "MIIDiTCCAnGgAwIBAgIJAMFO56WkVE1CMA0GCSqGSIb3DQEBBQUAMFsxCzAJBgNV\n" +
            "BAYTAlVTMRMwEQYDVQQIDApDYWxpZm9ybmlhMRYwFAYDVQQHDA1Nb3VudGFpbiBW\n" +
            "aWV3MR8wHQYDVQQKDBZHb29nbGUgQXV0b21vdGl2ZSBMaW5rMB4XDTE0MDYwNjE4\n" +
            "MjgxOVoXDTQ0MDYwNTE4MjgxOVowWzELMAkGA1UEBhMCVVMxEzARBgNVBAgMCkNh\n" +
            "bGlmb3JuaWExFjAUBgNVBAcMDU1vdW50YWluIFZpZXcxHzAdBgNVBAoMFkdvb2ds\n" +
            "ZSBBdXRvbW90aXZlIExpbmswggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB\n" +
            "AQDUH+iIbwwVb74NdI5eBv/ACFmh4ml/NOW7gUVWdYX50n8uQQsHHLCNIhk5VV2H\n" +
            "hanvAZ/XXHPuVAPadE2HpnNqePKF/RDo4eJo/+rOief8gBYq/Z+OQTZeLdNm+GoI\n" +
            "HBrEjU4Ms8IdLuFW0jF8LlIRgekjLHpc7duUl3QpwBlmAWQK40T/SZjprlmhyqfJ\n" +
            "g1rxFdnGbrSibmCsTmb3m6WZyZUyrcwmd7t6q3pHbMABO+o02asPG/YPj/SJo4+i\n" +
            "fb5/Nk56f3hH9pBiPKQXJnVUdVLKMXSRgydDBsGSBol4C0JL77MNDrMR5jdafJ4j\n" +
            "mWmsa2+mnzoAv9AxEL9T0LiNAgMBAAGjUDBOMB0GA1UdDgQWBBS5dqvv8DPQiwrM\n" +
            "fgn8xKR91k7wgjAfBgNVHSMEGDAWgBS5dqvv8DPQiwrMfgn8xKR91k7wgjAMBgNV\n" +
            "HRMEBTADAQH/MA0GCSqGSIb3DQEBBQUAA4IBAQDKcnBsrbB0Jbz2VGJKP2lwYB6P\n" +
            "dCTCCpQu7dVp61UQOX+zWfd2hnNMnLs/r1xPO+eyN0vmw7sD05phaIhbXVauKWZi\n" +
            "9WqWHTaR+9s6CTyBOc1Mye0DMj+4vHt+WLmf0lYjkYUVYvR1EImX8ktXzkVmOqn+\n" +
            "e30siqlZ8pQpsOgegIKfJ+pNQM8c3eXVv3KFMUgjZW33SziZL8IMsLvSO+1LtH37\n" +
            "KqbTEMP6XUwVuZopgGvaHU74eT/WSRGlL7vX4OL5/UXXP4qsGH2Zp7uQlErv4H9j\n" +
            "kMs37UL1vGb4M8RM7Eyu9/RulepSmqZUF+3i+3eby8iGq/3OWk9wgJf7AXnx\n" +
            "-----END CERTIFICATE-----\n"
        )
}
