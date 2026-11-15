package net.prototux.aare.ui.car

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import java.io.File

/**
 * The offline OpenStreetMap view: renders a downloaded mapsforge `.map` country file with the
 * maintained mapsforge library (the same `.map` files StradaOS downloads from mapsforge.org). Shown
 * as F-Auto's "StradaOS Offline Maps" navigation option, chosen alongside HudHud / projected Maps.
 *
 * Offline and self-contained: no tiles are fetched from the network; everything is drawn from the
 * local `.map`. [mapFile] is the country map to show; [centerLat]/[centerLon]/[zoom] set the camera.
 */
@Composable
fun OfflineMapView(
    mapFile: File,
    centerLat: Double,
    centerLon: Double,
    zoom: Byte = 15,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ensureGraphicFactory(context)
            MapView(context).apply {
                isClickable = true
                mapScaleBar.isVisible = false
                setBuiltInZoomControls(false)
                val tileCache = AndroidUtil.createTileCache(
                    context, "aare-offline-map",
                    model.displayModel.tileSize, 1f,
                    model.frameBufferModel.overdrawFactor,
                )
                val layer = TileRendererLayer(tileCache, MapFile(mapFile), model.mapViewPosition, AndroidGraphicFactory.INSTANCE).apply {
                    setXmlRenderTheme(MapsforgeThemes.DEFAULT)
                }
                layerManager.layers.add(layer)
                setZoomLevel(zoom)
                setCenter(LatLong(centerLat, centerLon))
            }
        },
        update = { view -> view.setCenter(LatLong(centerLat, centerLon)) },
        onReset = {},
        onRelease = { view -> view.destroyAll() },
    )
    DisposableEffect(Unit) { onDispose { } }
}

@Volatile private var graphicFactoryReady = false

/** mapsforge needs its Android graphics backend created once per process before any map view. */
private fun ensureGraphicFactory(context: Context) {
    if (graphicFactoryReady) return
    synchronized(OfflineMapGuard) {
        if (!graphicFactoryReady) {
            AndroidGraphicFactory.createInstance(context.applicationContext as Application)
            graphicFactoryReady = true
        }
    }
}

private object OfflineMapGuard
