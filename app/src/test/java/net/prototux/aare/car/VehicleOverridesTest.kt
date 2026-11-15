package net.prototux.aare.car

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Matching a head unit against a vehicles.json rule. */
class VehicleOverridesTest {
    private fun rule(make: String? = null, model: String? = null, name: String? = null) =
        VehicleRule(make, model, name, VehicleOverride())

    @Test fun matchesByMakeSubstringCaseInsensitive() =
        assertTrue(rule(make = "ford").matches("Ford Motor Company", "Focus", "SYNC 3"))

    @Test fun everySetFieldMustMatch() {
        assertTrue(rule(make = "Ford", name = "SYNC").matches("Ford", "Focus", "SYNC 3"))
        assertFalse(rule(make = "Ford", name = "MMI").matches("Ford", "Focus", "SYNC 3"))
    }

    @Test fun unsetFieldsAreIgnored() =
        assertTrue(rule(model = "Mach-E").matches("Ford", "Mustang Mach-E", "SYNC 4"))

    @Test fun anEmptyRuleNeverMatches() =
        assertFalse(rule().matches("Ford", "Focus", "SYNC"))

    @Test fun aMismatchFails() =
        assertFalse(rule(make = "Audi").matches("Ford", "Focus", "SYNC"))

    /** The built-in MG/SAIC (Desay SV) patterns match the real head-unit identity from the logs. */
    @Test fun mgSaicDesayHeadUnitMatches() {
        val make = "Huizhou Desay SV Automotive Co., Ltd."
        val model = "NV6064/58"
        val name = "SAIC MOTOR Overseas Intelligent Mobility Technology Co., Ltd. MG $make $model"
        assertTrue(rule(name = "SAIC MOTOR").matches(make, model, name))
        assertTrue(rule(name = "NV6064").matches(make, model, name))
    }

    /** The forceH264 override flag carries through a rule. */
    @Test fun forceH264OverrideCarries() =
        assertEquals(true, VehicleRule(null, null, "NV6064", VehicleOverride(forceH264 = true)).override.forceH264)
}
