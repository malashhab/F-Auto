package net.prototux.aare.map

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.prototux.aare.ui.car.OfflineMaps
import org.mapsforge.map.reader.MapFile
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Downloads offline OpenStreetMap country files from download.mapsforge.org (the free mapsforge
 * `.map` v5 maps), ported from StradaOS's `MapDownload.java` (GPL-3.0) to Kotlin/coroutines.
 *
 * - Lists continents and countries by reading the server's directory index (always current).
 * - Downloads in the background to the app's offline-maps dir, into a `.part` file that resumes on a
 *   dropped connection (the server supports `Range`).
 * - On finish it validates the file with the mapsforge reader before publishing it as a `.map`.
 *
 * F-Auto targets modern Android, so StradaOS's TLS-1.2 shim for Android 4.x is dropped — the system
 * handles HTTPS. One download at a time; [state] drives the settings UI.
 */
object MapDownload {
    private const val BASE = "https://download.mapsforge.org/maps/v5/"

    /** A directory index entry: a folder (continent / country-with-regions) or a `.map` file. */
    data class Entry(val path: String, val name: String, val dir: Boolean, val size: Long, val date: String)

    data class Progress(
        val running: Boolean = false,
        val name: String = "",
        val done: Long = 0,
        val total: Long = 0,
        val speed: Long = 0,
        val error: String? = null,
        val finishedName: String? = null,
    ) {
        val percent get() = if (total > 0) (done * 100 / total).toInt() else 0
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    @Volatile private var cancelRequested = false
    @Volatile private var deleteOnCancel = false

    private val _state = MutableStateFlow(Progress())
    val state: StateFlow<Progress> = _state.asStateFlow()

    private val rowRegex = Regex(
        """<a href="([^"?/][^"]*)">[^<]*</a>\s*</td>\s*<td[^>]*>\s*(\d{4}-\d\d-\d\d)[^<]*</td>\s*<td[^>]*>\s*([0-9.]+[KMG]?|-)\s*</td>""",
    )

    /** Reads a server folder ("" = continents, "south-america/" = countries…). */
    suspend fun list(folder: String): List<Entry> = withContext(Dispatchers.IO) {
        val html = String(get(BASE + folder), Charsets.UTF_8)
        val out = ArrayList<Entry>()
        for (m in rowRegex.findAll(html)) {
            val href = m.groupValues[1]
            val dir = href.endsWith("/")
            if (!dir && !href.endsWith(".map")) continue
            val slug = if (dir) href.dropLast(1) else href.dropLast(4)
            if (folder.isEmpty() && slug == "world") continue // low-detail world map: not useful in a car
            out += Entry(folder + href, pretty(slug), dir, parseSize(m.groupValues[3]), m.groupValues[2])
        }
        out.sortedWith(compareBy({ it.name }, { it.dir }))
    }

    /** Starts (or resumes) downloading a `.map` path, e.g. "south-america/colombia.map". */
    fun start(context: Context, entry: Entry) {
        if (_state.value.running) return
        val app = context.applicationContext
        cancelRequested = false
        deleteOnCancel = false
        _state.value = Progress(running = true, name = entry.name)
        job = scope.launch { runDownload(app, entry) }
    }

    /** Pause keeps the partial file (resumes next time); cancel deletes it. */
    fun cancel(delete: Boolean) {
        deleteOnCancel = delete
        cancelRequested = true
    }

    private suspend fun runDownload(context: Context, entry: Entry) {
        val dir = OfflineMaps.dir(context)
        val fileName = entry.path.substringAfterLast('/')
        val dest = File(dir, fileName)
        val part = File(dir, "$fileName.part")
        var error: String? = null
        var ok = false
        try {
            var fails = 0
            while (isActiveAndWanted()) {
                try {
                    if (downloadChunk(entry.path, part)) { ok = true; break }
                    fails = 0
                } catch (e: IOException) {
                    if (++fails > 8 || cancelRequested) throw e
                    _state.value = _state.value.copy(error = "offline, retrying…")
                    repeat(fails * 3) { if (!cancelRequested) delay(1000) }
                    _state.value = _state.value.copy(error = null)
                }
            }
            if (ok && !cancelRequested) {
                val total = _state.value.total
                if (total > 0 && part.length() != total) throw IOException("file incomplete")
                // Validate it really is a mapsforge map before publishing it.
                runCatching { MapFile(part).close() }.onFailure { part.delete(); throw IOException("not a valid map file") }
                if (dest.exists()) dest.delete()
                if (!part.renameTo(dest)) throw IOException("could not save the file")
            }
        } catch (e: Throwable) {
            error = (e as? IOException)?.message ?: e.toString()
        } finally {
            if (cancelRequested && deleteOnCancel) part.delete()
            _state.value = Progress(
                running = false,
                error = if (cancelRequested) null else error,
                finishedName = if (ok && error == null && !cancelRequested) entry.name else null,
            )
        }
    }

    private fun isActiveAndWanted() = scope.isActive && !cancelRequested

    /** Downloads from where the `.part` left off. true = complete; false = server changed it, restart. */
    private fun downloadChunk(path: String, part: File): Boolean {
        var from = part.length()
        val conn = (URL(BASE + path).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20000
            readTimeout = 20000
            if (from > 0) setRequestProperty("Range", "bytes=$from-")
        }
        try {
            val code = conn.responseCode
            if (code == 416) return true // already complete
            if (code != 200 && code != 206) throw IOException("HTTP $code")
            if (code == 200) from = 0 // server ignored Range: start over
            val len = conn.getHeaderField("Content-Length")?.toLongOrNull() ?: -1
            val total = if (len >= 0) from + len else _state.value.total
            _state.value = _state.value.copy(total = total, done = from)
            java.io.RandomAccessFile(part, "rw").use { out ->
                out.setLength(from)
                out.seek(from)
                conn.inputStream.use { input ->
                    val buf = ByteArray(64 * 1024)
                    var done = from
                    var t0 = System.currentTimeMillis()
                    var b0 = from
                    while (!cancelRequested) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        out.write(buf, 0, n)
                        done += n
                        val now = System.currentTimeMillis()
                        if (now - t0 >= 1500) {
                            _state.value = _state.value.copy(done = done, speed = (done - b0) * 1000 / (now - t0))
                            t0 = now; b0 = done
                        }
                    }
                    _state.value = _state.value.copy(done = done)
                    if (cancelRequested) return false
                    if (total > 0 && done < total) throw IOException("download cut off")
                }
            }
            return true
        } finally {
            conn.disconnect()
        }
    }

    private fun get(url: String): ByteArray {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 15000; readTimeout = 15000 }
        try {
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseSize(s: String): Long {
        if (s == "-") return 0
        val u = s.last()
        val mul = when (u) { 'K' -> 1024.0; 'M' -> 1024.0 * 1024; 'G' -> 1024.0 * 1024 * 1024; else -> 1.0 }
        val num = (if (u.isDigit()) s else s.dropLast(1)).toDoubleOrNull() ?: return 0
        return (num * mul).toLong()
    }

    private fun pretty(slug: String): String = slug.split('-', '_')
        .filter { it.isNotEmpty() }
        .joinToString(" ") { it.replaceFirstChar { c -> c.titlecase(Locale.US) } }

    fun mb(bytes: Long): String = when {
        bytes >= 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024 * 1024))
        else -> "${maxOf(1, Math.round(bytes / (1024.0 * 1024)))} MB"
    }
}
