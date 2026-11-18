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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Smartphone
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.prototux.aare.R
import net.prototux.aare.ui.theme.LocalCarDimens

/*
 * The Material layout: Material 3 "Obsidian" for the car (the Stitch design "F-Auto Material 3").
 * A labelled icon rail (Home, Maps, Media, Phone, Apps, Settings, voice), a status bar (weather,
 * phone, clock, notifications), and the screen in one tonal panel: map + media cards on Home, a
 * categorised grid for Apps, any app full size with a mini player under it. Tonal layers instead
 * of shadows or blur, 24 dp cards, pill controls, touch targets of 64 dp and more.
 */

// ---------------------------------------------------------------- tokens

/** The design's dark scheme (Obsidian): the default, and the one at night. */
private val ObsidianDark: ColorScheme = darkColorScheme(
    primary = Color(0xFFD3E2FF), onPrimary = Color(0xFF0A315B),
    primaryContainer = Color(0xFFA8C7FA), onPrimaryContainer = Color(0xFF0F2038),
    inversePrimary = Color(0xFF405F8C),
    secondary = Color(0xFF80CFFF), onSecondary = Color(0xFF00344B),
    secondaryContainer = Color(0xFF00729E), onSecondaryContainer = Color(0xFFDAEFFF),
    tertiary = Color(0xFF92F4DA), onTertiary = Color(0xFF00382D),
    tertiaryContainer = Color(0xFF76D7BE), onTertiaryContainer = Color(0xFF005D4D),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121316), onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF121316), onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF343538), onSurfaceVariant = Color(0xFFC3C6D0),
    surfaceTint = Color(0xFFA9C8FB),
    inverseSurface = Color(0xFFE3E2E6), inverseOnSurface = Color(0xFF2F3034),
    outline = Color(0xFF8D9199), outlineVariant = Color(0xFF43474F),
    surfaceDim = Color(0xFF121316), surfaceBright = Color(0xFF38393C),
    surfaceContainerLowest = Color(0xFF0D0E11), surfaceContainerLow = Color(0xFF1B1B1F),
    surfaceContainer = Color(0xFF1F1F23), surfaceContainerHigh = Color(0xFF292A2D),
    surfaceContainerHighest = Color(0xFF343538),
)

/** The daylight variant ("High Sunlight Contrast"): same hues, light tonal surfaces. */
private val ObsidianLight: ColorScheme = lightColorScheme(
    primary = Color(0xFF405F8C), onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E3FF), onPrimaryContainer = Color(0xFF001C3B),
    inversePrimary = Color(0xFFA9C8FB),
    secondary = Color(0xFF00668A), onSecondary = Color.White,
    secondaryContainer = Color(0xFFC5E7FF), onSecondaryContainer = Color(0xFF001E2D),
    tertiary = Color(0xFF006B5A), onTertiary = Color.White,
    tertiaryContainer = Color(0xFF93F4DB), onTertiaryContainer = Color(0xFF002019),
    error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF9F9FC), onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF9F9FC), onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE0E2EC), onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF74777F), outlineVariant = Color(0xFFC3C6CF),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF3F3F6),
    surfaceContainer = Color(0xFFEDEDF0), surfaceContainerHigh = Color(0xFFE7E8EB),
    surfaceContainerHighest = Color(0xFFE2E2E5),
)

/** The design's type scale (Roboto, weights 500+ for legibility at arm's length). */
private object M3Type {
    val headlineMd = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold)
    val headlineSm = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium)
    val titleLg = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium)
    val titleMd = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
    val bodyLg = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal)
    val labelLg = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
    val labelMd = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
}

/** 1 px keylines on level 2+ cards, so their edge holds up in sun glare. */
private val Keyline = Color.White.copy(alpha = 0.07f)

// ---------------------------------------------------------------- shell

@Composable
internal fun MaterialShell(
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    overlay: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = if (state.night) ObsidianDark else ObsidianLight
    net.prototux.aare.ui.theme.CarColors(scheme) {
        CompositionLocalProvider(LocalCarDimens provides LocalCarDimens.current.copy(cardRadius = 24.dp, gutter = 16.dp)) {
            Box(Modifier.fillMaxSize().background(scheme.surface).then(modifier)) {
                Row(Modifier.fillMaxSize()) {
                    if (!state.railOnRight) M3Rail(state, actions)
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        M3TopBar(state, actions)
                        // The screen sits in one tonal panel (level 1), its cards a level above.
                        Box(
                            Modifier.weight(1f).fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                .clip(RoundedCornerShape(28.dp)).background(scheme.surfaceContainerLow),
                        ) {
                            M3Content(state, actions, appContent, navMap)
                            overlay(Modifier.align(Alignment.BottomCenter))
                            NoticeOverlay(state, actions, Modifier.align(Alignment.TopCenter))
                            CallOverlay(state, actions, Modifier.align(Alignment.TopCenter))
                        }
                    }
                    if (state.railOnRight) M3Rail(state, actions)
                }
            }
        }
    }
}

