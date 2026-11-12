package net.prototux.aare.ui.car

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.prototux.aare.R
import net.prototux.aare.ui.theme.LocalCarDimens

/**
 * "Dusk": AARE's second layout, matching `CarUI/dusk-8in.html` (built for an 8", 17.5x10.5 cm,
 * ~5:3 head unit — tighter gaps/rail than the first Dusk pass) — a floating glass rail with a
 * conic logo orb, a Home screen with a Full/Map+Music toggle led by a resume-playback header, and
 * an apps grid of rounded-square tiles. The rail can also be collapsed to a thin edge handle to
 * give the map/media more width on an 8" screen. Picked independently of color theme by
 * `state.shellLayout` ([net.prototux.aare.Settings.LAYOUT_DUSK]); reuses
 * [HybridPalette]/[paletteFor]/[colorSchemeFor] so any of the 5 color themes applies here too.
 *
 * Full-screen apps (media browser, phone, settings, messaging, templated apps) still come from
 * [appContent]/[navMap] exactly like the Classic layout — only the chrome around them (rail,
 * Home, Apps grid) differs here. [PhoneScreen] and [CarSettingsScreen] read `shellLayout`
 * themselves to offer a matching arrangement without duplicating their logic per layout.
 */
@Composable
internal fun DuskUIShell(
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    overlay: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = paletteFor(state.shellStyle, state.night)
    // "Dark bar": the rail keeps the dark theme while the page has day colors.
    val railPalette = if (state.darkBar) barPaletteFor(state.shellStyle) else palette
    val onRight = state.railOnRight
    var railVisible by remember(state.railAutoHide) { mutableStateOf(!state.railAutoHide) }
    MaterialTheme(colorScheme = colorSchemeFor(palette)) {
        CompositionLocalProvider(LocalCarDimens provides LocalCarDimens.current.copy(cardRadius = 24.dp, gutter = 10.dp)) {
            Box(Modifier.fillMaxSize().background(palette.bg).padding(10.dp).then(modifier)) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val rail: @Composable () -> Unit = {
                        if (railVisible) {
                            if (railPalette === palette) DuskRail(state, actions, palette) { railVisible = false }
                            else MaterialTheme(colorScheme = colorSchemeFor(railPalette)) { DuskRail(state, actions, railPalette) { railVisible = false } }
                        }
                    }
                    val content: @Composable RowScope.() -> Unit = {
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            val screen = state.screen
                            val np = state.nowPlaying
                            val mediaApp = state.app(np?.appId ?: state.activeMediaAppId)
                            val showMiniPlayer = np != null && screen is CarScreen.App && screen.appId != mediaApp?.id
                            // The mini player gets its own strip under the app instead of floating over
                            // it: floating, it hid the last list rows, the keypad's bottom keys and a
                            // navigation app's own bottom controls.
                            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.weight(1f).fillMaxWidth()) {
                                    when (screen) {
                                        CarScreen.Dashboard -> DuskHome(state, actions, navMap, palette, Modifier.fillMaxSize())
                                        CarScreen.Launcher -> DuskLauncher(state, actions, palette, Modifier.fillMaxSize())
                                        is CarScreen.App -> {
                                            val app = state.app(screen.appId)
                                            if (app != null) appContent(app, Modifier.fillMaxSize())
                                            else DuskHome(state, actions, navMap, palette, Modifier.fillMaxSize())
                                        }
                                    }
                                }
                                if (showMiniPlayer) DuskMiniPlayerBar(state, actions, palette, Modifier.align(Alignment.CenterHorizontally))
                            }
                            overlay(Modifier.align(Alignment.BottomCenter))
                            NoticeOverlay(state, actions, Modifier.align(Alignment.TopCenter))
                            CallOverlay(state, actions, Modifier.align(Alignment.TopCenter))
                            if (!railVisible) {
                                val handleAlign = if (onRight) Alignment.CenterEnd else Alignment.CenterStart
                                DuskRailHandle(palette, onRight, Modifier.align(handleAlign)) { railVisible = true }
                            }
                        }
                    }
                    if (onRight) { content(); rail() } else { rail(); content() }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- rail

