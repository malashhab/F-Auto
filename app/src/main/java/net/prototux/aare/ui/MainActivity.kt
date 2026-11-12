package net.prototux.aare.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.prototux.aare.DiagLog
import net.prototux.aare.R
import net.prototux.aare.SessionAapLog
import net.prototux.aare.Settings
import net.prototux.aare.UpdateChecker
import net.prototux.aare.audio.CaptureMethod
import net.prototux.aare.audio.CaptureMethods
import net.prototux.aare.car.DrivingMode
import net.prototux.aare.car.PhoneLocationFeed
import net.prototux.aare.cert.CertificateAutoUpdate
import net.prototux.aare.cert.CertificateDownload
import net.prototux.aare.cert.GearheadKeyExtractor
import net.prototux.aare.cert.CertificateStore
import net.prototux.aare.host.AndroidAutoApps
import net.prototux.aare.host.AssistantApp
import net.prototux.aare.host.Assistants
import net.prototux.aare.host.CarAppCatalog
import net.prototux.aare.host.CarAppInfo
import net.prototux.aare.host.CarRuntime
import net.prototux.aare.host.PhoneApp
import net.prototux.aare.host.UnsupportedCarApp
import net.prototux.aare.mirror.MirrorTouchService
import androidx.core.graphics.drawable.toBitmap
import net.prototux.aare.launch.AppLauncher
import net.prototux.aare.car.CarData
import net.prototux.aare.media.MediaHub
import net.prototux.aare.service.ProjectionService
import net.prototux.aare.ui.car.WALLPAPER_NAMES
import net.prototux.aare.ui.theme.PhoneTheme
import net.prototux.aare.ui.theme.brandAccentScheme
import rikka.shizuku.Shizuku
import java.text.DateFormat
import kotlin.math.roundToInt

/** The phone side: what's connected, what's set up, and the settings. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        applyDebugExtras(intent)
        setContent { PhoneTheme(dark = isSystemInDarkTheme()) { Surface(Modifier.fillMaxSize()) { Home() } } }
    }

    // singleTask: when AARE is already open, ./headunit.sh's intent arrives here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (applyDebugExtras(intent)) {
            ProjectionService.startIdleIfEnabled(this)
            recreate() // the settings screen shows the new state
        }
    }

    /** Debug builds only: lets ./headunit.sh switch the head unit server on over adb. Returns true if applied. */
    private fun applyDebugExtras(intent: Intent?): Boolean {
        if (!net.prototux.aare.BuildConfig.DEBUG) return false
        // adb shell am start -n net.prototux.aare/.ui.MainActivity --ez car_preview true: opens the
        // car screen preview, to check a layout change from a computer.
        if (intent?.getBooleanExtra("car_preview", false) == true) startActivity(Intent(this, CarPreviewActivity::class.java))
        if (intent?.hasExtra("hu_server") != true) return false
        Settings(this).headUnitServer = intent.getBooleanExtra("hu_server", false)
        return true
    }
}

/** The settings pages, Android Auto style: one per topic, opened from the Settings tab. */
private enum class Page(val titleRes: Int, val subtitleRes: Int, val icon: ImageVector, val tint: Color) {
    CAR_SCREEN(R.string.setup_page_car_screen_title, R.string.setup_page_car_screen_subtitle, Icons.Rounded.Dashboard, Color(0xFFD92D45)),
    APPS(R.string.setup_page_apps_title, R.string.setup_page_apps_subtitle, Icons.Rounded.Apps, Color(0xFF5766E8)),
    ASSISTANT(R.string.assistant_settings_title, R.string.assistant_page_subtitle, Icons.Rounded.Mic, Color(0xFF8A5BD8)),
    MESSAGES(R.string.msg_settings_title, R.string.msg_page_subtitle, Icons.Rounded.Forum, Color(0xFF1F9D6E)),
    AUDIO(R.string.audio_settings_title, R.string.audio_page_subtitle, Icons.AutoMirrored.Rounded.VolumeUp, Color(0xFFE0802A)),
    CONNECTION(R.string.setup_page_connection_title, R.string.setup_page_connection_subtitle, Icons.Rounded.Bluetooth, Color(0xFF2B7DE0)),
    CAR_DATA(R.string.setup_page_car_data_title, R.string.setup_page_car_data_subtitle, Icons.Rounded.DirectionsCar, Color(0xFFC2447A)),
    ADVANCED(R.string.setup_page_advanced_title, R.string.setup_page_advanced_subtitle, Icons.Rounded.Tune, Color(0xFF5A6380)),
}

/** The phone app's bottom navigation: Home (connect/quick settings), Customize (car-screen look), My cars (status/data) and Settings (the Page list). */
private enum class MainTab(val labelRes: Int, val icon: ImageVector) {
    HOME(R.string.dash_menu_home, Icons.Rounded.Home),
    CUSTOMIZE(R.string.setup_customize_title, Icons.Rounded.Palette),
    CARS(R.string.setup_tab_cars, Icons.Rounded.DirectionsCar),
    SETTINGS(R.string.setup_app_entry_settings, Icons.Rounded.Settings),
}