@Composable
private fun M3Content(
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
) {
    val screen = state.screen
    val np = state.nowPlaying
    val mediaApp = state.app(np?.appId ?: state.activeMediaAppId)
    val showMini = np != null && screen is CarScreen.App && screen.appId != mediaApp?.id
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (screen) {
                CarScreen.Dashboard -> M3Home(state, actions, navMap, Modifier.fillMaxSize())
                CarScreen.Launcher -> M3AppsHub(state, actions, Modifier.fillMaxSize())
                is CarScreen.App -> {
                    val app = state.app(screen.appId)
                    if (app != null) Box(Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))) { appContent(app, Modifier.fillMaxSize()) }
                    else M3Home(state, actions, navMap, Modifier.fillMaxSize())
                }
            }
        }
        if (showMini) M3MiniPlayer(state, actions)
    }
}

// ---------------------------------------------------------------- rail

@Composable
private fun M3Rail(state: CarUiState, actions: CarActions) {
    val scheme = MaterialTheme.colorScheme
    val nav = state.navApp()
    val media = state.app(state.activeMediaAppId) ?: state.apps.firstOrNull { it.kind == CarAppKind.MEDIA }
    val phone = state.apps.firstOrNull { it.kind == CarAppKind.PHONE }
    val settings = state.apps.firstOrNull { it.kind == CarAppKind.SETTINGS }
    val exit = state.apps.firstOrNull { it.kind == CarAppKind.EXIT }
    fun on(app: CarAppEntry?) = app != null && state.screen == CarScreen.App(app.id)
    Column(
        Modifier.fillMaxHeight().width(104.dp).background(scheme.surfaceContainerLowest).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The car: hands the screen back to the car's own UI where it allows that, else Home.
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(scheme.surfaceContainer)
                .carFocusHighlight(CircleShape)
                .clickable(onClickLabel = stringResource(R.string.m3_exit_to_car)) { exit?.let(actions::openApp) ?: actions.goHome() },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.DirectionsCar, stringResource(R.string.m3_exit_to_car), tint = scheme.onSurfaceVariant, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val (tab, fits) = railTabs(maxHeight, count = 6, min = 56.dp, max = 88.dp)
            val column = if (fits) Modifier.fillMaxSize() else Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            Column(column, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = if (fits) Arrangement.Top else Arrangement.Top) {
                M3Tab(Icons.Rounded.Home, stringResource(R.string.dash_menu_home), state.screen == CarScreen.Dashboard, tab) { actions.goHome() }
                M3Tab(Icons.Rounded.Navigation, stringResource(R.string.dash_maps), on(nav), tab) { nav?.let(actions::openApp) ?: actions.openLauncher() }
                M3Tab(Icons.Rounded.PlayCircle, stringResource(R.string.m3_tab_media), on(media), tab) { media?.let(actions::openApp) ?: actions.openLauncher() }
                M3Tab(Icons.Rounded.Call, stringResource(R.string.setup_app_entry_phone), on(phone), tab) { phone?.let(actions::openApp) ?: actions.openLauncher() }
                M3Tab(Icons.Rounded.Apps, stringResource(R.string.dash_menu_apps), state.screen == CarScreen.Launcher, tab) { actions.openLauncher() }
                M3Tab(Icons.Rounded.Settings, stringResource(R.string.m3_tab_settings), on(settings), tab) { settings?.let(actions::openApp) ?: actions.openLauncher() }
            }
        }
        Spacer(Modifier.height(12.dp))
        // Voice: the largest control on the rail (80 dp target for primary operations).
        Box(
            Modifier.size(76.dp).clip(CircleShape).background(if (state.listening) scheme.primary else scheme.primaryContainer)
                .carFocusHighlight(CircleShape).clickable { actions.assistant() },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Mic, null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(32.dp)) }
    }
}

