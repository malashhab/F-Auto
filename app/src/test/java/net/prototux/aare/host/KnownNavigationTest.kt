package net.prototux.aare.host

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnownNavigationTest {
    @Test
    fun listedPackagesAndTheirBuildVariantsAreKnown() {
        assertTrue(CarAppInfo.isKnownNavigationPackage("com.headunit.nav"))
        assertTrue(CarAppInfo.isKnownNavigationPackage("com.headunit.nav.play"))
        assertTrue(CarAppInfo.isKnownNavigationPackage("com.headunit.nav.public"))
        assertTrue(CarAppInfo.isKnownNavigationPackage("com.waze"))
    }

    @Test
    fun lookalikePackagesAreNot() {
        // A prefix match must stop at a dot: "com.wazeclone" isn't a build of Waze.
        assertFalse(CarAppInfo.isKnownNavigationPackage("com.wazeclone"))
        assertFalse(CarAppInfo.isKnownNavigationPackage("com.headunit.navigator"))
        assertFalse(CarAppInfo.isKnownNavigationPackage("com.example.music"))
    }
}
