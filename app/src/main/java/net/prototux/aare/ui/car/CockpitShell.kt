package net.prototux.aare.ui.car

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.prototux.aare.R
import net.prototux.aare.host.templates.CarFormat
import net.prototux.aare.ui.theme.LocalCarDimens

/*
 * The Cockpit layout: the Stitch design "Automotive Cockpit OS". Dark-first and glare-suppressed:
 * an icon-only rail with the clock and status on top and voice at the bottom, a top bar of live
 * pills (the next turn, the track playing), a map taking 60 % of Home next to a media card, apps
 * as bordered cards. Tonal tiers with 1 px micro-borders instead of shadows or blur; an electric
 * blue for what is active, emerald and amber for status; 64 dp touch targets and up.
 */

// ---------------------------------------------------------------- tokens

/** Core Signal Blue: active tabs, primary buttons, route and scrubber states. */
private val SignalBlue = Color(0xFF388BFD)
private val Emerald = Color(0xFF34D399)
private val Amber = Color(0xFFF59E0B)
private val Coral = Color(0xFFEF4444)
/** Micro-borders: neutral cards, then active ones. */
private val Border = Color.White.copy(alpha = 0.08f)
private val BorderActive = Color.White.copy(alpha = 0.16f)

private val CockpitDark: ColorScheme = darkColorScheme(
    primary = Color(0xFFAAC7FF), onPrimary = Color(0xFF002F65),
    primaryContainer = Color(0xFF418FFF), onPrimaryContainer = Color(0xFF002959),
    inversePrimary = Color(0xFF005CBA),
    secondary = Color(0xFF45DFA4), onSecondary = Color(0xFF003825),
    secondaryContainer = Color(0xFF00BD85), onSecondaryContainer = Color(0xFF00452E),
    tertiary = Color(0xFFFFB95F), onTertiary = Color(0xFF472A00),
    tertiaryContainer = Color(0xFFCA8100), onTertiaryContainer = Color(0xFF3E2400),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF10131A), onBackground = Color(0xFFE0E2EC),
    surface = Color(0xFF10131A), onSurface = Color(0xFFE0E2EC),
    surfaceVariant = Color(0xFF32353C), onSurfaceVariant = Color(0xFFC1C6D6),
    surfaceTint = Color(0xFFAAC7FF),
    inverseSurface = Color(0xFFE0E2EC), inverseOnSurface = Color(0xFF2D3038),
    outline = Color(0xFF8B919F), outlineVariant = Color(0xFF414753),
    surfaceDim = Color(0xFF10131A), surfaceBright = Color(0xFF363941),
    surfaceContainerLowest = Color(0xFF0B0E15), surfaceContainerLow = Color(0xFF161A23),
    surfaceContainer = Color(0xFF1E232E), surfaceContainerHigh = Color(0xFF262C3A),
    surfaceContainerHighest = Color(0xFF32353C),
)

/** Daylight: the same signal colors on light matte tiers. */
private val CockpitLight: ColorScheme = lightColorScheme(
    primary = Color(0xFF005CBA), onPrimary = Color.White,
    primaryContainer = Color(0xFF388BFD), onPrimaryContainer = Color.White,
    secondary = Color(0xFF006C4C), onSecondary = Color.White,
    secondaryContainer = Color(0xFF68FCBF), onSecondaryContainer = Color(0xFF002114),
    tertiary = Color(0xFF855300), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB8), onTertiaryContainer = Color(0xFF2A1700),
    error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7F8FC), onBackground = Color(0xFF181C22),
    surface = Color(0xFFF7F8FC), onSurface = Color(0xFF181C22),
    surfaceVariant = Color(0xFFDFE2EC), onSurfaceVariant = Color(0xFF414753),
    outline = Color(0xFF717785), outlineVariant = Color(0xFFC1C6D6),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF0F2F8),
    surfaceContainer = Color(0xFFEAEDF4), surfaceContainerHigh = Color(0xFFE2E6EE),
    surfaceContainerHighest = Color(0xFFDADEE8),
)

