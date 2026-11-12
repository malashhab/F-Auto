package net.prototux.aare.launch

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Build
import android.util.Log
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.system.exitProcess

/**
 * Puts an ordinary phone app on the car screen, from a process with adb-level (Shizuku) or root
 * rights. It creates a virtual display that renders into the car screen's [Surface], launches the
 * chosen app there, and injects the car's touches and keys into that display.
 *
 * DEVICE-TESTED, NOT verified here: creating a display an arbitrary app can run on
 * (VIRTUAL_DISPLAY_FLAG_TRUSTED and friends) and injecting input into it use hidden framework APIs
 * whose exact shape and permission requirements vary by Android version and by whether this runs as
 * root (su) or shell (Shizuku). Everything is wrapped so a failure logs and degrades rather than
 * crashing, but the privileged path WILL need on-device iteration. This is the scrcpy approach,
 * adapted to feed AARE's car screen.
 */
class LaunchHostService : ILaunchHost.Stub {
    private val injectedContext: Context?
    @Volatile private var display: VirtualDisplay? = null
    @Volatile private var displayId: Int = -1

    constructor() : this(null)

    /** Shizuku passes a Context when the service has this constructor; the su helper passes null. */
    constructor(context: Context?) {
        this.injectedContext = context
        exemptHiddenApis()
    }

    override fun start(surface: Surface, width: Int, height: Int, dpi: Int, packageName: String): Int {
        stop()
        val ctx = ownerContext() ?: return -1
        val vd = createDisplay(ctx, surface, width, height, dpi) ?: return -1
        display = vd
        displayId = vd.display.displayId
        if (!launch(ctx, packageName, displayId)) {
            Log.w(TAG, "created display ${displayId} but could not launch $packageName on it")
        }
        return displayId
    }

    override fun resize(surface: Surface, width: Int, height: Int, dpi: Int) {
        val vd = display ?: return
        runCatching { vd.resize(width, height, dpi) }
        runCatching { vd.surface = surface }
    }

    override fun touch(downTime: Long, eventTime: Long, action: Int, x: Float, y: Float, pointerId: Int) {
        val ev = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
        ev.source = SOURCE_TOUCHSCREEN
        setDisplayId(ev, displayId)
        inject(ev)
        ev.recycle()
    }

    /** A complete touch event from the car screen, pointers and all: pinch-zoom on a map needs two. */
    override fun motion(event: MotionEvent?) {
        val src = event ?: return
        val ev = MotionEvent.obtain(src)
        ev.source = SOURCE_TOUCHSCREEN
        setDisplayId(ev, displayId)
        inject(ev)
        ev.recycle()
    }

