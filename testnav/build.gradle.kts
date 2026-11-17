// A tiny Car App Library navigation app for testing F-Auto's map hosting on an emulator, without
// installing a third-party navigation app. It draws what the host tells it onto its map surface
// (surface size, visible/stable areas, the screen configuration) and uses the icon kinds real
// navigation apps use. Debug tool only: not shipped, never part of the F-Auto APK.
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "net.prototux.aare.testnav"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "net.prototux.aare.testnav"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.car.app)
}
