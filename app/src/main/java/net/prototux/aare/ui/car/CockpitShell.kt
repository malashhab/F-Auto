package net.prototux.aare.ui.car

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.prototux.aare.R
import net.prototux.aare.ui.theme.LocalCarDimens

/**
 * "Cockpit": a collapsible-dock layout matching `CarUI` Frames A/B. A slim left **dock** (brand,
 * clock, Home / Nav / Media / Phone / Apps, a collapse chevron) sits beside a two-pane dashboard —
 * a big live **map** card and a scaled **now-playing** card. Collapsing the dock to an 8px edge
 * handle lets the two cards grow edge-to-edge (better on small head units). Full-screen apps and the
 * app grid come from [appContent]/[navMap] like the other shells.
 *
 * This is a phone-projection head unit, so the mockups' EV/telemetry chrome (battery %, 5G, charging,
 * EV app, speed-limit/lane HUD) is intentionally dropped — that data isn't available over projection.
 * Its own fixed dark look uses the [Co] palette, independent of `Settings.carStyle`.
 */
private object Co {
    val base = Color(0xFF10131A)
    val text = Color.White
    val muted = Color.White.copy(alpha = 0.70f)
    val card = Color(0xFF161922)
    val dock = Color(0xFF131620)
    val glassLine = Color.White.copy(alpha = 0.12f)
    val tile = Color.White.copy(alpha = 0.06f)
    val accent = Color(0xFFADC6FF)
    val cyan = Color(0xFF38BDF8)
    val success = Color(0xFF53E16F)
    val onAccent = Color(0xFF0B1220)
}

/** The ambient cockpit backdrop: near-black with soft cyan/indigo glows, like the mockup. */
private fun Modifier.cockpitBackground() = this
    .background(Co.base)
    .drawBehind {
        drawRect(Brush.radialGradient(
            listOf(Co.cyan.copy(alpha = 0.10f), Color.Transparent),
            center = Offset(size.width * 0.20f, size.height * 0.15f), radius = size.maxDimension * 0.55f))
        drawRect(Brush.radialGradient(
            listOf(Color(0xFF8B5CF6).copy(alpha = 0.14f), Color.Transparent),
            center = Offset(size.width * 0.85f, size.height * 0.85f), radius = size.maxDimension * 0.6f))
    }

@Composable
internal fun CockpitShell(
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    overlay: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dockOpen by remember { mutableStateOf(true) }
    // A dark Material scheme so stray MaterialTheme children (overlays, notices) read on the backdrop.
    MaterialTheme(colorScheme = colorSchemeFor(paletteFor(net.prototux.aare.Settings.STYLE_MIDNIGHT))) {
        CompositionLocalProvider(LocalCarDimens provides LocalCarDimens.current.copy(cardRadius = 28.dp, gutter = 14.dp)) {
            Box(Modifier.fillMaxSize().cockpitBackground().padding(14.dp).then(modifier)) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (dockOpen) CockpitDock(state, actions) { dockOpen = false }
                    else CockpitEdgeHandle { dockOpen = true }

                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        when (val screen = state.screen) {
                            CarScreen.Dashboard -> CockpitDashboard(state, actions, navMap, Modifier.fillMaxSize())
                            CarScreen.Launcher -> CockpitLauncher(state, actions, Modifier.fillMaxSize())
                            is CarScreen.App -> {
                                val app = state.app(screen.appId)
                                if (app != null) appContent(app, Modifier.fillMaxSize())
                                else CockpitDashboard(state, actions, navMap, Modifier.fillMaxSize())
                            }
                        }
                        overlay(Modifier.align(Alignment.BottomCenter))
                        NoticeOverlay(state, actions, Modifier.align(Alignment.TopCenter))
                        CallOverlay(state, actions, Modifier.align(Alignment.TopCenter))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- dock

@Composable
private fun CockpitDock(state: CarUiState, actions: CarActions, onCollapse: () -> Unit) {
    val onDashboard = state.screen == CarScreen.Dashboard
    val onLauncher = state.screen == CarScreen.Launcher
    Column(
        Modifier.fillMaxHeight().width(104.dp).clip(RoundedCornerShape(28.dp))
            .background(Co.dock).border(1.dp, Co.glassLine, RoundedCornerShape(28.dp))
            .padding(vertical = 18.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Brand badge + clock.
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Co.cyan, Color(0xFF4F46E5)))),
            contentAlignment = Alignment.Center,
        ) { Text("F", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
        Clock()

        Spacer(Modifier.weight(1f))
        CockpitNav(Icons.Rounded.GridView, stringResource(R.string.dash_menu_home), active = onDashboard) { actions.goHome() }
        CockpitNav(Icons.Rounded.Navigation, stringResource(R.string.dash_nav), active = false) {
            state.navApp()?.let { actions.openApp(it) }
        }
        CockpitNav(Icons.Rounded.MusicNote, stringResource(R.string.dash_media), active = false) {
            state.app(state.activeMediaAppId)?.let { actions.openApp(it) } ?: actions.openLauncher()
        }
        CockpitNav(Icons.Rounded.Call, stringResource(R.string.common_call), active = false) {
            state.apps.firstOrNull { it.kind == CarAppKind.PHONE }?.let { actions.openApp(it) }
        }
        CockpitNav(Icons.Rounded.Apps, stringResource(R.string.dash_menu_apps), active = onLauncher) { actions.openLauncher() }
        Spacer(Modifier.weight(1f))

        // Collapse the dock to the edge handle.
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(Co.tile).clickable(onClick = onCollapse),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.ChevronLeft, stringResource(R.string.common_dismiss), tint = Co.muted, modifier = Modifier.size(24.dp)) }
    }
}

