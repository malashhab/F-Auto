package net.prototux.aare.ui.car

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.media.Pill
import net.prototux.aare.ui.theme.LocalCarDimens

/**
 * The car's own settings app: what drivers change from the car, like Android Auto's. The rest
 * stays in AARE on the phone.
 *
 * Reads `settings.carLayout` itself (rather than taking it as a parameter) so changing it here,
 * like changing the color style, takes effect immediately without the caller needing to know
 * about layouts. The Dusk layout shows a sidebar + detail panel, matching `CarUI/dusk.html`'s
 * Settings screen, instead of the Classic layout's flat scrolling list.
 */
@Composable
internal fun CarSettingsScreen(settings: Settings, modifier: Modifier) {
    var layout by remember { mutableStateOf(settings.carLayout) }
    if (layout == Settings.LAYOUT_DUSK) {
        DuskSettingsScreen(settings, modifier) { layout = it }
    } else {
        ClassicSettingsScreen(settings, modifier) { layout = it }
    }
}

@Composable
private fun ClassicSettingsScreen(settings: Settings, modifier: Modifier, onLayoutChange: (String) -> Unit) {
    var theme by remember { mutableStateOf(settings.carTheme) }
    var rail by remember { mutableStateOf(railChoiceValue(settings)) }
    var style by remember { mutableStateOf(settings.carStyle) }
    var layout by remember { mutableStateOf(settings.carLayout) }
    var banners by remember { mutableStateOf(settings.messageBanners) }
    var aloud by remember { mutableStateOf(settings.readMessagesAloud) }
    var audio by remember { mutableStateOf(settings.carAudio) }
    var darkBar by remember { mutableStateOf(settings.darkBar) }
    var passenger by remember { mutableStateOf(settings.ignoreDriving) }
    var autoSpeaker by remember { mutableStateOf(settings.autoSpeakerOnCall) }
    LazyColumn(
        modifier.background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { Text(stringResource(R.string.setup_app_entry_settings), style = MaterialTheme.typography.headlineSmall) }
        item {
            Choice(stringResource(R.string.setup_choice_colors), listOf(Settings.THEME_AUTO to stringResource(R.string.setup_choice_colors_auto), Settings.THEME_DARK to stringResource(R.string.setup_choice_dark), Settings.THEME_LIGHT to stringResource(R.string.setup_choice_light)), theme) {
                theme = it; settings.carTheme = it
            }
        }
        item {
            Choice(stringResource(R.string.setup_choice_bar), railChoiceOptions(), rail) {
                rail = it; applyRailChoice(settings, it)
            }
        }
        item { StyleChoice(style) { style = it; settings.carStyle = it } }
        item { LayoutChoice(layout) { layout = it; settings.carLayout = it; onLayoutChange(it) } }
        item { SwitchRow(stringResource(R.string.setup_switch_dark_bar), darkBar) { darkBar = it; settings.darkBar = it } }
        item { SwitchRow(stringResource(R.string.setup_switch_auto_speaker), autoSpeaker) { autoSpeaker = it; settings.autoSpeakerOnCall = it } }
        item { SwitchRow(stringResource(R.string.setup_switch_passenger), passenger) { passenger = it; settings.ignoreDriving = it } }
        item { SwitchRow(stringResource(R.string.setup_switch_message_notifications), banners) { banners = it; settings.messageBanners = it } }
        item { SwitchRow(stringResource(R.string.msg_toggle_read_aloud_title), aloud) { aloud = it; settings.readMessagesAloud = it } }
        item { SwitchRow(stringResource(R.string.setup_switch_audio_next_connection), audio) { audio = it; settings.carAudio = it } }
    }
}

@Composable
private fun StyleChoice(style: String, onChange: (String) -> Unit) {
    Choice(stringResource(R.string.setup_choice_style), listOf(
        Settings.STYLE_MIDNIGHT to stringResource(R.string.setup_style_midnight),
        Settings.STYLE_AURORA to stringResource(R.string.setup_style_aurora),
        Settings.STYLE_FOREST to stringResource(R.string.setup_style_forest),
        Settings.STYLE_LIGHT to stringResource(R.string.setup_style_light),
        Settings.STYLE_SPORT to stringResource(R.string.setup_style_sport),
        Settings.STYLE_DUSK to stringResource(R.string.setup_style_dusk),
    ), style, onChange)
}

private const val RAIL_CHOICE_AUTO_HIDE = "auto_hide"

/** The side-bar picker's current value: [RAIL_CHOICE_AUTO_HIDE] when [Settings.railAutoHide] is on, else [Settings.railPosition]. */
private fun railChoiceValue(settings: Settings) = if (settings.railAutoHide) RAIL_CHOICE_AUTO_HIDE else settings.railPosition

/** Applies a side-bar choice: Left/Right turn auto-hide off and set the side; Auto-hide turns it on without losing the side. */
private fun applyRailChoice(settings: Settings, choice: String) {
    if (choice == RAIL_CHOICE_AUTO_HIDE) settings.railAutoHide = true
    else { settings.railAutoHide = false; settings.railPosition = choice }
}

@Composable
private fun railChoiceOptions() = listOf(
    Settings.RAIL_LEFT to stringResource(R.string.setup_choice_left),
    Settings.RAIL_RIGHT to stringResource(R.string.setup_choice_right),
    RAIL_CHOICE_AUTO_HIDE to stringResource(R.string.setup_choice_auto_hide),
)

