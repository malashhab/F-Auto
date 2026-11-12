package net.prototux.aare.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The in-car look: calm dark surfaces, one warm accent, big type and big targets.
 *
 * Car screens are viewed from ~70 cm at a glance, so sizes follow the car-UI rule of thumb:
 * touch targets ≥ 76 dp, body text ≥ 24 sp, and no more than two text styles per card.
 */
@Immutable
data class CarDimens(
    val touchTarget: Dp = 76.dp,
    val railWidth: Dp = 96.dp,
    val gutter: Dp = 12.dp,
    val cardRadius: Dp = 28.dp,
    val iconSize: Dp = 36.dp,
)

val LocalCarDimens = staticCompositionLocalOf { CarDimens() }

/** Brand accent: a river teal (the Aare is famously turquoise). */
private val Teal = Color(0xFF3DDBC6)
private val TealDeep = Color(0xFF00695C)

private val NightColors: ColorScheme = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF0E3B36),
    onPrimaryContainer = Color(0xFFB9F5EA),
    secondary = Color(0xFFB4CAC5),
    onSecondary = Color(0xFF1F3531),
    secondaryContainer = Color(0xFF263330),
    onSecondaryContainer = Color(0xFFD0E6E1),
    tertiary = Color(0xFFFFB86B),
    onTertiary = Color(0xFF2E1500),
    background = Color(0xFF0B0F0F),
    onBackground = Color(0xFFE3E9E7),
    surface = Color(0xFF0B0F0F),
    onSurface = Color(0xFFE3E9E7),
    surfaceVariant = Color(0xFF1A2120),
    onSurfaceVariant = Color(0xFFAFBAB7),
    surfaceContainerLowest = Color(0xFF060909),
    surfaceContainerLow = Color(0xFF121818),
    surfaceContainer = Color(0xFF161D1C),
    surfaceContainerHigh = Color(0xFF1D2524),
    surfaceContainerHighest = Color(0xFF26302E),
    outline = Color(0xFF5E6B68),
    outlineVariant = Color(0xFF2E3836),
    error = Color(0xFFFF8A80),
)

private val DayColors: ColorScheme = lightColorScheme(
    primary = TealDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F5EA),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4A635F),
    secondaryContainer = Color(0xFFCCE8E2),
    onSecondaryContainer = Color(0xFF06201C),
    tertiary = Color(0xFF8B5000),
    background = Color(0xFFF3F7F6),
    onBackground = Color(0xFF161D1C),
    surface = Color(0xFFF3F7F6),
    onSurface = Color(0xFF161D1C),
    surfaceVariant = Color(0xFFDAE5E2),
    onSurfaceVariant = Color(0xFF3F4947),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFECF2F0),
    surfaceContainer = Color(0xFFE6ECEA),
    surfaceContainerHigh = Color(0xFFE0E7E5),
    surfaceContainerHighest = Color(0xFFD9E1DF),
    outline = Color(0xFF6F7977),
    outlineVariant = Color(0xFFBEC9C6),
)

private val CarType = Typography(
    displaySmall = TextStyle(fontSize = 44.sp, lineHeight = 52.sp, fontWeight = FontWeight.Medium),
    headlineMedium = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium),
    headlineSmall = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Medium),
    titleLarge = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 24.sp, lineHeight = 32.sp),
    bodyMedium = TextStyle(fontSize = 22.sp, lineHeight = 30.sp),
    labelLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
)

private val CarShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun CarTheme(night: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (night) NightColors else DayColors,
        typography = CarType,
        shapes = CarShapes,
        content = content,
    )
}

/** Phone UI theme: same palette, normal phone type scale. */
@Composable
fun PhoneTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) NightColors else DayColors, content = content)
}

// ---------------------------------------------------------------- car-style accent (phone UI)

private val AuroraAccent = Color(0xFFFF5E9E) to Color(0xFF9C1C64)
private val ForestAccentPair = Color(0xFF5FD66C) to Color(0xFF1B5E20)
private val SportAccentPair = Color(0xFFFF4D3D) to Color(0xFFB71C1C)

/** The accent a car style reads as on the phone: (vivid, for dark mode) to (deep, for light mode). Midnight and Light share the brand teal. */
private fun accentPairFor(style: String): Pair<Color, Color> = when (style) {
    "aurora" -> AuroraAccent
    "forest" -> ForestAccentPair
    "sport" -> SportAccentPair
    else -> Teal to TealDeep
}

private fun blend(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
)

private fun onColorFor(bg: Color) = if (bg.luminance() > 0.45f) Color(0xFF101418) else Color.White

/**
 * [scheme] with its brand accent (primary/tertiary and their containers) swapped for the chosen
 * car style's — everything else (background, surfaces, text) stays as it was. Lets the phone app's
 * own settings UI read as the same brand as the car screen the driver just picked, without a
 * ground-up redesign of either.
 */
fun styleAccentScheme(scheme: ColorScheme, dark: Boolean, style: String): ColorScheme {
    val (vivid, deep) = accentPairFor(style)
    val primary = if (dark) vivid else deep
    val container = blend(primary, scheme.surface, if (dark) 0.78f else 0.85f)
    return scheme.copy(
        primary = primary,
        onPrimary = onColorFor(primary),
        primaryContainer = container,
        onPrimaryContainer = onColorFor(container),
        tertiary = if (dark) vivid else deep,
    )
}

private val BrandPink = Color(0xFFFF4D63)
private val BrandPinkDeep = Color(0xFFB71C3E)

// The phone app's own backdrop: a deep navy/indigo, not flat black — matches the Dusk mockup's page
// background instead of the stark near-black the car screen itself uses.
private val BrandNightBackground = Color(0xFF130F26)
private val BrandNightSurface = Color(0xFF150F28)
private val BrandNightSurfaceContainer = Color(0xFF1D1934)
private val BrandNightSurfaceContainerHigh = Color(0xFF26213F)
private val BrandNightSurfaceContainerHighest = Color(0xFF2E2849)
private val BrandNightSurfaceVariant = Color(0xFF231F3C)

/**
 * [scheme] with its brand accent fixed to F-Auto's own pink/indigo identity, regardless of which
 * car-screen style is selected — the phone app's own chrome (switches, default-tinted icons,
 * segmented buttons) should always read as F-Auto, not whichever teal/green accent the old AARE
 * branding used. In dark mode, also swaps the flat near-black backdrop for the brand's deep
 * navy/indigo, matching the Dusk mockup's page background instead of the car screen's own black.
 */
fun brandAccentScheme(scheme: ColorScheme, dark: Boolean): ColorScheme {
    val primary = if (dark) BrandPink else BrandPinkDeep
    val container = blend(primary, scheme.surface, if (dark) 0.78f else 0.85f)
    val base = if (dark) {
        scheme.copy(
            background = BrandNightBackground,
            surface = BrandNightSurface,
            surfaceVariant = BrandNightSurfaceVariant,
            surfaceContainer = BrandNightSurfaceContainer,
            surfaceContainerHigh = BrandNightSurfaceContainerHigh,
            surfaceContainerHighest = BrandNightSurfaceContainerHighest,
            surfaceContainerLow = BrandNightBackground,
            surfaceContainerLowest = BrandNightBackground,
        )
    } else scheme
    return base.copy(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = container,
        onPrimaryContainer = onColorFor(container),
        tertiary = primary,
        secondary = primary,
    )
}