@Composable
private fun Home() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val certs = remember { CertificateStore(ctx) }
    val state by ProjectionService.state.collectAsState()
    var refresh by remember { mutableIntStateOf(0) }
    var page by rememberSaveable { mutableStateOf<Page?>(null) }
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var carStyle by remember { mutableStateOf(settings.carStyle) }
    LaunchedEffect(Unit) { ProjectionService.startIdleIfEnabled(ctx); ProjectionService.probeUsb(ctx); ProjectionService.probeWireless(ctx); UpdateChecker.refresh(ctx); CertificateAutoUpdate.refresh(ctx) }
    // Re-check permissions and capture methods whenever the user comes back to the app.
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    BackHandler(enabled = page != null) { page = null }

    // One shared, secondary (non-driving) CarRuntime for every live car-screen preview thumbnail
    // on the Home/Customize tabs, so switching tabs doesn't re-scan apps or re-attach listeners.
    val scope = rememberCoroutineScope()
    val previewRuntime = remember { CarRuntime(ctx, scope, settings, MediaHub(ctx, scope), primary = false) }
    DisposableEffect(previewRuntime) {
        previewRuntime.densityDpi = ctx.resources.displayMetrics.densityDpi
        previewRuntime.screenWidthDp = 1280
        previewRuntime.screenHeightDp = 720
        previewRuntime.start()
        onDispose { previewRuntime.stop() }
    }

    MaterialTheme(colorScheme = brandAccentScheme(MaterialTheme.colorScheme, isSystemInDarkTheme())) {
    AnimatedContent(page, label = "page", transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) }) { p ->
        if (p == null) {
            // No Scaffold bottom bar: that reserves an opaque strip the full width of the screen.
            // The bar is drawn over the page instead, so only the pill itself has a background and
            // the page scrolls underneath it (each tab leaves room at the end: [TabContentPadding]).
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when (tab) {
                    MainTab.HOME -> HomeTab(settings, state, refresh, { refresh++ }, previewRuntime, { page = it }) { tab = MainTab.SETTINGS }
                    MainTab.CUSTOMIZE -> CustomizeTab(settings, previewRuntime, carStyle) { carStyle = it }
                    MainTab.CARS -> CarsTab(settings, state, previewRuntime)
                    MainTab.SETTINGS -> SettingsTab { page = it }
                }
                MainNavBar(tab, Modifier.align(Alignment.BottomCenter)) { tab = it }
            }
        } else {
            // The same backdrop as the tabs: without it the settings pages fell back to the plain
            // near-black surface behind them, a different color from the rest of the app.
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { page = null }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.common_back)) }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(p.titleRes), style = MaterialTheme.typography.titleLarge)
                }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (p) {
                        Page.CAR_SCREEN -> item { CarScreenSettings(settings, refresh) { carStyle = it } }
                        Page.APPS -> item { AppsCard(settings) }
                        Page.ASSISTANT -> item { AssistantCard(settings) }
                        Page.MESSAGES -> item { MessageSettings(settings, refresh) }
                        Page.AUDIO -> item { AudioSettings(settings, state, refresh) { refresh++ } }
                        Page.CONNECTION -> item { ConnectionSettings(settings, refresh) }
                        Page.CAR_DATA -> item { CarDataCard() }
                        Page.ADVANCED -> {
                            item { HeadUnitServerSettings(settings) }
                            item { CertificatesCard(certs, state) }
                            item { VehicleOverridesCard() }
                            item { LogCard() }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun CategoryRow(page: Page, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(page.tint), contentAlignment = Alignment.Center) {
            Icon(page.icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(page.titleRes), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(page.subtitleRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------- bottom nav + tabs

/** Room each tab's list leaves at its end, so its last item can scroll clear of the floating [MainNavBar]. */
private val TabContentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 112.dp)

/**
 * The bottom navigation, as in the mockup: one rounded bar holding the four tabs, each an icon over
 * its label, with a gradient oval behind the selected tab's icon only. Nothing around the bar has a
 * background of its own: it floats over the page (see [Home]), which shows through at its sides and
 * corners and scrolls underneath it.
 */
@Composable
private fun MainNavBar(tab: MainTab, modifier: Modifier = Modifier, onSelect: (MainTab) -> Unit) {
    val dark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(30.dp)
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .shadow(16.dp, shape, clip = false)
            .clip(shape)
            // Solid: even a slightly see-through bar let the page's text show through behind the
            // labels. Dark navy at night, the theme's own raised surface by day (it was a fixed
            // dark navy in both).
            .background(if (dark) Color(0xFF1B1733) else MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, (if (dark) Color.White else Color.Black).copy(alpha = 0.08f), shape)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MainTab.entries.forEach { t -> NavPillItem(t, tab == t, dark, Modifier.weight(1f)) { onSelect(t) } }
    }
}

@Composable
private fun NavPillItem(t: MainTab, selected: Boolean, dark: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val idle = if (dark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
    val active = if (dark) Color.White else MaterialTheme.colorScheme.onSurface
    Column(
        modifier.clip(RoundedCornerShape(22.dp)).clickable(onClickLabel = stringResource(t.labelRes), onClick = onClick).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The oval: behind the selected tab's icon only, the same size for every tab so the icons
        // and labels never shift when the selection moves.
        Box(
            Modifier.size(width = 58.dp, height = 34.dp).clip(RoundedCornerShape(percent = 50))
                .background(if (selected) Brand.gradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))),
            contentAlignment = Alignment.Center,
        ) { Icon(t.icon, null, tint = if (selected) Color.White else idle, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(t.labelRes), color = if (selected) active else idle, maxLines = 1,
            style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Home tab: connect/preview the car, four one-tap quick settings, then the first-run checklist if it isn't done. */
@Composable
private fun HomeTab(settings: Settings, state: ProjectionService.State, refresh: Int, recheck: () -> Unit, previewRuntime: CarRuntime, open: (Page) -> Unit, onSettingsClick: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().systemBarsPadding(),
        contentPadding = TabContentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Header(onSettingsClick) }
        item { UpdateBanner() }
        item { ConnectCard(settings, state, previewRuntime) }
        item { QuickSettingsGrid(settings) }
        item { Setup(state, refresh, recheck, open) }
        item { About() }
    }
}

/** The "not connected"/"connected" car card: name, last-connected, a live preview, Connect + Preview. */
@Composable
private fun ConnectCard(settings: Settings, state: ProjectionService.State, previewRuntime: CarRuntime) {
    val ctx = LocalContext.current
    val lastCar = settings.knownCars.entries.maxByOrNull { it.value }
    val bg = if (state.connected)
        Brush.linearGradient(listOf(Color(0xFF1F9D6E).copy(alpha = 0.35f), Color(0xFF5766E8).copy(alpha = 0.3f)))
    else
        Brush.linearGradient(listOf(Color(0xFFD92D45).copy(alpha = 0.35f), Color(0xFF5766E8).copy(alpha = 0.35f)))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(bg)
            .border(1.dp, Brand.glassLine, RoundedCornerShape(28.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (state.connected) Color(0xFF6EE8CC) else Color(0xFFFF8595)))
            Spacer(Modifier.width(8.dp))
            Text(
                if (state.connected) stringResource(R.string.setup_status_connected_caps) else stringResource(R.string.notif_status_not_connected).uppercase(),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(lastCar?.key ?: stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
        val subtitle = when {
            state.connected -> listOf(state.transport, state.video).filter { it.isNotBlank() }.joinToString(" · ")
            lastCar != null -> stringResource(R.string.setup_car_last_connected, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(lastCar.value))
            else -> stringResource(R.string.setup_status_disconnected_hint)
        }
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        LiveCarPreview(previewRuntime, Modifier.fillMaxWidth().aspectRatio(16f / 9f)) { ctx.startActivity(Intent(ctx, CarPreviewActivity::class.java)) }
        Spacer(Modifier.height(14.dp))
        state.pendingTrust?.let { p ->
            val trustLabel = stringResource(if (p.changed) R.string.setup_trust_new_cert else R.string.setup_trust_head_unit)
            Pill(stringResource(R.string.setup_trust_fingerprint_suffix, trustLabel, p.fingerprint.take(16)), null) { ProjectionService.trust(ctx, p.peerKey, p.fingerprint) }
            Spacer(Modifier.height(10.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.connected) {
                Pill(stringResource(R.string.setup_disconnect), null) { ProjectionService.stop(ctx) }
            } else {
                WhitePill(stringResource(R.string.setup_action_connect)) {
                    ProjectionService.startIdleIfEnabled(ctx); ProjectionService.probeUsb(ctx); ProjectionService.probeWireless(ctx)
                    Toast.makeText(ctx, R.string.setup_action_connect_toast, Toast.LENGTH_SHORT).show()
                }
            }
            Pill(stringResource(R.string.setup_preview_car_screen), Icons.Rounded.Tv) { ctx.startActivity(Intent(ctx, CarPreviewActivity::class.java)) }
        }
        state.lastError?.let {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.setup_status_last_error, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun WhitePill(label: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(24.dp)).background(Color.White).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { Text(label, color = Color(0xFF1A1030), style = MaterialTheme.typography.labelLarge) }
}

/**
 * A live, non-interactive thumbnail of [previewRuntime]'s car screen; tap opens the real preview.
 * Renders the shell at a fixed 1280x720 "car" canvas and scales that whole layer down to fit
 * [modifier]'s box, so the car UI's own responsive (weight-based) layout always fills the canvas
 * evenly, however small the thumbnail ends up on screen.
 */
@Composable
private fun LiveCarPreview(previewRuntime: CarRuntime, modifier: Modifier, onClick: () -> Unit) {
    BoxWithConstraints(modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black)) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val scale = minOf(maxWidth / 1280.dp, maxHeight / 720.dp)
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density * scale, density.fontScale),
        ) {
            Box(Modifier.width(1280.dp).height(720.dp)) { previewRuntime.Root(Modifier.fillMaxSize()) }
        }
        Box(Modifier.matchParentSize().clickable(onClick = onClick))
    }
}

/** Four one-tap toggles for settings people flip often, matching the Customize tab's own controls one-for-one. */
@Composable
private fun QuickSettingsGrid(settings: Settings) {
    var wireless by remember { mutableStateOf(settings.wireless) }
    var mapMusic by remember { mutableStateOf(settings.homeMapMusicDefault) }
    var readAloud by remember { mutableStateOf(settings.readMessagesAloud) }
    var hideRail by remember { mutableStateOf(settings.railAutoHide) }
    Column {
        SectionLabel(stringResource(R.string.setup_quick_settings_title))
        Spacer(Modifier.height(10.dp))
        val tiles = listOf(
            Quad(stringResource(R.string.setup_toggle_wireless_title), stringResource(R.string.setup_quick_wireless_subtitle), Color(0xFF5766E8), wireless) { v: Boolean -> wireless = v; settings.wireless = v },
            Quad(stringResource(R.string.setup_quick_map_music_title), stringResource(R.string.setup_quick_map_music_subtitle), Color(0xFFD92D45), mapMusic) { v: Boolean -> mapMusic = v; settings.homeMapMusicDefault = v },
            Quad(stringResource(R.string.msg_toggle_read_aloud_title), stringResource(R.string.setup_quick_read_subtitle), Color(0xFF1F9D6E), readAloud) { v: Boolean -> readAloud = v; settings.readMessagesAloud = v },
            Quad(stringResource(R.string.setup_quick_hide_rail_title), stringResource(R.string.setup_quick_hide_rail_subtitle), Color(0xFF8A5BD8), hideRail) { v: Boolean -> hideRail = v; settings.railAutoHide = v },
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tiles.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { QuickTile(it, Modifier.weight(1f)) }
                }
            }
        }
    }
}

private class Quad(val title: String, val subtitle: String, val color: Color, val checked: Boolean, val onChange: (Boolean) -> Unit)

@Composable
private fun QuickTile(t: Quad, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { t.onChange(!t.checked) }.padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(t.color))
            Spacer(Modifier.weight(1f))
            Switch(checked = t.checked, onCheckedChange = t.onChange)
        }
        Spacer(Modifier.height(6.dp))
        Text(t.title, style = MaterialTheme.typography.bodyLarge)
        Text(t.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Customize tab: a live preview plus the same shell-look controls the mockup groups together. */
@Composable
private fun CustomizeTab(settings: Settings, previewRuntime: CarRuntime, carStyle: String, onStyleChange: (String) -> Unit) {
    var style by remember { mutableStateOf(carStyle) }
    var mapMusic by remember { mutableStateOf(settings.homeMapMusicDefault) }
    var railAutoHide by remember { mutableStateOf(settings.railAutoHide) }
    var railRight by remember { mutableStateOf(settings.railPosition == Settings.RAIL_RIGHT) }
    var musicSize by remember { mutableFloatStateOf(settings.musicWidgetSize) }
    var duskLayout by remember { mutableStateOf(settings.carLayout == Settings.LAYOUT_DUSK) }
    val ctx = LocalContext.current
    LazyColumn(
        Modifier.fillMaxSize().systemBarsPadding(),
        contentPadding = TabContentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text(stringResource(R.string.setup_customize_title), style = MaterialTheme.typography.headlineSmall) }
        item {
            Column {
                LiveCarPreview(previewRuntime, Modifier.fillMaxWidth().aspectRatio(16f / 9f)) { ctx.startActivity(Intent(ctx, CarPreviewActivity::class.java)) }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.setup_live_preview_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        stringResource(R.string.setup_open_on_car), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        // The theme's own accent, not fixed white: white vanished on the phone's light theme.
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { ctx.startActivity(Intent(ctx, CarPreviewActivity::class.java)) },
                    )
                }
            }
        }
        item { ThemeSwatchRow(style) { style = it; settings.carStyle = it; onStyleChange(it) } }
        // The layout decides which of the controls below do anything, so it belongs here with them
        // rather than only under Settings > Car screen.
        item {
            Segmented2(stringResource(R.string.setup_choice_layout), stringResource(R.string.setup_layout_classic), stringResource(R.string.setup_layout_dusk), duskLayout) {
                duskLayout = it; settings.carLayout = if (it) Settings.LAYOUT_DUSK else Settings.LAYOUT_CLASSIC
            }
        }
        item {
            Segmented2(stringResource(R.string.setup_choice_layout_home), stringResource(R.string.setup_layout_full), stringResource(R.string.setup_quick_map_music_title), mapMusic) {
                mapMusic = it; settings.homeMapMusicDefault = it
            }
        }
        // Auto-hide and the music widget's size exist only in the Dusk layout: Classic gets the
        // side-bar side alone, rather than controls that do nothing.
        if (duskLayout) item {
            Segmented3(
                stringResource(R.string.setup_choice_bar),
                stringResource(R.string.setup_choice_left), !railRight && !railAutoHide,
                stringResource(R.string.setup_choice_right), railRight && !railAutoHide,
                stringResource(R.string.setup_choice_auto_hide), railAutoHide,
            ) { choice ->
                when (choice) {
                    0 -> { railRight = false; railAutoHide = false; settings.railAutoHide = false; settings.railPosition = Settings.RAIL_LEFT }
                    1 -> { railRight = true; railAutoHide = false; settings.railAutoHide = false; settings.railPosition = Settings.RAIL_RIGHT }
                    else -> { railAutoHide = true; settings.railAutoHide = true }
                }
            }
        } else item {
            Segmented2(stringResource(R.string.setup_choice_bar), stringResource(R.string.setup_choice_left), stringResource(R.string.setup_choice_right), railRight) {
                railRight = it; settings.railPosition = if (it) Settings.RAIL_RIGHT else Settings.RAIL_LEFT
            }
        }
        if (duskLayout) item {
            Column {
                SectionLabel(stringResource(R.string.setup_music_widget_size))
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("S", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = musicSize, onValueChange = { musicSize = it; settings.musicWidgetSize = it },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White, activeTrackColor = Brand.pink,
                            inactiveTrackColor = Brand.glass,
                        ),
                    )
                    Text("L", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatchRow(selected: String, onSelect: (String) -> Unit) {
    data class Swatch(val style: String, val labelRes: Int, val colors: List<Color>)
    val swatches = listOf(
        Swatch(Settings.STYLE_DUSK, R.string.setup_style_dusk, listOf(Color(0xFF8A1D38), Color(0xFF1B3BB0))),
        Swatch(Settings.STYLE_MIDNIGHT, R.string.setup_style_midnight, listOf(Color(0xFF141925), Color(0xFF46E0C8))),
        Swatch(Settings.STYLE_AURORA, R.string.setup_style_aurora, listOf(Color(0xFFFF9AD0), Color(0xFF4FB4FF))),
        Swatch(Settings.STYLE_FOREST, R.string.setup_style_forest, listOf(Color(0xFF14241C), Color(0xFF5FD66C))),
        Swatch(Settings.STYLE_SPORT, R.string.setup_style_sport, listOf(Color(0xFF111111), Color(0xFFFF3B3B))),
        Swatch(Settings.STYLE_LIGHT, R.string.setup_style_light, listOf(Color.White, Color(0xFFC9D8F5))),
    )
    Column {
        SectionLabel(stringResource(R.string.setup_choice_style))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            swatches.forEach { s ->
                val on = s.style == selected
                Column(Modifier.weight(1f).clickable { onSelect(s.style) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(s.colors))
                            .then(if (on) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(14.dp)) else Modifier),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(s.labelRes), style = MaterialTheme.typography.labelSmall,
                        color = if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun Segmented2(title: String, leftLabel: String, rightLabel: String, right: Boolean, onChange: (Boolean) -> Unit) {
    Column {
        SectionLabel(title)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SegChip(leftLabel, !right, Modifier.weight(1f)) { onChange(false) }
            SegChip(rightLabel, right, Modifier.weight(1f)) { onChange(true) }
        }
    }
}

@Composable
private fun Segmented3(title: String, l1: String, s1: Boolean, l2: String, s2: Boolean, l3: String, s3: Boolean, onSelect: (Int) -> Unit) {
    Column {
        SectionLabel(title)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SegChip(l1, s1, Modifier.weight(1f)) { onSelect(0) }
            SegChip(l2, s2, Modifier.weight(1f)) { onSelect(1) }
            SegChip(l3, s3, Modifier.weight(1f)) { onSelect(2) }
        }
    }
}

@Composable
private fun SegChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(36.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) Brand.gradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** My cars tab: connection status + live data while connected, a now-playing card, and known cars otherwise. */
@Composable
private fun CarsTab(settings: Settings, state: ProjectionService.State, previewRuntime: CarRuntime) {
    val car by CarData.state.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize().systemBarsPadding(),
        contentPadding = TabContentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text(stringResource(R.string.setup_my_cars_title), style = MaterialTheme.typography.headlineSmall) }
        if (state.connected) {
            item { ConnectedStatusCard(state) }
            item { DataTilesRow(car) }
            item { NowPlayingCard(previewRuntime.media) }
        } else {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(20.dp)) {
                    Text(stringResource(R.string.notif_status_not_connected), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.setup_status_disconnected_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        val cars = settings.knownCars
        if (cars.isNotEmpty()) item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(vertical = 8.dp)) {
                cars.entries.sortedByDescending { it.value }.forEach { (name, time) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(R.string.setup_car_last_connected, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(time)),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { settings.forgetCar(name) }) { Text(stringResource(R.string.setup_action_forget)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectedStatusCard(state: ProjectionService.State) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1F9D6E).copy(alpha = 0.35f), Color(0xFF5766E8).copy(alpha = 0.3f))))
            .border(1.dp, Color(0xFF6EE8CC).copy(alpha = 0.35f), RoundedCornerShape(28.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF6EE8CC)))
            Spacer(Modifier.width(8.dp))
            Text(
                (stringResource(R.string.setup_status_connected_caps) + " · " + state.transport).uppercase(),
                style = MaterialTheme.typography.labelMedium, color = Color(0xFF6EE8CC), fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(state.status, style = MaterialTheme.typography.headlineSmall)
                Text(state.video, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val ctx = LocalContext.current
            Pill(stringResource(R.string.setup_disconnect), null) { ProjectionService.stop(ctx) }
        }
    }
}

@Composable
private fun DataTilesRow(car: CarData.State) {
    val speed = car.speed?.let { "%.0f".format(it.value.metersPerSecond * 3.6f) } ?: "–"
    val gear = car.gearName ?: "–"
    val range = car.fuel?.value?.rangeKm?.toString() ?: "–"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DataTile(speed, stringResource(R.string.car_data_speed_unit), Modifier.weight(1f))
        DataTile(gear, stringResource(R.string.car_data_gear), Modifier.weight(1f))
        DataTile(range, stringResource(R.string.car_data_range_unit), Modifier.weight(1f))
    }
}

@Composable
private fun DataTile(value: String, label: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NowPlayingCard(media: net.prototux.aare.media.MediaHub) {
    val playing by media.playback.collectAsState()
    val np = playing ?: return
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Color(0xFFFF7A59), Color(0xFF4A6BFF)))))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(np.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(np.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable { media.playPause() },
            contentAlignment = Alignment.Center,
        ) { Icon(if (np.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color(0xFF1A1030)) }
    }
}