/** Headlines and telemetry heavy and tight; labels with positive tracking (the design's Plus Jakarta Sans scale). */
private object CkType {
    val display = TextStyle(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).em)
    val headlineMd = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em)
    val headlineSm = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
    val bodyLg = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium)
    val labelLg = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.02.em)
    val labelMd = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.04.em)
    val labelSm = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em)
}

// ---------------------------------------------------------------- shell

@Composable
internal fun CockpitShell(
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    overlay: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = if (state.night) CockpitDark else CockpitLight
    net.prototux.aare.ui.theme.CarColors(scheme) {
        CompositionLocalProvider(LocalCarDimens provides LocalCarDimens.current.copy(cardRadius = 24.dp, gutter = 16.dp)) {
            Box(Modifier.fillMaxSize().background(scheme.surfaceContainerLowest).then(modifier)) {
                Row(Modifier.fillMaxSize()) {
                    if (!state.railOnRight) CkRail(state, actions)
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        CkTopBar(state)
                        Box(Modifier.weight(1f).fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                            CkContent(state, actions, appContent, navMap)
                            overlay(Modifier.align(Alignment.BottomCenter))
                            NoticeOverlay(state, actions, Modifier.align(Alignment.TopCenter))
                            CallOverlay(state, actions, Modifier.align(Alignment.TopCenter))
                        }
                    }
                    if (state.railOnRight) CkRail(state, actions)
                }
            }
        }
    }
}

@Composable
private fun CkContent(
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
) {
    val screen = state.screen
    val np = state.nowPlaying
    val mediaApp = state.app(np?.appId ?: state.activeMediaAppId)
    val showMini = np != null && screen is CarScreen.App && screen.appId != mediaApp?.id
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (screen) {
                CarScreen.Dashboard -> CkHome(state, actions, navMap, Modifier.fillMaxSize())
                CarScreen.Launcher -> CkAppsHub(state, actions, Modifier.fillMaxSize())
                is CarScreen.App -> {
                    val app = state.app(screen.appId)
                    if (app != null) {
                        val shape = RoundedCornerShape(24.dp)
                        Box(Modifier.fillMaxSize().clip(shape).background(MaterialTheme.colorScheme.surface).border(1.dp, Border, shape)) {
                            appContent(app, Modifier.fillMaxSize())
                        }
                    } else CkHome(state, actions, navMap, Modifier.fillMaxSize())
                }
            }
        }
        if (showMini) CkPlayerBar(state, actions)
    }
}

// ---------------------------------------------------------------- rail

@Composable
private fun CkRail(state: CarUiState, actions: CarActions) {
    val scheme = MaterialTheme.colorScheme
    val nav = state.navApp()
    val media = state.app(state.activeMediaAppId) ?: state.apps.firstOrNull { it.kind == CarAppKind.MEDIA }
    val phone = state.apps.firstOrNull { it.kind == CarAppKind.PHONE }
    val settings = state.apps.firstOrNull { it.kind == CarAppKind.SETTINGS }
    fun on(app: CarAppEntry?) = app != null && state.screen == CarScreen.App(app.id)
    Column(
        Modifier.fillMaxHeight().width(104.dp).background(scheme.surfaceContainerLowest).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The clock and the phone's status at the top, as in the design's status column.
        CkClock(CkType.labelLg.copy(fontSize = 18.sp), scheme.primary)
        Spacer(Modifier.height(6.dp))
        PhoneStatusIcons()
        Spacer(Modifier.height(16.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val (tab, fits) = railTabs(maxHeight, count = 6, min = 56.dp, max = 88.dp)
            val column = if (fits) Modifier.fillMaxSize() else Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            Column(column, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = if (fits) Arrangement.Center else Arrangement.Top) {
                CkTab(Icons.Rounded.Home, stringResource(R.string.dash_menu_home), state.screen == CarScreen.Dashboard, tab) { actions.goHome() }
                CkTab(Icons.Rounded.Navigation, stringResource(R.string.dash_maps), on(nav), tab) { nav?.let(actions::openApp) ?: actions.openLauncher() }
                CkTab(Icons.Rounded.MusicNote, stringResource(R.string.m3_tab_media), on(media), tab) { media?.let(actions::openApp) ?: actions.openLauncher() }
                CkTab(Icons.Rounded.Call, stringResource(R.string.setup_app_entry_phone), on(phone), tab) { phone?.let(actions::openApp) ?: actions.openLauncher() }
                CkTab(Icons.Rounded.Apps, stringResource(R.string.dash_menu_apps), state.screen == CarScreen.Launcher, tab) { actions.openLauncher() }
                CkTab(Icons.Rounded.Tune, stringResource(R.string.m3_tab_settings), on(settings), tab) { settings?.let(actions::openApp) ?: actions.openLauncher() }
            }
        }
        Spacer(Modifier.height(16.dp))
        // Voice: emerald on a raised pill (green means "ready" in this design's status palette).
        val shape = RoundedCornerShape(28.dp)
        Box(
            Modifier.size(width = 80.dp, height = 64.dp).clip(shape)
                .background(if (state.listening) Emerald else scheme.surfaceContainerHigh).border(1.dp, Border, shape)
                .carFocusHighlight(shape).clickable { actions.assistant() },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Mic, null, tint = if (state.listening) Color(0xFF003825) else Emerald, modifier = Modifier.size(30.dp)) }
    }
}

