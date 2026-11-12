package net.prototux.aare

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.UUID

/** User settings, in one SharedPreferences file. */
class Settings(context: Context) {
    /** Not the Activity [Settings] may be made with, so an instance kept around does not leak it. */
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Serve the developer "head unit server" like Android Auto does, through adb (for DHU/openauto/headunit-revived). */
    var headUnitServer: Boolean
        get() = prefs.getBoolean("hu_server", false)
        set(v) = prefs.edit { putBoolean("hu_server", v) }

    /** Write the protocol frame trace to the in-app connection log too, to capture it without adb. */
    var verboseProtocolLog: Boolean
        get() = prefs.getBoolean("verbose_protocol_log", false)
        set(v) = prefs.edit { putBoolean("verbose_protocol_log", v) }

    /** Also accept head units from the network (TCP 5277, no authentication). Switched off again after one session if [headUnitServerLanOnce]. */
    var headUnitServerLan: Boolean
        get() = prefs.getBoolean("hu_server_lan", false)
        set(v) = prefs.edit { putBoolean("hu_server_lan", v) }

    /** Switch [headUnitServerLan] off once a network session starts. Off keeps it on, for openauto/Headunit Revived. */
    var headUnitServerLanOnce: Boolean
        get() = prefs.getBoolean("hu_server_lan_once", true)
        set(v) = prefs.edit { putBoolean("hu_server_lan_once", v) }

    /** Offer "Phone screen" on head unit server sessions too: the server's peer is unauthenticated, so off by default. */
    var headUnitServerMirror: Boolean
        get() = prefs.getBoolean("head_unit_server_mirror", false)
        set(v) = prefs.edit { putBoolean("head_unit_server_mirror", v) }

    /**
     * Accept head-unit connections over loopback TCP ([TcpServer.PORT]) from an Android-Auto-in-the-
     * browser bridge on this phone (webaa, paath — for cars without Android Auto, like Tesla). Any app
     * on the phone can then reach the server, so it is off by default.
     */
    var headUnitServerLocalhost: Boolean
        get() = prefs.getBoolean("hu_server_localhost", false)
        set(v) = prefs.edit { putBoolean("hu_server_localhost", v) }

    /** Connect wirelessly to bonded head units that advertise wireless Android Auto. */
    var wireless: Boolean
        get() = prefs.getBoolean("wireless", true)
        set(v) = prefs.edit { putBoolean("wireless", v) }

    /**
     * Fortuner fork: the bonded device name (substring, case-insensitive) the "Connect now" button
     * dials directly over RFCOMM, the way `D:\aa-phone-side` (stage1) does — skipping AARE's normal
     * check that Android has already cached the wireless-Android-Auto UUID for that device (SDP),
     * which some adapters never do while the system's own Android Auto is disabled.
     */
    var wirelessConnectNowTarget: String
        get() = prefs.getString("wireless_connect_now_target", "") ?: ""
        set(v) = prefs.edit { putString("wireless_connect_now_target", v) }

    /**
     * Some wireless adapters (cheap aftermarket dongles especially) only complete the handshake
     * when every message matches exactly what a genuine phone sends ([WIRELESS_COMPAT_ON]); most
     * don't need that and work with AARE's normal, slightly different sequence
     * ([WIRELESS_COMPAT_OFF]). [WIRELESS_COMPAT_AUTO] (the default) works it out per adapter: it
     * starts with whichever worked with that adapter before ([wirelessProfileFor]), else a guess
     * from its Bluetooth name, tries the other when the first gets no answer, and remembers the one
     * that reaches a working session. On and Off pin one sequence for every adapter.
     */
    var wirelessCompatMode: String
        get() = prefs.getString("wireless_compat_mode", WIRELESS_COMPAT_AUTO) ?: WIRELESS_COMPAT_AUTO
        set(v) = prefs.edit { putString("wireless_compat_mode", v) }

    /**
     * The handshake that reached a working session with the head unit or adapter at this Bluetooth
     * address: true the genuine-phone one, false AARE's normal one, null if it never connected.
     */
    fun wirelessProfileFor(address: String): Boolean? =
        prefs.getStringSet("wireless_profiles", emptySet()).orEmpty().firstOrNull { it.startsWith("$address\t") }?.endsWith("\t1")

    fun rememberWirelessProfile(address: String, official: Boolean) {
        val others = prefs.getStringSet("wireless_profiles", emptySet()).orEmpty().filterNot { it.startsWith("$address\t") }
        prefs.edit { putStringSet("wireless_profiles", (others + "$address\t${if (official) 1 else 0}").toSet()) }
    }

