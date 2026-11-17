package net.prototux.aare.ui.car

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.prototux.aare.R
import net.prototux.aare.ui.theme.LocalCarDimens

/**
 * "CarPlay": AARE's third layout, matching `CarUI/carplay-style.html`. Unlike Classic/Dusk it does
 * NOT take the chosen color theme — it is its own fixed look: a signature multi-radial gradient
 * (maroon top-left, blue bottom-right, violet top) over near-black, with translucent white "glass"
 * cards floating on it. A fixed left **dock** (clock + status line, the last apps opened as tiles,
 * a mic and a round Home button), a **paged 4×2 app grid** (CarPlay's home) with paging dots, and a
 * split **dashboard** (a big live-map card beside a now-playing card and a suggestions card).
 *
 * Full-screen apps (media browser, phone, settings, templated or projected apps) still come from
 * [appContent]/[navMap]; the live projected map renders in the dashboard's map card through
 * [NavCard]. The dock's Home button opens the app grid; the pill on the grid / dashboard toggles
 * between the two.
 */

/** The CarPlay palette: fixed white-glass on the signature gradient, independent of the color theme. */
private object Cp {
    val text = Color.White
    val muted = Color.White.copy(alpha = 0.72f)
    val glass = Color.White.copy(alpha = 0.10f)
    val glassLine = Color.White.copy(alpha = 0.20f)
    val tile = Color.White.copy(alpha = 0.14f)
    val tileLine = Color.White.copy(alpha = 0.22f)
    val playBg = Color.White
    val playFg = Color(0xFF1A1030)
    val base = Color(0xFF0A0B1A)
    val maroon = Color(0xFFAA2346)
    val blue = Color(0xFF285AE6)
    val violet = Color(0xFF785AF0)
    // Per-app-kind tile gradients, matching the mockup's colorful squircles (fallback when an app
    // has no icon bitmap; the real icon sits on top otherwise).
    fun tileBrush(app: CarAppEntry): Brush {
        val (a, b) = when (app.kind) {
            CarAppKind.NAVIGATION, CarAppKind.PROJECTED -> Color(0xFF6FC2FF) to Color(0xFF2B6BFF)
            CarAppKind.MEDIA -> Color(0xFFFF8AA0) to Color(0xFFE0245E)
            CarAppKind.PHONE, CarAppKind.PHONE_SCREEN -> Color(0xFF7CF0B0) to Color(0xFF12A05A)
            CarAppKind.MESSAGING, CarAppKind.NOTIFICATIONS -> Color(0xFF9CF08A) to Color(0xFF26B24A)
            CarAppKind.SETTINGS -> Color(0xFFAAB3C6) to Color(0xFF5D667C)
            else -> {
                val pairs = listOf(
                    Color(0xFFD29BFF) to Color(0xFF7A3BD8), Color(0xFFFFC36B) to Color(0xFFFF7A1F),
                    Color(0xFF79B8FF) to Color(0xFF3A63E0), Color(0xFF6EE8CC) to Color(0xFF1C9C94),
                    Color(0xFFFFC27A) to Color(0xFFFF7A2E),
                )
                pairs[(app.id.hashCode() and 0x7fffffff) % pairs.size]
            }
        }
        return Brush.linearGradient(listOf(a, b))
    }
}

/** The signature background: near-black with maroon (top-left) and blue (bottom-right) radial glows. */
private fun Modifier.cpBackground() = this
    .background(Cp.base)
    .drawBehind {
        drawRect(Brush.radialGradient(
            listOf(Cp.maroon.copy(alpha = 0.75f), Color.Transparent),
            center = Offset(size.width * 0.05f, size.height * 0.35f), radius = size.maxDimension * 0.7f))
        drawRect(Brush.radialGradient(
            listOf(Cp.blue.copy(alpha = 0.80f), Color.Transparent),
            center = Offset(size.width * 0.98f, size.height * 1.0f), radius = size.maxDimension * 0.75f))
        drawRect(Brush.radialGradient(
            listOf(Cp.violet.copy(alpha = 0.40f), Color.Transparent),
            center = Offset(size.width * 0.55f, -size.height * 0.08f), radius = size.maxDimension * 0.5f))
    }

