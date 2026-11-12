package net.prototux.aare.cert

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.prototux.aare.Settings
import net.prototux.aare.service.ProjectionService
import java.util.Date
import kotlin.concurrent.thread

/**
 * Keeps the downloaded Android Auto certificate ([CertificateDownload]) current without the user
 * having to open Advanced > Phone certificates and tap Download again - needed because the
 * certificate embedded in the real app expires (2026-12-31 in the build this was last checked
 * against; see docs/certificates.md), and iscle/android-auto-certificates republishes a new one
 * whenever Google ships one.
 *
 * At most once a day, off the main thread, this asks that repository for its current version with
 * one small request ([CertificateDownload.fetchVersion]); only when that differs from the one
 * already imported (or none was ever imported) does it make the larger request for the actual
 * certificate and key ([CertificateDownload.fetch]) and import it, exactly as the manual button
 * does. Silent on any failure - the manual button and the post-rejection prompt in
 * [ProjectionService] still work regardless, so a network hiccup here never blocks a connection.
 *
 * Off entirely when [CertificateDownload.ENABLED] is false, when the user switched off
 * [Settings.autoUpdateCert], or while a head unit is connected (no extra traffic during a session).
 *
 * This only helps as long as iscle/android-auto-certificates actually has a current certificate to
 * serve - it's a third party, maintained by hand, and nothing obliges it to publish one the moment
 * Google ships a new build. If it's stale (or gone: see [CertificateDownload.Unavailable]) this
 * object has nothing newer to import, so it instead watches the Google-issued certificate already
 * on the phone and, once it's within [WARN_DAYS] of [CertificateStore.Entry.notAfter], surfaces
 * [expiringSoon] so the UI can point the user at the always-available fallback: run
 * `extract_key.py` against a current Android Auto APK themselves and import the result (Advanced >
 * Phone certificates > Import certificate).
 */
object CertificateAutoUpdate {
    private const val TAG = "AareCertAutoUpdate"
    private const val DAY_MS = 24 * 60 * 60 * 1000L
    private const val WARN_DAYS = 21

    /** The version just imported automatically, for a one-time notice; null once [consumed]. */
    private val _lastUpdated = MutableStateFlow<String?>(null)
    val lastUpdated: StateFlow<String?> = _lastUpdated.asStateFlow()

    /** A Google-issued certificate already on the phone expiring soon, with no newer one found upstream. */
    data class Expiring(val name: String, val notAfter: Date)

    private val _expiringSoon = MutableStateFlow<Expiring?>(null)
    val expiringSoon: StateFlow<Expiring?> = _expiringSoon.asStateFlow()

    @Volatile private var checking = false

    /** Returns at once; checks on a background thread if due. Safe to call on every app open. */
    fun refresh(context: Context) {
        if (!CertificateDownload.ENABLED) return
        val appContext = context.applicationContext
        val settings = Settings(appContext)
        if (!settings.autoUpdateCert) return
        if (ProjectionService.state.value.connected) return
        val now = System.currentTimeMillis()
        if (checking || now - settings.certAutoCheckLastMs < DAY_MS) return
        checking = true
        thread(isDaemon = true, name = "aare-cert-auto-update") {
            try {
                check(appContext, settings)
            } finally {
                checking = false
            }
        }
    }

    /** Stops showing the current "certificate updated" notice. */
    fun consumed() {
        _lastUpdated.value = null
    }

    /** Stops showing [expiringSoon] for the certificate it currently names; comes back only for a different one. */
    fun dismissExpiryWarning(context: Context) {
        _expiringSoon.value?.let { Settings(context.applicationContext).certExpiryWarningDismissedFor = it.name }
        _expiringSoon.value = null
    }

    private fun check(context: Context, settings: Settings) {
        val certs = CertificateStore(context)
        runCatching {
            val latestVersion = CertificateDownload.fetchVersion()
            settings.certAutoCheckLastMs = System.currentTimeMillis()
            if (latestVersion == certs.downloadedVersion()) {
                // Up to date with what the repository has - which may itself be stale; still worth
                // warning if the certificate we're relying on is running out regardless.
                warnIfExpiringSoon(certs, settings)
                return
            }
            val d = CertificateDownload.fetch()
            // Changed again between the two requests: let the next daily check pick up whatever is
            // current then, rather than import something already stale.
            if (d.version != latestVersion) { warnIfExpiringSoon(certs, settings); return }
            val wasGoogleIssued = certs.pick(settings.certificateIndex).isGoogleIssued
            val entry = certs.importDownloaded(d)
            // Only take over as the first-offered certificate if we didn't already have a
            // Google-issued one: a head unit that already accepts the old one keeps working, and a
            // head unit that doesn't check certificates at all is unaffected either way.
            if (!wasGoogleIssued) {
                settings.certificateIndex = certs.entries().indexOfFirst { it.name == entry.name }.coerceAtLeast(0)
            }
            _lastUpdated.value = d.version
            _expiringSoon.value = null // a fresh certificate just landed; any earlier warning no longer applies
            Log.i(TAG, "certificate updated automatically to ${d.version}")
        }.onFailure {
            Log.i(TAG, "certificate auto-update check: ${it.message}")
            // The request itself failed - commonly [CertificateDownload.Unavailable] (the repository
            // was deleted, renamed or taken down) - so there is nothing newer to offer either way.
            // Fall back to what the phone already has.
            warnIfExpiringSoon(certs, settings)
        }
    }

    /**
     * Surfaces [expiringSoon] when the best Google-issued certificate already on the phone is
     * within [WARN_DAYS] of expiring and the user hasn't already dismissed a warning for that exact
     * certificate. A head unit that doesn't check certificates at all is unaffected by the
     * certificate expiring, so this is informational, not a connection failure.
     */
    private fun warnIfExpiringSoon(certs: CertificateStore, settings: Settings) {
        val entry = certs.entries().filter { it.isGoogleIssued }.minByOrNull { it.notAfter } ?: return
        val daysLeft = (entry.notAfter.time - System.currentTimeMillis()) / DAY_MS
        if (daysLeft > WARN_DAYS) return
        if (settings.certExpiryWarningDismissedFor == entry.name) return
        _expiringSoon.value = Expiring(entry.name, entry.notAfter)
    }
}
