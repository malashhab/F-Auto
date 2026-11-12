package net.prototux.aare.ui.car

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.prototux.aare.R
import net.prototux.aare.notify.PhoneGuidance
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.LocalCarDimens

/**
 * Root of the car screen: AARE's one shell (a fixed rail, a persistent bottom bar, a dashboard
 * that toggles map+widgets/map+music), in the color theme from `Settings.carStyle`.
 *
 * [appContent] renders an app full screen (templates or the media browser); [navMap] renders a
 * navigation app's map surface, both on the dashboard card and behind its templates.
 */
@Composable
fun CarShell(
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    /** Drawn over everything but notices: the voice assistant's bar. */
    overlay: @Composable (Modifier) -> Unit = {},
    /** Insets the UI into the head unit's visible area (the codec frame minus its margins). */
    modifier: Modifier = Modifier,
) = if (state.shellLayout == net.prototux.aare.Settings.LAYOUT_DUSK) {
    DuskUIShell(state, actions, appContent, navMap, overlay, modifier)
} else {
    HybridShell(state, actions, appContent, navMap, overlay, modifier)
}

@Composable
internal fun Clock() {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000 - now % 1_000 + 5)
        }
    }
    // Hours and minutes only: "AM/PM" doesn't fit the rail, and the car shows the period anyway.
    val text = android.text.format.DateFormat.format(
        if (android.text.format.DateFormat.is24HourFormat(androidx.compose.ui.platform.LocalContext.current)) "H:mm" else "h:mm", now,
    ).toString()
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
}

@Composable
internal fun Card(modifier: Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val r = LocalCarDimens.current.cardRadius
    val shape = RoundedCornerShape(r)
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            // A hairline edge, not just a flat color change from the page behind it: without it a
            // dark card on a near-black background (most of these themes) reads as barely there.
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .let { if (onClick != null) it.carFocusHighlight(shape).clickable(onClick = onClick) else it },
    ) { content() }
}

@Composable
internal fun NavCard(state: CarUiState, actions: CarActions, navMap: @Composable (CarAppEntry, Modifier) -> Unit, modifier: Modifier) {
    val nav = state.app(state.activeNavAppId)
    val guidance = state.phoneGuidance
    Card(modifier) {
        if (guidance != null) {
            GuidanceCard(guidance)
        } else if (nav != null) {
            navMap(nav, Modifier.fillMaxSize())
        } else {
            val navApps = state.apps.filter { it.isNav }
            Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.Map, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(if (navApps.isEmpty()) stringResource(R.string.nav_dashboard_no_app_title) else stringResource(R.string.nav_dashboard_where_to), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (navApps.isEmpty()) stringResource(R.string.nav_dashboard_install_hint) else stringResource(R.string.nav_dashboard_choose_app),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    navApps.take(4).forEach { app -> AppChip(app) { actions.openApp(app) } }
                }
            }
        }
    }
}

/**
 * The dashboard's map card for a navigation phone app launched on the car screen (Google Maps
 * through Shizuku): the app draws on its own virtual display, so the card can't show its map
 * alongside the full screen. It shows the app and opens it full screen on tap.
 */
