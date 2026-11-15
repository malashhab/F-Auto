package net.prototux.aare.car

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The customizable vehicle photo: pick a photo of the car and keep it with its background removed,
 * to show on the dashboard. Ported from StradaOS's `CarPhoto.java` (GPL-3.0). Background removal is
 * a self-contained edge flood-fill (grows transparent pixels inward from the border by colour
 * similarity) — no ML, no dependency — which works well on photos with a fairly even background.
 */
object CarPhoto {
    private const val FILE = "vehicle_photo.png"
    private const val MAX = 900 // max side while processing, to bound memory/CPU

    fun file(context: Context): File = File(context.filesDir, FILE)
    fun load(context: Context): Bitmap? = runCatching { BitmapFactory.decodeFile(file(context).absolutePath) }.getOrNull()
    fun delete(context: Context) { file(context).delete() }

    fun save(context: Context, bitmap: Bitmap): Boolean = runCatching {
        file(context).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        true
    }.getOrDefault(false)

    /** Decode a picked image (honouring EXIF rotation), scale it down, and remove its background. */
    fun fromUri(context: Context, uri: Uri, strength: Int = 2): Bitmap? = try {
        val raw = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
        if (raw == null) null else {
            val rotation = exifRotation(context, uri)
            val oriented = if (rotation != 0) {
                Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
            } else raw
            val small = scaled(oriented, MAX)
            removeBackground(small, strength) ?: small
        }
    } catch (e: Exception) { null }

    private fun exifRotation(context: Context, uri: Uri): Int = try {
        context.contentResolver.openInputStream(uri).use { stream ->
            if (stream == null) 0 else when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }
    } catch (e: Exception) { 0 }

    fun scaled(b: Bitmap, maxSide: Int): Bitmap {
        val k = min(1f, maxSide.toFloat() / max(b.width, b.height))
        if (k >= 1f) return b
        return Bitmap.createScaledBitmap(b, max(1, (b.width * k).roundToInt()), max(1, (b.height * k).roundToInt()), true)
    }

    /**
     * Removes the background by growing from the edges over pixels close to the border colour.
     * [strength] 0..3: higher removes more (shadows, gradients). Returns null if no clear background
     * was found (too little or nearly everything would be removed).
     */
    fun removeBackground(src: Bitmap, strength: Int): Bitmap? {
        val w = src.width
        val h = src.height
        val n = w * h
        val p = IntArray(n)
        src.getPixels(p, 0, w, 0, 0, w, h)

        val seedTol = 90 + strength * 45
        val stepTol = 26 + strength * 10

        // Dominant border colour: the per-channel median of the border pixels, then averaged over
        // the border pixels close to it (robust to foreground objects touching the edge).
        val border = IntArray(2 * (w + h))
        var bi = 0
        for (x in 0 until w) { border[bi++] = p[x]; border[bi++] = p[(h - 1) * w + x] }
        for (y in 0 until h) { border[bi++] = p[y * w]; border[bi++] = p[y * w + w - 1] }
        var seed = medianColor(border, bi)
        run {
            var sr = 0L; var sg = 0L; var sb = 0L; var sc = 0
            for (i in 0 until bi) if (dist(border[i], seed) < seedTol) {
                sr += (border[i] shr 16) and 255; sg += (border[i] shr 8) and 255; sb += border[i] and 255; sc++
            }
            if (sc > 0) seed = (0xFF000000.toInt()) or (((sr / sc).toInt()) shl 16) or (((sg / sc).toInt()) shl 8) or (sb / sc).toInt()
        }

        val bg = BooleanArray(n)
        val q = IntArray(n)
        var tail = 0
        fun push(i: Int, tol: Int) { if (!bg[i] && dist(p[i], seed) < tol) { bg[i] = true; q[tail++] = i } }
        fun grow(i: Int, from: Int, step: Int, tol: Int) { if (!bg[i] && dist(p[i], from) < step && dist(p[i], seed) < tol) { bg[i] = true; q[tail++] = i } }

        for (x in 0 until w) { push(x, seedTol); push((h - 1) * w + x, seedTol) }
        for (y in 0 until h) { push(y * w, seedTol); push(y * w + w - 1, seedTol) }
        var head = 0
        while (head < tail) {
            val i = q[head++]
            val x = i % w; val y = i / w; val c = p[i]
            if (x > 0) grow(i - 1, c, stepTol, seedTol)
            if (x < w - 1) grow(i + 1, c, stepTol, seedTol)
            if (y > 0) grow(i - w, c, stepTol, seedTol)
            if (y < h - 1) grow(i + w, c, stepTol, seedTol)
        }
        if (strength >= 2) removeReflection(p, bg, w, h)

        var removed = 0
        for (i in 0 until n) if (bg[i]) removed++
        if (removed < n * 0.08 || removed > n * 0.97) return null

        // Drop foreground islands and soften the edge with a simple neighbour-count alpha.
        val out = IntArray(n)
        for (i in 0 until n) {
            if (bg[i]) { out[i] = 0; continue }
            val x = i % w; val y = i / w
            var around = 0
            if (x > 0 && bg[i - 1]) around++
            if (x < w - 1 && bg[i + 1]) around++
            if (y > 0 && bg[i - w]) around++
            if (y < h - 1 && bg[i + w]) around++
            val a = when (around) { 0 -> 255; 1 -> 200; 2 -> 130; else -> 60 }
            out[i] = (a shl 24) or (p[i] and 0x00FFFFFF)
        }
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { setPixels(out, 0, w, 0, 0, w, h) }
    }

