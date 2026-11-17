package net.prototux.aare.ui.car

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** The rails' tab sizing ([railTabs]); the rails' tab sizing. */
class RailTabsTest {
    @Test
    fun tallRailCapsTheTabs() {
        assertEquals(RailTabs(88.dp, fits = true), railTabs(768.dp, count = 6, min = 48.dp, max = 88.dp))
    }

    @Test
    fun mediumRailSharesTheHeight() {
        assertEquals(RailTabs(60.dp, fits = true), railTabs(360.dp, count = 6, min = 48.dp, max = 88.dp))
    }

    @Test
    fun exactlyTheMinimumStillFits() {
        assertEquals(RailTabs(48.dp, fits = true), railTabs(288.dp, count = 6, min = 48.dp, max = 88.dp))
    }

    /** An 800x480 head unit at 213 dpi leaves about 190 dp for the tabs: keep 48 dp and scroll, don't squeeze. */
    @Test
    fun shortRailScrollsInsteadOfSqueezing() {
        assertEquals(RailTabs(48.dp, fits = false), railTabs(190.dp, count = 6, min = 48.dp, max = 88.dp))
        assertEquals(RailTabs(44.dp, fits = false), railTabs(200.dp, count = 6, min = 44.dp, max = 72.dp))
    }

    @Test
    fun zeroTabsDoesNotDivideByZero() {
        assertEquals(RailTabs(88.dp, fits = true), railTabs(100.dp, count = 0, min = 48.dp, max = 88.dp))
    }
}