@Composable
internal fun StandaloneNavCard(app: CarAppEntry, modifier: Modifier, onOpen: () -> Unit) {
    Column(
        modifier.fillMaxSize().clickable(onClick = onOpen).padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        val icon = app.icon
        if (icon != null) Image(icon.asImageBitmap(), null, Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)))
        else Icon(Icons.Rounded.Map, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(app.label, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.standalone_nav_open), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Directions from a navigation app running on the phone (Google Maps, Waze…), from its notification. */
@Composable
private fun GuidanceCard(g: PhoneGuidance) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            g.arrow?.let { arrow ->
                Image(arrow.asImageBitmap(), null, Modifier.size(112.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface, BlendMode.SrcIn))
                Spacer(Modifier.width(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(g.primary, style = MaterialTheme.typography.displaySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (g.secondary.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(g.secondary, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (g.detail.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(g.detail, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.nav_guidance_on_phone, g.appLabel), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AppChip(app: CarAppEntry, onClick: () -> Unit) {
    Row(
        Modifier.height(64.dp).clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.secondaryContainer)
            .carFocusHighlight(RoundedCornerShape(32.dp)).clickable(onClick = onClick).padding(start = 10.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app, 44)
        Spacer(Modifier.width(12.dp))
        Text(app.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
fun AppIcon(app: CarAppEntry, sizeDp: Int) {
    Box(contentAlignment = Alignment.BottomEnd) {
        if (app.icon != null) {
            Image(app.icon.asImageBitmap(), app.label, Modifier.size(sizeDp.dp).clip(CircleShape))
        } else {
            Box(Modifier.size(sizeDp.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text(app.label.take(1), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        // A corner badge marks an ordinary phone app (not an Android Auto one), so a NewPipe that
        // appears both as its car app and as a launched phone app is told apart at a glance.
        if (app.kind == CarAppKind.STANDALONE) {
            Box(
                Modifier.size((sizeDp * 0.36f).dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary)
                    .border((sizeDp * 0.03f).dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Smartphone, stringResource(R.string.standalone_not_native),
                    tint = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size((sizeDp * 0.22f).dp))
            }
        }
    }
}

@Composable
internal fun MediaCard(state: CarUiState, actions: CarActions, compact: Boolean, modifier: Modifier) {
    val np = state.nowPlaying
    val media = state.app(np?.appId ?: state.activeMediaAppId)
    Card(modifier, onClick = { media?.let(actions::openApp) ?: actions.openLauncher() }) {
        if (np?.art != null && !compact) {
            Image(np.art.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.25f)
            // Ramps to fully opaque well before the title/subtitle (which sit in the card's lower
            // half): a plain top-to-bottom fade still shows busy album art straight through text.
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                0f to Color.Transparent,
                0.4f to MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                0.65f to MaterialTheme.colorScheme.surfaceContainer,
            )))
        }
        val content: @Composable () -> Unit = {
            if (np != null) {
                Text(np.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(np.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text(media?.label ?: stringResource(R.string.media_fallback_label), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.media_nothing_playing), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (compact) {
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Art(np, 80)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) { content() }
                MediaButtons(np, actions)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Art(np, 72)
                    Spacer(Modifier.width(16.dp))
                    if (media != null) AppIcon(media, 28)
                }
                Spacer(Modifier.weight(1f))
                content()
                Spacer(Modifier.height(12.dp))
                if (np != null && np.durationMs > 0) {
                    LinearProgressIndicator(
                        progress = { (np.positionMs.toFloat() / np.durationMs).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { MediaButtons(np, actions) }
            }
        }
    }
}

@Composable
private fun Art(np: NowPlaying?, sizeDp: Int) {
    val shape = RoundedCornerShape(16.dp)
    if (np?.art != null) {
        Image(np.art.asImageBitmap(), null, Modifier.size(sizeDp.dp).clip(shape), contentScale = ContentScale.Crop)
    } else {
        Box(Modifier.size(sizeDp.dp).clip(shape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size((sizeDp / 2).dp))
        }
    }
}

@Composable
private fun MediaButtons(np: NowPlaying?, actions: CarActions) {
    RoundIcon(Icons.Rounded.SkipPrevious, stringResource(R.string.media_action_previous), filled = false) { actions.mediaPrevious() }
    RoundIcon(if (np?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, stringResource(R.string.media_action_play_pause), filled = true) { actions.mediaPlayPause() }
    RoundIcon(Icons.Rounded.SkipNext, stringResource(R.string.media_action_next), filled = false) { actions.mediaNext() }
}

@Composable
fun RoundIcon(icon: ImageVector, label: String, filled: Boolean, onClick: () -> Unit) {
    val d = LocalCarDimens.current
    val bg = if (filled) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(Modifier.size(d.touchTarget).clip(CircleShape).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = fg, modifier = Modifier.size(d.iconSize))
    }
}

// ---------------------------------------------------------------- launcher

@Composable
internal fun Launcher(state: CarUiState, actions: CarActions, modifier: Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = modifier,
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Already in the user's order (CarRuntime.applyApps).
        items(state.apps, key = { it.id }) { app ->
            Column(
                Modifier.clip(RoundedCornerShape(24.dp)).clickable { actions.openApp(app) }.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppIcon(app, 80)
                Spacer(Modifier.height(10.dp))
                Text(app.label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ---------------------------------------------------------------- notices

@Composable
internal fun NoticeOverlay(state: CarUiState, actions: CarActions, modifier: Modifier) {
    val notice = state.notices.firstOrNull() ?: return
    LaunchedEffect(notice.key) {
        delay(8_000)
        actions.dismissNotice(notice)
    }
    Row(
        modifier.padding(16.dp).fillMaxWidth(0.6f).clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (notice.icon != null) {
            Image(notice.icon.asImageBitmap(), null, Modifier.size(56.dp).aspectRatio(1f).clip(CircleShape))
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(notice.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // While driving a message shows who wrote, not what: it can be read aloud instead.
            Text(if (state.drivingRestricted && notice.message) stringResource(R.string.notif_new_message_placeholder) else notice.text,
                style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        notice.actions.forEach { (label, run) ->
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { run(); actions.dismissNotice(notice) }.padding(16.dp))
        }
        RoundIcon(Icons.Rounded.Close, stringResource(R.string.common_dismiss), filled = false) { actions.dismissNotice(notice) }
    }
}
