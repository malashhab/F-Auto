package net.prototux.aare.ui.car

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import net.prototux.aare.R
import net.prototux.aare.Settings

/** One pickable car color style: its [Settings.carStyle] value, its name and the two colors of its swatch. */
internal class CarStyleOption(val style: String, @StringRes val labelRes: Int, val swatch: List<Color>)

/** Every car color style, in the order the pickers list them: the fork's own first, then AARE 1.6.0's. */
internal val CarStyleOptions: List<CarStyleOption> = listOf(
    CarStyleOption(Settings.STYLE_DUSK, R.string.setup_style_dusk, listOf(Color(0xFF8A1D38), Color(0xFF1B3BB0))),
    CarStyleOption(Settings.STYLE_MIDNIGHT, R.string.setup_style_midnight, listOf(Color(0xFF141925), Color(0xFF46E0C8))),
    CarStyleOption(Settings.STYLE_AURORA, R.string.setup_style_aurora, listOf(Color(0xFFFF9AD0), Color(0xFF4FB4FF))),
    CarStyleOption(Settings.STYLE_FOREST, R.string.setup_style_forest, listOf(Color(0xFF14241C), Color(0xFF5FD66C))),
    CarStyleOption(Settings.STYLE_SPORT, R.string.setup_style_sport, listOf(Color(0xFF111111), Color(0xFFFF3B3B))),
    CarStyleOption(Settings.STYLE_LIGHT, R.string.setup_style_light, listOf(Color.White, Color(0xFFC9D8F5))),
    CarStyleOption(Settings.STYLE_ANDROID_AUTO, R.string.setup_style_android_auto, listOf(Color(0xFF303134), Color(0xFF8AB4F8))),
    CarStyleOption(Settings.STYLE_ANDROID_AUTO_2019, R.string.setup_style_android_auto_2019, listOf(Color(0xFF202124), Color(0xFF81C995))),
    CarStyleOption(Settings.STYLE_CARPLAY, R.string.setup_style_carplay, listOf(Color(0xFF1C1C1E), Color(0xFF0A84FF))),
    CarStyleOption(Settings.STYLE_MATERIAL_YOU, R.string.setup_style_material_you, listOf(Color(0xFF4F378B), Color(0xFFD0BCFF))),
    CarStyleOption(Settings.STYLE_HIGH_CONTRAST, R.string.setup_style_high_contrast, listOf(Color.Black, Color(0xFFFFD600))),
    CarStyleOption(Settings.STYLE_NIGHT, R.string.setup_style_night, listOf(Color(0xFF1A1200), Color(0xFFFFB000))),
    CarStyleOption(Settings.STYLE_RETRO, R.string.setup_style_retro, listOf(Color(0xFF04140A), Color(0xFF33FF66))),
)

private fun solid(c: Color) = SolidColor(c) as Brush

// Colors from AARE 1.6.0's themes (ui/theme/*Theme.kt), fitted to this fork's palette roles.
// Android Auto's current look: true black, Google grey cards, Google blue accent.
private val AndroidAutoPalette = HybridPalette(
    bg = solid(Color.Black), card = Color(0xFF303134), card2 = Color(0xFF3C4043), line = Color(0xFF3C4043),
    text = Color(0xFFF5F5F5), muted = Color(0xFFB8B8B8), accent = Color(0xFF8AB4F8), accent2 = Color(0xFFFDD663),
    warn = Color(0xFFFDD663), bad = Color(0xFFF28B82), onAccent = Color(0xFF062E6F),
)

// Android Auto 2019-2022: black with darker cards and a green second accent.
private val AndroidAuto2019Palette = HybridPalette(
    bg = solid(Color.Black), card = Color(0xFF202124), card2 = Color(0xFF292A2E), line = Color(0xFF3C4043),
    text = Color.White, muted = Color(0xFFBDC1C6), accent = Color(0xFF8AB4F8), accent2 = Color(0xFF81C995),
    warn = Color(0xFFFDD663), bad = Color(0xFFF28B82), onAccent = Color(0xFF062E6F),
)

