package net.prototux.aare.ui.car

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.ui.theme.LocalCarDimens

/**
 * "Hybrid": AARE's one shell — a fixed left icon rail and a persistent bottom media/status bar
 * (both always on screen) around a dashboard that itself toggles between a map+widgets view and
 * a map+music view — matching the reference mockups in `CarUI/` (`hybrid.html` plus the
 * Midnight/Aurora/Forest/Light/Sport screenshots). It comes in 5 color themes, picked by
 * `state.shellStyle` (one of [Settings]'s `STYLE_*` constants); [HybridPalette] holds each
 * theme's colors.
 *
 * Maps/media/phone/messages *app* screens still come from [appContent]/[navMap] like every other
 * shell — this shell's own look lives in the rail, the bottom bar and the dashboard.
 */
internal data class HybridPalette(
    val bg: Brush,
    val card: Color,
    val card2: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val accent2: Color,
    val warn: Color,
    val bad: Color,
    val onAccent: Color,
    /** Aurora only: a distinct rail-pill color per tab, in rail order (home/nav/media/phone/chat/apps). */
    val railTints: List<Color>? = null,
)

private fun solid(c: Color) = SolidColor(c) as Brush

private val MidnightPalette = HybridPalette(
    bg = solid(Color(0xFF07090F)), card = Color(0xFF141925), card2 = Color(0xFF1B2232), line = Color(0xFF262E42),
    text = Color(0xFFF2F5FB), muted = Color(0xFF8A94AB), accent = Color(0xFF46E0C8), accent2 = Color(0xFF5B8CFF),
    warn = Color(0xFFFFB547), bad = Color(0xFFFF5D6C), onAccent = Color(0xFF07090F),
)

private val ForestPalette = HybridPalette(
    bg = solid(Color(0xFF0A1410)), card = Color(0xFF14241C), card2 = Color(0xFF1C3226), line = Color(0xFF2A4536),
    text = Color(0xFFEFF7F0), muted = Color(0xFF8FAE9B), accent = Color(0xFF5FD66C), accent2 = Color(0xFFA8E05A),
    warn = Color(0xFFFFB547), bad = Color(0xFFFF5D6C), onAccent = Color(0xFF0A1410),
)

private val LightPalette = HybridPalette(
    bg = solid(Color(0xFFF4F6F9)), card = Color(0xFFFFFFFF), card2 = Color(0xFFE9EDF3), line = Color(0xFFD7DEE8),
    text = Color(0xFF141A24), muted = Color(0xFF5B6678), accent = Color(0xFF1FB6A8), accent2 = Color(0xFF3D7CFF),
    warn = Color(0xFFCC7A00), bad = Color(0xFFD5323F), onAccent = Color(0xFFFFFFFF),
)

private val SportPalette = HybridPalette(
    bg = solid(Color(0xFF14151A)), card = Color(0xFF1E2027), card2 = Color(0xFF282A32), line = Color(0xFF3A3D47),
    text = Color(0xFFF4F4F6), muted = Color(0xFF96989F), accent = Color(0xFFFF4D3D), accent2 = Color(0xFFFF9A3D),
    warn = Color(0xFFFFB547), bad = Color(0xFFFF4D3D), onAccent = Color(0xFFFFFFFF),
)

private val AuroraRailTints = listOf(
    Color(0xFFFF5E9E), // home: pink
    Color(0xFF5B8CFF), // nav: blue
    Color(0xFFFF6B5E), // media: coral/red
    Color(0xFF4CD67A), // phone: green
    Color(0xFFB06BFF), // chat: purple
    Color(0xFFF2C94C), // apps: yellow
)

private val AuroraPalette = HybridPalette(
    bg = Brush.linearGradient(listOf(Color(0xFF241233), Color(0xFF3A1740), Color(0xFF461C33))),
    card = Color(0xFF2A1736), card2 = Color(0xFF3A2048), line = Color(0xFF4A2C5C),
    text = Color(0xFFF7F0FA), muted = Color(0xFFC3A9D6), accent = Color(0xFFFF5E9E), accent2 = Color(0xFFB06BFF),
    warn = Color(0xFFF2C94C), bad = Color(0xFFFF6B5E), onAccent = Color(0xFF241233), railTints = AuroraRailTints,
)