    /** Removes the grey floor reflection/shadow: grows from the background over light-grey pixels in the lower third. */
    private fun removeReflection(p: IntArray, bg: BooleanArray, w: Int, h: Int) {
        var minY = h; var maxY = -1
        for (y in 0 until h) for (x in 0 until w) if (!bg[y * w + x]) { if (y < minY) minY = y; maxY = y }
        if (maxY < 0) return
        val start = minY + ((maxY - minY) * 0.70f).toInt()
        val q = IntArray(w * h)
        var head = 0; var tail = 0
        for (y in start until h) for (x in 0 until w) if (bg[y * w + x]) q[tail++] = y * w + x
        while (head < tail) {
            val i = q[head++]
            val x = i % w; val y = i / w
            val nb = intArrayOf(if (x > 0) i - 1 else -1, if (x < w - 1) i + 1 else -1, if (y > start) i - w else -1, if (y < h - 1) i + w else -1)
            for (j in nb) {
                if (j < 0 || bg[j]) continue
                val c = p[j]; val r = (c shr 16) and 255; val g = (c shr 8) and 255; val b = c and 255
                val mx = max(r, max(g, b)); val mn = min(r, min(g, b))
                if (mx > 90 && mx - mn < 45) { bg[j] = true; q[tail++] = j }
            }
        }
    }

    private fun dist(a: Int, b: Int): Int =
        abs(((a shr 16) and 255) - ((b shr 16) and 255)) +
        abs(((a shr 8) and 255) - ((b shr 8) and 255)) +
        abs((a and 255) - (b and 255))

    private fun medianColor(c: IntArray, len: Int): Int {
        val r = IntArray(256); val g = IntArray(256); val b = IntArray(256)
        for (i in 0 until len) { r[(c[i] shr 16) and 255]++; g[(c[i] shr 8) and 255]++; b[c[i] and 255]++ }
        return (0xFF000000.toInt()) or (med(r, len) shl 16) or (med(g, len) shl 8) or med(b, len)
    }

    private fun med(hist: IntArray, len: Int): Int {
        var acc = 0
        for (v in 0 until 256) { acc += hist[v]; if (acc >= len / 2) return v }
        return 255
    }
}