    /** Opens [uri] with [pkg] on the app's display (a geo: link starts directions in a maps app). */
    override fun view(uri: String?, pkg: String?): Boolean {
        if (uri.isNullOrBlank() || displayId < 0) return false
        val ctx = ownerContext() ?: return false
        val viaAm = if (pkg.isNullOrBlank()) amStart(displayId, "-a", Intent.ACTION_VIEW, "-d", uri)
            else amStart(displayId, "-a", Intent.ACTION_VIEW, "-d", uri, "-p", pkg)
        if (viaAm) return true
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (!pkg.isNullOrBlank()) intent.setPackage(pkg)
            ctx.startActivity(intent, ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle())
            true
        }.onFailure { Log.w(TAG, "view($uri) failed: ${it.message}") }.getOrDefault(false)
    }

    override fun key(eventTime: Long, action: Int, keyCode: Int) {
        val ev = KeyEvent(eventTime, eventTime, action, keyCode, 0, 0, -1, 0, 0, SOURCE_KEYBOARD)
        setDisplayId(ev, displayId)
        inject(ev)
    }

    override fun stop() {
        display?.let { runCatching { it.release() } }
        display = null
        displayId = -1
    }

    override fun destroy() {
        stop()
        exitProcess(0)
    }

    // ---------------------------------------------------------------- privileged plumbing (reflection)

    /**
     * A context whose package this process's uid actually owns. DisplayManager rejects a virtual
     * display whose owner package doesn't belong to the calling uid ("packageName must match the
     * owner uid"), and Shizuku hands this service AARE's own context while running it as shell
     * (uid 2000), so under Shizuku the shell package's context is used instead. Root (uid 0) and
     * the su helper keep the system context.
     */
    private val ownerCtx: Context? by lazy {
        val base = injectedContext ?: systemContext()
        val uid = android.os.Process.myUid()
        if (uid == SHELL_UID && base != null) {
            runCatching { base.createPackageContext(SHELL_PACKAGE, 0) }
                .onFailure { Log.w(TAG, "no shell package context: ${it.message}") }
                .getOrNull() ?: base
        } else base
    }

    private fun ownerContext(): Context? = ownerCtx

    /** Build the virtual display. Trusted + its own display group lets an arbitrary app run and focus on it. */
    private fun createDisplay(ctx: Context, surface: Surface, w: Int, h: Int, dpi: Int): VirtualDisplay? {
        val dm = ctx.getSystemService(DisplayManager::class.java) ?: return null
        // Preferred flags for hosting a foreign app interactively; fall back to the plain public ones
        // if the framework rejects the hidden bits for this uid/version.
        for (flags in intArrayOf(FULL_FLAGS, FLAG_PUBLIC or FLAG_PRESENTATION)) {
            val vd = runCatching { dm.createVirtualDisplay("AARE app", w, h, dpi.coerceAtLeast(120), surface, flags) }
                .onFailure { Log.w(TAG, "createVirtualDisplay(flags=$flags) failed: ${it.message}") }
                .getOrNull()
            if (vd != null) return vd
        }
        return null
    }

    /** Launch [pkg]'s main activity onto [onDisplay]. */
    @SuppressLint("QueryPermissionsNeeded")
    private fun launch(ctx: Context, pkg: String, onDisplay: Int): Boolean = runCatching {
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) ?: return false
        val component = intent.component?.flattenToShortString()
        if (component != null && amStart(onDisplay, "-n", component, "--activity-clear-task")) return true
        val opts = ActivityOptions.makeBasic().setLaunchDisplayId(onDisplay)
        ctx.startActivity(intent, opts.toBundle())
        true
    }.onFailure { Log.w(TAG, "startActivity($pkg) failed: ${it.message}") }.getOrDefault(false)

    /**
     * `am start --display N …` as a child process. Under Shizuku this process is the shell uid but
     * every Context it can get carries another package's name (AARE's from Shizuku, "android" from
     * the system context), which ActivityTaskManager rejects ("package does not belong to uid").
     * The `am` command is what `adb shell` itself uses, so it carries the right identity.
     */
    private fun amStart(onDisplay: Int, vararg args: String): Boolean = runCatching {
        val cmd = listOf("am", "start", "--display", onDisplay.toString(), "--user", "0") + args
        val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().use { it.readText() }.trim()
        val code = p.waitFor()
        val ok = code == 0 && !out.contains("Error") && !out.contains("Exception")
        if (ok) Log.i(TAG, "am start ok: $out") else Log.w(TAG, "am start failed ($code): $out")
        ok
    }.onFailure { Log.w(TAG, "am start could not run: ${it.message}") }.getOrDefault(false)

    /** Inject one event into the launched app's display via InputManager's hidden injectInputEvent. */
    private fun inject(event: InputEvent) {
        runCatching {
            val im = inputManager ?: return
            val method = im.javaClass.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
            method.invoke(im, event, INJECT_INPUT_EVENT_MODE_ASYNC)
        }.onFailure { Log.w(TAG, "injectInputEvent failed: ${it.message}") }
    }

    /**
     * The InputManager that exposes the hidden injectInputEvent: the system service on Android 14+
     * (where the static getInstance() is gone), getInstance() on older versions.
     */
    private val inputManager: Any? by lazy {
        val fromService = runCatching {
            ownerContext()?.getSystemService(android.hardware.input.InputManager::class.java)
        }.getOrNull()
        fromService ?: runCatching {
            val cls = Class.forName("android.hardware.input.InputManager")
            cls.getMethod("getInstance").invoke(null)
        }.onFailure { Log.w(TAG, "no InputManager: ${it.message}") }.getOrNull()
    }

    private fun setDisplayId(event: InputEvent, id: Int) {
        if (id < 0) return
        runCatching { event.javaClass.getMethod("setDisplayId", Int::class.javaPrimitiveType).invoke(event, id) }
    }

    /** The system context of this (app_process) process, when Shizuku did not inject one. */
    @SuppressLint("PrivateApi")
    private fun systemContext(): Context? = runCatching {
        val at = Class.forName("android.app.ActivityThread")
        val thread = at.getMethod("currentActivityThread").invoke(null) ?: at.getMethod("systemMain").invoke(null)
        at.getMethod("getSystemContext").invoke(thread) as Context
    }.onFailure { Log.w(TAG, "no system context: ${it.message}") }.getOrNull()

    private fun exemptHiddenApis() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        runCatching {
            HiddenApiBypass.addHiddenApiExemptions(
                "Landroid/app/ActivityThread;", "Landroid/hardware/input/InputManager;",
                "Landroid/hardware/display/DisplayManager;", "Landroid/view/MotionEvent;", "Landroid/view/KeyEvent;",
            )
        }
    }

    private companion object {
        const val TAG = "AareLaunchHost"
        const val SOURCE_TOUCHSCREEN = 0x00001002 // InputDevice.SOURCE_TOUCHSCREEN
        const val SOURCE_KEYBOARD = 0x00000101 // InputDevice.SOURCE_KEYBOARD
        const val INJECT_INPUT_EVENT_MODE_ASYNC = 0
        const val SHELL_UID = 2000
        const val SHELL_PACKAGE = "com.android.shell"

        // VirtualDisplay flags (public + hidden bits). Hidden ones let a foreign app run and take focus.
        const val FLAG_PUBLIC = 1 shl 0
        const val FLAG_PRESENTATION = 1 shl 1
        const val FLAG_TRUSTED = 1 shl 10 // hidden, API 29+
        const val FLAG_OWN_DISPLAY_GROUP = 1 shl 11 // hidden, API 31+
        const val FLAG_ALWAYS_UNLOCKED = 1 shl 12 // hidden, API 30+
        const val FULL_FLAGS = FLAG_PUBLIC or FLAG_PRESENTATION or FLAG_TRUSTED or FLAG_OWN_DISPLAY_GROUP or FLAG_ALWAYS_UNLOCKED
    }
}