// Dusk: indigo-night background (a maroon glow top-left, a blue glow bottom-right, per
// `CarUI/dusk.html`'s radial gradients, approximated here as one diagonal sweep since Compose has
// no cheap multi-radial-gradient brush), coral/periwinkle accent duo, one consistent gradient on
// whichever rail tab is selected rather than Aurora's per-tab hues.
private val DuskPalette = HybridPalette(
    bg = Brush.linearGradient(listOf(Color(0xFF2A0F1C), Color(0xFF0B0E1F), Color(0xFF122048))),
    card = Color(0xFF171A2E), card2 = Color(0xFF1F2340), line = Color(0x24FFFFFF),
    text = Color(0xFFFFFFFF), muted = Color(0xFFBCC0D6), accent = Color(0xFFFF4D63), accent2 = Color(0xFF8B97FF),
    warn = Color(0xFFFFB547), bad = Color(0xFFFF7A8A), onAccent = Color(0xFFFFFFFF),
)

/**
 * The palette for the chosen theme, by day or night: the "Colors" setting (the car's day/night
 * signal, or always dark, or always light) picks [night], and by day every theme gives way to the
 * Light palette, the way Android Auto's own screen does. At night it's the chosen theme.
 */
internal fun paletteFor(style: String, night: Boolean): HybridPalette = if (night) paletteFor(style) else LightPalette

/**
 * The always-dark palette for the bars when "Dark bar" is on: the chosen theme, or Midnight when
 * the chosen theme is Light itself.
 */
internal fun barPaletteFor(style: String): HybridPalette = if (style == Settings.STYLE_LIGHT) MidnightPalette else paletteFor(style)

internal fun paletteFor(style: String): HybridPalette = when (style) {
    Settings.STYLE_AURORA -> AuroraPalette
    Settings.STYLE_FOREST -> ForestPalette
    Settings.STYLE_LIGHT -> LightPalette
    Settings.STYLE_SPORT -> SportPalette
    Settings.STYLE_DUSK -> DuskPalette
    else -> MidnightPalette
}

// Built on the light base scheme for a light palette: the roles not set here (inverse surfaces,
// scrims, disabled states) would otherwise stay the dark scheme's, on a light page.
internal fun colorSchemeFor(p: HybridPalette) = (if (p.text.luminance() < 0.5f) lightColorScheme() else darkColorScheme()).copy(
    background = p.card, // overridden per-pixel by the gradient-capable Box background below
    onBackground = p.text,
    surface = p.card,
    surfaceVariant = p.card2,
    surfaceContainerLowest = p.card,
    surfaceContainerLow = p.card,
    surfaceContainer = p.card,
    surfaceContainerHigh = p.card2,
    surfaceContainerHighest = p.card2,
    primary = p.accent,
    onPrimary = p.onAccent,
    primaryContainer = p.accent.copy(alpha = 0.22f),
    onPrimaryContainer = p.accent,
    secondaryContainer = p.card2,
    onSecondaryContainer = p.text,
    tertiary = p.accent2,
    onTertiary = p.onAccent,
    onSurface = p.text,
    onSurfaceVariant = p.muted,
    outline = p.line,
    outlineVariant = p.line,
    error = p.bad,
)

// ---------------------------------------------------------------- Classic's own contrast

