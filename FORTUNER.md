# CarDisplay: Fortuner 2025 fork of AARE

This project is a fork of [prototux/AARE](https://github.com/prototux/AARE) (GPL-3.0-or-later),
an open-source reimplementation of the **phone side** of Android Auto. AARE is the foundation, so
the protocol isn't reimplemented from scratch. Upstream is the `upstream` git remote. This fork's
changes are on the `fortuner` branch.

## Changes from upstream

| Change | Why |
|---|---|
| `CertificateDownload.ENABLED = true` (reverted 2026-11-11 at the owner's request; the flag stays so it can be switched off) | The owner dropped the earlier "never fetch Google's certificate" rule. Behaviour matches upstream AARE. |
| `extract_key.py` restored from upstream | Same revert. |
| "Connect now" button in Setup: dials a bonded device by name directly over RFCOMM (`WirelessConnector.findBondedByName`, `ProjectionService.startWireless`), skipping the normal check that Android cached that device's wireless-AA UUID (`isHeadUnit`/SDP) | Disabling Android Auto left AARE unable to find/trigger the Kowan adapter at all (it never got the chance to try RFCOMM); this bypasses that discovery step entirely, the way `D:\aa-phone-side` (stage1) does: just dial the known UUID on a bonded device matched by name (default "Kowan", editable in Setup). |
| For the Kowan adapter: force the downloaded Google-issued certificate (skip `settings.certificateIndex` rotation) and echo the head unit's control-channel VERSION_REQUEST exactly (`SessionOptions.echoHeadUnitVersion`), instead of AARE's 1.7/6.1 clamp | Matches `D:\aa-phone-side` stage1 exactly, the only attempt that got `AUTH_COMPLETE` status 0 from this car (protocol-notes.md B9). Download the certificate once first (Advanced > Phone certificates); without one it falls back to the normal rotation. |
| Official-phone message order for the Kowan KA05 adapter (`WirelessBootstrap.Options.officialPhoneOrder`, chosen in `ProjectionService.connectWireless` by BT name "Kowan…"/"AA-Wireless"): version response 6.1 status 0 only, status-only WifiStartResponse right after the WifiStartRequest and before the WifiInfoRequest, no pings of our own, WifiConnectionStatus after the join, TCP retried 20x at 500 ms | Copied from `D:\aa-phone-side` (stage1), which got through the bootstrap and TCP on the first try (2026-11-10). Stock AARE got ECONNREFUSED every time. |

Everything else is upstream as of commit `5d932ae` (2026-11-10).

## BREAKTHROUGH (2026-11-11 18:50-18:52): first full real-car session through AARE itself

After forgetting the Bluetooth pairing and re-pairing the Kowan KA05 from scratch (power-cycling the
adapter alone had not been enough), with Android Auto disabled and no manual "Connect now" needed
(auto-discovery worked on the fresh pairing): a full session ran for ~2.5 minutes against the real
Fortuner. Log evidence: steady `PING_REQUEST`/`PING_RESPONSE` (id 0xb/0xc) the whole time, two
`AUDIO_FOCUS_STATE_LOSS`/`AUDIO_FOCUS_STATE_GAIN` exchanges (id 0x13, `unsolicited=false`, i.e. the
car negotiated audio focus with AARE), and a clean end (`lost the head unit's network` / `Socket
closed`) caused by the owner deliberately disconnecting / turning off Wi-Fi, not a protocol failure.

This is the first time AARE's own TLS/AUTH_COMPLETE/service-discovery/session code (not the stage1
stub) has been run against the real car, and it worked: Bluetooth RFCOMM bootstrap, `officialPhoneOrder`
WPP exchange, Wi-Fi join, TCP, TLS with the downloaded Google certificate (`preferGoogleCertificate`),
exact version echo (`echoHeadUnitVersion`), service discovery, and a live session with real audio-focus
negotiation. Visual confirmation: CONFIRMED by the owner — AARE's screen was visible on the car's
display, and the owner was able to switch to the CarPlay-style theme from AARE's settings while
connected. M2/M3 (first real wireless connection, UI on the car's screen) are both reached.

Earlier the same adapter had gone silent after RFCOMM connected (no WifiVersionRequest/WifiStartRequest
at all, several attempts) even with byte-identical code that had worked the day before in `D:\aa-phone-side`
(stage1). Power-cycling the adapter did not fix it. A full forget-the-pairing + re-pair did. Conclusion:
the Kowan KA05 can get stuck in a state (after any prior session, success or failure) where it accepts a
new RFCOMM socket but never runs WPP, and only clears on a fresh Bluetooth pairing, not a power cycle alone.

## Verified vs. assumed

**Verified (read from AARE's source and docs, 2026-11-11):**
- The phone is the TLS 1.2 server inside AAP channel 0. The head unit sends `AUTH_COMPLETE` with a
  status: 0 accepted, -3 authentication failure, -23 not yet valid, -24 expired
  (`docs/certificates.md`, `MessageStatus.proto`).
- Wireless bootstrap (`aap/.../wireless/WirelessBootstrap.kt`) accepts all three orders seen in
  the field, including a bare `WifiStartRequest` from WirelessAndroidAutoDongle-style adapters.
- AARE's README reports real head units that accept its self-signed certificate (Denso Ten E-DA2,
  2026 Land Cruiser 79, over USB; a ZLink unit over wireless) and ones that reject it with status -3
  or a failed TLS handshake (2024 RAV4 Denso, Skoda MIB3, Ford Transit, Google DHU).

**Assumed, to be confirmed by testing:**
- A commercial wireless AA adapter acts as a byte relay: TLS runs end to end between phone and car,
  so **the car's head unit decides whether the certificate is accepted, not the adapter**. That
  holds for the open-source dongles (WirelessAndroidAutoDongle, aa-proxy-rs in passthrough mode).
  For your adapter model it's not confirmed until the adapter is identified and tested.
- Whether the Fortuner 2025 head unit checks the phone certificate is **unknown**. Toyota uses
  several suppliers (Denso Ten, Panasonic, Denso). One Toyota Denso unit accepted, and one RAV4
  Denso unit rejected.
- Whether your adapter starts the Bluetooth bootstrap with a phone that isn't running Google's
  Android Auto is unknown. Some commercial adapters may expect particular phone-side behaviour.

## M1/M2 test procedure (parked, engine or ACC on)

1. Install `app/build/outputs/apk/debug/app-debug.apk`. If Google's Android Auto is installed,
   leave it installed but don't let it grab the adapter. On the phone, under Settings > Apps >
   Android Auto, turn off "Start Android Auto wirelessly" if your phone shows that option.
2. Open the app, finish *Setup* (permissions, local network on Android 17). For car audio, choose
   *Shizuku* (you have it), or leave it unset if the Fortuner plays Bluetooth audio during AA.
3. Pair the phone with the **adapter** over Bluetooth (not the car's hands-free entry), with the
   adapter plugged into the car's AA USB port.
4. Let it connect. Then open *Advanced > Connection log*, or on a PC run:
   `adb logcat | grep -iE "aare|AareAap"`
5. Record which stage is the last one reached:

| Stage | Log evidence | If it fails here |
|---|---|---|
| A. Bluetooth RFCOMM to the adapter | `wireless:` lines | adapter not paired / not advertising the AA UUID |
| B. Wi-Fi credentials received and joined | `ssid="…"` credentials line, then joined | Android Wi-Fi join dialog, local-network permission |
| C. TCP to the adapter's AAP port | connect to `ip:port` (often 10.0.0.1:5288 on dongles) | VPN overlapping the adapter's subnet |
| D. Version exchange | version request/response | "no version request from head unit" means the car didn't start AA |
| E. TLS handshake | `TLS handshake did not complete` | the head unit hung up during TLS, likely refusing the certificate |
| F. `AUTH_COMPLETE` | `head unit rejected authentication (status N)` or `no AUTH_COMPLETE after the TLS handshake` | N = -3 certificate refused, -23/-24 dates |
| G. Service discovery, video focus | the car screen shows AARE's UI | M3 reached |

6. Send me the log. It tells us whether a failure is the adapter (A–C), the car's AA start (D) or
   the car's certificate check (E–F).

If the car rejects the certificate at E or F, the project rule rules out the Google key route.
The legitimate options left are then limited: whether the adapter has a passthrough mode that
behaves differently, and whether the head unit has firmware or options of its own. Neither is
promised.

## Reuse map for the custom UI (M3–M6)

| Need | AARE component | Reuse |
|---|---|---|
| Bluetooth + Wi-Fi bootstrap | `aap/wireless/*`, `transport/wireless/*` | as is |
| Framing, TLS, session, channels | `aap/*` | as is |
| Video to the car | `video/VideoEncoder.kt`, `ProjectionDisplay.kt`, `BitrateController.kt` | as is |
| Touch | `car/TouchMapper.kt` | as is |
| Audio out / mic | `audio/*` (Shizuku path), `MicRouter.kt` | as is |
| Diagnostics | `DiagLog.kt`, `AndroidAapLog.kt`, *Connection log* screen | as is, then extend |
| **Launcher / home screen** | `ui/car/CarShell.kt`, `DashboardWidgets.kt`, `ui/theme/CarTheme.kt` | **the part to customise** |
| Car App Library apps, media | `host/*`, `media/*` | keep |

The custom launcher work (M6) goes into `ui/car/` once M2/M3 are confirmed on the Fortuner.

## Build

```sh
./gradlew :aap:test :app:assembleDebug
```
JDK 17–21. `local.properties` holds `sdk.dir`.