@Composable
private fun CockpitNav(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(62.dp).clip(CircleShape)
            .background(if (active) Co.accent else Co.tile)
            .border(1.dp, if (active) Color.Transparent else Co.glassLine, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = if (active) Co.onAccent else Co.text, modifier = Modifier.size(28.dp)) }
}

/** The collapsed 8px edge handle with a soft cyan indicator; tap to bring the dock back. */
@Composable
private fun CockpitEdgeHandle(onExpand: () -> Unit) {
    Box(
        Modifier.width(12.dp).fillMaxHeight().clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0B0E14)).clickable(onClick = onExpand),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(5.dp).height(120.dp).clip(RoundedCornerShape(3.dp))
            .background(Brush.verticalGradient(listOf(Co.cyan, Co.accent))))
    }
}

// ---------------------------------------------------------------- dashboard

@Composable
private fun CockpitDashboard(
    state: CarUiState,
    actions: CarActions,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    modifier: Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        // Live map (projected Maps / offline map / phone guidance), in a glass frame.
        CoCard(Modifier.weight(1.6f).fillMaxHeight()) {
            val nav = state.app(state.activeNavAppId)
            val guidance = state.phoneGuidance
            val hosted = nav != null && (nav.kind == CarAppKind.NAVIGATION || nav.kind == CarAppKind.PROJECTED || nav.kind == CarAppKind.OFFLINE_MAP)
            if (nav != null && (hosted || guidance == null)) navMap(nav, Modifier.fillMaxSize())
            else NavCard(state, actions, navMap, Modifier.fillMaxSize())
        }
        CockpitNowPlaying(state, actions, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun CockpitNowPlaying(state: CarUiState, actions: CarActions, modifier: Modifier) {
    val np = state.nowPlaying
    val media = state.app(np?.appId ?: state.activeMediaAppId)
    CoCard(modifier) {
        Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.dash_now_playing), style = MaterialTheme.typography.labelLarge, color = Co.muted, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            // Album art (or a neon gradient placeholder).
            val artShape = RoundedCornerShape(26.dp)
            if (np?.art != null) {
                Image(np.art.asImageBitmap(), null, Modifier.size(176.dp).clip(artShape), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.size(176.dp).clip(artShape).background(Brush.linearGradient(listOf(Color(0xFFF97316), Color(0xFFEC4899), Color(0xFF8B5CF6)))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(64.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(np?.title ?: media?.label ?: stringResource(R.string.media_nothing_playing), style = MaterialTheme.typography.headlineSmall, color = Co.text, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(np?.subtitle ?: "", style = MaterialTheme.typography.bodyLarge, color = Co.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp))
            if (np != null && np.durationMs > 0) {
                LinearProgressIndicator(
                    progress = { (np.positionMs.toFloat() / np.durationMs).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Co.accent, trackColor = Co.tile,
                )
                Spacer(Modifier.height(16.dp))
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                CoRound(Icons.Rounded.SkipPrevious, stringResource(R.string.media_action_previous), 56, filled = false) { actions.mediaPrevious() }
                CoRound(if (np?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, stringResource(R.string.media_action_play_pause), 72, filled = true) { actions.mediaPlayPause() }
                CoRound(Icons.Rounded.SkipNext, stringResource(R.string.media_action_next), 56, filled = false) { actions.mediaNext() }
            }
        }
    }
}

// ---------------------------------------------------------------- launcher

/** A CarPlay-style grid of squircle app tiles. */
@Composable
private fun CockpitLauncher(state: CarUiState, actions: CarActions, modifier: Modifier) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier) {
        val cols = 4
        val gap = 20.dp
        val tile = ((maxWidth - gap * (cols - 1)) / cols).coerceAtMost(132.dp)
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            state.apps.chunked(cols).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { app ->
                        Column(
                            Modifier.width(tile).clickable { actions.openApp(app) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier.size(tile).clip(RoundedCornerShape(tile * 0.26f)).background(Co.card).border(1.dp, Co.glassLine, RoundedCornerShape(tile * 0.26f)),
                                contentAlignment = Alignment.Center,
                            ) { AppIcon(app, (tile.value * 0.5f).toInt()) }
                            Spacer(Modifier.height(10.dp))
                            Text(app.label, style = MaterialTheme.typography.labelLarge, color = Co.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- shared

@Composable
private fun CoCard(modifier: Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(modifier.clip(shape).background(Co.card).border(1.dp, Co.glassLine, shape)) { content() }
}

@Composable
private fun CoRound(icon: ImageVector, label: String, sizeDp: Int, filled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(sizeDp.dp).clip(CircleShape)
            .background(if (filled) Co.text else Co.tile)
            .border(1.dp, Co.glassLine, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = if (filled) Co.onAccent else Co.text, modifier = Modifier.size((sizeDp * 0.42f).dp)) }
}