/** WCAG contrast ratio between two opaque colors. */
private fun contrast(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

/** [fill] nudged away from [on], a little at a time so its hue stays, until text in [on] reads on it (about 4:1). */
private fun readableFill(fill: Color, on: Color): Color {
    val toward = if (on.luminance() > 0.5f) Color.Black else Color.White
    var c = fill
    repeat(8) {
        if (contrast(c, on) >= 4f) return c
        c = lerp(c, toward, 0.07f)
    }
    return c
}

/**
 * The palette as the Classic layout uses it. The themes' own colors are tuned for the Dusk
 * layout's large glass panels; Classic packs more small text and thin edges on screen, read at a
 * glance from the driver's seat, so here:
 *  - secondary text sits closer to the primary text (the muted greys were faint on dark cards);
 *  - card edges and dividers are strong enough to see, so cards separate from the page;
 *  - raised surfaces (chips, tiles, the mic) are a clear step above the card they sit on;
 *  - the accent is adjusted until the text drawn on it reads (white on the Light theme's teal and
 *    on Sport's and Dusk's red did not).
 */
private fun HybridPalette.forClassic(): HybridPalette {
    val light = text.luminance() < 0.5f
    return copy(
        muted = lerp(muted, text, 0.42f),
        line = lerp(line, text, if (light) 0.24f else 0.2f),
        card2 = lerp(card2, text, if (light) 0.06f else 0.08f),
        accent = readableFill(accent, onAccent),
        accent2 = readableFill(accent2, onAccent),
    )
}

/** The smallest text Classic draws: the rail's tab labels. (The theme's own labelSmall is 11 sp: unreadable in a car.) */
private val RailLabel = TextStyle(fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)

/** Today's date in the phone's language and order, from a skeleton such as "EEEEdMMMM". */
@Composable
private fun today(skeleton: String): String {
    val hour = System.currentTimeMillis() / 3_600_000
    return remember(skeleton, hour) {
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(java.util.Locale.getDefault(), skeleton)
        android.text.format.DateFormat.format(pattern, java.util.Date()).toString()
    }
}

@Composable
internal fun HybridShell(
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    overlay: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = remember(state.shellStyle, state.night) { paletteFor(state.shellStyle, state.night).forClassic() }
    // "Dark bar": the rail and the bottom bar keep the dark theme while the page has day colors.
    val darkBars = remember(state.shellStyle) { barPaletteFor(state.shellStyle).forClassic() }
    val barPalette = if (state.darkBar) darkBars else palette
    val bars: @Composable (@Composable () -> Unit) -> Unit = { content ->
        if (barPalette === palette) content() else MaterialTheme(colorScheme = colorSchemeFor(barPalette), content = content)
    }
    MaterialTheme(colorScheme = colorSchemeFor(palette)) {
        CompositionLocalProvider(LocalCarDimens provides LocalCarDimens.current.copy(cardRadius = 26.dp, gutter = 14.dp)) {
            Box(Modifier.fillMaxSize().background(palette.bg)) {
                Row(Modifier.fillMaxSize().then(modifier)) {
                    // The rail follows the "Side bar" setting here too, not just in the Dusk layout.
                    if (!state.railOnRight) bars { HybridRail(state, actions, barPalette, edgeAtEnd = true) }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            when (val screen = state.screen) {
                                CarScreen.Dashboard -> HybridDashboard(state, actions, navMap, palette, Modifier.fillMaxSize().padding(16.dp))
                                CarScreen.Launcher -> DuskLauncher(state, actions, palette, Modifier.fillMaxSize())
                                is CarScreen.App -> {
                                    val app = state.app(screen.appId)
                                    if (app != null) appContent(app, Modifier.fillMaxSize())
                                    else HybridDashboard(state, actions, navMap, palette, Modifier.fillMaxSize().padding(16.dp))
                                }
                            }
                            overlay(Modifier.align(Alignment.BottomCenter))
                            NoticeOverlay(state, actions, Modifier.align(Alignment.TopCenter))
                            CallOverlay(state, actions, Modifier.align(Alignment.TopCenter))
                        }
                        bars { HybridBottomBar(state, actions, barPalette) }
                    }
                    if (state.railOnRight) bars { HybridRail(state, actions, barPalette, edgeAtEnd = false) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- rail

/**
 * The rail: the time and date, six tabs sharing the height the screen has, the voice button. A
 * hairline on its inner side ([edgeAtEnd]: the right-hand side, for a rail on the left) parts it
 * from the page, which a same-colored flat panel did not.
 */
@Composable
private fun HybridRail(state: CarUiState, actions: CarActions, palette: HybridPalette, edgeAtEnd: Boolean) {
    val nav = state.navApp()
    val media = state.app(state.activeMediaAppId) ?: state.apps.firstOrNull { it.kind == CarAppKind.MEDIA }
    val phone = state.apps.firstOrNull { it.kind == CarAppKind.PHONE }
    val notif = state.apps.firstOrNull { it.kind == CarAppKind.NOTIFICATIONS }
    fun on(app: CarAppEntry?) = app != null && state.screen == CarScreen.App(app.id)
    val tints = palette.railTints
    val edge: @Composable () -> Unit = { Box(Modifier.fillMaxHeight().width(1.dp).background(palette.line)) }
    Row(Modifier.fillMaxHeight().background(palette.card)) {
        if (!edgeAtEnd) edge()
        Column(
            Modifier.fillMaxHeight().width(116.dp).padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Clock()
            Text(today("EEEd"), style = RailLabel, color = palette.muted, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            // The tabs share the height there is: up to 88 dp each, and on a short screen they
            // shrink (dropping their labels first) instead of running off the rail.
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val tab = (maxHeight / 6).coerceIn(48.dp, 88.dp)
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
                    HybridTab("home", stringResource(R.string.dash_menu_home), state.screen == CarScreen.Dashboard, palette, tints?.getOrNull(0), tab) { actions.goHome() }
                    HybridTab("nav", nav?.label ?: stringResource(R.string.dash_maps), on(nav), palette, tints?.getOrNull(1), tab) { nav?.let(actions::openApp) ?: actions.openLauncher() }
                    HybridTab("media", media?.label ?: stringResource(R.string.media_fallback_label), on(media), palette, tints?.getOrNull(2), tab) { media?.let(actions::openApp) ?: actions.openLauncher() }
                    HybridTab("phone", phone?.label ?: stringResource(R.string.setup_app_entry_phone), on(phone), palette, tints?.getOrNull(3), tab) { phone?.let(actions::openApp) ?: actions.openLauncher() }
                    HybridTab("chat", stringResource(R.string.dash_messages), on(notif), palette, tints?.getOrNull(4), tab, badge = state.unseen > 0) { notif?.let(actions::openApp) ?: actions.openLauncher() }
                    HybridTab("apps", stringResource(R.string.dash_menu_apps), state.screen == CarScreen.Launcher, palette, tints?.getOrNull(5), tab) { actions.openLauncher() }
                }
            }
            Spacer(Modifier.height(8.dp))
            HybridMic(state.listening, palette) { actions.assistant() }
        }
        if (edgeAtEnd) edge()
    }
}

/**
 * One rail tab. Selected, it is a solid accent fill with the icon and label in the accent's own
 * text color: the old 28 % tint with accent-colored text on top was the faintest thing on screen,
 * for the one item that says where you are.
 */
@Composable
private fun HybridTab(kind: String, label: String, selected: Boolean, palette: HybridPalette, tint: Color?, height: Dp, badge: Boolean = false, onClick: () -> Unit) {
    val fill = when {
        !selected -> null
        tint != null -> Brush.linearGradient(listOf(tint, lerp(tint, palette.accent2, 0.35f)))
        else -> Brush.linearGradient(listOf(palette.accent, palette.accent2))
    }
    val fg = if (selected) palette.onAccent else palette.muted
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier.size(width = 98.dp, height = height).padding(vertical = 3.dp).clip(shape)
            .then(if (fill != null) Modifier.background(fill) else Modifier)
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HybridIcon(kind, fg, 30.dp)
            if (height >= 68.dp) {
                Spacer(Modifier.height(3.dp))
                Text(label, style = RailLabel, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp))
            }
        }
        // Something unseen: a dot at the icon's corner.
        if (badge) Box(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 24.dp).size(13.dp).clip(CircleShape).background(palette.warn).border(2.dp, palette.card, CircleShape))
    }
}

