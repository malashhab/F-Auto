package net.prototux.aare.ui.car

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * An analog speedometer, ported from StradaOS's `Gauge.java` (GPL-3.0) to Compose: an open ring,
 * numbered ticks, an accent fill that grows with speed, and a needle, with the current speed and
 * unit in the centre. The needle animates smoothly toward each new reading (GPS updates once a
 * second) instead of jumping. `speedKmh == null` means "no fix": the needle rests at zero and the
 * centre shows "--".
 *
 * Pure: it only draws [speedKmh]. The caller picks the source — the car's own speed over AAP
 * ([CarData.state]) when connected, or the phone's GPS ([rememberGpsSpeedKmh]) otherwise.
 */
@Composable
fun SpeedGauge(
    speedKmh: Float?,
    accent: Color,
    ring: Color,
    soft: Color,
    modifier: Modifier = Modifier,
    unitMph: Boolean = false,
    showNumber: Boolean = true,
) {
    val max = if (unitMph) 120f else 200f
    val step = 20f
    val unit = if (unitMph) "mph" else "km/h"
    val hasFix = speedKmh != null
    val value = if (unitMph) (speedKmh ?: 0f) * 0.621371f else (speedKmh ?: 0f)
    val target = value.coerceIn(0f, max * 1.05f)
    // Frame-independent smoothing, like Gauge.java's exponential approach.
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = 1f, stiffness = Spring.StiffnessLow),
        label = "speed",
    )

    // Text paints kept across recompositions (they sit in an always-visible widget).
    val numPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER } }
    val bigPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD } }
    val unitPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER } }

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val r = min(w, h) / 2f * 0.96f
        val cx = w / 2f
        val cy = h / 2f + r * 0.04f
        val frac = (animated / max).coerceIn(0f, 1f)
        val sweepNow = SWEEP * frac

        fun arcRect(radius: Float) = Rect(Offset(cx - radius, cy - radius), Offset(cx + radius, cy + radius))

        // Outer ring (open at the bottom).
        drawArc(
            color = ring, startAngle = START - 22f, sweepAngle = SWEEP + 44f, useCenter = false,
            topLeft = arcRect(r).topLeft, size = arcRect(r).size,
            style = Stroke(width = max(1.5f, r * 0.03f), cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )

        // Fill: a faint full track, then an accent sweep up to the needle.
        val rb = r * 0.77f
        val bw = r * 0.30f
        drawArc(
            color = ring.copy(alpha = 0.06f), startAngle = START, sweepAngle = SWEEP, useCenter = false,
            topLeft = arcRect(rb).topLeft, size = arcRect(rb).size, style = Stroke(width = bw),
        )
        if (sweepNow > 0.6f) {
            val f0 = START / 360f
            val f1 = (START + sweepNow) / 360f
            val brush = Brush.sweepGradient(
                0f to Color.Transparent,
                f0 to accent.copy(alpha = 0.06f),
                (f0 + (f1 - f0) * 0.55f) to accent.copy(alpha = 0.5f),
                f1 to accent.copy(alpha = 0.95f),
                1f to accent.copy(alpha = 0.95f),
                center = Offset(cx, cy),
            )
            drawArc(
                brush = brush, startAngle = START, sweepAngle = sweepNow, useCenter = false,
                topLeft = arcRect(rb).topLeft, size = arcRect(rb).size, style = Stroke(width = bw),
            )
            val re = r * 0.925f
            drawArc(
                color = accent, startAngle = START, sweepAngle = sweepNow, useCenter = false,
                topLeft = arcRect(re).topLeft, size = arcRect(re).size, style = Stroke(width = max(1.5f, r * 0.022f)),
            )
        }

        // Ticks + number labels.
        numPaint.color = soft.toArgb()
        numPaint.textSize = r * 0.11f
        val n = (max / (step / 2f)).roundToInt()
        for (i in 0..n) {
            val v = i * step / 2f
            val major = i % 2 == 0
            val a = Math.toRadians((START + SWEEP * v / max).toDouble())
            val cs = cos(a).toFloat()
            val sn = sin(a).toFloat()
            val r1 = r * (if (major) 0.80f else 0.86f)
            val r2 = r * 0.92f
            drawLine(
                color = ring.copy(alpha = if (major) 0.69f else 0.33f),
                start = Offset(cx + cs * r1, cy + sn * r1), end = Offset(cx + cs * r2, cy + sn * r2),
                strokeWidth = max(1f, r * (if (major) 0.018f else 0.010f)),
            )
            if (major && (v % step == 0f || v == max)) {
                val rl = r * 0.64f
                val ty = cy + sn * rl - (numPaint.ascent() + numPaint.descent()) / 2f
                drawIntoCanvas { it.nativeCanvas.drawText(v.roundToInt().toString(), cx + cs * rl, ty, numPaint) }
            }
        }

        // Needle, with a soft glow under it.
        val na = Math.toRadians((START + sweepNow).toDouble())
        val ncs = cos(na).toFloat()
        val nsn = sin(na).toFloat()
        val n1 = if (showNumber) r * 0.50f else r * 0.06f
        val n2 = r * 0.97f
        drawLine(
            color = accent.copy(alpha = 0.4f),
            start = Offset(cx + ncs * n1, cy + nsn * n1), end = Offset(cx + ncs * n2, cy + nsn * n2),
            strokeWidth = r * 0.08f, cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawLine(
            color = ring,
            start = Offset(cx + ncs * n1, cy + nsn * n1), end = Offset(cx + ncs * n2, cy + nsn * n2),
            strokeWidth = max(2f, r * 0.028f), cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )

        // Centre: big number + unit.
        unitPaint.color = accent.toArgb()
        unitPaint.textSize = r * 0.12f
        if (showNumber) {
            bigPaint.color = ring.toArgb()
            bigPaint.textSize = r * 0.40f
            val s = if (hasFix) (if (animated < 1.5f && target == 0f) 0 else animated.roundToInt()).toString() else "--"
            drawIntoCanvas {
                it.nativeCanvas.drawText(s, cx, cy + r * 0.14f, bigPaint)
                it.nativeCanvas.drawText(unit, cx, cy - r * 0.26f, unitPaint)
            }
        } else {
            drawIntoCanvas { it.nativeCanvas.drawText(unit, cx, cy + r * 0.30f, unitPaint) }
        }
    }
}

private const val START = 150f
private const val SWEEP = 240f

/** DrawScope.drawIntoCanvas shortcut used above. */
private inline fun DrawScope.drawIntoCanvas(block: (androidx.compose.ui.graphics.Canvas) -> Unit) =
    drawContext.canvas.let(block)

/**
 * The phone's GPS speed in km/h, or null when there is no fix or no location permission. Registers
 * a GPS listener while on screen and removes it on dispose. Lets the gauge work on the phone preview
 * and in any car, not only when the head unit reports speed.
 */
@SuppressLint("MissingPermission")
@Composable
fun rememberGpsSpeedKmh(): State<Float?> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<Float?>(null) }
    DisposableEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (!granted || lm == null) {
            onDispose { }
        } else {
            val listener = LocationListener { loc: Location -> if (loc.hasSpeed()) state.value = loc.speed * 3.6f }
            val ok = runCatching {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener)
            }.isSuccess
            onDispose { if (ok) runCatching { lm.removeUpdates(listener) } }
        }
    }
    return state
}