/**
 * A floating "glass" rail (gap + border on all sides, unlike Classic's edge-to-edge bar): a
 * collapse chevron, then icon-only tabs for Home/Nav/Music/Apps, then the clock and mic — no
 * text labels, no logo, kept deliberately minimal. [onCollapse] hides the whole rail, giving an
 * 8" screen's map/media more width; [DuskRailHandle] brings it back.
 */
@Composable
private fun DuskRail(state: CarUiState, actions: CarActions, palette: HybridPalette, onCollapse: () -> Unit) {
    val nav = state.navApp()
    val media = state.app(state.activeMediaAppId) ?: state.apps.firstOrNull { it.kind == CarAppKind.MEDIA }
    val phone = state.apps.firstOrNull { it.kind == CarAppKind.PHONE }
    val notif = state.apps.firstOrNull { it.kind == CarAppKind.NOTIFICATIONS }
    fun on(app: CarAppEntry?) = app != null && state.screen == CarScreen.App(app.id)
    val chevron = if (state.railOnRight) Icons.AutoMirrored.Rounded.KeyboardArrowRight else Icons.AutoMirrored.Rounded.KeyboardArrowLeft
    Column(
        Modifier.fillMaxHeight().width(88.dp).clip(RoundedCornerShape(26.dp))
            .background(palette.card.copy(alpha = 0.55f)).border(1.dp, palette.line, RoundedCornerShape(26.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The whole top of the rail collapses it, not just the 20 dp chevron: a small target is a
        // long look away from the road.
        Box(
            Modifier.size(width = 72.dp, height = 52.dp).clip(RoundedCornerShape(16.dp)).background(palette.card2).clickable(onClick = onCollapse),
            contentAlignment = Alignment.Center,
        ) { Icon(chevron, stringResource(R.string.dash_hide_rail), tint = palette.muted, modifier = Modifier.size(26.dp)) }
        // The tabs share whatever height the screen has: as large as 72 dp each on a tall screen,
        // never below 48 dp on a short one (they would be cut off, not squeezed, otherwise).
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val tab = (maxHeight / 6).coerceIn(44.dp, 72.dp)
            // Six tabs at the minimum size still taller than the screen (a short head unit, the
            // phone preview): the column scrolls, so Messages and Apps stay reachable instead of
            // being cut off below the fold.
            val fits = maxHeight >= tab * 6
            val column = if (fits) Modifier.fillMaxSize()
                else Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            Column(column, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = if (fits) Arrangement.SpaceEvenly else Arrangement.Top) {
                DuskTab("home", stringResource(R.string.dash_menu_home), state.screen == CarScreen.Dashboard, palette, tab) { actions.goHome() }
                DuskTab("nav", nav?.label ?: stringResource(R.string.dash_maps), on(nav), palette, tab) { nav?.let(actions::openApp) ?: actions.openLauncher() }
                DuskTab("media", media?.label ?: stringResource(R.string.media_fallback_label), on(media), palette, tab) { media?.let(actions::openApp) ?: actions.openLauncher() }
                DuskTab("phone", phone?.label ?: stringResource(R.string.setup_app_entry_phone), on(phone), palette, tab) { phone?.let(actions::openApp) ?: actions.openLauncher() }
                // Messages were only reachable through the Apps grid here, with nothing to say one came in.
                DuskTab("chat", notif?.label ?: stringResource(R.string.dash_messages), on(notif), palette, tab, badge = state.unseen > 0) { notif?.let(actions::openApp) ?: actions.openLauncher() }
                DuskTab("apps", stringResource(R.string.dash_menu_apps), state.screen == CarScreen.Launcher, palette, tab) { actions.openLauncher() }
            }
        }
        Clock()
        Spacer(Modifier.height(10.dp))
        DuskMic(state.listening, palette) { actions.assistant() }
    }
}

/** The thin re-show tab (`.handle` in the mockup) that appears at the screen's edge once the rail is collapsed. */
@Composable
private fun DuskRailHandle(palette: HybridPalette, onRight: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = if (onRight) RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp) else RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
    val icon = if (onRight) Icons.AutoMirrored.Rounded.KeyboardArrowLeft else Icons.AutoMirrored.Rounded.KeyboardArrowRight
    Box(
        modifier.size(width = 44.dp, height = 132.dp).clip(shape)
            .background(palette.card.copy(alpha = 0.85f)).border(1.dp, palette.line, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, stringResource(R.string.dash_show_rail), tint = palette.text, modifier = Modifier.size(28.dp)) }
}

