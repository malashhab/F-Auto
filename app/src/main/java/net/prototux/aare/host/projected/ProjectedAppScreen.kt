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
 * The [host] is shared and outlives this composable: the surface is attached when created and
 * detached (paused, not torn down) when destroyed, so moving the map between the dashboard card and
 * the full screen re-points the one live projection instead of rebinding Maps each time. The host is
 * fully stopped by its owner ([net.prototux.aare.host.CarRuntime]), not here.
 */
@Composable
fun ProjectedAppScreen(
    app: ProjectedAppInfo,
    densityDpi: Int,
    host: ProjectedHost,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    // Attach only once a valid, non-zero size is known: attaching at 0×0 (a possible
                    // surfaceCreated size, depending on layout timing) gives a blank projected map.
                    // attach() re-points a running session to the new surface/size (cheap, no rebind).
                    private fun attachIfReady(w: Int, h: Int) {
                        if (w > 0 && h > 0) host.attach(holder.surface, w, h, densityDpi)
                    }

                    override fun surfaceCreated(holder: SurfaceHolder) = attachIfReady(width, height)

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = attachIfReady(width, height)

                    // Pause this surface (not a full stop): a remount re-attaches the same session.
                    override fun surfaceDestroyed(holder: SurfaceHolder) = host.detach(holder.surface)
                })
                // Forward touches to the projected app, scaled from the view's live size into the
                // projected display's pixel space. They match 1:1 only while the view is exactly the
                // display's size; after any resize (a sub-card map, a stale initial size, rotation)
                // the SurfaceView scales the app's buffer to fit, so a raw tap lands offset unless we
                // scale it the same way. A 1:1 case skips the copy.
                setOnTouchListener { v: View, event ->
                    val dw = host.displayWidth; val dh = host.displayHeight
                    val vw = v.width; val vh = v.height
                    if (dw > 0 && dh > 0 && vw > 0 && vh > 0 && (dw != vw || dh != vh)) {
                        val scaled = MotionEvent.obtain(event)
                        scaled.transform(Matrix().apply { setScale(dw.toFloat() / vw, dh.toFloat() / vh) })
                        host.sendMotion(scaled)
                        scaled.recycle()
                    } else {
                        host.sendMotion(event)
                    }
                    true
                }
            }
        },
    )

    // Nothing to clean up here directly; the SurfaceHolder callback owns the host's lifetime.
    DisposableEffect(app.service) { onDispose { } }
}