@Composable
private fun M3Tab(icon: ImageVector, label: String, selected: Boolean, height: Dp, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val fg = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant
    Box(
        Modifier.size(width = 84.dp, height = height).padding(vertical = 3.dp).clip(shape)
            .then(if (selected) Modifier.background(scheme.primaryContainer) else Modifier)
            .carFocusHighlight(shape).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(28.dp))
            if (height >= 68.dp) {
                Spacer(Modifier.height(4.dp))
                Text(label, style = M3Type.labelMd, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- top bar

@Composable
private fun M3TopBar(state: CarUiState, actions: CarActions) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    // The name the user gave the phone ("Galaxy S25 Ultra"), not its model code ("SM-S938B").
    val phoneName = remember {
        runCatching { android.provider.Settings.Global.getString(context.contentResolver, "device_name") }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: Build.MODEL.orEmpty()
    }
    val notifications = state.apps.firstOrNull { it.kind == CarAppKind.NOTIFICATIONS }
    Row(
        Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(scheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Text("F", style = M3Type.titleLg.copy(fontWeight = FontWeight.Bold), color = scheme.primaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Text("F-Auto", style = M3Type.titleLg.copy(fontWeight = FontWeight.SemiBold), color = scheme.onSurface, maxLines = 1)
        if (state.weather != null) {
            Spacer(Modifier.width(16.dp))
            Box(Modifier.width(1.dp).height(28.dp).background(scheme.outlineVariant))
            Spacer(Modifier.width(16.dp))
            WeatherChip(state.weather)
        }
        Spacer(Modifier.weight(1f))
        // The phone that runs it all: its name, battery and signal, in one pill.
        Row(
            Modifier.height(48.dp).clip(CircleShape).background(scheme.surfaceContainer).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Smartphone, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Text(phoneName, style = M3Type.labelMd, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 180.dp))
            // Signal bars and the battery gauge (it shows the percentage itself).
            PhoneStatusIcons()
        }
        Spacer(Modifier.width(20.dp))
        M3Clock()
        if (notifications != null) {
            Spacer(Modifier.width(16.dp))
            Box {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(scheme.surfaceContainer)
                        .carFocusHighlight(CircleShape).clickable { actions.openApp(notifications) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Notifications, notifications.label, tint = scheme.onSurfaceVariant, modifier = Modifier.size(26.dp)) }
                if (state.unseen > 0) {
                    Box(Modifier.align(Alignment.TopEnd).size(14.dp).clip(CircleShape).background(scheme.error).border(2.dp, scheme.surface, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun M3Clock() {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000 - now % 1_000 + 5)
        }
    }
    val context = LocalContext.current
    val text = android.text.format.DateFormat.getTimeFormat(context).format(now)
    Text(text, style = M3Type.headlineMd.copy(fontWeight = FontWeight.Normal), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
}

// ---------------------------------------------------------------- home

@Composable
private fun M3Home(state: CarUiState, actions: CarActions, navMap: @Composable (CarAppEntry, Modifier) -> Unit, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        if (maxWidth < 700.dp && maxHeight <= maxWidth) {
            // A small screen (800x480): side by side, the map card was too narrow for the navigation
            // app's own cards and buttons. The map takes the width, media a player bar under it.
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NavCard(state, actions, navMap, Modifier.weight(1f).fillMaxWidth())
                M3MiniPlayer(state, actions, alwaysShown = true)
            }
        } else if (maxHeight > maxWidth) {
            // Portrait: the map in the upper eye line, media under it.
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NavCard(state, actions, navMap, Modifier.weight(1.4f).fillMaxWidth())
                M3MediaCard(state, actions, Modifier.weight(1f).fillMaxWidth())
            }
        } else {
            // Widescreen split: navigation ~7 of 12 columns, media the other 5.
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NavCard(state, actions, navMap, Modifier.weight(7f).fillMaxHeight())
                M3MediaCard(state, actions, Modifier.weight(5f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun M3MediaCard(state: CarUiState, actions: CarActions, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val np = state.nowPlaying
    val media = state.app(np?.appId ?: state.activeMediaAppId)
    val shape = RoundedCornerShape(24.dp)
    BoxWithConstraints(
        modifier.clip(shape).background(scheme.surfaceContainer).border(1.dp, Keyline, shape)
            .clickable { media?.let(actions::openApp) ?: actions.openLauncher() }
            .padding(20.dp),
    ) {
        val roomy = maxHeight >= 440.dp
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (roomy && media != null) {
                // The source, like the design's audio-system chip: which app is playing.
                Row(
                    Modifier.align(Alignment.Start).height(44.dp).clip(CircleShape).background(scheme.surfaceContainerHigh).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AppIcon(media, 24)
                    Text(media.label, style = M3Type.labelMd, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(16.dp))
            }
            // The artwork takes what the text and controls leave.
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val art = np?.art
                val artShape = RoundedCornerShape(20.dp)
                if (art != null) {
                    Image(art.asImageBitmap(), null, Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true).clip(artShape),
                        contentScale = ContentScale.Crop)
                } else {
                    Box(
                        Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true).clip(artShape).background(scheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.MusicNote, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(48.dp)) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(np?.title ?: (media?.label ?: stringResource(R.string.media_fallback_label)), style = M3Type.headlineSm.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(np?.subtitle?.takeIf { it.isNotBlank() } ?: stringResource(R.string.media_nothing_playing), style = M3Type.bodyLg,
                color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            if (np != null && np.durationMs > 0) {
                Spacer(Modifier.height(12.dp))
                M3Progress(np)
            }
            Spacer(Modifier.height(12.dp))
            M3Transport(np?.playing == true, actions, big = roomy)
        }
    }
}

/** The track position, moved on locally while playing between the player's updates. */
@Composable
private fun M3Progress(np: NowPlaying) {
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
        BoxWithConstraints(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(scheme.surfaceContainerHighest))
            Box(Modifier.fillMaxWidth(fraction).height(8.dp).clip(CircleShape).background(scheme.primaryContainer))
            Box(Modifier.offset(x = (maxWidth - 20.dp) * fraction).size(20.dp).clip(CircleShape).background(scheme.primary))
        }
        Row(Modifier.fillMaxWidth()) {
            Text(time(pos), style = M3Type.labelMd, color = scheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(time(np.durationMs), style = M3Type.labelMd, color = scheme.onSurfaceVariant)
        }
    }
}

internal fun time(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}

/** Previous, play/pause (the big pill), next: what the car's media keys do. */
@Composable
private fun M3Transport(playing: Boolean, actions: CarActions, big: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        M3IconButton(Icons.Rounded.SkipPrevious, null) { actions.mediaPrevious() }
        Box(
            Modifier.size(width = if (big) 112.dp else 96.dp, height = if (big) 80.dp else 68.dp).clip(RoundedCornerShape(28.dp))
                .background(scheme.primaryContainer).carFocusHighlight(RoundedCornerShape(28.dp)).clickable { actions.mediaPlayPause() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(40.dp))
        }
        M3IconButton(Icons.Rounded.SkipNext, null) { actions.mediaNext() }
    }
}

/** A 64 dp round control (the design's icon-only button). */
@Composable
private fun M3IconButton(icon: ImageVector, label: String?, tonal: Boolean = false, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.size(64.dp).clip(CircleShape).then(if (tonal) Modifier.background(scheme.surfaceContainerHigh) else Modifier)
            .carFocusHighlight(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = scheme.onSurface, modifier = Modifier.size(34.dp)) }
}

// ---------------------------------------------------------------- mini player

/** Under an app, and on small screens' Home ([alwaysShown]: also with nothing playing, as the media card). */
@Composable
private fun M3MiniPlayer(state: CarUiState, actions: CarActions, alwaysShown: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val np = state.nowPlaying
    if (np == null && !alwaysShown) return
    val media = state.app(np?.appId ?: state.activeMediaAppId)
    Row(
        Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(24.dp)).background(scheme.surfaceContainerHigh)
            .clickable { media?.let(actions::openApp) ?: actions.openLauncher() }.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val art = np?.art
        if (art != null) Image(art.asImageBitmap(), null, Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
        else Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(scheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MusicNote, null, tint = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(np?.title ?: (media?.label ?: stringResource(R.string.media_fallback_label)), style = M3Type.titleMd, color = scheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(np?.subtitle?.takeIf { it.isNotBlank() } ?: stringResource(R.string.media_nothing_playing), style = M3Type.labelMd,
                color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        M3IconButton(Icons.Rounded.SkipPrevious, null) { actions.mediaPrevious() }
        M3IconButton(if (np?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tonal = true) { actions.mediaPlayPause() }
        M3IconButton(Icons.Rounded.SkipNext, null) { actions.mediaNext() }
    }
}

// ---------------------------------------------------------------- apps hub

internal enum class M3Category { ALL, NAVIGATION, MEDIA, COMMUNICATION, VEHICLE }

internal fun categoryOf(app: CarAppEntry): M3Category = when {
    // A known map app is Navigation however it was listed: Google Maps declares notifications in its
    // car descriptor, so it can come in as a messaging entry.
    app.isNav || net.prototux.aare.host.CarAppInfo.isKnownNavigationPackage(app.packageName) -> M3Category.NAVIGATION
    app.kind == CarAppKind.MEDIA || app.playsMedia -> M3Category.MEDIA
    app.kind == CarAppKind.PHONE || app.kind == CarAppKind.MESSAGING || app.kind == CarAppKind.NOTIFICATIONS || app.messaging -> M3Category.COMMUNICATION
    app.kind == CarAppKind.CAR_DATA || app.kind == CarAppKind.SETTINGS || app.kind == CarAppKind.EXIT -> M3Category.VEHICLE
    else -> M3Category.ALL
}

@Composable
private fun M3AppsHub(state: CarUiState, actions: CarActions, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    var filter by remember { mutableStateOf(M3Category.ALL) }
    val shown = state.apps.filter { filter == M3Category.ALL || categoryOf(it) == filter }
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                M3Chip(Icons.Rounded.GridView, stringResource(R.string.m3_apps_all), filter == M3Category.ALL) { filter = M3Category.ALL }
                M3Chip(Icons.Rounded.Navigation, stringResource(R.string.m3_apps_navigation), filter == M3Category.NAVIGATION) { filter = M3Category.NAVIGATION }
                M3Chip(Icons.Rounded.MusicNote, stringResource(R.string.m3_apps_media), filter == M3Category.MEDIA) { filter = M3Category.MEDIA }
                M3Chip(Icons.Rounded.ChatBubble, stringResource(R.string.m3_apps_communication), filter == M3Category.COMMUNICATION) { filter = M3Category.COMMUNICATION }
                M3Chip(Icons.Rounded.Tune, stringResource(R.string.m3_apps_vehicle), filter == M3Category.VEHICLE) { filter = M3Category.VEHICLE }
            }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.m3_apps_count, state.apps.size), style = M3Type.labelLg, color = scheme.primary, maxLines = 1)
        }
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(156.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(shown, key = { it.id }) { app -> M3AppTile(state, app) { actions.openApp(app) } }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.heightIn(min = 64.dp).clip(CircleShape).background(scheme.primaryContainer)
                .carFocusHighlight(CircleShape).clickable { actions.goHome() }.padding(horizontal = 28.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Home, null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.m3_apps_back_to_dashboard), style = M3Type.labelLg, color = scheme.onPrimaryContainer)
        }
    }
}

/** A filter chip: 56 dp pill, filled when selected. */
@Composable
private fun M3Chip(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.height(56.dp).clip(CircleShape)
            .background(if (selected) scheme.primaryContainer else scheme.surfaceContainer)
            .then(if (selected) Modifier else Modifier.border(1.dp, Keyline, CircleShape))
            .carFocusHighlight(CircleShape).clickable(onClick = onClick).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (selected) scheme.onPrimaryContainer else scheme.onSurface
        Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = M3Type.labelLg, color = fg, maxLines = 1)
    }
}

@Composable
private fun M3AppTile(state: CarUiState, app: CarAppEntry, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tile = when (categoryOf(app)) {
        M3Category.NAVIGATION -> scheme.secondaryContainer
        M3Category.MEDIA -> scheme.primaryContainer
        M3Category.COMMUNICATION -> scheme.tertiaryContainer
        else -> scheme.surfaceContainerHighest
    }
    // A live status under the name where F-Auto knows one, as in the design's subtitles.
    val playing = state.nowPlaying?.let { it.playing && it.appId == app.id } == true
    val (status, statusColor) = when {
        playing -> stringResource(R.string.m3_apps_playing) to scheme.tertiary
        app.kind == CarAppKind.NOTIFICATIONS && state.unseen > 0 -> stringResource(R.string.m3_apps_new, state.unseen) to scheme.error
        app.isNav && app.id == state.activeNavAppId && state.trip != null -> stringResource(R.string.m3_apps_guiding) to scheme.tertiary
        else -> null to scheme.onSurfaceVariant
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).carFocusHighlight(RoundedCornerShape(24.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(Modifier.size(108.dp).clip(RoundedCornerShape(28.dp)).background(tile), contentAlignment = Alignment.Center) {
                val icon = app.icon
                if (icon != null) Image(icon.asImageBitmap(), app.label, Modifier.size(68.dp).clip(RoundedCornerShape(18.dp)))
                else Text(app.label.take(1), style = M3Type.headlineMd, color = scheme.onSurface)
            }
            if (app.kind == CarAppKind.NOTIFICATIONS && state.unseen > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp).size(34.dp).clip(CircleShape).background(scheme.errorContainer)
                        .border(2.dp, scheme.surfaceContainerLow, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(state.unseen.coerceAtMost(99).toString(), style = M3Type.labelMd, color = scheme.onErrorContainer) }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(app.label, style = M3Type.titleMd, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))
        if (status != null) Text(status, style = M3Type.labelMd, color = statusColor, maxLines = 1)
    }
}