@Composable
private fun DuskTab(kind: String, label: String, selected: Boolean, palette: HybridPalette, size: Dp, badge: Boolean = false, onClick: () -> Unit) {
    val bg = if (selected) Brush.linearGradient(listOf(palette.accent, palette.accent2)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    val fg = if (selected) palette.onAccent else palette.muted
    Box(
        Modifier.size(width = 72.dp, height = size).padding(vertical = 2.dp).clip(RoundedCornerShape(20.dp)).background(bg).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        HybridIcon(kind, fg, 28.dp)
        // Something unseen: a dot at the icon's corner.
        if (badge) Box(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 14.dp).size(12.dp).clip(CircleShape).background(palette.warn).border(2.dp, palette.card, CircleShape))
    }
}

@Composable
private fun DuskMic(listening: Boolean, palette: HybridPalette, onClick: () -> Unit) {
    val ring = Brush.linearGradient(listOf(palette.accent, palette.accent2))
    Box(
        Modifier.size(60.dp).clip(CircleShape).background(ring).padding(3.dp).clip(CircleShape)
            .background(if (listening) palette.accent else palette.card2).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        HybridIcon("mic", if (listening) palette.onAccent else palette.text, 24.dp)
    }
}

/**
 * A floating "now playing" bar kept over full-screen apps (e.g. navigation) so playback stays
 * reachable without leaving the app — mirrors the Classic layout's persistent bottom bar, but as
 * a Dusk-style floating glass card rather than an edge-to-edge strip.
 */
@Composable
private fun DuskMiniPlayerBar(state: CarUiState, actions: CarActions, palette: HybridPalette, modifier: Modifier) {
    val np = state.nowPlaying
    val media = state.app(np?.appId ?: state.activeMediaAppId)
    Row(
        modifier.fillMaxWidth(0.86f).height(64.dp).clip(RoundedCornerShape(24.dp))
            .background(palette.card.copy(alpha = 0.92f)).border(1.dp, palette.line, RoundedCornerShape(24.dp))
            .clickable { media?.let(actions::openApp) }.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The album art (or the music-note placeholder), not an anonymous gradient square.
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { Box(Modifier.requiredSize(56.dp).scale(40f / 56f)) { MiniArt(np, palette) } }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(np?.title ?: media?.label ?: stringResource(R.string.media_nothing_playing), style = MaterialTheme.typography.labelLarge, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(np?.subtitle ?: "", style = MaterialTheme.typography.labelSmall, color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        RoundIcon(Icons.Rounded.SkipPrevious, stringResource(R.string.media_action_previous), filled = false) { actions.mediaPrevious() }
        RoundIcon(if (np?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, stringResource(R.string.media_action_play_pause), filled = true) { actions.mediaPlayPause() }
        RoundIcon(Icons.Rounded.SkipNext, stringResource(R.string.media_action_next), filled = false) { actions.mediaNext() }
    }
}

// ---------------------------------------------------------------- home

/**
 * Home: "Full" shows a header (greeting, date/time, the Full/Map+Music toggle, a "continue
 * playing" pill) over a Nav card + a narrow Media column; "Map+Music" drops the header for a
 * full-bleed map with the toggle floated over it and a taller Media column — matching
 * `dusk-8in.html`'s two home states exactly (it has no Classic-style Suggested/Recent cards).
 */
@Composable
private fun DuskHome(state: CarUiState, actions: CarActions, navMap: @Composable (CarAppEntry, Modifier) -> Unit, palette: HybridPalette, modifier: Modifier) {
    // Keyed on the setting, so changing "Home layout" on the phone shows on the car at once.
    var mapMusic by remember(state.homeMapMusicDefault) { mutableStateOf(state.homeMapMusicDefault) }
    // musicWidgetSize (0..1, a setting) trades nav width for media width, total weight kept at 3.3.
    val mediaWeight = 0.6f + state.musicWidgetSize * 1.2f
    val navWeight = 3.3f - mediaWeight
    // One Column for both views, with only the top strip swapped: the map and media cards keep
    // their place in the composition, so toggling doesn't tear down and rebuild the live map surface.
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth()) {
            if (mapMusic) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    DuskSegmented(mapMusic, palette, Modifier) { mapMusic = it }
                }
            } else {
                DuskHeader(state, actions, palette, mapMusic) { mapMusic = it }
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NavCard(state, actions, navMap, Modifier.weight(navWeight).fillMaxHeight())
            MediaCard(state, actions, compact = false, modifier = Modifier.weight(mediaWeight).fillMaxHeight())
        }
    }
}