/** Settings tab: every category row, colored, replacing the old single flat scrolling list. */
@Composable
private fun SettingsTab(open: (Page) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().systemBarsPadding(),
        contentPadding = TabContentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text(stringResource(R.string.setup_app_entry_settings), style = MaterialTheme.typography.headlineSmall) }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainer)) {
                Page.entries.forEach { CategoryRow(it) { open(it) } }
            }
        }
    }
}

@Composable
private fun Header(onSettingsClick: () -> Unit = {}) {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when { hour < 12 -> stringResource(R.string.greeting_morning); hour < 18 -> stringResource(R.string.greeting_afternoon); else -> stringResource(R.string.greeting_evening) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(
            androidx.compose.ui.res.painterResource(R.mipmap.ic_launcher), null,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(greeting, style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.setup_tagline), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Brand.glass).border(1.dp, Brand.glassLine, CircleShape)
                .clickable(onClick = onSettingsClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Settings, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(20.dp)) {
        SectionLabel(title)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/** A quiet warning when a newer release is on GitHub: tap to view it, or dismiss it. No popup. */
@Composable
private fun UpdateBanner() {
    val ctx = LocalContext.current
    val update by UpdateChecker.available.collectAsState()
    update?.let { u ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.tertiaryContainer)
                .clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u.url))) }
                .padding(start = 18.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.SystemUpdate, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.update_available_title, u.version), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(stringResource(R.string.update_available_subtitle, net.prototux.aare.BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            IconButton(onClick = { UpdateChecker.dismiss(ctx) }) {
                Icon(Icons.Rounded.Close, stringResource(R.string.common_dismiss), tint = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}

/** F-Auto's own brand look (the pink→indigo gradient from the Dusk/phone mockups) — fixed, not tied to whichever car-screen theme is picked. */
private object Brand {
    val pink = Color(0xFFFF4D63)
    val blue = Color(0xFF8B97FF)
    val gradient = Brush.linearGradient(listOf(pink, blue))
    val glass = Color.White.copy(alpha = 0.14f)
    val glassLine = Color.White.copy(alpha = 0.18f)
}

/** A section heading in the mockup's style: small, uppercase, tracked, muted. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold,
    )
}

/** A translucent "glass" pill (the mockup's `.sec` button) for secondary actions like Preview/Disconnect/Trust. */
@Composable
private fun Pill(label: String, icon: ImageVector?, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(24.dp)).background(Brand.glass).border(1.dp, Brand.glassLine, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(it, null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

// ---------------------------------------------------------------- setup checklist

@Composable
private fun Setup(state: ProjectionService.State, refresh: Int, recheck: () -> Unit, open: (Page) -> Unit) {
    val ctx = LocalContext.current
    fun granted(p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
    val activity = ctx as? android.app.Activity
    val settings = remember { Settings(ctx) }
    fun rationale(p: String) = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, p)
    // The rationale of each permission just before the request, and the ones asked for in an earlier one.
    var rationaleBefore by remember { mutableStateOf(emptySet<String>()) }
    var askedBefore by remember { mutableStateOf(emptySet<String>()) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        recheck()
        // Denied twice, a permission is no longer asked for: the request answers at once and the row
        // would seem dead, so open AARE's system settings to grant it there.
        val blocked = activity != null && result.filterValues { !it }.keys.any {
            permissionBlocked(it in rationaleBefore, rationale(it), it in askedBefore)
        }
        if (blocked) ctx.startSafely(appDetails(ctx))
    }
    // Only the permissions this Android version has: a newer one reads as denied on older releases and
    // can't be requested, so setup would never finish. Before 12, Bluetooth and Wi-Fi scans need location.
    // Android 12+ ignores a request for fine location without coarse; precise stays required (wireless
    // before 13, car apps' location).
    val runtimePerms = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.ACCESS_COARSE_LOCATION); add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { add(Manifest.permission.BLUETOOTH_CONNECT); add(Manifest.permission.BLUETOOTH_SCAN) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { add(Manifest.permission.POST_NOTIFICATIONS); add(Manifest.permission.NEARBY_WIFI_DEVICES) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) add(Manifest.permission.ACCESS_LOCAL_NETWORK)
        addAll(listOf(Manifest.permission.CALL_PHONE, Manifest.permission.ANSWER_PHONE_CALLS, Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALENDAR))
    }
    @Suppress("UNUSED_EXPRESSION") refresh
    val perms = runtimePerms.all(::granted)
    val notificationAccess = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
    val audio = CaptureMethods.audioPolicyAvailable(ctx) || CaptureMethods.shizukuAvailable() || CaptureMethods.rootAvailable(ctx) || state.audioCapture
    if (perms && notificationAccess && audio) return

    Section(stringResource(R.string.setup_finish_title)) {
        Check(stringResource(R.string.setup_check_permissions_title), stringResource(R.string.setup_check_permissions_subtitle), perms) {
            rationaleBefore = runtimePerms.filter(::rationale).toSet()
            askedBefore = settings.requestedPermissions
            settings.requestedPermissions = askedBefore + runtimePerms
            permissions.launch(runtimePerms.toTypedArray())
        }
        Check(stringResource(R.string.setup_check_notification_access_title), stringResource(R.string.setup_check_notification_access_subtitle), notificationAccess) {
            ctx.startSafely(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS), appDetails(ctx))
        }
        Check(stringResource(R.string.audio_settings_title), stringResource(R.string.setup_check_audio_subtitle), audio) { open(Page.AUDIO) }
    }
}

@Composable
private fun Check(title: String, subtitle: String, done: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(enabled = !done, onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null,
            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!done) Text(stringResource(R.string.setup_action_set_up), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Whether a permission the request just denied can no longer be asked for (main setup row). Since Android 11,
 * backing out of the dialog also denies with no rationale, as before any request: so it counts as refused for
 * good only if the rationale went from shown to gone (refused a second time), or it was asked for in an earlier
 * request and still has none (blocked back then).
 */
internal fun permissionBlocked(rationaleBefore: Boolean, rationaleAfter: Boolean, askedBefore: Boolean): Boolean =
    !rationaleAfter && (rationaleBefore || askedBefore)

/** AARE's page in the system settings, where every permission and access can be granted by hand. */
private fun appDetails(ctx: Context) =
    Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))

/**
 * Opens a settings screen or a link. Some phones lack the screen (Go builds and OEM ROMs drop some)
 * or have no browser: then [fallback] opens instead, or a short message, rather than a crash. No
 * resolveActivity() check first: package visibility hides browsers from it without a <queries> entry.
 */
private fun Context.startSafely(intent: Intent, fallback: Intent? = null) {
    val opened = listOfNotNull(intent, fallback).any { runCatching { startActivity(it) }.isSuccess }
    if (!opened) Toast.makeText(this, R.string.setup_nothing_to_open, Toast.LENGTH_SHORT).show()
}

// ---------------------------------------------------------------- settings

@Composable
private fun ConnectionSettings(settings: Settings, refresh: Int) {
    var wireless by remember { mutableStateOf(settings.wireless) }
    var checkUpdates by remember { mutableStateOf(settings.checkUpdates) }
    var fps60 by remember { mutableStateOf(settings.prefer60fps) }
    var maxH by remember { mutableIntStateOf(settings.maxVideoHeight) }
    var forceH264 by remember { mutableStateOf(settings.forceH264) }
    var resume by remember { mutableStateOf(settings.resumeMedia) }
    var cars by remember { mutableStateOf(settings.knownCars) }
    var carGps by remember { mutableStateOf(settings.carGpsForPhone) }
    var btAddr by remember { mutableStateOf(settings.phoneBluetoothAddress) }
    var connectNowTarget by remember { mutableStateOf(settings.wirelessConnectNowTarget) }
    var compatMode by remember { mutableStateOf(settings.wirelessCompatMode) }
    val ctx = LocalContext.current
    val pinned = remember { net.prototux.aare.PinnedHeadUnits(ctx) }
    var pins by remember { mutableStateOf(pinned.all) }
    // A head unit trusted from the notification while this screen is open shows up right away.
    DisposableEffect(pinned) { onDispose(pinned.listen { pins = pinned.all }) }
    // Read here, not in the Section's lambda, so coming back from Developer options re-checks it.
    @Suppress("UNUSED_EXPRESSION") refresh
    val mockAllowed = PhoneLocationFeed.allowed(ctx)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Section(stringResource(R.string.setup_section_connecting)) {
            Toggle(stringResource(R.string.setup_toggle_wireless_title), stringResource(R.string.setup_toggle_wireless_subtitle), wireless) { wireless = it; settings.wireless = it; if (it) ProjectionService.probeWireless(ctx) }
            Toggle(stringResource(R.string.setup_toggle_resume_music_title), stringResource(R.string.setup_toggle_resume_music_subtitle), resume) { resume = it; settings.resumeMedia = it }
            Spacer(Modifier.height(8.dp))
            Choices(stringResource(R.string.setup_choice_wireless_compat), listOf(
                Settings.WIRELESS_COMPAT_AUTO to stringResource(R.string.setup_choice_wireless_compat_auto),
                Settings.WIRELESS_COMPAT_ON to stringResource(R.string.setup_choice_wireless_compat_on),
                Settings.WIRELESS_COMPAT_OFF to stringResource(R.string.setup_choice_wireless_compat_off),
            ), compatMode) { compatMode = it; settings.wirelessCompatMode = it }
            Text(stringResource(R.string.setup_choice_wireless_compat_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = {
                settings.forgetWirelessProfiles()
                Toast.makeText(ctx, R.string.setup_wireless_compat_forgotten, Toast.LENGTH_SHORT).show()
            }) { Text(stringResource(R.string.setup_wireless_compat_forget)) }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(btAddr, { btAddr = it; settings.phoneBluetoothAddress = it.trim() }, Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.setup_bt_address_label)) },
                supportingText = { Text(stringResource(R.string.setup_bt_address_hint)) },
                singleLine = true)
            Spacer(Modifier.height(8.dp))
            // Fortuner fork: dial a bonded device directly over RFCOMM once Bluetooth is already
            // connected, the way D:\aa-phone-side (stage1) does, without waiting for Android to have
            // cached that device's wireless-Android-Auto UUID (some adapters never answer that SDP
            // query while the system's own Android Auto is disabled).
            OutlinedTextField(connectNowTarget, { connectNowTarget = it; settings.wirelessConnectNowTarget = it.trim() }, Modifier.fillMaxWidth(),
                label = { Text("Connect now: bonded device name contains") }, singleLine = true)
            TextButton(onClick = {
                val target = connectNowTarget.trim()
                val connector = net.prototux.aare.transport.wireless.WirelessConnector(ctx, net.prototux.aare.DiagAapLog)
                val matches = runCatching { connector.findBondedByName(target) }.getOrElse {
                    Toast.makeText(ctx, "Can't look up bonded devices: ${it.message}", Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                val device = matches.firstOrNull()
                if (device == null) {
                    Toast.makeText(ctx, "No bonded device with a name containing \"$target\"", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(ctx, "Connecting to ${device.name ?: device.address}…", Toast.LENGTH_SHORT).show()
                    ProjectionService.startWireless(ctx, device.address)
                }
            }) { Text("Connect now") }
        }
        Section(stringResource(R.string.setup_section_updates)) {
            Toggle(stringResource(R.string.setup_toggle_updates_title), stringResource(R.string.setup_toggle_updates_subtitle), checkUpdates) {
                checkUpdates = it; settings.checkUpdates = it; UpdateChecker.refresh(ctx)
            }
        }
        Section(stringResource(R.string.setup_section_location)) {
            Toggle(stringResource(R.string.setup_toggle_car_gps_title), stringResource(R.string.setup_toggle_car_gps_subtitle), carGps) {
                carGps = it; settings.carGpsForPhone = it
            }
            if (carGps) Check(stringResource(R.string.setup_check_mock_location_title), stringResource(R.string.setup_check_mock_location_subtitle), mockAllowed) {
                ctx.startSafely(Intent(AndroidSettings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS), appDetails(ctx))
            }
        }
        Section(stringResource(R.string.setup_section_video)) {
            Toggle(stringResource(R.string.setup_toggle_60fps_title), stringResource(R.string.setup_toggle_60fps_subtitle), fps60) { fps60 = it; settings.prefer60fps = it }
            Choices(stringResource(R.string.setup_choice_max_resolution), listOf(480, 720, 1080, 1440, 2160).map { "$it" to "${it}p" }, "$maxH") {
                maxH = it.toInt(); settings.maxVideoHeight = maxH
            }
            Text(stringResource(R.string.setup_hint_lower_resolution), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Toggle(stringResource(R.string.setup_toggle_force_h264_title), stringResource(R.string.setup_toggle_force_h264_subtitle), forceH264) { forceH264 = it; settings.forceH264 = it }
        }
        Section(stringResource(R.string.setup_section_your_cars)) {
            if (cars.isEmpty()) Text(stringResource(R.string.setup_cars_empty), style = MaterialTheme.typography.bodyMedium)
            cars.entries.sortedByDescending { it.value }.forEach { (name, time) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.setup_car_last_connected, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(time)),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { settings.forgetCar(name); cars = settings.knownCars }) { Text(stringResource(R.string.setup_action_forget)) }
                }
            }
        }
        Section(stringResource(R.string.setup_section_trusted_head_units)) {
            Text(stringResource(R.string.setup_trusted_head_units_hint),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (pins.isEmpty()) Text(stringResource(R.string.setup_trusted_head_units_empty), style = MaterialTheme.typography.bodyMedium)
            pins.entries.sortedBy { it.key }.forEach { (key, shas) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(headUnitLabel(ctx, key), style = MaterialTheme.typography.bodyLarge)
                        // stringResource can't run inside joinToString's lambda: get the template once, format each.
                        val fpTemplate = stringResource(R.string.setup_sha256_fingerprint)
                        Text(shas.joinToString { fpTemplate.format(it.take(16)) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { pinned.forget(key); pins = pinned.all }) { Text(stringResource(R.string.setup_action_forget)) }
                }
            }
        }
    }
}

/** A trusted head unit's key as the user knows it: the paired car's name, the USB accessory, or the developer server. */
@android.annotation.SuppressLint("MissingPermission") // wrapped: without BLUETOOTH_CONNECT the address is shown
private fun headUnitLabel(ctx: android.content.Context, key: String): String = when {
    key == "tcp" -> ctx.getString(R.string.huserver_label)
    key.startsWith("usb:") -> key.removePrefix("usb:").replace('|', ' ').trim().ifBlank { key }
    else -> runCatching { ctx.getSystemService(android.bluetooth.BluetoothManager::class.java)?.adapter?.getRemoteDevice(key)?.name }.getOrNull() ?: key
}

/** Android Auto's developer "head unit server": for the desktop head unit and other test tools. */
@Composable
private fun HeadUnitServerSettings(settings: Settings) {
    val ctx = LocalContext.current
    var huServer by remember { mutableStateOf(settings.headUnitServer) }
    var lan by remember { mutableStateOf(settings.headUnitServerLan) }
    // The service switches the LAN option off when a session ends: pick that up.
    val connected = ProjectionService.state.collectAsState().value.connected
    LaunchedEffect(connected) { lan = settings.headUnitServerLan }
    var lanOnce by remember { mutableStateOf(settings.headUnitServerLanOnce) }
    var mirror by remember { mutableStateOf(settings.headUnitServerMirror) }
    var localhost by remember { mutableStateOf(settings.headUnitServerLocalhost) }
    Section(stringResource(R.string.huserver_label)) {
        Toggle(stringResource(R.string.huserver_label), stringResource(R.string.huserver_toggle_subtitle), huServer) {
            huServer = it; settings.headUnitServer = it
            // Off only closes the listener: the service stays for a session, a wireless attempt or the
            // audio consent, and goes away by itself when it has nothing left.
            ProjectionService.startIdleIfEnabled(ctx)
        }
        if (huServer) Toggle(stringResource(R.string.huserver_toggle_network_title), stringResource(R.string.huserver_toggle_network_subtitle), lan) {
            lan = it; settings.headUnitServerLan = it
            ProjectionService.startIdleIfEnabled(ctx)
        }
        if (huServer) Toggle(stringResource(R.string.huserver_toggle_lanonce_title), stringResource(R.string.huserver_toggle_lanonce_subtitle), lanOnce) {
            lanOnce = it; settings.headUnitServerLanOnce = it
        }
        if (huServer) Toggle(stringResource(R.string.huserver_toggle_mirror_title), stringResource(R.string.huserver_toggle_mirror_subtitle), mirror) {
            mirror = it; settings.headUnitServerMirror = it
        }
        if (huServer) Toggle(stringResource(R.string.huserver_toggle_localhost_title), stringResource(R.string.huserver_toggle_localhost_subtitle), localhost) {
            localhost = it; settings.headUnitServerLocalhost = it
            ProjectionService.startIdleIfEnabled(ctx)
        }
    }
}

@Composable
private fun MessageSettings(settings: Settings, refresh: Int) {
    val ctx = LocalContext.current
    var banners by remember { mutableStateOf(settings.messageBanners) }
    var groups by remember { mutableStateOf(settings.groupMessages) }
    var aloud by remember { mutableStateOf(settings.readMessagesAloud) }
    var dnd by remember { mutableStateOf(settings.dndWhileDriving) }
    var autoReply by remember { mutableStateOf(settings.autoReply) }
    var replyText by remember { mutableStateOf(settings.autoReplyText) }
    @Suppress("UNUSED_EXPRESSION") refresh
    val access = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Section(stringResource(R.string.msg_settings_title)) {
        if (!access) Check(stringResource(R.string.setup_check_notification_access_title), stringResource(R.string.msg_check_notification_access_subtitle), false) {
            ctx.startSafely(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS), appDetails(ctx))
        }
        Toggle(stringResource(R.string.msg_toggle_banners_title), stringResource(R.string.msg_toggle_banners_subtitle), banners) { banners = it; settings.messageBanners = it }
        Toggle(stringResource(R.string.msg_toggle_group_title), stringResource(R.string.msg_toggle_group_subtitle), groups) { groups = it; settings.groupMessages = it }
        Toggle(stringResource(R.string.msg_toggle_read_aloud_title), stringResource(R.string.msg_toggle_read_aloud_subtitle), aloud) { aloud = it; settings.readMessagesAloud = it }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.msg_hint_apps),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Section(stringResource(R.string.msg_section_while_driving)) {
        Toggle(stringResource(R.string.msg_toggle_dnd_title), stringResource(R.string.msg_toggle_dnd_subtitle), dnd) {
            dnd = it; settings.dndWhileDriving = it
        }
        if (dnd) Check(stringResource(R.string.msg_check_dnd_access_title), stringResource(R.string.msg_check_dnd_access_subtitle), DrivingMode.accessGranted(ctx)) {
            ctx.startSafely(Intent(AndroidSettings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS), appDetails(ctx))
        }
        Toggle(stringResource(R.string.msg_toggle_autoreply_title), stringResource(R.string.msg_toggle_autoreply_subtitle), autoReply) {
            autoReply = it; settings.autoReply = it
        }
        if (autoReply) OutlinedTextField(replyText, { replyText = it; settings.autoReplyText = it }, Modifier.fillMaxWidth().padding(top = 8.dp),
            label = { Text(stringResource(R.string.msg_autoreply_field_label)) }, singleLine = true)
    }
    }
}