    /** Forgets every remembered handshake: the next connection to each adapter is worked out again. */
    fun forgetWirelessProfiles() = prefs.edit { remove("wireless_profiles") }

    /** Check GitHub for a newer release and warn on the home screen. Off never touches the network. */
    var checkUpdates: Boolean
        get() = prefs.getBoolean("check_updates", true)
        set(v) = prefs.edit { putBoolean("check_updates", v) }

    // Update-check cache (see UpdateChecker): the latest release seen and its page, when we last
    // asked, and the version the user dismissed. Versions are stored without a leading "v".
    var updateLatestVersion: String?
        get() = prefs.getString("update_latest_version", null)
        set(v) = prefs.edit { putString("update_latest_version", v) }
    var updateLatestUrl: String?
        get() = prefs.getString("update_latest_url", null)
        set(v) = prefs.edit { putString("update_latest_url", v) }
    var updateLastCheckMs: Long
        get() = prefs.getLong("update_last_check_ms", 0)
        set(v) = prefs.edit { putLong("update_last_check_ms", v) }
    var updateDismissedVersion: String?
        get() = prefs.getString("update_dismissed_version", null)
        set(v) = prefs.edit { putString("update_dismissed_version", v) }

    /** Largest video height we accept from the head unit's offers (480, 720, 1080, 1440, 2160). */
    var maxVideoHeight: Int
        get() = prefs.getInt("max_video_height", 1080)
        set(v) = prefs.edit { putInt("max_video_height", v) }

    /** Prefer 60 fps configurations when the head unit offers them. */
    var prefer60fps: Boolean
        get() = prefs.getBoolean("prefer_60fps", true)
        set(v) = prefs.edit { putBoolean("prefer_60fps", v) }

    /**
     * Force H.264 video even when the head unit and phone could both do a newer codec (H.265...).
     * The newer codec is used automatically otherwise; this is the escape hatch for head units that
     * advertise one but decode it poorly.
     */
    var forceH264: Boolean
        get() = prefs.getBoolean("force_h264", false)
        set(v) = prefs.edit { putBoolean("force_h264", v) }

    /** Which side of the Dusk car screen the floating rail sits on: [RAIL_LEFT] (the default) or [RAIL_RIGHT]. */
    var railPosition: String
        get() = prefs.getString("rail_position", null)
            ?: if (prefs.contains("rail_right")) (if (prefs.getBoolean("rail_right", false)) RAIL_RIGHT else RAIL_LEFT) else RAIL_LEFT
        set(v) = prefs.edit { putString("rail_position", v); remove("rail_right") }

    /** The Dusk car screen's rail starts collapsed, for more map/media width ("Side bar: Auto-hide" / "Hide side bar"). */
    var railAutoHide: Boolean
        get() = prefs.getBoolean("rail_auto_hide", false)
        set(v) = prefs.edit { putBoolean("rail_auto_hide", v) }

    /** The Dusk car screen's Home starts in Map + Music focus instead of the Full widget view. */
    var homeMapMusicDefault: Boolean
        get() = prefs.getBoolean("home_map_music_default", false)
        set(v) = prefs.edit { putBoolean("home_map_music_default", v) }

    /** How much of Dusk Home's Map + Music row the music card takes, from 0 (smallest) to 1 (largest). */
    var musicWidgetSize: Float
        get() = prefs.getFloat("music_widget_size", 0.5f)
        set(v) = prefs.edit { putFloat("music_widget_size", v) }

    /** The car screen's color theme: [STYLE_MIDNIGHT] (the default), [STYLE_AURORA], [STYLE_FOREST], [STYLE_LIGHT] or [STYLE_SPORT]. */
    var carStyle: String
        get() = prefs.getString("car_style", STYLE_MIDNIGHT) ?: STYLE_MIDNIGHT
        set(v) = prefs.edit { putString("car_style", v) }

    /**
     * The car screen's layout — independent of [carStyle]'s colors: [LAYOUT_CLASSIC] (the default,
     * AARE's original rail/dashboard/bottom-bar arrangement) or [LAYOUT_DUSK] (the restructured
     * layout from `CarUI/dusk.html`: a floating glass rail, a resume-playback header on Home, a
     * full-bleed Nav view, a three-column Phone screen, a sidebar Settings screen). Any of the 5
     * color themes can be paired with either layout.
     */
    var carLayout: String
        get() = prefs.getString("car_layout", LAYOUT_CLASSIC) ?: LAYOUT_CLASSIC
        set(v) = prefs.edit { putString("car_layout", v) }

