package net.prototux.aare.host.projected

import android.graphics.Matrix
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Shows a projected app on the car screen. A [SurfaceView] provides the render target; its surface
 * is handed to a [ProjectedHost], which binds the app and asks it to draw there. Touch on the view
 * is forwarded to the app scaled into the projected display's pixel space (the display is created at
 * one size; if the view is later a different size the surface buffer is scaled to fit, so taps are
 * scaled the same way to stay aligned).
 *
 * The host lives for as long as this composable is on screen: it starts when the surface is created
 * and stops when the surface is destroyed or the composable leaves.
 */
@Composable
fun ProjectedAppScreen(
    app: ProjectedAppInfo,
    densityDpi: Int,
    hostFactory: (ProjectedAppInfo) -> ProjectedHost,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            SurfaceView(context).apply {
                var host: ProjectedHost? = null
                holder.addCallback(object : SurfaceHolder.Callback {
                    // Start only once a valid, non-zero size is known: starting at 0×0 (a possible
                    // surfaceCreated size, depending on layout timing) gives a blank projected map.
                    // surfaceChanged never restarts a running host — re-initialising Maps' projection
                    // renderer mid-session is what the touch scaling exists to avoid.
                    private fun startIfNeeded(w: Int, h: Int) {
                        if (host == null && w > 0 && h > 0) {
                            host = hostFactory(app).also { it.start(holder.surface, w, h, densityDpi) }
                        }
                    }

                    override fun surfaceCreated(holder: SurfaceHolder) = startIfNeeded(width, height)

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = startIfNeeded(width, height)

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        host?.stop()
                        host = null
                    }
                })
                // Forward touches to the projected app, scaled from the view's live size into the
                // projected display's pixel space. They match 1:1 only while the view is exactly the
                // display's size; after any resize (a sub-card map, a stale initial size, rotation)
                // the SurfaceView scales the app's buffer to fit, so a raw tap lands offset unless we
                // scale it the same way. A 1:1 case skips the copy.
                setOnTouchListener { v: View, event ->
                    val h = host
                    if (h != null) {
                        val dw = h.displayWidth; val dh = h.displayHeight
                        val vw = v.width; val vh = v.height
                        if (dw > 0 && dh > 0 && vw > 0 && vh > 0 && (dw != vw || dh != vh)) {
                            val scaled = MotionEvent.obtain(event)
                            scaled.transform(Matrix().apply { setScale(dw.toFloat() / vw, dh.toFloat() / vh) })
                            h.sendMotion(scaled)
                            scaled.recycle()
                        } else {
                            h.sendMotion(event)
                        }
                    }
                    true
                }
            }
        },
    )

    // Nothing to clean up here directly; the SurfaceHolder callback owns the host's lifetime.
    DisposableEffect(app.service) { onDispose { } }
}
