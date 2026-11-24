package net.prototux.aare.host.projected

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface

/**
 * Hosts one app that renders through the private `ICarProjection` protocol (Google Maps' car UI).
 *
 * The host binds the app's exported projection service, hands it a [Surface] to draw into, and
 * drives the projection lifecycle over Binder. The app creates its own window on the supplied
 * surface, so its pixels arrive wherever the surface is shown (a `SurfaceView` on the car screen).
 *
 * The app enforces a `CAPTURE_VIDEO_OUTPUT` signature-permission check on the binding host inside
 * its own process, so on a stock phone `onSetup` is rejected. Getting past it needs root (the
 * AARE Xposed module grants that one check to AARE's uid); the protocol here is identical either
 * way. See docs/maps-projection.md.
 */
class ProjectedHost(
    context: Context,
    private val component: ComponentName,
    private val listener: Listener,
) {
    interface Listener {
        fun onStatus(text: String) {}
        fun onError(message: String) {}
    }

    private val context = context.applicationContext
    private val worker = HandlerThread("projected-host").apply { start() }
    private val handler = Handler(worker.looper)

    private var connection: ServiceConnection? = null
    private var service: IBinder? = null
    private var surface: Surface? = null
    private var width = 0
    private var height = 0
    private var densityDpi = 0
    private var motionDownTime = 0L

    /** A pending navigation destination (`google.navigation:`/`geo:` URI) delivered via onProjectionStart. */
    @Volatile private var navUri: String? = null

    /** The projected display's render size (pixels). Touches must be scaled into this space. */
    val displayWidth get() = width
    val displayHeight get() = height

    // ---- host-side Binder stubs the app calls back into ----

    /** ICarProjectionCallback: the app signals readiness and connection state here; we just ack. */
    private val callback = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            Log.i(TAG, "rx callback code=$code flags=$flags")
            reply?.writeNoException()
            return true
        }
    }

    /** ICarDisplay: returns the car UI capabilities the legacy UI reads. */
    private val carDisplay = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            Log.i(TAG, "rx carDisplay code=$code flags=$flags")
            if (code == ProjectedProtocol.TX_DISPLAY_GET_CAR_UI_INFO) {
                reply?.writeNoException()
                reply?.writeInt(1)
                reply?.let { ProjectedProtocol.writeCarUiInfo(it) }
            } else {
                reply?.writeNoException()
            }
            return true
        }
    }

    /** ICarDisplayManager: hands back the single [carDisplay]. */
    private val carDisplayManager = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            Log.i(TAG, "rx carDisplayManager code=$code flags=$flags")
            reply?.writeNoException()
            if (code == ProjectedProtocol.TX_DISPLAY_MANAGER_GET_DISPLAY) reply?.writeStrongBinder(carDisplay)
            return true
        }
    }

    /** ICar: the app reads CarInfo/CarUiInfo and the display manager before it draws. */
    private val car = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            Log.i(TAG, "rx car code=$code flags=$flags")
            when (code) {
                ProjectedProtocol.TX_CAR_GET_CAR_INFO -> {
                    reply?.writeNoException(); reply?.writeInt(1); reply?.let { ProjectedProtocol.writeCarInfo(it) }
                }
                ProjectedProtocol.TX_CAR_GET_CAR_UI_INFO -> {
                    reply?.writeNoException(); reply?.writeInt(1); reply?.let { ProjectedProtocol.writeCarUiInfo(it) }
                }
                ProjectedProtocol.TX_CAR_GET_DISPLAY_MANAGER -> {
                    reply?.writeNoException(); reply?.writeStrongBinder(carDisplayManager)
                }
                else -> reply?.writeNoException()
            }
            return true
        }
    }

    // ---- lifecycle ----

    /** Binds the service and starts projection onto [surface] once connected. */
    fun start(surface: Surface, width: Int, height: Int, densityDpi: Int) {
        this.surface = surface
        this.width = width
        this.height = height
        this.densityDpi = densityDpi
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                service = binder
                handler.post { onConnected(binder) }
            }
            override fun onServiceDisconnected(name: ComponentName) {
                service = null
                status("projection service disconnected")
            }
        }
        connection = conn
        val intent = Intent().setComponent(component)
        if (!context.bindService(intent, conn, Context.BIND_AUTO_CREATE)) {
            connection = null
            error("could not bind ${component.flattenToShortString()}")
        }
    }

    /**
     * Points the projection at [surface]. The first call binds the app (like [start]); later calls on
     * the same live host re-target the running session to the new surface/size without rebinding — so
     * a SurfaceView remount (the dashboard map card ⇄ the full-screen app, a resize, a brief recompose)
     * does NOT tear down and reload Maps. An unchanged surface+size is a no-op resume.
     */
    fun attach(surface: Surface, width: Int, height: Int, densityDpi: Int) {
        val sameGeometry = surface === this.surface && width == this.width && height == this.height
        this.surface = surface
        this.width = width
        this.height = height
        this.densityDpi = densityDpi
        val binder = service
        when {
            binder == null -> if (connection == null) start(surface, width, height, densityDpi) // else: a bind is in flight; onConnected uses the fields just set
            sameGeometry -> resume()
            else -> handler.post { if (transactProjectionStart(binder, surface)) resume() }
        }
    }

    /**
     * Pauses rendering but keeps the binding alive, so a quick [attach] resumes instantly instead of
     * rebinding. Only acts when [surface] is the one currently shown, so a late detach from an old
     * SurfaceView cannot pause a newer one that already attached.
     */
    fun detach(surface: Surface) {
        if (surface === this.surface) pause()
    }

    private fun onConnected(binder: IBinder) {
        status("bound ${component.flattenToShortString()}")
        if (!onSetup(binder)) return
        val surface = surface ?: return
        if (!transactProjectionStart(binder, surface)) return
        resume()
    }

    /** onSetup (tx 1): registers our callback and ICar binders. Rejected without the permission. */
    private fun onSetup(binder: IBinder): Boolean = transact("onSetup", binder, ProjectedProtocol.TX_ON_SETUP) { data ->
        data.writeInterfaceToken(ProjectedProtocol.PROJECTION)
        data.writeStrongBinder(callback)
        data.writeStrongBinder(car)
    }

    private fun transactProjectionStart(binder: IBinder, surface: Surface): Boolean =
        transact("onProjectionStart", binder, ProjectedProtocol.TX_ON_PROJECTION_START) { data ->
            // With a pending destination, the start intent is the navigation intent, so the car
            // activity begins turn-by-turn; otherwise it is a bare launch of the projected component.
            val intent = navUri?.let { Intent(Intent.ACTION_VIEW, Uri.parse(it)).setComponent(component) }
                ?: Intent().setComponent(component)
            val config = ProjectedProtocol.projectionConfiguration(width, height, densityDpi)
            ProjectedProtocol.writeProjectionStart(data, surface, width, height, densityDpi, intent, config)
        }

    /**
     * Starts turn-by-turn to [uri] (a `google.navigation:` or `geo:` URI) in the projected app by
     * re-sending onProjectionStart carrying that intent, so the running car activity gets the
     * destination in context (no task-to-front, unlike a plain `startActivity`). If not yet bound, the
     * destination is applied on the next start.
     */
    fun navigate(uri: String) {
        navUri = uri
        val binder = service
        val s = surface
        if (binder != null && s != null) handler.post { if (transactProjectionStart(binder, s)) resume() }
    }

    /** onProjectionResume (tx 4): the app stays stopped after onProjectionStart until this. */
    fun resume() {
        val binder = service ?: return
        handler.post {
            transact("onProjectionResume", binder, ProjectedProtocol.TX_ON_PROJECTION_RESUME) { data ->
                data.writeInterfaceToken(ProjectedProtocol.PROJECTION)
                data.writeInt(ProjectedProtocol.DISPLAY_ID)
            }
        }
    }

    fun pause() {
        val binder = service ?: return
        handler.post {
            transact("onProjectionPause", binder, ProjectedProtocol.TX_ON_PROJECTION_PAUSE) { data ->
                data.writeInterfaceToken(ProjectedProtocol.PROJECTION)
                data.writeInt(ProjectedProtocol.DISPLAY_ID)
            }
        }
    }

    // ---- input forwarding ----

    /**
     * Forwards a touch event (tx 7). Coordinates must already be in the projected display's space.
     * A private copy is taken synchronously and recycled on the worker thread after it is written,
     * so the caller keeps ownership of [event] (the framework reuses it once onTouch returns, and
     * the parcel write happens later on the worker thread).
     */
    fun sendMotion(event: MotionEvent) {
        val binder = service ?: return
        if (event.action == MotionEvent.ACTION_DOWN) motionDownTime = event.downTime
        val copy = MotionEvent.obtain(event)
        handler.post {
            try {
                transact("onMotionEvent", binder, ProjectedProtocol.TX_ON_MOTION_EVENT, oneWay = true) { data ->
                    data.writeInterfaceToken(ProjectedProtocol.PROJECTION)
                    data.writeInt(ProjectedProtocol.DISPLAY_ID)
                    data.writeInt(1)
                    copy.writeToParcel(data, 0)
                }
            } finally {
                copy.recycle()
            }
        }
    }

    /** Forwards a key event (tx 8). */
    fun sendKey(event: KeyEvent) {
        val binder = service ?: return
        handler.post {
            transact("onKeyEvent", binder, ProjectedProtocol.TX_ON_KEY_EVENT, oneWay = true) { data ->
                data.writeInterfaceToken(ProjectedProtocol.PROJECTION)
                data.writeInt(ProjectedProtocol.DISPLAY_ID)
                data.writeInt(1)
                event.writeToParcel(data, 0)
            }
        }
    }

    /** Stops projection and unbinds. Safe to call more than once. */
    fun stop() {
        val binder = service
        if (binder != null) {
            handler.post {
                transact("onProjectionStop", binder, ProjectedProtocol.TX_ON_PROJECTION_STOP, oneWay = true) { data ->
                    data.writeInterfaceToken(ProjectedProtocol.PROJECTION)
                    data.writeInt(ProjectedProtocol.DISPLAY_ID)
                }
            }
        }
        connection?.let { runCatching { context.unbindService(it) } }
        connection = null
        service = null
        surface = null
        worker.quitSafely()
    }

    // ---- helpers ----

    private inline fun transact(
        name: String,
        binder: IBinder,
        code: Int,
        oneWay: Boolean = false,
        write: (Parcel) -> Unit,
    ): Boolean {
        val data = Parcel.obtain()
        val reply = if (oneWay) null else Parcel.obtain()
        return try {
            write(data)
            binder.transact(code, data, reply, if (oneWay) IBinder.FLAG_ONEWAY else 0)
            reply?.readException()
            status("$name ok")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "$name failed", t)
            error("$name failed: ${t.message ?: t.javaClass.simpleName}")
            false
        } finally {
            data.recycle()
            reply?.recycle()
        }
    }

    private fun status(text: String) {
        Log.i(TAG, text)
        listener.onStatus(text)
    }

    private fun error(message: String) {
        Log.w(TAG, message)
        listener.onError(message)
    }

    private companion object {
        const val TAG = "ProjectedHost"
    }
}
