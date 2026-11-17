package net.prototux.aare.testnav

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import androidx.car.app.AppManager
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.model.Distance
import androidx.car.app.model.Template
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.TravelEstimate
import androidx.car.app.validation.HostValidator
import androidx.core.graphics.drawable.IconCompat
import java.util.TimeZone

/** Accepts any host: this is a local test app, F-Auto (net.prototux.aare) is the host under test. */
class TestNavService : CarAppService() {
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession(): Session = object : Session() {
        override fun onCreateScreen(intent: Intent): Screen = TestNavScreen(carContext)
        override fun onCarConfigurationChanged(newConfiguration: Configuration) {
            Log.i(TAG, "car configuration changed: ${describe(newConfiguration)}")
        }
    }
}

private const val TAG = "TestNav"

private fun describe(c: Configuration) = "${c.screenWidthDp}x${c.screenHeightDp}dp ${c.densityDpi}dpi " +
    (if (c.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) "night" else "day")

/**
 * A navigation screen whose map is a diagnostic: the surface's edge in red, a 100 px grid, the
 * visible area in green (corners marked), the stable area dashed blue, and the numbers as text.
 * A host that sizes things right shows the green box hugging the space between its cards and strips.
 */
private class TestNavScreen(ctx: CarContext) : Screen(ctx), SurfaceCallback {
    private var container: SurfaceContainer? = null
    private var visible: Rect? = null
    private var stable: Rect? = null
    private var taps = 0
    private var lastClick: String = "-"

    init {
        ctx.getCarService(AppManager::class.java).setSurfaceCallback(this)
        val nav = ctx.getCarService(NavigationManager::class.java)
        nav.setNavigationManagerCallback(object : NavigationManagerCallback {
            override fun onStopNavigation() = Unit
        })
        runCatching { nav.navigationStarted() }.onFailure { Log.w(TAG, "navigationStarted", it) }
    }

    override fun onGetTemplate(): Template {
        val themed = CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_themed_star)).build()
        val plain = CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_flag)).build()
        val strip = ActionStrip.Builder()
            .addAction(Action.Builder().setIcon(themed).setOnClickListener { taps++; invalidate() }.build())
            .addAction(Action.Builder().setTitle("Taps $taps").setIcon(plain).setOnClickListener { taps++; invalidate() }.build())
            .build()
        val mapStrip = ActionStrip.Builder()
            .addAction(Action.PAN)
            .addAction(Action.Builder().setIcon(themed).setOnClickListener { taps++; invalidate() }.build())
            .addAction(Action.Builder().setIcon(plain).setOnClickListener { taps++; invalidate() }.build())
            .build()
        val step = Step.Builder("Turn right onto Test Street")
            .setManeuver(Maneuver.Builder(Maneuver.TYPE_TURN_NORMAL_RIGHT).setIcon(plain).build())
            .build()
        val routing = RoutingInfo.Builder().setCurrentStep(step, Distance.create(250.0, Distance.UNIT_METERS)).build()
        val eta = TravelEstimate.Builder(
            Distance.create(12.0, Distance.UNIT_KILOMETERS),
            DateTimeWithZone.create(System.currentTimeMillis() + 15 * 60_000, TimeZone.getDefault()),
        ).setRemainingTimeSeconds(15 * 60).build()
        return NavigationTemplate.Builder()
            .setNavigationInfo(routing)
            .setDestinationTravelEstimate(eta)
            .setActionStrip(strip)
            .setMapActionStrip(mapStrip)
            .build()
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        container = surfaceContainer
        Log.i(TAG, "surface ${surfaceContainer.width}x${surfaceContainer.height} ${surfaceContainer.dpi}dpi; " +
            "config ${describe(carContext.resources.configuration)}")
        draw()
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        container = null
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        visible = Rect(visibleArea)
        Log.i(TAG, "visible area $visibleArea")
        draw()
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        stable = Rect(stableArea)
        Log.i(TAG, "stable area $stableArea")
        draw()
    }

    override fun onClick(x: Float, y: Float) {
        lastClick = "%.0f,%.0f".format(x, y)
        draw()
    }

    private fun draw() {
        val c = container ?: return
        val surface = c.surface ?: return
        val canvas = runCatching { surface.lockCanvas(null) }.getOrNull() ?: return
        try {
            val w = c.width.toFloat()
            val h = c.height.toFloat()
            canvas.drawColor(Color.rgb(28, 32, 38))
            val grid = Paint().apply { color = Color.rgb(60, 66, 76); strokeWidth = 1f }
            var x = 0f
            while (x < w) { canvas.drawLine(x, 0f, x, h, grid); x += 100f }
            var y = 0f
            while (y < h) { canvas.drawLine(0f, y, w, y, grid); y += 100f }
            canvas.drawRect(3f, 3f, w - 3f, h - 3f, Paint().apply { color = Color.RED; style = Paint.Style.STROKE; strokeWidth = 6f })
            stable?.let { r ->
                canvas.drawRect(r, Paint().apply {
                    color = Color.rgb(66, 165, 245); style = Paint.Style.STROKE; strokeWidth = 4f
                    pathEffect = DashPathEffect(floatArrayOf(18f, 12f), 0f)
                })
            }
            visible?.let { r ->
                val green = Paint().apply { color = Color.rgb(102, 187, 106); style = Paint.Style.STROKE; strokeWidth = 4f }
                canvas.drawRect(r, green)
                val dot = Paint().apply { color = Color.rgb(102, 187, 106) }
                for ((cx, cy) in listOf(r.left to r.top, r.right to r.top, r.left to r.bottom, r.right to r.bottom)) {
                    canvas.drawCircle(cx.toFloat(), cy.toFloat(), 10f, dot)
                }
            }
            val text = Paint().apply { color = Color.WHITE; textSize = 26f; isAntiAlias = true }
            val left = ((visible?.left ?: 0) + 24).toFloat()
            var line = ((visible?.top ?: 0) + 44).toFloat()
            for (s in listOf(
                "surface ${c.width}x${c.height} @ ${c.dpi}dpi",
                "config ${describe(carContext.resources.configuration)}",
                "visible ${visible?.toShortString() ?: "-"}",
                "stable ${stable?.toShortString() ?: "-"}",
                "last map tap $lastClick",
            )) { canvas.drawText(s, left, line, text); line += 34f }
        } finally {
            surface.unlockCanvasAndPost(canvas)
        }
    }
}
