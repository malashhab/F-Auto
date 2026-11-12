package net.prototux.aare.host

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.graphics.drawable.toBitmap

/** A Car App Library app installed on the phone. */
data class CarAppInfo(
    val service: ComponentName,
    val label: String,
    val icon: Bitmap?,
    val categories: Set<String>,
    val minApiLevel: Int,
) {
    val packageName: String get() = service.packageName
    /**
     * The NAVIGATION category is self-declared by the app, in its own manifest — nothing stops an
     * app that isn't a map from claiming it to grab the dashboard's map widget (seen with Nuvio).
     * So it's trusted only for packages in [KNOWN_NAVIGATION_PACKAGES], actual map/navigation apps.
     */
    val isNavigation: Boolean get() = CATEGORY_NAVIGATION in categories && packageName in KNOWN_NAVIGATION_PACKAGES

    companion object {
        const val SERVICE_ACTION = "androidx.car.app.CarAppService"
        const val CATEGORY_NAVIGATION = "androidx.car.app.category.NAVIGATION"
        const val CATEGORY_POI = "androidx.car.app.category.POI"
        const val CATEGORY_IOT = "androidx.car.app.category.IOT"
        const val CATEGORY_MESSAGING = "androidx.car.app.category.MESSAGING"
        const val CATEGORY_CLUSTER = "androidx.car.app.category.FEATURE_CLUSTER"
        const val MIN_API_KEY = "androidx.car.app.minCarApiLevel"

        /** Popular map/navigation apps allowed onto the dashboard's map widget. Ask to add more. */
        val KNOWN_NAVIGATION_PACKAGES = setOf(
            "com.google.android.apps.maps", // Google Maps
            "com.waze",                     // Waze
            "sa.hudhud.maps",                // HUDHUD Maps
            "com.sygic.aura",               // Sygic
            "com.here.app.maps",            // HERE WeGo
            "net.osmand", "net.osmand.plus",// OsmAnd
            "com.tomtom.gplay.navapp",      // TomTom GO
            "ru.yandex.yandexnavi", "ru.yandex.yandexmaps", // Yandex Navigator / Maps
            "com.mapswithme.maps.pro",      // MAPS.ME
            "cz.seznam.mapy",               // Mapy.cz
            "com.citymapper.app.release",   // Citymapper
        )
    }
}

/** Finds Car App Library apps: services answering `androidx.car.app.CarAppService`. */
object CarAppCatalog {

    fun query(context: Context, iconSizePx: Int = 128): List<CarAppInfo> {
        val pm = context.packageManager
        val intent = Intent(CarAppInfo.SERVICE_ACTION)
        val flags = PackageManager.GET_META_DATA or PackageManager.GET_RESOLVED_FILTER
        val found = if (Build.VERSION.SDK_INT >= 33) pm.queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
            else @Suppress("DEPRECATION") pm.queryIntentServices(intent, flags)
        return found.mapNotNull { ri ->
            val si = ri.serviceInfo ?: return@mapNotNull null
            if (si.packageName == context.packageName) return@mapNotNull null
            val categories = buildSet {
                ri.filter?.categoriesIterator()?.forEach { add(it) }
            }
            val appInfo = si.applicationInfo
            val minApi = appInfo.metaData?.getInt(CarAppInfo.MIN_API_KEY, 0)?.takeIf { it > 0 }
                ?: si.metaData?.getInt(CarAppInfo.MIN_API_KEY, 0) ?: 0
            CarAppInfo(
                service = ComponentName(si.packageName, si.name),
                label = (si.loadLabel(pm).takeIf { it.isNotBlank() } ?: appInfo.loadLabel(pm)).toString(),
                icon = runCatching { si.loadIcon(pm).toBitmap(iconSizePx, iconSizePx) }.getOrNull(),
                categories = categories,
                minApiLevel = minApi,
            )
        }.distinctBy { it.service }
    }
}