@Composable
private fun CkTab(icon: ImageVector, label: String, selected: Boolean, height: Dp, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier.size(width = 84.dp, height = height).padding(vertical = 4.dp).clip(shape)
            .then(if (selected) Modifier.background(SignalBlue).border(1.dp, Color.White.copy(alpha = 0.20f), shape) else Modifier)
            .carFocusHighlight(shape).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = if (selected) Color.White else scheme.onSurfaceVariant, modifier = Modifier.size(30.dp)) }
}

@Composable
private fun CkClock(style: TextStyle, color: Color) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000 - now % 1_000 + 5)
        }
    }
    val context = LocalContext.current
    val pattern = if (android.text.format.DateFormat.is24HourFormat(context)) "H:mm" else "h:mm"
    Text(android.text.format.DateFormat.format(pattern, now).toString(), style = style, color = color, maxLines = 1)
}

// ---------------------------------------------------------------- top bar

/** "250 m · Turn right onto Test Street" from the hosted app's trip, or a phone navigation app's directions. */
private fun nextTurn(state: CarUiState): String? {
    state.phoneGuidance?.let { g -> return listOf(g.primary, g.secondary).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { null } }
    val trip = state.trip ?: return null
    val step = trip.steps.firstOrNull()
    val distance = trip.stepTravelEstimates.firstOrNull()?.remainingDistance?.let { CarFormat.distance(it.displayDistance, it.displayUnit) }
    val what = (step?.cue ?: step?.road ?: trip.currentRoad)?.toString()?.takeIf { it.isNotBlank() }
    return listOfNotNull(distance, what).joinToString(" · ").ifBlank { null }
}

@Composable
private fun CkTopBar(state: CarUiState) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val phoneName = remember {
        runCatching { android.provider.Settings.Global.getString(context.contentResolver, "device_name") }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: Build.MODEL.orEmpty()
    }
    Row(
        Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(SignalBlue), contentAlignment = Alignment.Center) {
            Text("F", style = CkType.headlineSm.copy(fontWeight = FontWeight.ExtraBold), color = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Text("F-Auto OS", style = CkType.headlineMd.copy(fontWeight = FontWeight.Bold), color = scheme.onSurface, maxLines = 1)
        Spacer(Modifier.width(16.dp))
        // Live pills: what the driver wants at a glance without leaving the screen they're on.
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            nextTurn(state)?.let { CkPill(Icons.Rounded.NearMe, Emerald, stringResource(R.string.cockpit_next, it)) }
            state.nowPlaying?.let { np ->
                val text = listOf(np.subtitle, np.title).filter { it.isNotBlank() }.joinToString(" - ")
                if (text.isNotBlank()) CkPill(Icons.Rounded.GraphicEq, SignalBlue, text)
            }
            state.weather?.let { Box(Modifier.height(44.dp).clip(CircleShape).background(scheme.surfaceContainer).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) { WeatherChip(it) } }
        }
        Spacer(Modifier.width(16.dp))
        Text(phoneName, style = CkType.labelLg, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp))
    }
}