    /**
     * Passenger mode: ignore the car's "driving" signal, so a passenger can type a destination or
     * browse long lists while moving. The driver must not use the screen then.
     */
    var ignoreDriving: Boolean
        get() = prefs.getBoolean("ignore_driving", false)
        set(v) = prefs.edit { putBoolean("ignore_driving", v) }

    /** The weather next to the clock (from Open-Meteo, with an approximate position). */
    var showWeather: Boolean
        get() = prefs.getBoolean("show_weather", true)
        set(v) = prefs.edit { putBoolean("show_weather", v) }

    /** The next calendar event on the dashboard. */
    var showCalendar: Boolean
        get() = prefs.getBoolean("show_calendar", true)
        set(v) = prefs.edit { putBoolean("show_calendar", v) }

    /** Behind the dashboard: none, one of the built-in gradients, or [WALLPAPER_CUSTOM] (filesDir/wallpaper.jpg). */
    var wallpaper: String
        get() = prefs.getString("wallpaper", "none") ?: "none"
        set(v) = prefs.edit { putString("wallpaper", v) }

    /** Keep the car screen's bar dark even with day colors. */
    var darkBar: Boolean
        get() = prefs.getBoolean("dark_bar", false)
        set(v) = prefs.edit { putBoolean("dark_bar", v) }

    /** Car screen colors: follow the car's day/night signal ([THEME_AUTO]), or always [THEME_DARK] / [THEME_LIGHT]. */
    var carTheme: String
        get() = prefs.getString("car_theme", THEME_AUTO) ?: THEME_AUTO
        set(v) = prefs.edit { putString("car_theme", v) }

    /** Stream phone audio to the car (needs the screen-capture consent for audio). */
    var carAudio: Boolean
        get() = prefs.getBoolean("car_audio", true)
        set(v) = prefs.edit { putBoolean("car_audio", v) }

    /**
     * Switch calls to speakerphone as soon as they connect, while the car screen is up. AARE only
     * controls calls through Android's telephony; it never carries their audio itself (unlike
     * media/navigation, which it streams over its own channel), so a call's audio still goes
     * wherever the phone's normal call routing sends it. Many wireless adapters advertise a
     * Bluetooth hands-free profile with no real microphone behind it, silently swallowing the
     * other side's ability to hear the driver until speakerphone is tapped by hand; this setting
     * does that automatically instead of leaving the driver to notice and fix it mid-call.
     */
    var autoSpeakerOnCall: Boolean
        get() = prefs.getBoolean("auto_speaker_on_call", true)
        set(v) = prefs.edit { putBoolean("auto_speaker_on_call", v) }

    /**
     * The phone's own Bluetooth MAC, for the car's in-session pairing handshake. Android hides it
     * from apps on most devices, and some head units withhold the projected screen until they know
     * which phone is connected, so the user can set it (Settings > About phone > Status). Empty: try
     * to read it, else skip.
     */
    var phoneBluetoothAddress: String
        get() = prefs.getString("phone_bt_address", "") ?: ""
        set(v) = prefs.edit { putString("phone_bt_address", v) }

    /** Capture car audio through su (a rooted phone), once the user has allowed it in the audio settings. */
    var rootAudio: Boolean
        get() = prefs.getBoolean("root_audio", false)
        set(v) = prefs.edit { putBoolean("root_audio", v) }

    /**
     * Package of the assistant the steering-wheel voice button opens (a car app such as Lapin, or a
     * phone assistant); null = follow the phone's default digital assistant.
     */
    var assistantPackage: String?
        get() = prefs.getString("assistant_pkg", null)
        set(v) = prefs.edit { if (v == null) remove("assistant_pkg") else putString("assistant_pkg", v) }

    /** Offer "Phone screen" in the car: the phone's own screen, for apps AARE can't host (Google Maps, Waze). */
    var phoneScreen: Boolean
        get() = prefs.getBoolean("phone_screen", false)
        set(v) = prefs.edit { putBoolean("phone_screen", v) }

    /**
     * Launch ordinary phone apps on the car screen (NewPipe, emulators, a browser…): a parked-only,
     * opt-in convenience that needs root or Shizuku. These apps are not Android Auto apps; AARE
     * mirrors their normal phone UI and injects the car's touches, so it is paused while driving.
     */
    var standaloneApps: Boolean
        get() = prefs.getBoolean("standalone_apps", false)
        set(v) = prefs.edit { putBoolean("standalone_apps", v) }