@Composable
internal fun CarPlayShell(
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    overlay: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onRight = state.railOnRight
    net.prototux.aare.ui.theme.CarColors(colorSchemeFor(DuskPaletteForCp)) {
        CompositionLocalProvider(LocalCarDimens provides LocalCarDimens.current.copy(cardRadius = 30.dp, gutter = 14.dp)) {
            Box(Modifier.fillMaxSize().cpBackground().padding(14.dp).then(modifier)) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    val dock: @Composable () -> Unit = { CarPlayDock(state, actions) }
                    val content: @Composable RowScope.() -> Unit = {
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            val screen = state.screen
                            val np = state.nowPlaying
                            val mediaApp = state.app(np?.appId ?: state.activeMediaAppId)
                            val showMini = np != null && screen is CarScreen.App && screen.appId != mediaApp?.id
                            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(Modifier.weight(1f).fillMaxWidth()) {
                                    when (screen) {
                                        CarScreen.Dashboard -> CarPlayDashboard(state, actions, navMap, Modifier.fillMaxSize())
                                        CarScreen.Launcher -> CarPlayLauncher(state, actions, Modifier.fillMaxSize())
                                        is CarScreen.App -> {
                                            val app = state.app(screen.appId)
                                            if (app != null) appContent(app, Modifier.fillMaxSize())
                                            else CarPlayDashboard(state, actions, navMap, Modifier.fillMaxSize())
                                        }
                                    }
                                }
                                if (showMini) CarPlayMiniPlayer(state, actions, Modifier.align(Alignment.CenterHorizontally))
                            }
                            overlay(Modifier.align(Alignment.BottomCenter))
                            NoticeOverlay(state, actions, Modifier.align(Alignment.TopCenter))
                            CallOverlay(state, actions, Modifier.align(Alignment.TopCenter))
                        }
                    }
                    if (onRight) { content(); dock() } else { dock(); content() }
                }
            }
        }
    }
}

// A dark Material scheme so any stray MaterialTheme-styled child (overlays, notices) reads on the
// dark gradient; CarPlay's own surfaces use the Cp.* colors directly, not this.
private val DuskPaletteForCp: HybridPalette get() = paletteFor(net.prototux.aare.Settings.STYLE_MIDNIGHT)

// ---------------------------------------------------------------- glass card

@Composable
private fun CpCard(modifier: Modifier, radius: Int = 36, padding: Int = 0, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(radius.dp)
    Box(
        modifier.clip(shape).background(Cp.glass).border(1.dp, Cp.glassLine, shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .let { if (padding > 0) it.padding(padding.dp) else it },
    ) { content() }
}

// ---------------------------------------------------------------- dock

@Composable
private fun CarPlayDock(state: CarUiState, actions: CarActions) {
    val current = (state.screen as? CarScreen.App)?.appId
    val recents = state.recentAppIds.mapNotNull { state.app(it) }.filter { it.id != current }.distinctBy { it.id }.take(3)
    Column(
        Modifier.fillMaxHeight().width(118.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        CpStatus(state)
        recents.forEach { app -> CpTile(app, 88, 26) { actions.openApp(app) } }
        Spacer(Modifier.weight(1f))
        // Mic kept as a small glass circle (CarPlay has no dock mic, but F-Auto needs the assistant
        // reachable without a steering-wheel button).
        Box(
            Modifier.size(58.dp).clip(CircleShape)
                .background(if (state.listening) Color.White.copy(alpha = 0.9f) else Cp.tile)
                .border(1.dp, Cp.tileLine, CircleShape).clickable { actions.assistant() },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Mic, stringResource(R.string.assistant_mic_label), tint = if (state.listening) Cp.playFg else Cp.text, modifier = Modifier.size(26.dp)) }
        // The glass Home button (opens the app grid).
        Box(
            Modifier.size(92.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))
                .border(1.5.dp, Color.White.copy(alpha = 0.28f), CircleShape).clickable { actions.openLauncher() },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Home, stringResource(R.string.dash_menu_home), tint = Cp.text, modifier = Modifier.size(40.dp)) }
    }
}

/** Big clock + a status line (signal glyph · battery %), matching `.st` in the mockup. */
@Composable
private fun CpStatus(state: CarUiState) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(1_000 - now % 1_000 + 5) }
    }
    val ctx = LocalContext.current
    val is24 = android.text.format.DateFormat.is24HourFormat(ctx)
    val time = android.text.format.DateFormat.format(if (is24) "H:mm" else "h:mm", now).toString()
    val battery = remember {
        val i = ctx.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) level * 100 / scale else -1
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(time, style = MaterialTheme.typography.headlineMedium, color = Cp.text, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(if (battery >= 0) "▮▮▮▮ · $battery%" else "▮▮▮▮", style = MaterialTheme.typography.labelMedium, color = Cp.muted, maxLines = 1)
    }
}