@Composable
private fun CkPill(icon: ImageVector, tint: Color, text: String) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.height(44.dp).clip(CircleShape).background(scheme.surfaceContainer).border(1.dp, Border, CircleShape).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = CkType.labelLg, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 360.dp))
    }
}

// ---------------------------------------------------------------- home

@Composable
private fun CkHome(state: CarUiState, actions: CarActions, navMap: @Composable (CarAppEntry, Modifier) -> Unit, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        when {
            // Small screens: the map takes the width (its app's cards need it), media a bar under it.
            maxWidth < 700.dp && maxHeight <= maxWidth -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NavCard(state, actions, navMap, Modifier.weight(1f).fillMaxWidth())
                CkPlayerBar(state, actions, alwaysShown = true)
            }
            // Portrait center stacks: turn-by-turn on top, media below.
            maxHeight > maxWidth -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NavCard(state, actions, navMap, Modifier.weight(1.5f).fillMaxWidth())
                CkMediaCard(state, actions, Modifier.weight(1f).fillMaxWidth())
            }
            // Navigation keeps its focal viewport: 60 % of the width.
            else -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NavCard(state, actions, navMap, Modifier.weight(3f).fillMaxHeight())
                CkMediaCard(state, actions, Modifier.weight(2f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun CkMediaCard(state: CarUiState, actions: CarActions, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val np = state.nowPlaying
    val media = state.app(np?.appId ?: state.activeMediaAppId)
    val shape = RoundedCornerShape(24.dp)
    // A faint blue rise toward the top: depth by tone, not by shadow.
    val wash = Brush.verticalGradient(listOf(lerpColor(scheme.surfaceContainer, SignalBlue, 0.10f), scheme.surfaceContainer))
    BoxWithConstraints(
        modifier.clip(shape).background(wash).border(1.dp, Border, shape)
            .clickable { media?.let(actions::openApp) ?: actions.openLauncher() }.padding(20.dp),
    ) {
        val roomy = maxHeight >= 440.dp
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (roomy && media != null) {
                Row(
                    Modifier.align(Alignment.Start).height(40.dp).clip(CircleShape).border(1.dp, BorderActive, CircleShape).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.GraphicEq, null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(media.label.uppercase(), style = CkType.labelMd, color = scheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(16.dp))
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val artShape = RoundedCornerShape(20.dp)
                val art = np?.art
                if (art != null) {
                    Image(art.asImageBitmap(), null, Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true).clip(artShape),
                        contentScale = ContentScale.Crop)
                } else {
                    Box(
                        Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true).clip(artShape).background(scheme.surfaceContainerHigh)
                            .border(1.dp, Border, artShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.MusicNote, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(48.dp)) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(np?.title ?: (media?.label ?: stringResource(R.string.media_fallback_label)), style = CkType.display,
                color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(np?.subtitle?.takeIf { it.isNotBlank() } ?: stringResource(R.string.media_nothing_playing), style = CkType.headlineSm.copy(fontWeight = FontWeight.Medium),
                color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            if (np != null && np.durationMs > 0) {
                Spacer(Modifier.height(12.dp))
                CkProgress(np)
            }
            Spacer(Modifier.height(12.dp))
            CkTransport(np?.playing == true, actions, big = roomy)
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t, green = a.green + (b.green - a.green) * t, blue = a.blue + (b.blue - a.blue) * t, alpha = 1f,
)

@Composable
private fun CkProgress(np: NowPlaying) {
    val scheme = MaterialTheme.colorScheme
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(np) {
        while (np.playing) {
            now = System.currentTimeMillis()
            delay(500)
        }
    }
    val pos = (np.positionMs + if (np.playing) (now - np.updatedAtMs).coerceAtLeast(0) else 0).coerceIn(0, np.durationMs)
    val fraction = pos.toFloat() / np.durationMs
    Column(Modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(scheme.surfaceContainerHighest))
            Box(Modifier.fillMaxWidth(fraction).height(12.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(SignalBlue, scheme.primary))))
            Box(Modifier.offset(x = (maxWidth - 28.dp) * fraction).size(28.dp).clip(CircleShape).background(scheme.primary).border(4.dp, SignalBlue.copy(alpha = 0.35f), CircleShape))
        }
        Row(Modifier.fillMaxWidth()) {
            Text(time(pos), style = CkType.labelMd, color = scheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("-" + time(np.durationMs - pos), style = CkType.labelMd, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CkTransport(playing: Boolean, actions: CarActions, big: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        CkSquareButton(Icons.Rounded.SkipPrevious) { actions.mediaPrevious() }
        val size = if (big) 88.dp else 72.dp
        Box(
            Modifier.size(size).clip(CircleShape).background(scheme.primary).carFocusHighlight(CircleShape).clickable { actions.mediaPlayPause() },
            contentAlignment = Alignment.Center,
        ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = scheme.onPrimary, modifier = Modifier.size(42.dp)) }
        CkSquareButton(Icons.Rounded.SkipNext) { actions.mediaNext() }
    }
}

/** The design's secondary control: a 64 dp squircle on the deep container fill, micro-bordered. */
@Composable
private fun CkSquareButton(icon: ImageVector, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.size(64.dp).clip(shape).background(scheme.surfaceContainerHigh).border(1.dp, Border, shape)
            .carFocusHighlight(shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = scheme.onSurface, modifier = Modifier.size(32.dp)) }
}

// ---------------------------------------------------------------- player bar

/** Under an app, and on small screens' Home ([alwaysShown]: also with nothing playing). */
@Composable
private fun CkPlayerBar(state: CarUiState, actions: CarActions, alwaysShown: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val np = state.nowPlaying
    if (np == null && !alwaysShown) return
    val media = state.app(np?.appId ?: state.activeMediaAppId)
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().height(80.dp).clip(shape).background(scheme.surfaceContainer).border(1.dp, Border, shape)
            .clickable { media?.let(actions::openApp) ?: actions.openLauncher() }.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val art = np?.art
        if (art != null) Image(art.asImageBitmap(), null, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
        else Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(scheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MusicNote, null, tint = scheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f)) {
            Text(np?.title ?: (media?.label ?: stringResource(R.string.media_fallback_label)), style = CkType.headlineSm, color = scheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(np?.subtitle?.takeIf { it.isNotBlank() } ?: stringResource(R.string.media_nothing_playing), style = CkType.labelMd,
                color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        CkSquareButton(Icons.Rounded.SkipPrevious) { actions.mediaPrevious() }
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(scheme.primary).carFocusHighlight(CircleShape).clickable { actions.mediaPlayPause() },
            contentAlignment = Alignment.Center,
        ) { Icon(if (np?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = scheme.onPrimary, modifier = Modifier.size(32.dp)) }
        CkSquareButton(Icons.Rounded.SkipNext) { actions.mediaNext() }
    }
}

// ---------------------------------------------------------------- apps hub

@Composable
private fun CkAppsHub(state: CarUiState, actions: CarActions, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    var filter by remember { mutableStateOf(M3Category.ALL) }
    val shown = state.apps.filter { filter == M3Category.ALL || categoryOf(it) == filter }
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CkChip(Icons.Rounded.GridView, stringResource(R.string.m3_apps_all), filter == M3Category.ALL) { filter = M3Category.ALL }
                CkChip(Icons.Rounded.Navigation, stringResource(R.string.m3_apps_navigation), filter == M3Category.NAVIGATION) { filter = M3Category.NAVIGATION }
                CkChip(Icons.Rounded.Headphones, stringResource(R.string.m3_apps_media), filter == M3Category.MEDIA) { filter = M3Category.MEDIA }
                CkChip(Icons.Rounded.ChatBubble, stringResource(R.string.m3_apps_communication), filter == M3Category.COMMUNICATION) { filter = M3Category.COMMUNICATION }
                CkChip(Icons.Rounded.Tune, stringResource(R.string.m3_apps_vehicle), filter == M3Category.VEHICLE) { filter = M3Category.VEHICLE }
            }
            Spacer(Modifier.width(12.dp))
            // Connection status pill: connected to the head unit, and how many apps are ready.
            Row(
                Modifier.height(48.dp).clip(CircleShape).background(scheme.surfaceContainerLow).border(1.dp, Border, CircleShape).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Emerald))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.cockpit_apps_ready, state.apps.size), style = CkType.labelLg, color = scheme.onSurface, maxLines = 1)
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(168.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(shown, key = { it.id }) { app -> CkAppCard(state, app) { actions.openApp(app) } }
        }
        Spacer(Modifier.height(16.dp))
        val bar = RoundedCornerShape(16.dp)
        Row(
            Modifier.fillMaxWidth().clip(bar).background(scheme.surfaceContainerLow).border(1.dp, Border, bar).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.height(56.dp).clip(CircleShape).background(scheme.surfaceContainerHigh).carFocusHighlight(CircleShape)
                    .clickable { actions.goHome() }.padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = scheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.m3_apps_back_to_dashboard), style = CkType.labelLg, color = scheme.onSurface)
            }
        }
    }
}

