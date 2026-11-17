package net.prototux.aare.ui.car

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.prototux.aare.R
import net.prototux.aare.car.CarPhoto
import net.prototux.aare.Settings
import java.util.concurrent.TimeUnit

/**
 * "Strada": AARE's fourth layout, matching StradaOS's home screen — a three-card dashboard (a
 * **My vehicle** card with the vehicle photo, speed and service life; a central **map** card; and a
 * right **Audio** card with a circular progress ring) over a row of round app buttons, on a dark
 * (or light, by day) background with StradaOS's orange accent. It brings together the features
 * ported from StradaOS: the vehicle photo, the speed readout, the offline map, and the service
 * reminder. Picked by `state.shellLayout` ([net.prototux.aare.Settings.LAYOUT_STRADA]).
 */

private class StradaColors(night: Boolean) {
    val accent = Color(0xFFFF3D00)
    val accent2 = Color(0xFFFF7A2C)
    val green = Color(0xFF35C759)
    val bg = if (night) Color(0xFF0A0A0C) else Color(0xFFEDEFF3)
    val card = if (night) Color(0xFF16171B) else Color(0xFFFFFFFF)
    val inner = if (night) Color(0xFF1E1F24) else Color(0xFFF1F3F7)
    val line = if (night) Color(0x22FFFFFF) else Color(0x14000000)
    val text = if (night) Color(0xFFFFFFFF) else Color(0xFF14151A)
    val muted = if (night) Color(0xFF9A9BA1) else Color(0xFF6B6E78)
    val onAccent = Color.White
}

@Composable
internal fun StradaShell(
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    overlay: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = remember(state.night) { StradaColors(state.night) }
    val scheme = (if (state.night) darkColorScheme() else lightColorScheme()).copy(
        primary = c.accent, onPrimary = c.onAccent, surface = c.card, onSurface = c.text, background = c.bg,
    )
    net.prototux.aare.ui.theme.CarColors(scheme) {
        Box(Modifier.fillMaxSize().background(c.bg).padding(16.dp).then(modifier)) {
            Column(Modifier.fillMaxSize()) {
                StradaTopBar(state, c)
                Spacer(Modifier.height(14.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (val screen = state.screen) {
                        CarScreen.Dashboard -> StradaDashboard(state, actions, navMap, c, Modifier.fillMaxSize())
                        CarScreen.Launcher -> Launcher(state, actions, Modifier.fillMaxSize())
                        is CarScreen.App -> {
                            val app = state.app(screen.appId)
                            if (app != null) appContent(app, Modifier.fillMaxSize())
                            else StradaDashboard(state, actions, navMap, c, Modifier.fillMaxSize())
                        }
                    }
                    overlay(Modifier.align(Alignment.BottomCenter))
                    NoticeOverlay(state, actions, Modifier.align(Alignment.TopCenter))
                    CallOverlay(state, actions, Modifier.align(Alignment.TopCenter))
                }
                Spacer(Modifier.height(12.dp))
                StradaDock(state, actions, c)
            }
        }
    }
}

@Composable
private fun StradaTopBar(state: CarUiState, c: StradaColors) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Clock()
        Spacer(Modifier.width(12.dp))
        Text(dateLine(), style = MaterialTheme.typography.titleMedium, color = c.muted, maxLines = 1)
        Spacer(Modifier.weight(1f))
        WeatherChip(state.weather)
        Spacer(Modifier.width(14.dp))
        Text(greeting(), style = MaterialTheme.typography.titleMedium, color = c.text, maxLines = 1)
    }
}

@Composable
private fun StradaCard(c: StradaColors, modifier: Modifier, padding: Int = 20, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Box(modifier.clip(shape).background(c.card).let { if (padding > 0) it.padding(padding.dp) else it }) { content() }
}

// ---------------------------------------------------------------- dashboard

@Composable
private fun StradaDashboard(
    state: CarUiState,
    actions: CarActions,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    c: StradaColors,
    modifier: Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        StradaVehicleCard(state, c, Modifier.weight(1f).fillMaxHeight())
        StradaMapCard(state, actions, navMap, c, Modifier.weight(1.6f).fillMaxHeight())
        StradaAudioCard(state, actions, c, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun StradaVehicleCard(state: CarUiState, c: StradaColors, modifier: Modifier) {
    val context = LocalContext.current
    val photo = remember { CarPhoto.load(context) }
    val settings = remember { Settings(context) }
    val speed by rememberGpsSpeedKmh()
    val days = serviceDays(settings.serviceDueDate)
    StradaCard(c, modifier, padding = 18) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // The dashboard cards can be short; give the photo a fraction of the card so the title and
            // stat block always fit below it, at any screen density (a fixed weight overflowed).
            val photoH = (maxHeight * 0.40f).coerceAtLeast(60.dp)
            Column(Modifier.fillMaxSize()) {
                Text(stringResource(R.string.strada_my_vehicle), style = MaterialTheme.typography.titleLarge, color = c.text, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(photoH).clip(RoundedCornerShape(18.dp)).background(c.inner), contentAlignment = Alignment.Center) {
                    if (photo != null) Image(photo.asImageBitmap(), null, Modifier.fillMaxSize().padding(10.dp), contentScale = ContentScale.Fit)
                    else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.DirectionsCar, null, tint = c.muted, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.strada_add_photo_hint), style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 2)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.inner).padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.strada_speed).uppercase(), style = MaterialTheme.typography.labelMedium, color = c.muted, modifier = Modifier.weight(1f))
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (speed != null) c.green else c.muted))
                        Spacer(Modifier.width(5.dp))
                        Text("GPS", style = MaterialTheme.typography.labelSmall, color = c.muted, maxLines = 1)
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text((speed?.toInt() ?: 0).toString(), style = MaterialTheme.typography.headlineMedium, color = c.text, fontWeight = FontWeight.Bold, maxLines = 1)
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.strada_kmh), style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.padding(bottom = 3.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.strada_service).uppercase(), style = MaterialTheme.typography.labelMedium, color = c.muted, modifier = Modifier.weight(1f))
                        Text(days?.let { stringResource(R.string.strada_service_days, it) } ?: "—", style = MaterialTheme.typography.bodyLarge, color = c.text, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    Spacer(Modifier.height(8.dp))
                    val frac = days?.let { (it.toFloat() / 90f).coerceIn(0f, 1f) } ?: 0f
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.line)) {
                        Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(c.green))
                    }
                }
            }
        }
    }
}