// CarPlay: iOS system colors, dark.
private val CarPlayPalette = HybridPalette(
    bg = solid(Color.Black), card = Color(0xFF1C1C1E), card2 = Color(0xFF2C2C2E), line = Color(0xFF38383A),
    text = Color.White, muted = Color(0xFFAEAEB2), accent = Color(0xFF0A84FF), accent2 = Color(0xFFFF9F0A),
    warn = Color(0xFFFFD60A), bad = Color(0xFFFF453A), onAccent = Color.White,
)

// High contrast: white outlines on black, yellow accent.
private val HighContrastPalette = HybridPalette(
    bg = solid(Color.Black), card = Color(0xFF141414), card2 = Color(0xFF1F1F1F), line = Color.White,
    text = Color.White, muted = Color(0xFFE6E6E6), accent = Color(0xFFFFD600), accent2 = Color(0xFF80D8FF),
    warn = Color(0xFFFFD600), bad = Color(0xFFFF6B6B), onAccent = Color.Black,
)

// Night: one amber color on black, like many cars' night lighting.
private val NightPalette = HybridPalette(
    bg = solid(Color.Black), card = Color(0xFF1A1200), card2 = Color(0xFF261A00), line = Color(0xFF4A3200),
    text = Color(0xFFFFDEA6), muted = Color(0xFFB38A3A), accent = Color(0xFFFFB000), accent2 = Color(0xFFFFDEA6),
    warn = Color(0xFFFFB000), bad = Color(0xFFFF5A4E), onAccent = Color(0xFF2A1A00),
)

// Retro: green phosphor on black.
private val RetroPalette = HybridPalette(
    bg = solid(Color.Black), card = Color(0xFF04140A), card2 = Color(0xFF08200F), line = Color(0xFF0F3D1E),
    text = Color(0xFFB8FFC8), muted = Color(0xFF4FBF6E), accent = Color(0xFF33FF66), accent2 = Color(0xFF2EF2D0),
    warn = Color(0xFFFFB000), bad = Color(0xFFFF5A4E), onAccent = Color(0xFF002910),
)

// Material You before the phone's colors are read, or on a phone older than Android 12: the baseline.
private val MaterialYouBaseline = materialYouPalette(darkColorScheme())

private fun materialYouPalette(s: androidx.compose.material3.ColorScheme) = HybridPalette(
    bg = solid(s.surface), card = s.surfaceContainer, card2 = s.surfaceContainerHigh, line = s.outlineVariant,
    text = s.onSurface, muted = s.onSurfaceVariant, accent = s.primary, accent2 = s.tertiary,
    warn = Color(0xFFFFB547), bad = s.error, onAccent = s.onPrimary,
)

@Volatile private var materialYou: HybridPalette? = null

/** Reads the phone's wallpaper colors for the Material You style (Android 12+); until then the baseline shows. */
internal fun loadMaterialYouPalette(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    materialYou = runCatching { materialYouPalette(dynamicDarkColorScheme(context)) }.getOrNull()
}

/** The palette of one of the styles ported from AARE 1.6.0, or null for the fork's own. */
internal fun portedPaletteFor(style: String): HybridPalette? = when (style) {
    Settings.STYLE_ANDROID_AUTO -> AndroidAutoPalette
    Settings.STYLE_ANDROID_AUTO_2019 -> AndroidAuto2019Palette
    Settings.STYLE_CARPLAY -> CarPlayPalette
    Settings.STYLE_MATERIAL_YOU -> materialYou ?: MaterialYouBaseline
    Settings.STYLE_HIGH_CONTRAST -> HighContrastPalette
    Settings.STYLE_NIGHT -> NightPalette
    Settings.STYLE_RETRO -> RetroPalette
    else -> null
}
