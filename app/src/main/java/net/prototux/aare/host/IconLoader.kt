package net.prototux.aare.host

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.util.LruCache
import androidx.car.app.model.CarIcon
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import net.prototux.aare.media.ArtLoader

/**
 * Loads car app icons. Resource icons live in the app's own package, so they're resolved
 * against its resources, with the car's configuration (night mode, density) rather than the
 * phone's: apps ship -night and density variants for the car. URI icons rely on the app
 * granting us read access (it does so for the host package it handshook with); they are only opened
 * when the owning app's package is known and the URI passes [ArtLoader.allowed].
 */
class IconLoader(private val context: Context, private val env: HostEnvironment) {
    private val cache = object : LruCache<String, ImageBitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }
    /** Package contexts by "package:night:dpi" (creating one parses the APK's resource table). */
    private val packageContexts = HashMap<String, Context>()
    /** Icons that failed to load, so the reason is logged once and not on every template refresh. */
    private val failed = HashSet<String>()

    /** [owner] is the package that supplied [icon]; without it URI icons are not loaded. */
    fun load(icon: CarIcon, sizePx: Int, owner: String?): ImageBitmap? {
        val ic = icon.icon ?: return null
        val night = env.night
        val key = when (ic.type) {
            IconCompat.TYPE_RESOURCE -> "res:${ic.resPackage}:${ic.resId}:$sizePx:${if (night) "night" else "day"}"
            IconCompat.TYPE_URI, IconCompat.TYPE_URI_ADAPTIVE_BITMAP -> "uri:$owner:${ic.uri}:$sizePx"
            else -> "obj:${System.identityHashCode(ic)}:$sizePx"
        }
        cache.get(key)?.let { return it }
        val drawable = runCatching {
            if (ic.type == IconCompat.TYPE_RESOURCE) {
                val pkgContext = packageContext(ic.resPackage, night)
                androidx.core.content.res.ResourcesCompat.getDrawable(pkgContext.resources, ic.resId, pkgContext.theme)
            } else {
                if (ic.type == IconCompat.TYPE_URI || ic.type == IconCompat.TYPE_URI_ADAPTIVE_BITMAP) {
                    require(ArtLoader.allowed(context, ic.uri, owner)) { "icon uri of $owner not allowed" }
                }
                ic.loadDrawable(context)
            }
        }.onFailure { e ->
            // Once per icon: an icon that can't be drawn leaves an empty button, say why in logcat.
            if (synchronized(failed) { failed.add(key) }) Log.w(TAG, "can't load car icon $key from $owner: $e")
        }.getOrNull() ?: return null
        val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else sizePx
        val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else sizePx
        val scale = sizePx.toFloat() / maxOf(w, h)
        val bmp = runCatching {
            drawable.toBitmap(maxOf(1, (w * scale).toInt()), maxOf(1, (h * scale).toInt())).asImageBitmap()
        }.getOrNull() ?: return null
        cache.put(key, bmp)
        return bmp
    }

    /** [pkg]'s resources as the car sees them: same night bit and density as CarAppConnection.configuration(). */
    private fun packageContext(pkg: String, night: Boolean): Context {
        val dpi = env.densityDpi
        val key = "$pkg:$night:$dpi"
        synchronized(packageContexts) { packageContexts[key] }?.let { return it }
        val cfg = Configuration(context.resources.configuration).apply {
            densityDpi = dpi
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                (if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
        }
        val ctx = context.createPackageContext(pkg, 0).createConfigurationContext(cfg)
        // With the app's own theme, not the platform default a package context starts with: apps
        // color their vector icons with their own theme attributes (?attr/...), which don't resolve
        // without it. The icon then fails to inflate and its button is drawn empty.
        val theme = runCatching { context.packageManager.getApplicationInfo(pkg, 0).theme }.getOrDefault(0)
        if (theme != 0) runCatching { ctx.setTheme(theme) }
        synchronized(packageContexts) { packageContexts[key] = ctx }
        return ctx
    }

    private companion object {
        const val TAG = "AareIcons"
    }
}