@Composable
private fun StradaMapCard(
    state: CarUiState,
    actions: CarActions,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    c: StradaColors,
    modifier: Modifier,
) {
    val nav = state.navApp()
    StradaCard(c, modifier, padding = 0) {
        Box(Modifier.fillMaxSize()) {
            if (nav != null) navMap(nav, Modifier.fillMaxSize())
            else Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Map, null, tint = c.accent, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.nav_dashboard_no_app_title), style = MaterialTheme.typography.titleMedium, color = c.text)
            }
            // The nav label pill, top-left (mockup's "Offline map").
            Row(
                Modifier.align(Alignment.TopStart).padding(14.dp).clip(RoundedCornerShape(20.dp))
                    .background(c.card.copy(alpha = 0.9f)).clickable { actions.openLauncher() }.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Map, null, tint = c.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(nav?.label ?: stringResource(R.string.dash_maps), style = MaterialTheme.typography.labelLarge, color = c.text, maxLines = 1)
            }
        }
    }
}

@Composable
private fun StradaAudioCard(state: CarUiState, actions: CarActions, c: StradaColors, modifier: Modifier) {
    val np = state.nowPlaying
    val media = state.app(np?.appId ?: state.activeMediaAppId)
    StradaCard(c, modifier) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.strada_audio), style = MaterialTheme.typography.labelMedium, color = c.muted, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text((media?.label ?: stringResource(R.string.strada_bluetooth)).uppercase(), style = MaterialTheme.typography.labelMedium, color = c.accent, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            // Circular art with a progress ring.
            Box(contentAlignment = Alignment.Center) {
                val frac = if (np != null && np.durationMs > 0) (np.positionMs.toFloat() / np.durationMs).coerceIn(0f, 1f) else 0f
                Canvas(Modifier.size(150.dp)) {
                    val sw = 7.dp.toPx()
                    val inset = sw / 2
                    drawArc(c.line, -90f, 360f, false, topLeft = Offset(inset, inset), size = Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Round))
                    if (frac > 0f) drawArc(Brush.linearGradient(listOf(c.accent2, c.accent)), -90f, 360f * frac, false, topLeft = Offset(inset, inset), size = Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Round))
                }
                Box(Modifier.size(120.dp).clip(CircleShape).background(Brush.linearGradient(listOf(c.accent2, c.accent))), contentAlignment = Alignment.Center) {
                    if (np?.art != null) Image(np.art.asImageBitmap(), null, Modifier.size(120.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    else Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(52.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(np?.title ?: media?.label ?: stringResource(R.string.media_nothing_playing), style = MaterialTheme.typography.titleMedium, color = c.text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(np?.subtitle ?: "", style = MaterialTheme.typography.bodyMedium, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                StradaRound(Icons.Rounded.SkipPrevious, stringResource(R.string.media_action_previous), 54, filled = false, c) { actions.mediaPrevious() }
                StradaRound(if (np?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, stringResource(R.string.media_action_play_pause), 72, filled = true, c) { actions.mediaPlayPause() }
                StradaRound(Icons.Rounded.SkipNext, stringResource(R.string.media_action_next), 54, filled = false, c) { actions.mediaNext() }
            }
        }
    }
}

@Composable
private fun StradaRound(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, sizeDp: Int, filled: Boolean, c: StradaColors, onClick: () -> Unit) {
    Box(
        Modifier.size(sizeDp.dp).clip(CircleShape).background(if (filled) c.accent else c.inner).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = if (filled) c.onAccent else c.text, modifier = Modifier.size((sizeDp * 0.42f).dp)) }
}

// ---------------------------------------------------------------- dock (app row)

@Composable
private fun StradaDock(state: CarUiState, actions: CarActions, c: StradaColors) {
    // A representative set, plus an Apps tile for the full grid.
    val apps = state.apps.filter { it.kind != CarAppKind.EXIT }
    LazyRow(
        Modifier.fillMaxWidth().height(96.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(apps, key = { it.id }) { app ->
            Column(Modifier.width(96.dp).clickable { actions.openApp(app) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(c.inner), contentAlignment = Alignment.Center) { AppIcon(app, 40) }
                Spacer(Modifier.height(6.dp))
                Text(app.label, style = MaterialTheme.typography.labelMedium, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ---------------------------------------------------------------- helpers

private fun serviceDays(due: Long): Long? {
    if (due <= 0L) return null
    return TimeUnit.MILLISECONDS.toDays(due - System.currentTimeMillis()).coerceAtLeast(0)
}

@Composable
private fun dateLine(): String {
    // Recomputed each minute via a ticking state so it stays current.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(60_000) } }
    return android.text.format.DateFormat.format("EEEE, MMMM d", now).toString()
}
