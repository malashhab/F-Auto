package net.prototux.aare.ui.car

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import net.prototux.aare.R
import org.mapsforge.map.reader.MapFile
import java.io.File

/**
 * Where F-Auto keeps downloaded offline map files and which are present. The maps are the same
 * mapsforge `.map` country files StradaOS downloads; they live in the app's own external files dir
 * (no storage permission needed) and are rendered by [OfflineMapView].
 */
object OfflineMaps {
    fun dir(context: Context): File = File(context.getExternalFilesDir(null), "offline-maps").apply { mkdirs() }

    /** The downloaded country maps, newest-looking first (by name). */
    fun downloaded(context: Context): List<File> =
        dir(context).listFiles { f -> f.isFile && f.name.endsWith(".map") }?.sortedBy { it.name } ?: emptyList()

    fun hasAny(context: Context): Boolean = downloaded(context).isNotEmpty()

    /** A `.map` file's coverage box (immutable; safe to keep after the file is closed). */
    fun boundingBox(file: File): org.mapsforge.core.model.BoundingBox? = runCatching {
        val mf = MapFile(file)
        try { mf.boundingBox() } finally { mf.close() }
    }.getOrNull()

    /** The centre of a `.map` file's coverage, for framing it before there's a GPS fix. */
    fun center(file: File): Pair<Double, Double>? = boundingBox(file)?.centerPoint?.let { it.latitude to it.longitude }
}

/**
 * The "Offline Maps" navigation screen: renders the first downloaded country map, following the
 * phone's GPS when there's a fix, else framed on the map's centre. With no map downloaded it points
 * the user to the download screen in settings.
 */
@Composable
fun OfflineMapScreen(modifier: Modifier) {
    val context = LocalContext.current
    val mapFile = remember { OfflineMaps.downloaded(context).firstOrNull() }
    if (mapFile == null) {
        Box(modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.offline_map_none),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    val bbox = remember(mapFile) { OfflineMaps.boundingBox(mapFile) }
    val fallback = bbox?.centerPoint?.let { it.latitude to it.longitude } ?: (0.0 to 0.0)
    val live by rememberGpsLatLon()
    // Follow GPS at street level only when it's inside this map's coverage; otherwise frame the whole
    // map at a low zoom, so a map for another region (or before a fix) shows the country, not a patch
    // of sea at street zoom.
    val inBounds = live != null && bbox?.contains(org.mapsforge.core.model.LatLong(live!!.first, live!!.second)) == true
    val center = if (inBounds) live!! else fallback
    val zoom: Byte = if (inBounds) 15 else 8
    OfflineMapView(mapFile, center.first, center.second, zoom = zoom, modifier = modifier.fillMaxSize())
}

/**
 * The phone's current position (GPS, seeded from the last known fix), or null without a fix or
 * location permission. Registers a listener while on screen and removes it on dispose.
 */
@SuppressLint("MissingPermission")
@Composable
fun rememberGpsLatLon(): State<Pair<Double, Double>?> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<Pair<Double, Double>?>(null) }
    DisposableEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (!granted || lm == null) {
            onDispose { }
        } else {
            runCatching { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }
                .getOrNull()?.let { state.value = it.latitude to it.longitude }
            val listener = LocationListener { loc -> state.value = loc.latitude to loc.longitude }
            val ok = runCatching { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 2f, listener) }.isSuccess
            onDispose { if (ok) runCatching { lm.removeUpdates(listener) } }
        }
    }
    return state
}