@Composable
private fun CkChip(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.height(48.dp).clip(CircleShape)
            .background(if (selected) SignalBlue else scheme.surfaceContainerLow)
            .border(1.dp, if (selected) Color.White.copy(alpha = 0.20f) else Border, CircleShape)
            .carFocusHighlight(CircleShape).clickable(onClick = onClick).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (selected) Color.White else scheme.onSurface
        Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = CkType.labelLg, color = fg, maxLines = 1)
    }
}

@Composable
private fun CkAppCard(state: CarUiState, app: CarAppEntry, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val accent = when (categoryOf(app)) {
        M3Category.NAVIGATION -> SignalBlue
        M3Category.MEDIA -> Amber
        M3Category.COMMUNICATION -> Emerald
        else -> scheme.outline
    }
    val playing = state.nowPlaying?.let { it.playing && it.appId == app.id } == true
    val unread = if (app.kind == CarAppKind.NOTIFICATIONS) state.unseen else 0
    val (status, statusColor) = when {
        playing -> stringResource(R.string.m3_apps_playing) to Emerald
        unread > 0 -> stringResource(R.string.m3_apps_new, unread) to Coral
        app.isNav && app.id == state.activeNavAppId && (state.trip != null || state.phoneGuidance != null) -> stringResource(R.string.m3_apps_guiding) to Emerald
        // Otherwise what kind of app it is, as the design's context lines do; nothing for the rest.
        else -> when (categoryOf(app)) {
            M3Category.NAVIGATION -> stringResource(R.string.m3_apps_navigation)
            M3Category.MEDIA -> stringResource(R.string.m3_apps_media)
            M3Category.COMMUNICATION -> stringResource(R.string.m3_apps_communication)
            M3Category.VEHICLE -> stringResource(R.string.m3_apps_vehicle)
            M3Category.ALL -> ""
        } to scheme.onSurfaceVariant
    }
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(scheme.surfaceContainer).border(1.dp, if (playing) BorderActive else Border, shape)
            .carFocusHighlight(shape).clickable(onClick = onClick).padding(vertical = 18.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(Modifier.size(80.dp).clip(RoundedCornerShape(18.dp)).background(accent.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                val icon = app.icon
                if (icon != null) Image(icon.asImageBitmap(), app.label, Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
                else Text(app.label.take(1), style = CkType.headlineMd, color = scheme.onSurface)
            }
            when {
                playing -> Box(Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp).size(22.dp).clip(CircleShape).background(Emerald)
                    .border(3.dp, scheme.surfaceContainer, CircleShape))
                unread > 0 -> Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp).size(28.dp).clip(CircleShape).background(scheme.errorContainer),
                    contentAlignment = Alignment.Center,
                ) { Text(unread.coerceAtMost(99).toString(), style = CkType.labelSm, color = scheme.onErrorContainer) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(app.label, style = CkType.headlineSm, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(status.uppercase(), style = CkType.labelSm, color = statusColor, maxLines = 1)
    }
}