/** A colorful squircle app tile: the real icon over a per-app gradient, like the mockup's `.sq`/`.dk`. */
@Composable
private fun CpTile(app: CarAppEntry, sizeDp: Int, radiusDp: Int, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(radiusDp.dp)
    Box(
        Modifier.size(sizeDp.dp).clip(shape).background(Cp.tileBrush(app))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center,
    ) {
        val icon = app.icon
        if (icon != null) Image(icon.asImageBitmap(), app.label, Modifier.size((sizeDp * 0.62f).dp).clip(RoundedCornerShape((radiusDp * 0.5f).dp)), contentScale = ContentScale.Crop)
        else Text(app.label.take(1), style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------- dashboard

@Composable
private fun CarPlayDashboard(
    state: CarUiState,
    actions: CarActions,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    modifier: Modifier,
) {
    Box(modifier) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // Live map in a glass frame.
            CpCard(Modifier.weight(1.9f).fillMaxHeight()) {
                NavCardBare(state, actions, navMap, Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CarPlayNowPlaying(state, actions, Modifier.weight(1.2f).fillMaxWidth())
                CarPlaySuggestions(state, actions, Modifier.weight(1f).fillMaxWidth())
            }
        }
        // The "⊞ Dashboard"/apps toggle pill, floated top-right over the cards (mockup's `.sw`).
        CpPill(stringResource(R.string.dash_menu_apps), Icons.Rounded.GridView, Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp)) { actions.openLauncher() }
    }
}

/** The map card's content without CarShell's own opaque [Card] frame (CpCard supplies the glass). */
@Composable
private fun NavCardBare(state: CarUiState, actions: CarActions, navMap: @Composable (CarAppEntry, Modifier) -> Unit, modifier: Modifier) {
    val nav = state.app(state.activeNavAppId)
    val guidance = state.phoneGuidance
    val hosted = nav != null && (nav.kind == CarAppKind.NAVIGATION || nav.kind == CarAppKind.PROJECTED || nav.kind == CarAppKind.OFFLINE_MAP)
    Box(modifier) {
        if (nav != null && (hosted || guidance == null)) {
            navMap(nav, Modifier.fillMaxSize())
        } else {
            // No hosted map: fall back to CarShell's NavCard (guidance card or "choose a nav app").
            NavCard(state, actions, navMap, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun CarPlayNowPlaying(state: CarUiState, actions: CarActions, modifier: Modifier) {
    val np = state.nowPlaying
    val media = state.app(np?.appId ?: state.activeMediaAppId)
    CpCard(modifier, padding = 22, onClick = { media?.let(actions::openApp) ?: actions.openLauncher() }) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            // Album art (or a gradient placeholder with a note).
            val artShape = RoundedCornerShape(26.dp)
            if (np?.art != null) {
                Image(np.art.asImageBitmap(), null, Modifier.size(132.dp).clip(artShape), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.size(132.dp).clip(artShape).background(Brush.linearGradient(listOf(Color(0xFFFF7A59), Color(0xFF4A6BFF)))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(52.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(np?.title ?: media?.label ?: stringResource(R.string.media_nothing_playing), style = MaterialTheme.typography.titleLarge, color = Cp.text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(np?.subtitle ?: "", style = MaterialTheme.typography.bodyLarge, color = Cp.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                CpRound(Icons.Rounded.SkipPrevious, stringResource(R.string.media_action_previous), 56, filled = false) { actions.mediaPrevious() }
                CpRound(if (np?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, stringResource(R.string.media_action_play_pause), 72, filled = true) { actions.mediaPlayPause() }
                CpRound(Icons.Rounded.SkipNext, stringResource(R.string.media_action_next), 56, filled = false) { actions.mediaNext() }
            }
        }
    }
}

@Composable
private fun CpRound(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, sizeDp: Int, filled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(sizeDp.dp).clip(CircleShape)
            .background(if (filled) Cp.playBg else Color.White.copy(alpha = 0.18f))
            .border(1.dp, Cp.tileLine, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = if (filled) Cp.playFg else Cp.text, modifier = Modifier.size((sizeDp * 0.42f).dp)) }
}

/** Quick-launch rows (the mockup's "Call Mom / Home" card): the nav app, the phone, and the media app. */
@Composable
private fun CarPlaySuggestions(state: CarUiState, actions: CarActions, modifier: Modifier) {
    val picks = buildList {
        state.navApp()?.let { add(it) }
        state.apps.firstOrNull { it.kind == CarAppKind.PHONE }?.let { add(it) }
        state.app(state.activeMediaAppId)?.let { m -> if (none { it.id == m.id }) add(m) }
    }.take(3)
    CpCard(modifier, padding = 22) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.dash_suggested), style = MaterialTheme.typography.labelLarge, color = Cp.muted)
            if (picks.isEmpty()) {
                Text(stringResource(R.string.media_nothing_playing), style = MaterialTheme.typography.bodyMedium, color = Cp.muted)
            } else picks.forEach { app ->
                Row(
                    Modifier.fillMaxWidth().clickable { actions.openApp(app) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CpTile(app, 56, 18)
                    Spacer(Modifier.width(16.dp))
                    Text(app.label, style = MaterialTheme.typography.titleMedium, color = Cp.text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- app grid

/** CarPlay "home": a paged 4×2 grid of colorful tiles with paging dots and a "Dashboard" pill. */
@Composable
private fun CarPlayLauncher(state: CarUiState, actions: CarActions, modifier: Modifier) {
    val perPage = 8
    val pages = if (state.apps.isEmpty()) listOf(emptyList()) else state.apps.chunked(perPage)
    val pager = rememberPagerState(pageCount = { pages.size })
    Box(modifier) {
        CpPill(stringResource(R.string.nav_dashboard_where_to), Icons.Rounded.GridView, Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp)) { actions.goHome() }
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { pageIndex ->
            CarPlayGridPage(pages[pageIndex], actions, Modifier.fillMaxSize().padding(top = 56.dp, bottom = 36.dp))
        }
        if (pages.size > 1) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(pages.size) { i ->
                    Box(Modifier.size(11.dp).clip(CircleShape).background(if (i == pager.currentPage) Color.White else Color.White.copy(alpha = 0.35f)))
                }
            }
        }
    }
}

/** One page: 4 columns × up to 2 rows of tiles sized to the available width. */
@Composable
private fun CarPlayGridPage(apps: List<CarAppEntry>, actions: CarActions, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val cols = 4
        val gap = 16.dp
        val tile = ((maxWidth - gap * (cols - 1)) / cols).coerceAtMost((maxHeight - gap) / 2 - 36.dp)
        val rows = apps.chunked(cols)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap), horizontalAlignment = Alignment.CenterHorizontally) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { app ->
                        Column(
                            Modifier.width(tile).clickable { actions.openApp(app) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CpTile(app, tile.value.toInt(), (tile.value * 0.26f).toInt())
                            Spacer(Modifier.height(10.dp))
                            Text(app.label, style = MaterialTheme.typography.labelLarge, color = Cp.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- shared

@Composable
private fun CpPill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(46.dp).clip(RoundedCornerShape(23.dp)).background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Cp.glassLine, RoundedCornerShape(23.dp)).clickable(onClick = onClick).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Cp.text, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = Cp.text, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** A floating glass "now playing" bar kept over full-screen apps so playback stays reachable. */
@Composable
private fun CarPlayMiniPlayer(state: CarUiState, actions: CarActions, modifier: Modifier) {
    val np = state.nowPlaying
    val media = state.app(np?.appId ?: state.activeMediaAppId)
    Row(
        modifier.fillMaxWidth(0.86f).height(64.dp).clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.14f)).border(1.dp, Cp.glassLine, RoundedCornerShape(28.dp))
            .clickable { media?.let(actions::openApp) }.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val artShape = RoundedCornerShape(14.dp)
        if (np?.art != null) Image(np.art.asImageBitmap(), null, Modifier.size(44.dp).clip(artShape), contentScale = ContentScale.Crop)
        else Box(Modifier.size(44.dp).clip(artShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null, tint = Cp.text, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(np?.title ?: media?.label ?: stringResource(R.string.media_nothing_playing), style = MaterialTheme.typography.labelLarge, color = Cp.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(np?.subtitle ?: "", style = MaterialTheme.typography.labelSmall, color = Cp.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        CpRound(Icons.Rounded.SkipPrevious, stringResource(R.string.media_action_previous), 46, filled = false) { actions.mediaPrevious() }
        CpRound(if (np?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, stringResource(R.string.media_action_play_pause), 52, filled = true) { actions.mediaPlayPause() }
        CpRound(Icons.Rounded.SkipNext, stringResource(R.string.media_action_next), 46, filled = false) { actions.mediaNext() }
    }
}
