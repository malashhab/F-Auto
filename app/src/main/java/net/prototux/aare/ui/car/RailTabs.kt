package net.prototux.aare.ui.car

import androidx.compose.ui.unit.Dp

/** How the rail lays out its tabs: each one's [height], and whether they all [fit] without scrolling. */
internal data class RailTabs(val height: Dp, val fits: Boolean)

/**
 * Shares [available] height among [count] rail tabs, between [min] and [max] each.
 *
 * A short screen (800x480 head units, wide 1920x720 ones at a high density) can't give every tab
 * the minimum. The tabs then keep the minimum and the rail scrolls ([RailTabs.fits] false). Fixed-
 * size tabs in a column too short for them get squeezed: their tap areas shrink, but the icons
 * still draw at full size, so a tap on one icon lands on the tab next to it.
 */
internal fun railTabs(available: Dp, count: Int, min: Dp, max: Dp): RailTabs {
    val height = (available / count.coerceAtLeast(1)).coerceIn(min, max)
    return RailTabs(height, available >= height * count)
}