@Composable
private fun Toggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun CarScreenSettings(settings: Settings, refresh: Int, onStyleChange: (String) -> Unit = {}) {
    val ctx = LocalContext.current
    var theme by remember { mutableStateOf(settings.carTheme) }
    var style by remember { mutableStateOf(settings.carStyle) }
    var layout by remember { mutableStateOf(settings.carLayout) }
    var phoneScreen by remember { mutableStateOf(settings.phoneScreen) }
    var darkBar by remember { mutableStateOf(settings.darkBar) }
    var passenger by remember { mutableStateOf(settings.ignoreDriving) }
    var weather by remember { mutableStateOf(settings.showWeather) }
    var wallpaper by remember { mutableStateOf(settings.wallpaper) }
    val scope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { saveWallpaper(ctx, uri) }
            if (ok) {
                // Through "none", so the car reloads the photo even if one was already set.
                settings.wallpaper = "none"; settings.wallpaper = Settings.WALLPAPER_CUSTOM
                wallpaper = Settings.WALLPAPER_CUSTOM
            } else {
                Toast.makeText(ctx, R.string.setup_wallpaper_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    var calendar by remember { mutableStateOf(settings.showCalendar) }
    var autoSpeaker by remember { mutableStateOf(settings.autoSpeakerOnCall) }
    @Suppress("UNUSED_EXPRESSION") refresh
    val touch = AndroidSettings.Secure.getString(ctx.contentResolver, AndroidSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        .split(':').any { ComponentName.unflattenFromString(it) == ComponentName(ctx, MirrorTouchService::class.java) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Section(stringResource(R.string.setup_section_look)) {
            Choices(stringResource(R.string.setup_choice_colors), listOf(Settings.THEME_AUTO to stringResource(R.string.setup_choice_colors_auto), Settings.THEME_DARK to stringResource(R.string.setup_choice_dark), Settings.THEME_LIGHT to stringResource(R.string.setup_choice_light)), theme) {
                theme = it; settings.carTheme = it
            }
            Text(stringResource(R.string.setup_choice_colors_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Choices(stringResource(R.string.setup_choice_style), listOf(
                Settings.STYLE_MIDNIGHT to stringResource(R.string.setup_style_midnight),
                Settings.STYLE_AURORA to stringResource(R.string.setup_style_aurora),
                Settings.STYLE_FOREST to stringResource(R.string.setup_style_forest),
                Settings.STYLE_LIGHT to stringResource(R.string.setup_style_light),
                Settings.STYLE_SPORT to stringResource(R.string.setup_style_sport),
                Settings.STYLE_DUSK to stringResource(R.string.setup_style_dusk),
            ), style) {
                style = it; settings.carStyle = it; onStyleChange(it)
            }
            Spacer(Modifier.height(12.dp))
            Choices(stringResource(R.string.setup_choice_layout), listOf(
                Settings.LAYOUT_CLASSIC to stringResource(R.string.setup_layout_classic),
                Settings.LAYOUT_DUSK to stringResource(R.string.setup_layout_dusk),
            ), layout) {
                layout = it; settings.carLayout = it
            }
            Toggle(stringResource(R.string.setup_toggle_dark_bar_title), stringResource(R.string.setup_toggle_dark_bar_subtitle), darkBar) { darkBar = it; settings.darkBar = it }
        }
        Section(stringResource(R.string.setup_section_dashboard)) {
            Choices(stringResource(R.string.setup_choice_wallpaper), listOf("none" to stringResource(R.string.setup_wallpaper_none)) + WALLPAPER_NAMES.map { it to it.replaceFirstChar(Char::uppercase) } +
                listOf(Settings.WALLPAPER_CUSTOM to stringResource(R.string.setup_wallpaper_photo)), wallpaper) { choice ->
                if (choice == Settings.WALLPAPER_CUSTOM) photoPicker.launch("image/*")
                else { wallpaper = choice; settings.wallpaper = choice }
            }
            Spacer(Modifier.height(8.dp))
            Toggle(stringResource(R.string.setup_toggle_weather_title), stringResource(R.string.setup_toggle_weather_subtitle), weather) {
                weather = it; settings.showWeather = it
            }
            Toggle(stringResource(R.string.setup_toggle_calendar_title), stringResource(R.string.setup_toggle_calendar_subtitle), calendar) {
                calendar = it; settings.showCalendar = it
            }
        }
        Section(stringResource(R.string.setup_section_driving)) {
            Toggle(stringResource(R.string.setup_toggle_passenger_title), stringResource(R.string.setup_toggle_passenger_subtitle), passenger) {
                passenger = it; settings.ignoreDriving = it
            }
        }
        Section(stringResource(R.string.phone_section_title)) {
            Toggle(stringResource(R.string.setup_toggle_auto_speaker_title), stringResource(R.string.setup_toggle_auto_speaker_subtitle), autoSpeaker) {
                autoSpeaker = it; settings.autoSpeakerOnCall = it
            }
            Toggle(stringResource(R.string.phone_toggle_enable_title), stringResource(R.string.phone_toggle_enable_subtitle), phoneScreen) {
                phoneScreen = it; settings.phoneScreen = it
            }
            if (phoneScreen) {
                Check(stringResource(R.string.phone_check_touch_title), stringResource(R.string.phone_check_touch_subtitle), touch) {
                    ctx.startSafely(Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS), appDetails(ctx))
                }
                Text(stringResource(R.string.phone_hint_touch_capture),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Copies the chosen photo into AARE's files, at most 1920 pixels on its longest side, as the car's wallpaper. */
private fun saveWallpaper(context: android.content.Context, uri: Uri): Boolean {
    // Written to a temporary file and renamed over the current one only once complete, so a photo
    // that can't be read or written (a full disk) leaves the current wallpaper alone.
    val tmp = java.io.File(context.filesDir, Settings.WALLPAPER_FILE + ".tmp")
    val ok = runCatching {
        val bmp = decodeWallpaper(context, uri, 1920)
        tmp.outputStream().use { check(bmp.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
        check(tmp.renameTo(java.io.File(context.filesDir, Settings.WALLPAPER_FILE)))
    }.isSuccess
    if (!ok) tmp.delete()
    return ok
}

/**
 * Decodes [uri] upright (cameras store portrait photos sideways with an EXIF orientation tag) and
 * already scaled down towards [maxPx], so a 200 MP photo is never decoded at full size.
 */
private fun decodeWallpaper(context: android.content.Context, uri: Uri, maxPx: Int): Bitmap {
    val resolver = context.contentResolver
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // ImageDecoder applies the EXIF orientation itself.
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // compress() needs a software bitmap
            val (w, h) = info.size.width to info.size.height
            val scale = maxPx.toFloat() / maxOf(w, h)
            if (scale < 1f) decoder.setTargetSize((w * scale).roundToInt().coerceAtLeast(1), (h * scale).roundToInt().coerceAtLeast(1))
        }
    }
    // Android 8: a power-of-two sample size, then the exact scale and the EXIF rotation in one matrix.
    // Each pass opens its own stream: a content stream can be read only once.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
    val src = resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        ?: error("can't decode the photo")
    val orientation = runCatching {
        resolver.openInputStream(uri)!!.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val scale = minOf(1f, maxPx.toFloat() / maxOf(src.width, src.height))
    val m = Matrix().apply {
        postScale(scale, scale)
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(-90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(-90f)
        }
    }
    val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    if (out !== src) src.recycle()
    return out
}

/** Car audio: the capture methods, best first, and what each needs. */
@Composable
private fun AudioSettings(settings: Settings, state: ProjectionService.State, refresh: Int, recheck: () -> Unit) {
    val ctx = LocalContext.current
    var enabled by remember { mutableStateOf(settings.carAudio) }
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == android.app.Activity.RESULT_OK && r.data != null) ProjectionService.giveAudioConsent(ctx, r.resultCode, r.data!!)
        recheck()
    }
    DisposableEffect(Unit) {
        val l = Shizuku.OnRequestPermissionResultListener { _, _ -> recheck() }
        runCatching { Shizuku.addRequestPermissionResultListener(l) }
        onDispose { runCatching { Shizuku.removeRequestPermissionResultListener(l) } }
    }
    @Suppress("UNUSED_EXPRESSION") refresh
    val policy = CaptureMethods.audioPolicyAvailable(ctx)
    val shizuku = CaptureMethods.shizukuAvailable()
    val shizukuAsk = CaptureMethods.shizukuNeedsPermission()
    val root = CaptureMethods.rootAvailable(ctx)
    val best = when {
        policy -> CaptureMethod.AUDIO_POLICY
        shizuku -> CaptureMethod.SHIZUKU_POLICY
        root -> CaptureMethod.SHIZUKU
        state.audioCapture -> CaptureMethod.PLAYBACK_CAPTURE
        else -> null
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Section(stringResource(R.string.audio_settings_title)) {
            Toggle(stringResource(R.string.audio_toggle_enable_title), stringResource(R.string.audio_toggle_enable_subtitle), enabled) {
                enabled = it; settings.carAudio = it
            }
            Spacer(Modifier.height(4.dp))
            Text(best?.let { stringResource(R.string.audio_using_method, it.label) } ?: stringResource(R.string.audio_no_method),
                style = MaterialTheme.typography.bodyMedium, color = if (best == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
        Section(stringResource(R.string.audio_section_methods)) {
            Check(CaptureMethod.AUDIO_POLICY.label,
                if (policy) stringResource(R.string.audio_policy_ready)
                else stringResource(R.string.audio_policy_not_ready),
                policy) {
                ctx.startSafely(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/prototux/AARE#car-audio")))
            }
            Check(CaptureMethod.SHIZUKU.label,
                when {
                    shizuku -> stringResource(R.string.audio_shizuku_ready)
                    root -> stringResource(R.string.audio_root_ready)
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> stringResource(R.string.audio_shizuku_needs_android12)
                    shizukuAsk -> stringResource(R.string.audio_shizuku_tap_allow)
                    else -> stringResource(R.string.audio_shizuku_or_root_hint)
                },
                shizuku || root) {
                when {
                    shizukuAsk -> runCatching { Shizuku.requestPermission(SHIZUKU_REQUEST) }
                    shizuku || root -> Unit
                    // Check for root (prompts su on a rooted phone); otherwise point to Shizuku.
                    else -> CaptureMethods.checkRoot { granted ->
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            if (granted) { settings.rootAudio = true; recheck() }
                            else ctx.startSafely(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")))
                        }
                    }
                }
            }
            Check(CaptureMethod.PLAYBACK_CAPTURE.label,
                if (state.audioCapture) stringResource(R.string.audio_playback_ready)
                else stringResource(R.string.audio_playback_not_ready),
                state.audioCapture) {
                val mpm = ctx.getSystemService(MediaProjectionManager::class.java)
                consent.launch(if (Build.VERSION.SDK_INT >= 34) mpm.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
                    else mpm.createScreenCaptureIntent())
            }
        }
    }
}

private const val SHIZUKU_REQUEST = 42

/** A labelled row of mutually exclusive options, in the brand's gradient-chip style. */
@Composable
private fun Choices(title: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Text(title, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            SegChip(label, value == selected, Modifier.widthIn(min = 72.dp)) { onSelect(value) }
        }
    }
}

// ---------------------------------------------------------------- apps

/** Everything the car's launcher lists, as it groups it. */
private class LauncherLists(val carApps: List<CarAppInfo>, val media: List<PhoneApp>, val messaging: List<PhoneApp>)

@Composable
private fun AppsCard(settings: Settings) {
    val ctx = LocalContext.current
    var hidden by remember { mutableStateOf(settings.hiddenApps) }
    var phoneScreen by remember { mutableStateOf(settings.phoneScreen) }
    var browser by remember { mutableStateOf(settings.browser) }
    var browserUrl by remember { mutableStateOf(settings.browserUrl) }
    // Same rules as the car's launcher (CarRuntime.applyApps): an app with a car app shows once, as that.
    val lists by produceState<LauncherLists?>(null) {
        value = withContext(Dispatchers.IO) {
            val car = CarAppCatalog.query(ctx)
            val carPkgs = car.mapTo(HashSet()) { it.packageName }
            val media = AndroidAutoApps.mediaApps(ctx).filter { it.packageName !in carPkgs }
            val shown = carPkgs + media.map { it.packageName }
            LauncherLists(car, media, AndroidAutoApps.messagingApps(ctx).filter { it.packageName !in shown })
        }
    }
    fun toggle(pkg: String, visible: Boolean) {
        hidden = if (visible) hidden - pkg else hidden + pkg
        settings.hiddenApps = hidden
    }
    val l = lists
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Section(stringResource(R.string.setup_section_app_list)) {
            Text(stringResource(R.string.setup_app_list_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (l == null) {
                Text(stringResource(R.string.setup_app_list_loading), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 12.dp))
                return@Section
            }
            AppGroup(stringResource(R.string.setup_app_group_nav))
            if (l.carApps.isEmpty()) Text(stringResource(R.string.setup_app_list_no_nav), style = MaterialTheme.typography.bodyMedium)
            val navigationKind = stringResource(R.string.setup_app_kind_navigation)
            val genericKind = stringResource(R.string.setup_app_kind_generic)
            l.carApps.forEach { app ->
                val kind = if (app.isNavigation) navigationKind
                    else app.categories.firstOrNull()?.substringAfterLast('.')?.lowercase()?.replaceFirstChar { it.uppercase() } ?: genericKind
                AppRow(app.icon, app.label, kind, app.packageName !in hidden) { toggle(app.packageName, it) }
            }
            if (l.media.isNotEmpty()) {
                AppGroup(stringResource(R.string.setup_app_group_media))
                val mediaKind = stringResource(R.string.setup_app_kind_media)
                l.media.forEach { app -> AppRow(app.icon, app.label, mediaKind, app.packageName !in hidden) { toggle(app.packageName, it) } }
            }
            if (l.messaging.isNotEmpty()) {
                AppGroup(stringResource(R.string.setup_app_group_messaging))
                val messagingKind = stringResource(R.string.setup_app_kind_messaging)
                l.messaging.forEach { app -> AppRow(app.icon, app.label, messagingKind, app.packageName !in hidden) { toggle(app.packageName, it) } }
            }
            AppGroup(stringResource(R.string.setup_app_group_aare))
            AppRow(null, stringResource(R.string.phone_section_title), stringResource(R.string.phone_app_row_subtitle), phoneScreen) { phoneScreen = it; settings.phoneScreen = it }
            AppRow(null, stringResource(R.string.browser_app_label), stringResource(R.string.browser_app_row_subtitle), browser) { browser = it; settings.browser = it }
            if (browser) {
                OutlinedTextField(
                    value = browserUrl, onValueChange = { browserUrl = it; settings.browserUrl = it },
                    label = { Text(stringResource(R.string.browser_url_label)) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text(stringResource(R.string.browser_hint_parked), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
        if (l != null) AppOrder(settings, l, hidden, phoneScreen, browser)
        StandaloneAppsSection(settings, ctx)
        val unsupported by produceState(emptyList<UnsupportedCarApp>(), l) {
            if (l != null) value = withContext(Dispatchers.IO) { AndroidAutoApps.unsupported(ctx, l.carApps.map { it.packageName }) }
        }
        if (unsupported.isNotEmpty()) {
            Section(stringResource(R.string.setup_section_unsupported_apps)) {
                unsupported.forEach { UnsupportedRow(it) }
            }
        }
        Text(stringResource(R.string.setup_hint_allowlist),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The order of the car's app list: the visible apps, with up and down buttons. */
@Composable
private fun AppOrder(settings: Settings, l: LauncherLists, hidden: Set<String>, phoneScreen: Boolean, browser: Boolean) {
    // Keys as CarRuntime.orderKey: packages, or the built-in apps' ids.
    val all = buildList {
        l.carApps.forEach { add(Triple(it.packageName, it.label, it.icon)) }
        l.media.forEach { add(Triple(it.packageName, it.label, it.icon)) }
        l.messaging.forEach { add(Triple(it.packageName, it.label, it.icon)) }
    }.filter { it.first !in hidden } + listOfNotNull(
        Triple(CarRuntime.PHONE_ID, stringResource(R.string.setup_app_entry_phone), null),
        Triple(CarRuntime.SETTINGS_ID, stringResource(R.string.setup_app_entry_settings), null),
        Triple(CarRuntime.NOTIFICATIONS_ID, stringResource(R.string.setup_app_entry_notifications), null),
        if (phoneScreen) Triple(CarRuntime.PHONE_SCREEN_ID, stringResource(R.string.phone_section_title), null) else null,
        if (browser) Triple(CarRuntime.BROWSER_ID, stringResource(R.string.browser_app_label), null) else null,
    )
    var order by remember { mutableStateOf(settings.appOrder) }
    val index = order.withIndex().associate { (i, k) -> k to i }
    val sorted = all.sortedWith(compareBy({ index[it.first] ?: Int.MAX_VALUE }, { it.second.lowercase() }))
    fun move(i: Int, by: Int) {
        val keys = sorted.map { it.first }.toMutableList()
        val j = (i + by).coerceIn(0, keys.lastIndex)
        keys.add(j, keys.removeAt(i))
        // Hidden apps keep their saved slots, so showing one again puts it back where it was:
        // the shown keys fill their old slots in the new order, new ones go at the end.
        val queue = ArrayDeque(keys)
        val shown = keys.toSet()
        val merged = order.mapNotNull { if (it in shown) queue.removeFirstOrNull() else it } + queue
        order = merged; settings.appOrder = merged
    }
    Section(stringResource(R.string.setup_section_app_order)) {
        sorted.forEachIndexed { i, (_, label, icon) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) Image(icon.asImageBitmap(), null, Modifier.size(32.dp).clip(CircleShape))
                else Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer))
                Spacer(Modifier.width(12.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { move(i, -1) }, enabled = i > 0) { Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.common_up)) }
                IconButton(onClick = { move(i, 1) }, enabled = i < sorted.lastIndex) { Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.common_down)) }
            }
        }
    }
}

@Composable
private fun AppGroup(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
}

/** Opt-in: launch ordinary phone apps on the car screen (parked only; needs root or Shizuku). */
@Composable
private fun StandaloneAppsSection(settings: Settings, ctx: android.content.Context) {
    var enabled by remember { mutableStateOf(settings.standaloneApps) }
    var allowed by remember { mutableStateOf(settings.standaloneAppPackages) }
    val available = remember { AppLauncher.available(ctx) }
    Section(stringResource(R.string.standalone_section_title)) {
        Text(stringResource(R.string.standalone_section_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Toggle(stringResource(R.string.standalone_toggle_title), stringResource(R.string.standalone_toggle_subtitle), enabled) {
            enabled = it; settings.standaloneApps = it
        }
        if (enabled && !available) {
            Text(stringResource(R.string.standalone_need_root), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
        if (enabled) {
            Text(stringResource(R.string.standalone_nav_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            val apps by produceState<List<Triple<String, String, android.graphics.Bitmap?>>?>(null) {
                value = withContext(Dispatchers.IO) { launchableApps(ctx) }
            }
            val list = apps
            if (list == null) {
                Text(stringResource(R.string.setup_app_list_loading), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 12.dp))
            } else {
                Spacer(Modifier.height(8.dp))
                list.forEach { (pkg, label, icon) ->
                    AppRow(icon, label, pkg, pkg in allowed) { on ->
                        allowed = if (on) allowed + pkg else allowed - pkg
                        settings.standaloneAppPackages = allowed
                    }
                }
            }
        }
    }
}

/** Every app with a launcher icon, except AARE itself, for the standalone allow-list: navigation apps first. */
private fun launchableApps(ctx: android.content.Context): List<Triple<String, String, android.graphics.Bitmap?>> {
    val pm = ctx.packageManager
    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .mapNotNull { it.activityInfo?.packageName }
        .filter { it != ctx.packageName }
        .distinct()
        .mapNotNull { pkg ->
            val info = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull() ?: return@mapNotNull null
            val icon = runCatching { pm.getApplicationIcon(info).toBitmap(96, 96) }.getOrNull()
            Triple(pkg, pm.getApplicationLabel(info).toString(), icon)
        }.sortedWith(compareBy({ it.first !in net.prototux.aare.host.CarAppInfo.KNOWN_NAVIGATION_PACKAGES }, { it.second.lowercase() }))
}

@Composable
private fun AppRow(icon: android.graphics.Bitmap?, label: String, kind: String, visible: Boolean, onVisible: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onVisible(!visible) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Image(icon.asImageBitmap(), null, Modifier.size(40.dp).clip(CircleShape))
        else Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Text(label.take(1), color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(kind, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = visible, onCheckedChange = onVisible)
    }
}

@Composable
private fun UnsupportedRow(app: UnsupportedCarApp) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        app.icon?.let { Image(it.asImageBitmap(), null, Modifier.size(40.dp).clip(CircleShape)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge)
            Text(app.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------- assistant

/** Which assistant the steering-wheel voice button opens: the phone's default, or one picked here. */
@Composable
private fun AssistantCard(settings: Settings) {
    val ctx = LocalContext.current
    val choices = remember { Assistants.query(ctx, CarAppCatalog.query(ctx)) }
    val phoneDefault = remember { Assistants.phoneDefault(ctx) }
    var chosen by remember { mutableStateOf(settings.assistantPackage) }
    fun choose(pkg: String?) { chosen = pkg; settings.assistantPackage = pkg }
    Section(stringResource(R.string.assistant_settings_title)) {
        val defaultLabel = phoneDefault?.let { pkg -> choices.firstOrNull { it.packageName == pkg }?.label ?: Assistants.label(ctx, pkg) ?: pkg }
        AssistantRow(
            title = stringResource(R.string.assistant_same_as_phone),
            subtitle = defaultLabel ?: stringResource(R.string.assistant_no_default),
            app = null, selected = chosen == null, onSelect = { choose(null) },
        )
        val hasCarAppSubtitle = stringResource(R.string.assistant_has_car_app_subtitle)
        val opensOnPhoneSubtitle = stringResource(R.string.assistant_opens_on_phone)
        choices.forEach { app ->
            AssistantRow(
                title = app.label,
                subtitle = if (app.hasCarApp) hasCarAppSubtitle else opensOnPhoneSubtitle,
                app = app, selected = chosen == app.packageName, onSelect = { choose(app.packageName) },
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.assistant_hint),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AssistantRow(title: String, subtitle: String, app: AssistantApp?, selected: Boolean, onSelect: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.width(8.dp))
        app?.icon?.let {
            Image(it.asImageBitmap(), null, Modifier.size(40.dp).clip(CircleShape))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------- certificates

@Composable
private fun CertificatesCard(certs: CertificateStore, state: ProjectionService.State) {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    var entries by remember { mutableStateOf(certs.entries()) }
    // Saveable: the system picker can outlive the activity, and the key picker's result needs the certificate.
    var pickCert by rememberSaveable { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var p12 by rememberSaveable { mutableStateOf<Uri?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var extracting by remember { mutableStateOf(false) }
    val installedGearheadVersion = remember { GearheadKeyExtractor.installedVersion(ctx) }
    var autoUpdate by remember { mutableStateOf(settings.autoUpdateCert) }
    val scope = rememberCoroutineScope()
    val autoUpdated by CertificateAutoUpdate.lastUpdated.collectAsState()
    val expiringSoon by CertificateAutoUpdate.expiringSoon.collectAsState()
    // CertificateAutoUpdate runs on its own thread; pick up what it imported.
    LaunchedEffect(autoUpdated) { if (autoUpdated != null) entries = certs.entries() }
    val keyPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { keyUri ->
        val certUri = pickCert ?: return@rememberLauncherForActivityResult
        if (keyUri == null) return@rememberLauncherForActivityResult
        error = runCatching {
            val cert = ctx.contentResolver.openInputStream(certUri)!!.use { it.readBytes().decodeToString() }
            val key = ctx.contentResolver.openInputStream(keyUri)!!.use { it.readBytes().decodeToString() }
            certs.importPem(displayName(ctx, certUri), cert, key)
            entries = certs.entries()
        }.exceptionOrNull()?.message
    }
    val certPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Document URIs (msf:1234) carry no extension: ask the provider for the file name and type.
        val name = displayName(ctx, uri)
        val type = ctx.contentResolver.getType(uri)
        if (name.endsWith(".p12", true) || name.endsWith(".pfx", true) || type == "application/x-pkcs12") p12 = uri
        else { pickCert = uri; keyPicker.launch(arrayOf("*/*")) }
    }
    fun runExtraction(block: () -> GearheadKeyExtractor.Extracted) {
        extracting = true
        error = null
        scope.launch {
            error = runCatching {
                val extracted = withContext(Dispatchers.IO) { block() }
                certs.importPem("Android Auto (extracted)", extracted.certPem, extracted.keyPem)
                entries = certs.entries()
            }.exceptionOrNull()?.let { it.message ?: it.toString() }
            extracting = false
        }
    }
    val apkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runExtraction { GearheadKeyExtractor.extractFromUri(ctx, uri) }
    }
    Section(stringResource(R.string.cert_section_title)) {
        Text(stringResource(R.string.cert_hint),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        entries.forEach { e ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.name, style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.cert_entry_subtitle, e.subject.take(60), DateFormat.getDateInstance().format(e.notAfter)), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (e.removable) TextButton(onClick = { certs.remove(e); entries = certs.entries() }) { Text(stringResource(R.string.cert_action_remove)) }
            }
        }
        Row {
            TextButton(onClick = { certPicker.launch(arrayOf("*/*")) }) { Text(stringResource(R.string.cert_action_import)) }
            if (CertificateDownload.ENABLED) TextButton(enabled = !downloading, onClick = {
                downloading = true
                error = null
                scope.launch {
                    error = runCatching {
                        val d = withContext(Dispatchers.IO) { CertificateDownload.fetch() }
                        certs.importDownloaded(d)
                        entries = certs.entries()
                    }.exceptionOrNull()?.let { downloadError(ctx, it) }
                    downloading = false
                }
            }) { Text(stringResource(if (downloading) R.string.cert_action_downloading else R.string.cert_action_download)) }
        }
        Row {
            if (installedGearheadVersion != null) TextButton(enabled = !extracting, onClick = {
                runExtraction { GearheadKeyExtractor.extractFromInstalled(ctx) }
            }) { Text(stringResource(if (extracting) R.string.cert_action_extracting else R.string.cert_action_extract_installed)) }
            TextButton(enabled = !extracting, onClick = { apkPicker.launch(arrayOf("*/*")) }) {
                Text(stringResource(if (extracting) R.string.cert_action_extracting else R.string.cert_action_extract_file))
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        if (CertificateDownload.ENABLED) {
            Spacer(Modifier.height(8.dp))
            Toggle(stringResource(R.string.cert_toggle_auto_update_title), stringResource(R.string.cert_toggle_auto_update_subtitle), autoUpdate) {
                autoUpdate = it; settings.autoUpdateCert = it
            }
            autoUpdated?.let { version ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.cert_auto_updated_message, version), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    IconButton(onClick = { CertificateAutoUpdate.consumed() }) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.common_dismiss), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            expiringSoon?.let { exp ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.cert_expiring_soon_message, exp.name, DateFormat.getDateInstance().format(exp.notAfter)),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    IconButton(onClick = { CertificateAutoUpdate.dismissExpiryWarning(ctx) }) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.common_dismiss), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    p12?.let { uri ->
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { p12 = null },
            title = { Text(stringResource(R.string.cert_p12_dialog_title)) },
            text = { OutlinedTextField(password, { password = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    error = runCatching { certs.importPkcs12(displayName(ctx, uri), uri, password.toCharArray()); entries = certs.entries() }
                        .exceptionOrNull()?.message
                    p12 = null
                }) { Text(stringResource(R.string.cert_action_import_confirm)) }
            },
            dismissButton = { TextButton(onClick = { p12 = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

/** What to tell the user when Download certificate fails. */
private fun downloadError(ctx: Context, e: Throwable): String = when (e) {
    is CertificateDownload.Unavailable -> ctx.getString(R.string.cert_download_unavailable, CertificateDownload.SOURCE)
    is CertificateDownload.Invalid -> ctx.getString(R.string.cert_download_invalid, e.message)
    is java.io.IOException -> ctx.getString(R.string.cert_download_network, e.message ?: e.toString())
    else -> e.message ?: e.toString()
}

/** The picked file's name as the provider shows it, for naming an imported certificate. */
private fun displayName(ctx: Context, uri: Uri): String =
    runCatching {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: ctx.getString(R.string.cert_imported_fallback)

/** One label/value line; renders nothing when the car hasn't reported this value yet. */
@Composable
private fun DataRow(label: String, value: String?) {
    if (value == null) return
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
    }
}

/**
 * A read-only view of everything the head unit tells AARE about the car: its identity and the live
 * value of every sensor it reports (an "OBD-ish" viewer, but for the AAP sensor stream). All values
 * come from [CarData.state] and [ProjectionService.state], which update themselves.
 */
@Composable
private fun CarDataCard() {
    val car by CarData.state.collectAsState()
    val proj by ProjectionService.state.collectAsState()
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    var inCar by remember { mutableStateOf(settings.carDataApp) }
    Section(stringResource(R.string.setup_page_car_data_title)) {
        Toggle(stringResource(R.string.car_data_show_in_car_title), stringResource(R.string.car_data_show_in_car_subtitle), inCar) { inCar = it; settings.carDataApp = it }
        if (!car.connected) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.car_data_not_connected), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (!car.connected) return
    Spacer(Modifier.height(16.dp))
    val on = stringResource(R.string.car_data_on); val off = stringResource(R.string.car_data_off)
    val yes = stringResource(R.string.car_data_yes); val no = stringResource(R.string.car_data_no)
    fun onOff(b: Boolean?) = b?.let { if (it) on else off }
    fun yesNo(b: Boolean?) = b?.let { if (it) yes else no }

    Section(stringResource(R.string.car_data_section_car)) {
        val id = car.identity
        DataRow(stringResource(R.string.car_data_identity),
            id?.let { listOfNotNull(it.make.ifBlank { null }, it.model.ifBlank { null }, it.year?.toString()).joinToString(" ").ifBlank { null } })
        DataRow(stringResource(R.string.car_data_head_unit), proj.headUnit.ifBlank { null })
        DataRow(stringResource(R.string.car_data_connection), proj.transport.ifBlank { null })
        DataRow(stringResource(R.string.car_data_video), proj.video.ifBlank { null })
        if (car.advertised.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.car_data_advertised), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(car.advertised.map { it.name.removePrefix("SENSOR_") }.sorted().joinToString(", "),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Spacer(Modifier.height(16.dp))
    Section(stringResource(R.string.car_data_section_live)) {
        DataRow(stringResource(R.string.car_data_driving), car.drivingStatus?.let { if (it.value == 0) stringResource(R.string.car_data_parked) else stringResource(R.string.car_data_moving) })
        DataRow(stringResource(R.string.car_data_night), car.nightMode?.let { if (it.value) stringResource(R.string.car_data_night_on) else stringResource(R.string.car_data_night_off) })
        DataRow(stringResource(R.string.car_data_speed), car.speed?.let { "%.0f km/h".format(it.value.metersPerSecond * 3.6f) })
        DataRow(stringResource(R.string.car_data_cruise), car.speed?.value?.cruiseSetSpeed?.let { "$it" })
        DataRow(stringResource(R.string.car_data_rpm), car.rpm?.let { "%.0f".format(it.value) })
        DataRow(stringResource(R.string.car_data_gear), car.gearName)
        DataRow(stringResource(R.string.car_data_parking_brake), onOff(car.parkingBrake?.value))
        DataRow(stringResource(R.string.car_data_odometer), car.odometer?.let { o ->
            "%.0f km".format(o.value.km) + (o.value.tripKm?.let { " · trip %.0f km".format(it) } ?: "") })
        DataRow(stringResource(R.string.car_data_fuel), car.fuel?.let { f ->
            listOfNotNull(f.value.levelPercent?.let { "$it%" }, f.value.rangeKm?.let { "$it km" }, if (f.value.low == true) stringResource(R.string.car_data_low) else null)
                .joinToString(" · ").ifBlank { null } })
        DataRow(stringResource(R.string.car_data_compass), car.compass?.let { "%.0f°".format(it.value.bearingDeg) })
        DataRow(stringResource(R.string.car_data_satellites), car.satellites?.let { s -> "${s.value.inUse}" + (s.value.inView?.let { " / $it" } ?: "") })
        DataRow(stringResource(R.string.car_data_environment), car.environment?.let { e ->
            listOfNotNull(e.value.temperatureC?.let { "%.1f °C".format(it) }, e.value.pressureKpa?.let { "%.0f kPa".format(it) }, e.value.rain?.let { if (it > 0) stringResource(R.string.car_data_rain) else null })
                .joinToString(" · ").ifBlank { null } })
        DataRow(stringResource(R.string.car_data_climate), car.hvac?.let { h ->
            listOfNotNull(h.value.targetC?.let { "→ %.0f °C".format(it) }, h.value.currentC?.let { "%.0f °C".format(it) }).joinToString(" · ").ifBlank { null } })
        DataRow(stringResource(R.string.car_data_steering), car.deadReckoning?.value?.steeringAngleDeg?.let { "%.0f°".format(it) })
        DataRow(stringResource(R.string.car_data_tyres), car.tirePressuresKpa?.let { t -> t.value.joinToString(" · ") { "%.0f".format(it) } + " kPa" })
        DataRow(stringResource(R.string.car_data_doors), car.doors?.let { d ->
            val open = buildList {
                if (d.value.hoodOpen == true) add(stringResource(R.string.car_data_hood))
                if (d.value.trunkOpen == true) add(stringResource(R.string.car_data_trunk))
                repeat(d.value.doorsOpen.count { it }) { add(stringResource(R.string.car_data_door)) }
            }
            if (open.isEmpty()) stringResource(R.string.car_data_closed) else open.joinToString(", ") })
        DataRow(stringResource(R.string.car_data_lights), car.lights?.let { l ->
            listOfNotNull(l.value.headLights?.name?.lowercase(), l.value.turn?.takeIf { it != CarData.Turn.NONE }?.name?.lowercase(),
                if (l.value.hazard == true) stringResource(R.string.car_data_hazard) else null).joinToString(" · ").ifBlank { null } })
        DataRow(stringResource(R.string.car_data_passenger), yesNo(car.passengerPresent?.value))
        DataRow(stringResource(R.string.car_data_toll), yesNo(car.tollCardPresent?.value))
        DataRow(stringResource(R.string.car_data_accelerometer), car.accelerometer?.let { a -> xyz(a.value) }?.let { "$it m/s²" })
        DataRow(stringResource(R.string.car_data_gyroscope), car.gyroscope?.let { g -> xyz(g.value) }?.let { "$it rad/s" })
        DataRow(stringResource(R.string.car_data_dtc), car.diagnostics?.value?.takeIf { it.isNotEmpty() }?.joinToString(" ") { "%02x".format(it) })
    }
}

private fun xyz(v: CarData.Vector3): String? =
    listOf(v.x, v.y, v.z).let { if (it.all { c -> c == null }) null else it.joinToString(", ") { c -> c?.let { "%.2f".format(it) } ?: "–" } }

@Composable
private fun VehicleOverridesCard() {
    val ctx = LocalContext.current
    val file = remember { net.prototux.aare.car.VehicleOverrides.file(ctx) }
    var count by remember { mutableIntStateOf(net.prototux.aare.car.VehicleOverrides.ruleCount(ctx)) }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            ctx.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
        }.onFailure { Toast.makeText(ctx, it.message, Toast.LENGTH_SHORT).show() }
        count = net.prototux.aare.car.VehicleOverrides.ruleCount(ctx)
    }
    Section(stringResource(R.string.vehicles_section_title)) {
        Text(stringResource(R.string.vehicles_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.vehicles_rule_count, count), style = MaterialTheme.typography.bodyMedium)
        Text(file.absolutePath, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        Row {
            TextButton(onClick = { import.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text(stringResource(R.string.vehicles_import)) }
            TextButton(onClick = {
                runCatching { file.parentFile?.mkdirs(); file.writeText(net.prototux.aare.car.VehicleOverrides.template()) }
                    .onFailure { Toast.makeText(ctx, it.message, Toast.LENGTH_SHORT).show() }
                count = net.prototux.aare.car.VehicleOverrides.ruleCount(ctx)
                Toast.makeText(ctx, file.absolutePath, Toast.LENGTH_LONG).show()
            }) { Text(stringResource(R.string.vehicles_template)) }
        }
    }
}

@Composable
private fun LogCard() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val log by DiagLog.lines.collectAsState()
    val title = stringResource(R.string.setup_section_connection_log)
    // Writes every retained line to a file the user picks, to attach to a bug report.
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(DiagLog.lines.value.joinToString("\n").toByteArray()) }
        }.onFailure { Toast.makeText(ctx, it.message, Toast.LENGTH_SHORT).show() }
    }
    Section(title) {
        // Always shown, so the trace can be turned on before connecting.
        var verbose by remember { mutableStateOf(settings.verboseProtocolLog) }
        Toggle(stringResource(R.string.setup_toggle_verbose_log_title), stringResource(R.string.setup_toggle_verbose_log_subtitle), verbose) {
            verbose = it; settings.verboseProtocolLog = it; SessionAapLog.verbose = it
        }
        if (log.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            log.takeLast(40).forEach {
                Text(it, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row {
                // Copies every retained line, not only the ones shown above.
                TextButton(onClick = {
                    ctx.getSystemService(ClipboardManager::class.java)
                        ?.setPrimaryClip(ClipData.newPlainText(title, log.joinToString("\n")))
                }) { Text(stringResource(android.R.string.copy)) }
                TextButton(onClick = { save.launch("aare-log.txt") }) { Text(stringResource(R.string.setup_log_save)) }
                TextButton(onClick = { DiagLog.clear() }) { Text(stringResource(R.string.common_clear)) }
            }
        }
    }
}

@Composable
private fun About() {
    Column(Modifier.fillMaxWidth().padding(8.dp)) {
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.setup_about_text, net.prototux.aare.BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
