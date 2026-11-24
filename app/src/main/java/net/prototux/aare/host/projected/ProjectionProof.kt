package net.prototux.aare.host.projected

import android.content.ComponentName
import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface

/**
 * Debug-only smoke test for the no-root Maps projection patch.
 *
 * Binds a projection-capable Maps build and drives the real [ProjectedHost] handshake against it.
 * The patched Maps (`com.fauto1.android.apps.maps`) forces its in-process `CAPTURE_VIDEO_OUTPUT`
 * guard to "granted", so a non-Google-signed host like AARE must now get past `onSetup` instead of
 * being rejected with a SecurityException.
 *
 * Triggered from [net.prototux.aare.ui.MainActivity.applyDebugExtras] via:
 *   adb shell am start -n app.fauto.car/net.prototux.aare.ui.MainActivity --ez project_maps true
 * optionally with  --es project_pkg <package>  to target a different Maps package.
 *
 * Watch the result with:
 *   adb logcat -s ProjectedHost:*
 *   PASS  -> "onSetup ok"                (the permission wall is beaten)
 *   FAIL  -> "onSetup failed: ... SecurityException ... does not have permission"
 *
 * Uses an offscreen [SurfaceTexture]-backed [Surface] as the render target; the pixels go nowhere,
 * which is fine for proving the bind/handshake. A static reference keeps the host alive past this
 * call so its worker thread can run the Binder transactions.
 */
object ProjectionProof {
    private const val TAG = "ProjectedHost"
    private const val SERVICE = "com.google.android.apps.gmm.car.GmmCarProjectionService"
    private const val DEFAULT_PKG = "com.fauto1.android.apps.maps"

    // Keep the host and its surface from being collected while the async handshake runs.
    @Volatile private var host: ProjectedHost? = null
    @Volatile private var surface: Surface? = null

    fun run(context: Context, pkg: String = DEFAULT_PKG) {
        val component = ComponentName(pkg, SERVICE)
        val dpi = context.resources.displayMetrics.densityDpi
        Log.i(TAG, "PROOF: start — binding ${component.flattenToShortString()} (dpi=$dpi)")

        val texture = SurfaceTexture(false).apply { setDefaultBufferSize(1280, 720) }
        val surf = Surface(texture)
        surface = surf

        val h = ProjectedHost(context, component, object : ProjectedHost.Listener {
            override fun onStatus(text: String) { Log.i(TAG, "PROOF status: $text") }
            override fun onError(message: String) { Log.w(TAG, "PROOF error: $message") }
        })
        host = h
        h.start(surf, 1280, 720, dpi)
    }
}
