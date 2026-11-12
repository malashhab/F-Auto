package net.prototux.aare.car

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.telephony.TelephonyManager

/**
 * Phone calls from the car through Android's telephony, so their sound goes through the car's
 * Bluetooth hands-free link as with Android Auto. Needs CALL_PHONE, ANSWER_PHONE_CALLS and
 * READ_PHONE_STATE; every call fails quietly without them.
 */
@SuppressLint("MissingPermission")
object Calls {
    /** The last number dialled from the car, and when (elapsedRealtime): outgoing calls don't tell us their number. */
    @Volatile var lastPlaced: Pair<String, Long>? = null
        private set

    fun place(context: Context, number: String) {
        lastPlaced = number to android.os.SystemClock.elapsedRealtime()
        runCatching { context.getSystemService(TelecomManager::class.java).placeCall(Uri.fromParts("tel", number, null), Bundle()) }
    }

    fun answer(context: Context) {
        runCatching { context.getSystemService(TelecomManager::class.java).acceptRingingCall() }
    }

    /** Hangs up or declines. TelecomManager.endCall is Android 9+: on Android 8 the car's or phone's own buttons do it. */
    @Suppress("DEPRECATION") // still the way to hang up (or decline) without being the dialer
    fun end(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        runCatching { context.getSystemService(TelecomManager::class.java).endCall() }
    }

    /** Whether [end] works on this phone. */
    val canEnd: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    /** TelephonyManager.CALL_STATE_IDLE, _RINGING or _OFFHOOK. */
    @Suppress("DEPRECATION")
    fun state(context: Context): Int = runCatching { context.getSystemService(TelephonyManager::class.java).callState }
        .getOrDefault(TelephonyManager.CALL_STATE_IDLE)

    /**
     * Forces the call onto the phone's own loudspeaker and microphone, away from whatever route
     * Android picked (often a wireless adapter's Bluetooth hands-free profile, whose "microphone"
     * is frequently just not wired up). Safe to call repeatedly; a no-op once already on speaker.
     */
    @Suppress("DEPRECATION")
    fun useSpeaker(context: Context) {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        speakerForced = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val speaker = am.availableCommunicationDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            if (speaker != null) runCatching { am.setCommunicationDevice(speaker) }
            else runCatching { am.isSpeakerphoneOn = true }
        } else {
            runCatching { am.isSpeakerphoneOn = true }
        }
    }

    /** Undoes [useSpeaker] once the call is over; a no-op if it never ran. */
    @Suppress("DEPRECATION")
    fun releaseSpeaker(context: Context) {
        if (!speakerForced) return
        speakerForced = false
        val am = context.getSystemService(AudioManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) runCatching { am.clearCommunicationDevice() }
        else runCatching { am.isSpeakerphoneOn = false }
    }

    @Volatile private var speakerForced = false
}