@Composable
private fun DuskSegmented(mapMusic: Boolean, palette: HybridPalette, modifier: Modifier, onChange: (Boolean) -> Unit) {
    Row(
        modifier.height(44.dp).clip(RoundedCornerShape(24.dp)).background(palette.card2.copy(alpha = 0.85f)).border(1.dp, palette.line, RoundedCornerShape(24.dp)).padding(4.dp),
    ) {
        DuskSegmentChip(stringResource(R.string.setup_layout_full), !mapMusic, palette, Modifier.fillMaxHeight()) { onChange(false) }
        DuskSegmentChip(stringResource(R.string.setup_quick_map_music_title), mapMusic, palette, Modifier.fillMaxHeight()) { onChange(true) }
    }
}

@Composable
private fun DuskSegmentChip(label: String, selected: Boolean, palette: HybridPalette, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = if (selected) Brush.linearGradient(listOf(palette.accent, palette.accent2)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    val fg = if (selected) palette.onAccent else palette.muted
    Box(
        modifier.clip(RoundedCornerShape(19.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

@Composable
private fun DuskHeader(state: CarUiState, actions: CarActions, palette: HybridPalette, mapMusic: Boolean, onMapMusicChange: (Boolean) -> Unit) {
    val np = state.nowPlaying
    val media = state.app(np?.appId ?: state.activeMediaAppId)
    Card(Modifier.fillMaxWidth().height(84.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greeting(), style = MaterialTheme.typography.labelLarge, color = palette.muted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Clock()
                    Spacer(Modifier.width(10.dp))
                    WeatherChip(state.weather)
                }
            }
            DuskSegmented(mapMusic, palette, Modifier) { onMapMusicChange(it) }
            Spacer(Modifier.width(12.dp))
            if (np != null) {
                Row(
                    Modifier.height(44.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(palette.accent, palette.accent2)))
                        .clickable { media?.let(actions::openApp) ?: actions.openLauncher() }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(palette.onAccent.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                        Icon(if (np.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = palette.onAccent, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.dash_continue), style = MaterialTheme.typography.labelLarge, color = palette.onAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- apps grid

/** Rounded-square ("squircle") tiles, matching `dusk-8in.html`'s 4-column apps grid. The Classic layout's Apps screen uses it too. */
@Composable
internal fun DuskLauncher(state: CarUiState, actions: CarActions, palette: HybridPalette, modifier: Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        modifier = modifier,
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(state.apps, key = { it.id }) { app ->
            Column(
                Modifier.clip(RoundedCornerShape(24.dp)).clickable { actions.openApp(app) }.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val tileShape = RoundedCornerShape(28.dp)
                Box(
                    Modifier.size(96.dp).clip(tileShape).background(palette.card2).border(2.dp, palette.line, tileShape),
                    contentAlignment = Alignment.Center,
                ) { AppIcon(app, 80) }
                Spacer(Modifier.height(10.dp))
                Text(app.label, style = MaterialTheme.typography.labelLarge, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