    /** The packages the user allows onto the car screen (see [standaloneApps]). */
    var standaloneAppPackages: Set<String>
        get() = prefs.getStringSet("standalone_app_pkgs", emptySet()) ?: emptySet()
        set(v) = prefs.edit { putStringSet("standalone_app_pkgs", HashSet(v)) }
    /** Show a web browser (a WebView) on the car screen. Off by default; paused while driving. */
    var browser: Boolean
        get() = prefs.getBoolean("browser", false)
        set(v) = prefs.edit { putBoolean("browser", v) }

    /** The page the car browser opens. */
    var browserUrl: String
        get() = prefs.getString("browser_url", BROWSER_DEFAULT_URL) ?: BROWSER_DEFAULT_URL
        set(v) = prefs.edit { putString("browser_url", v) }
    /** Also show the car data (speed, sensors…) as an app on the car screen, for diagnostics. */
    var carDataApp: Boolean
        get() = prefs.getBoolean("car_data_app", false)
        set(v) = prefs.edit { putBoolean("car_data_app", v) }
    /** Where the user dragged the in-call bar on the car screen (px offset from the top centre). */
    var callBarOffsetX: Float
        get() = prefs.getFloat("call_bar_x", 0f)
        set(v) = prefs.edit { putFloat("call_bar_x", v) }
    var callBarOffsetY: Float
        get() = prefs.getFloat("call_bar_y", 0f)
        set(v) = prefs.edit { putFloat("call_bar_y", v) }

    /** While connected, the car's GPS becomes the phone's location (needs AARE as mock location app). */
    var carGpsForPhone: Boolean
        get() = prefs.getBoolean("car_gps", true)
        set(v) = prefs.edit { putBoolean("car_gps", v) }

    /** Start the last media app playing when the car connects. */
    var resumeMedia: Boolean
        get() = prefs.getBoolean("resume_media", false)
        set(v) = prefs.edit { putBoolean("resume_media", v) }

    /** Show incoming messages as banners on the car screen. */
    var messageBanners: Boolean
        get() = prefs.getBoolean("message_banners", true)
        set(v) = prefs.edit { putBoolean("message_banners", v) }

    /** Also show messages from group conversations. */
    var groupMessages: Boolean
        get() = prefs.getBoolean("group_messages", true)
        set(v) = prefs.edit { putBoolean("group_messages", v) }

    /** Do Not Disturb (priority only) while connected to the car. */
    var dndWhileDriving: Boolean
        get() = prefs.getBoolean("dnd_driving", false)
        set(v) = prefs.edit { putBoolean("dnd_driving", v) }

    /** Answer people who write while connected, once per conversation per drive. */
    var autoReply: Boolean
        get() = prefs.getBoolean("auto_reply", false)
        set(v) = prefs.edit { putBoolean("auto_reply", v) }

    /** The default is resolved on read and never stored, so it follows the phone's language. */
    var autoReplyText: String
        get() = prefs.getString("auto_reply_text", null)?.takeIf { it.isNotBlank() } ?: appContext.getString(R.string.msg_autoreply_default)
        set(v) = prefs.edit { putString("auto_reply_text", v) }

    /** Read incoming messages aloud as they arrive. */
    var readMessagesAloud: Boolean
        get() = prefs.getBoolean("read_messages", false)
        set(v) = prefs.edit { putBoolean("read_messages", v) }

    /** Cars AARE connected to: name to the last connection time (ms). */
    val knownCars: Map<String, Long>
        get() = prefs.getStringSet("known_cars", emptySet()).orEmpty().mapNotNull { e ->
            val i = e.lastIndexOf('\t')
            if (i <= 0) null else e.substring(0, i) to (e.substring(i + 1).toLongOrNull() ?: 0L)
        }.toMap()

    fun rememberCar(name: String) {
        if (name.isBlank()) return
        val cars = knownCars + (name to System.currentTimeMillis())
        prefs.edit { putStringSet("known_cars", cars.map { (n, t) -> "$n\t$t" }.toSet()) }
    }

    fun forgetCar(name: String) {
        prefs.edit { putStringSet("known_cars", (knownCars - name).map { (n, t) -> "$n\t$t" }.toSet()) }
    }

    /** Last navigation and media apps used, restored on the next connection. */
    var lastNavApp: String?
        get() = prefs.getString("last_nav", null)
        set(v) = prefs.edit { putString("last_nav", v) }
    var lastMediaApp: String?
        get() = prefs.getString("last_media", null)
        set(v) = prefs.edit { putString("last_media", v) }

    /** The car launcher's order: app keys (package names, or the built-in apps' ids), first first. */
    var appOrder: List<String>
        get() = prefs.getString("app_order", null)?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
        set(v) = prefs.edit { putString("app_order", v.joinToString("\n")) }