@Composable
private fun LayoutChoice(layout: String, onChange: (String) -> Unit) {
    Choice(stringResource(R.string.setup_choice_layout), listOf(
        Settings.LAYOUT_CLASSIC to stringResource(R.string.setup_layout_classic),
        Settings.LAYOUT_DUSK to stringResource(R.string.setup_layout_dusk),
    ), layout, onChange)
}

@Composable
private fun ConnectionChoice(settings: Settings) {
    var compat by remember { mutableStateOf(settings.wirelessCompatMode) }
    Choice(stringResource(R.string.setup_choice_wireless_compat), listOf(
        Settings.WIRELESS_COMPAT_AUTO to stringResource(R.string.setup_choice_wireless_compat_auto),
        Settings.WIRELESS_COMPAT_ON to stringResource(R.string.setup_choice_wireless_compat_on),
        Settings.WIRELESS_COMPAT_OFF to stringResource(R.string.setup_choice_wireless_compat_off),
    ), compat) { compat = it; settings.wirelessCompatMode = it }
}

/**
 * Dusk's own Settings screen: a category sidebar + a detail panel on the right, matching
 * `CarUI/dusk.html`'s Settings screen, instead of Classic's single scrolling list. Same settings,
 * grouped; nothing here is Dusk-color-theme-specific — any of the 5 color themes can show this.
 */
private enum class SettingsCategory(val labelRes: Int) {
    DISPLAY(R.string.setup_category_display), SOUND(R.string.setup_category_sound),
    CONNECTION(R.string.setup_category_connection), THEME(R.string.setup_category_theme),
    DRIVING(R.string.setup_category_driving), ABOUT(R.string.setup_category_about),
}

@Composable
private fun DuskSettingsScreen(settings: Settings, modifier: Modifier, onLayoutChange: (String) -> Unit) {
    var category by remember { mutableStateOf(SettingsCategory.DISPLAY) }
    var theme by remember { mutableStateOf(settings.carTheme) }
    var rail by remember { mutableStateOf(railChoiceValue(settings)) }
    var style by remember { mutableStateOf(settings.carStyle) }
    var layout by remember { mutableStateOf(settings.carLayout) }
    var banners by remember { mutableStateOf(settings.messageBanners) }
    var aloud by remember { mutableStateOf(settings.readMessagesAloud) }
    var audio by remember { mutableStateOf(settings.carAudio) }
    var darkBar by remember { mutableStateOf(settings.darkBar) }
    var passenger by remember { mutableStateOf(settings.ignoreDriving) }
    var autoSpeaker by remember { mutableStateOf(settings.autoSpeakerOnCall) }
    Row(modifier.background(MaterialTheme.colorScheme.background).padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            Modifier.width(240.dp).fillMaxHeight().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(10.dp),
        ) {
            SettingsCategory.entries.forEach { c ->
                val selected = c == category
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .clickable { category = c }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(c.labelRes), style = MaterialTheme.typography.titleMedium,
                        color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainer),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            when (category) {
                SettingsCategory.DISPLAY -> {
                    item {
                        Choice(stringResource(R.string.setup_choice_colors), listOf(Settings.THEME_AUTO to stringResource(R.string.setup_choice_colors_auto), Settings.THEME_DARK to stringResource(R.string.setup_choice_dark), Settings.THEME_LIGHT to stringResource(R.string.setup_choice_light)), theme) {
                            theme = it; settings.carTheme = it
                        }
                    }
                    item {
                        Choice(stringResource(R.string.setup_choice_bar), railChoiceOptions(), rail) {
                            rail = it; applyRailChoice(settings, it)
                        }
                    }
                    item { SwitchRow(stringResource(R.string.setup_switch_dark_bar), darkBar) { darkBar = it; settings.darkBar = it } }
                }
                SettingsCategory.THEME -> {
                    item { StyleChoice(style) { style = it; settings.carStyle = it } }
                    item { LayoutChoice(layout) { layout = it; settings.carLayout = it; onLayoutChange(it) } }
                }
                SettingsCategory.SOUND -> {
                    item { SwitchRow(stringResource(R.string.setup_switch_auto_speaker), autoSpeaker) { autoSpeaker = it; settings.autoSpeakerOnCall = it } }
                    item { SwitchRow(stringResource(R.string.msg_toggle_read_aloud_title), aloud) { aloud = it; settings.readMessagesAloud = it } }
                    item { SwitchRow(stringResource(R.string.setup_switch_audio_next_connection), audio) { audio = it; settings.carAudio = it } }
                }
                SettingsCategory.CONNECTION -> {
                    item { ConnectionChoice(settings) }
                }
                SettingsCategory.DRIVING -> {
                    item { SwitchRow(stringResource(R.string.setup_switch_passenger), passenger) { passenger = it; settings.ignoreDriving = it } }
                    item { SwitchRow(stringResource(R.string.setup_switch_message_notifications), banners) { banners = it; settings.messageBanners = it } }
                }
                SettingsCategory.ABOUT -> {
                    item { Text("F-Auto", style = MaterialTheme.typography.headlineSmall) }
                    item {
                        Text(
                            stringResource(R.string.setup_about_text, net.prototux.aare.BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Choice(title: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { (value, label) -> Pill(label, selected = value == selected, onClick = { onSelect(value) }) }
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = LocalCarDimens.current.touchTarget).clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