@Composable
private fun HybridMic(listening: Boolean, palette: HybridPalette, onClick: () -> Unit) {
    val ring = Brush.sweepGradient(listOf(palette.accent, palette.accent2, palette.warn, palette.bad, palette.accent))
    Box(
        Modifier.size(68.dp).clip(CircleShape).background(ring).padding(3.dp).clip(CircleShape)
            .background(if (listening) palette.accent else palette.card2).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        HybridIcon("mic", if (listening) palette.onAccent else palette.text, 28.dp)
    }
}

// ---------------------------------------------------------------- bottom bar

/**
 * The bottom bar: what's playing and its controls, the route, notifications, the phone's status.
 * Its top edge is the divider from the page and, while something plays, the playback progress.
 */
@Composable
private fun HybridBottomBar(state: CarUiState, actions: CarActions, palette: HybridPalette) {
    val np = state.nowPlaying
    val media = state.app(np?.appId ?: state.activeMediaAppId)
    val guidance = state.phoneGuidance
    val notif = state.apps.firstOrNull { it.kind == CarAppKind.NOTIFICATIONS }
    val navApp = state.navApp()
    Column(Modifier.fillMaxWidth().background(palette.card)) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(palette.line)) {
            if (np != null && np.durationMs > 0) {
                Box(Modifier.fillMaxHeight().fillMaxWidth((np.positionMs.toFloat() / np.durationMs).coerceIn(0f, 1f))
                    .background(Brush.horizontalGradient(listOf(palette.accent, palette.accent2))))
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(96.dp)) {
            // The route pill is the one thing a narrow head unit can do without: the rail's Maps tab is right there.
            val roomy = maxWidth >= 900.dp
            Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1.2f).clip(RoundedCornerShape(20.dp)).clickable { media?.let(actions::openApp) ?: actions.openLauncher() }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiniArt(np, palette, 68.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(np?.title ?: media?.label ?: stringResource(R.string.media_nothing_playing), style = MaterialTheme.typography.labelLarge, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val subtitle = np?.subtitle ?: if (media != null) stringResource(R.string.media_nothing_playing) else ""
                        if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.labelMedium, color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.width(8.dp))
                RoundIcon(Icons.Rounded.SkipPrevious, stringResource(R.string.media_action_previous), filled = false) { actions.mediaPrevious() }
                RoundIcon(if (np?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, stringResource(R.string.media_action_play_pause), filled = true) { actions.mediaPlayPause() }
                RoundIcon(Icons.Rounded.SkipNext, stringResource(R.string.media_action_next), filled = false) { actions.mediaNext() }
                if (roomy) {
                    Spacer(Modifier.width(14.dp))
                    val pill = RoundedCornerShape(22.dp)
                    Row(
                        Modifier.weight(1f).height(68.dp).clip(pill).background(palette.card2).border(1.dp, palette.line, pill)
                            .clickable { navApp?.let(actions::openApp) ?: actions.openLauncher() }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(palette.accent.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            HybridIcon("nav", if (palette.text.luminance() < 0.5f) palette.accent else lerp(palette.accent, Color.White, 0.25f), 22.dp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(guidance?.primary ?: state.trip?.let { stringResource(R.string.dash_on_route) } ?: stringResource(R.string.dash_no_route), style = MaterialTheme.typography.labelLarge, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (guidance?.secondary?.isNotEmpty() == true) {
                                Text(guidance.secondary, style = MaterialTheme.typography.labelMedium, color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Box(
                    Modifier.size(68.dp).clip(CircleShape).background(palette.card2).border(1.dp, palette.line, CircleShape)
                        .clickable(onClickLabel = stringResource(R.string.dash_messages)) { notif?.let(actions::openApp) ?: actions.openLauncher() },
                    contentAlignment = Alignment.Center,
                ) {
                    HybridIcon("bell", if (state.unseen > 0) palette.warn else palette.text, 28.dp)
                    if (state.unseen > 0) Box(Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 14.dp).size(13.dp).clip(CircleShape).background(palette.warn).border(2.dp, palette.card2, CircleShape))
                }
                Spacer(Modifier.width(12.dp))
                PhoneStatusIcons()
            }
        }
    }
}

@Composable
internal fun MiniArt(np: NowPlaying?, palette: HybridPalette, size: Dp = 56.dp) {
    val shape = RoundedCornerShape(size * 0.22f)
    if (np?.art != null) {
        Image(np.art.asImageBitmap(), null, Modifier.size(size).clip(shape), contentScale = ContentScale.Crop)
    } else {
        Box(Modifier.size(size).clip(shape).background(palette.card2), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MusicNote, null, tint = palette.muted, modifier = Modifier.size(size * 0.45f))
        }
    }
}

// ---------------------------------------------------------------- dashboard

@Composable
private fun HybridDashboard(state: CarUiState, actions: CarActions, navMap: @Composable (CarAppEntry, Modifier) -> Unit, palette: HybridPalette, modifier: Modifier) {
    // Starts in the view the "Home layout" setting picks, like the Dusk layout's Home.
    var mapMusic by remember(state.homeMapMusicDefault) { mutableStateOf(state.homeMapMusicDefault) }
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greeting(), style = MaterialTheme.typography.headlineSmall, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(today("EEEEdMMMM"), style = MaterialTheme.typography.labelLarge, color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (state.weather != null) {
                val pill = RoundedCornerShape(28.dp)
                Box(Modifier.height(56.dp).clip(pill).background(palette.card).border(1.dp, palette.line, pill).padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                    WeatherChip(state.weather)
                }
                Spacer(Modifier.width(12.dp))
            }
            HybridSegmented(mapMusic, palette) { mapMusic = it }
        }
        // One Row for both views, the map first in each: toggling changes its width, not its
        // place in the composition, so the live map surface isn't torn down and rebuilt.
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            NavCard(state, actions, navMap, Modifier.weight(if (mapMusic) 1.8f else 1.55f).fillMaxHeight())
            if (mapMusic) {
                MediaCard(state, actions, compact = false, modifier = Modifier.weight(1f).fillMaxHeight())
            } else {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    MediaCard(state, actions, compact = false, modifier = Modifier.weight(1.3f).fillMaxWidth())
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        HybridSuggested(state, actions, palette, Modifier.weight(1f).fillMaxHeight())
                        RecentCard(state, actions, palette, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
private fun HybridSegmented(mapMusic: Boolean, palette: HybridPalette, onChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(Modifier.height(56.dp).clip(shape).background(palette.card).border(1.dp, palette.line, shape).padding(4.dp)) {
        SegmentChip(stringResource(R.string.setup_layout_full), !mapMusic, palette) { onChange(false) }
        SegmentChip(stringResource(R.string.setup_quick_map_music_title), mapMusic, palette) { onChange(true) }
    }
}

@Composable
private fun SegmentChip(label: String, selected: Boolean, palette: HybridPalette, onClick: () -> Unit) {
    val fg = if (selected) palette.onAccent else palette.muted
    Box(
        Modifier.fillMaxHeight().clip(RoundedCornerShape(24.dp))
            .then(if (selected) Modifier.background(Brush.linearGradient(listOf(palette.accent, palette.accent2))) else Modifier)
            .clickable(onClick = onClick).padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1) }
}

/**
 * Real shortcuts, one tap each: navigate home, navigate to work, the phone app. Round buttons in a
 * row rather than a list: three list rows at car text sizes didn't fit the card without scrolling.
 */
@Composable
private fun HybridSuggested(state: CarUiState, actions: CarActions, palette: HybridPalette, modifier: Modifier) {
    val phone = state.apps.firstOrNull { it.kind == CarAppKind.PHONE }
    Card(modifier) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(stringResource(R.string.dash_suggested), style = MaterialTheme.typography.labelLarge, color = palette.muted, maxLines = 1)
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Shortcut("home", stringResource(R.string.dash_menu_home), palette) { actions.navigateTo("Home") }
                Shortcut("work", stringResource(R.string.dash_work), palette) { actions.navigateTo("Work") }
                phone?.let { Shortcut("phone", it.label, palette) { actions.openApp(it) } }
            }
        }
    }
}

@Composable
private fun Shortcut(kind: String, label: String, palette: HybridPalette, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClickLabel = label, onClick = onClick).padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(66.dp).clip(CircleShape).background(palette.card2).border(1.dp, palette.line, CircleShape), contentAlignment = Alignment.Center) {
            HybridIcon(kind, if (palette.text.luminance() < 0.5f) palette.accent else lerp(palette.accent, Color.White, 0.25f), 28.dp)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The last couple of notifications, or an empty state, with a count of the unseen ones. */
@Composable
private fun RecentCard(state: CarUiState, actions: CarActions, palette: HybridPalette, modifier: Modifier) {
    val recent = state.history.take(2)
    Card(modifier, onClick = { state.apps.firstOrNull { it.kind == CarAppKind.NOTIFICATIONS }?.let(actions::openApp) }) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.dash_messages), style = MaterialTheme.typography.labelLarge, color = palette.muted, maxLines = 1, modifier = Modifier.weight(1f))
                if (state.unseen > 0) {
                    Box(Modifier.clip(CircleShape).background(palette.warn).padding(horizontal = 12.dp, vertical = 1.dp)) {
                        Text("${state.unseen}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF1A1200))
                    }
                }
            }
            if (recent.isEmpty()) {
                Text(stringResource(R.string.dash_no_messages), style = MaterialTheme.typography.bodyMedium, color = palette.muted)
            } else {
                recent.forEach { n ->
                    Text(
                        "${n.title}: ${if (state.drivingRestricted && n.message) stringResource(net.prototux.aare.R.string.notif_new_message_placeholder) else n.text}",
                        style = MaterialTheme.typography.bodyMedium, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** "Good morning/afternoon/evening" by the phone's clock, in the phone's language. */
@Composable
internal fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return stringResource(when {
        hour < 12 -> R.string.dash_greeting_morning
        hour < 18 -> R.string.dash_greeting_afternoon
        else -> R.string.dash_greeting_evening
    })
}

// ---------------------------------------------------------------- icons

/**
 * Stroke-only icons matching `hybrid.html`'s own inline SVGs (same path data, same 24x24 viewBox,
 * same 1.8 stroke width) — simple enough to redraw directly rather than approximate with a
 * differently-shaped Material icon.
 */
@Composable
internal fun HybridIcon(kind: String, tint: Color, sizeDp: Dp) {
    val strokePx = with(LocalDensity.current) { 1.8.dp.toPx() }
    // Parsed once per icon, not on every draw pass: these sit in the always-visible rail and bar.
    val parsed = remember { HashMap<String, androidx.compose.ui.graphics.Path>() }
    Canvas(Modifier.size(sizeDp)) {
        val s = size.width / 24f
        if (s <= 0f) return@Canvas
        scale(s, s, pivot = Offset.Zero) {
            val stroke = Stroke(width = strokePx / s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            fun path(d: String) = drawPath(parsed.getOrPut(d) { PathParser().parsePathString(d).toPath() }, tint, style = stroke)
            when (kind) {
                "home" -> path("M3 11l9-8 9 8v9a1 1 0 01-1 1h-5v-6H9v6H4a1 1 0 01-1-1z")
                "nav" -> path("M12 2l8 19-8-4-8 4z")
                "media" -> { drawCircle(tint, radius = 9f, center = Offset(12f, 12f), style = stroke); path("M10 8.5v7l6-3.5z") }
                "phone" -> path("M5 4h4l2 5-2.5 1.5a11 11 0 005 5L15 13l5 2v4a2 2 0 01-2 2A16 16 0 013 6a2 2 0 012-2z")
                "chat" -> path("M4 5h16v11H9l-5 4z")
                "apps" -> listOf(4f to 4f, 14f to 4f, 4f to 14f, 14f to 14f).forEach { (x, y) ->
                    drawRoundRect(tint, topLeft = Offset(x, y), size = Size(6f, 6f), cornerRadius = CornerRadius(1.5f), style = stroke)
                }
                "mic" -> { drawRoundRect(tint, topLeft = Offset(9f, 3f), size = Size(6f, 12f), cornerRadius = CornerRadius(3f), style = stroke); path("M5 11a7 7 0 0014 0M12 18v3") }
                "bell" -> { path("M6 16V11a6 6 0 0112 0v5l2 2H4z"); path("M10 21h4") }
                "work" -> { path("M4 8h16v11H4z"); path("M9 8V5h6v3"); path("M4 13h16") }
            }
        }
    }
}