    /** Runtime permissions setup has asked for at least once, to tell a dismissed dialog from a refusal for good. */
    var requestedPermissions: Set<String>
        get() = prefs.getStringSet("requested_permissions", emptySet()) ?: emptySet()
        set(v) = prefs.edit { putStringSet("requested_permissions", v) }

    /** Apps hidden from the car launcher. */
    var hiddenApps: Set<String>
        get() = prefs.getStringSet("hidden_apps", emptySet()) ?: emptySet()
        set(v) = prefs.edit { putStringSet("hidden_apps", v) }

    /** Stable random id sent to head units so they recognise this phone. */
    val instanceId: String
        get() = prefs.getString("instance_id", null) ?: UUID.randomUUID().toString().also { prefs.edit { putString("instance_id", it) } }

    /** Index of the certificate to offer first (rotates after a head unit rejects one). */
    var certificateIndex: Int
        get() = prefs.getInt("cert_index", 0)
        set(v) = prefs.edit { putInt("cert_index", v) }

    /**
     * Let [net.prototux.aare.cert.CertificateAutoUpdate] fetch and import a newer Android Auto
     * certificate on its own, once one is published, instead of waiting for a head unit to reject
     * the old one or for the user to tap Download in Advanced. Off never touches that repository
     * except from the manual button or the post-rejection prompt.
     */
    var autoUpdateCert: Boolean
        get() = prefs.getBoolean("auto_update_cert", true)
        set(v) = prefs.edit { putBoolean("auto_update_cert", v) }

    /** [net.prototux.aare.cert.CertificateAutoUpdate]'s check cache: when it last asked, at most once a day. */
    var certAutoCheckLastMs: Long
        get() = prefs.getLong("cert_auto_check_last_ms", 0)
        set(v) = prefs.edit { putLong("cert_auto_check_last_ms", v) }

    /**
     * The certificate entry name the user has already seen an "expires soon, no update found"
     * warning for (see [net.prototux.aare.cert.CertificateAutoUpdate]); the warning stays quiet for
     * that exact certificate and comes back only once a different one is the one running out.
     */
    var certExpiryWarningDismissedFor: String?
        get() = prefs.getString("cert_expiry_warning_dismissed_for", null)
        set(v) = prefs.edit { if (v == null) remove("cert_expiry_warning_dismissed_for") else putString("cert_expiry_warning_dismissed_for", v) }

    companion object {
        const val BROWSER_DEFAULT_URL = "https://duckduckgo.com/"
        const val RAIL_AUTO = "auto"
        const val RAIL_LEFT = "left"
        const val RAIL_RIGHT = "right"
        const val RAIL_BOTTOM = "bottom"
        const val THEME_AUTO = "auto"
        const val THEME_DARK = "dark"
        const val THEME_LIGHT = "light"
        // The car screen's one shell (a fixed left icon rail + persistent bottom media/status bar,
        // a dashboard toggling between a map+widgets view and a map+music view) comes in these
        // color themes, matching Dashboard/CarUI's reference mockups.
        /** The default: dark, teal/blue. */
        const val STYLE_MIDNIGHT = "midnight"
        const val STYLE_AURORA = "aurora"
        const val STYLE_FOREST = "forest"
        const val STYLE_LIGHT = "light"
        const val STYLE_SPORT = "sport"
        const val STYLE_DUSK = "dusk"
        /** [carLayout]: AARE's original rail/dashboard/bottom-bar arrangement (the default). */
        const val LAYOUT_CLASSIC = "classic"
        /** [carLayout]: the restructured layout from `CarUI/dusk.html`. */
        const val LAYOUT_DUSK = "dusk_layout"
        const val WALLPAPER_CUSTOM = "custom"
        /** The user's wallpaper photo, in the app's files. */
        const val WALLPAPER_FILE = "wallpaper.jpg"
        /** [wirelessCompatMode]: guess from the adapter's Bluetooth name (the default). */
        const val WIRELESS_COMPAT_AUTO = "auto"
        /** [wirelessCompatMode]: always send the exact message order/certificate/version echo a genuine phone uses. */
        const val WIRELESS_COMPAT_ON = "on"
        /** [wirelessCompatMode]: always use AARE's normal sequence. */
        const val WIRELESS_COMPAT_OFF = "off"
    }

    fun registerListener(l: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.registerOnSharedPreferenceChangeListener(l)
    fun unregisterListener(l: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.unregisterOnSharedPreferenceChangeListener(l)
}
