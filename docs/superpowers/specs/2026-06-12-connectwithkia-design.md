# ConnectWithKia — Design

**Date:** 2026-06-12
**Status:** Implemented
**Owner:** y3knik (Nikhil Bhatia)
**Repo:** https://github.com/y3knik/ConnectWithKia (rename from `ConnectiwthKia` typo)

## Summary

An Android app that locks a Kia EV9 about five minutes after Android Auto disconnects from the car — a "walk-away lock" feature the EV9 does not provide natively. All logic runs on-device; there is no backend.

## Goals

- Detect Android Auto disconnect on an Android phone paired with a Kia EV9 (wired or wireless).
- Five minutes after disconnect, send a lock command to the car via Kia Connect Canada (`kiaconnect.ca`).
- Cancel the pending lock automatically if Android Auto reconnects within the window.
- Let the user cancel a pending lock from a notification.
- Let the user reduce the prominence of the countdown notification (move it out of the status bar to the shade only). Android does not allow a foreground-service notification to be fully hidden, so this is a prominence toggle, not a visibility toggle.
- Surface failures clearly; never fail silently.
- Ship through GitHub with CI, CodeRabbit code review, and signed-APK releases.

## Non-goals

- No remote unlock, climate control, charging control, or anything beyond `lock`.
- No multi-vehicle support — single EV9, single Kia Connect account.
- No regions other than Canada.
- No Play Store distribution. Sideload via APK from GitHub Releases.
- No backend service. No server-side state. No cloud database.

## High-level architecture

One Android app, no backend. Kotlin + Jetpack Compose, minSdk 29 (Android 10), targetSdk current. Two Gradle modules:

- **`:app`** — Android UI, foreground service, scheduler, settings storage, permissions handling.
- **`:kia`** — pure-Kotlin (JVM) Kia Connect Canada client. No Android dependencies; unit-testable with `MockWebServer`.

Three logical units, each with one responsibility:

1. **AA Detector** — observes `androidx.car.app.CarConnection` LiveData. On `NOT_CONNECTED`, signals the Scheduler. On reconnect, signals cancel. Works for both wired and wireless Android Auto.
2. **Lock Scheduler** — owns the state machine (Section: State machine). Runs a short-lived foreground service while a lock is pending so the OS doesn't kill the alarm. Uses `AlarmManager.setExactAndAllowWhileIdle` for the five-minute wake.
3. **Kia Client (`:kia` module)** — Retrofit + OkHttp + kotlinx.serialization. Talks to `kiaconnect.ca`. Three public methods: `login`, `vehicles`, `lock`.

Credentials and state persist in `EncryptedSharedPreferences` (AES-256 via Android Keystore).

## UI and settings

Three Compose screens, Material 3.

### Status screen (home)

Shows one of:
- "Not configured — add credentials" (CTA → Credentials)
- "Watching — Android Auto connected"
- "Watching — Android Auto disconnected, lock in 4:23" + **Cancel** button
- "Idle — waiting for next drive"
- "Last attempt: locked at 6:42 PM" / "Last attempt failed: invalid PIN — tap to fix"

Top of screen: **Enabled / Disabled** master toggle.
Bottom of screen: **Lock now** test button (verifies credentials end-to-end without driving anywhere).

### Credentials screen

- Kia Connect email
- Kia Connect password
- 4-digit vehicle PIN
- **Test connection** button — runs `login` + `vehicles`, reports success or specific error.

First-launch onboarding lands the user here.

### Settings screen

| Setting | Default | Notes |
|---|---|---|
| Countdown notification prominence | High | Toggle: **High** = status-bar icon and heads-up countdown updates. **Low** = present only in the notification shade (no status-bar icon, no heads-up). The notification cannot be fully hidden — Android requires foreground-service notifications to exist while the service runs. |
| Success notification | On | Brief notification after a successful lock. |
| Failure notification | (always on, not toggleable) | User must know about failures. |
| Lock delay | 5 min | Slider, range 1–15 min. |
| Re-enter credentials / Sign out | — | Clears stored credentials and PIN. |
| About | — | Version, repo link, library credits. |

### Permissions

Requested at first launch with a clear rationale, with one-tap re-prompt from the Status screen if any are missing later:

- `POST_NOTIFICATIONS` (Android 13+, runtime prompt)
- `SCHEDULE_EXACT_ALARM` (Android 12+, system settings deep-link)
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` (Android 14+ requires a service type declared in the manifest; "specialUse" with a justification string is the correct fit for a walk-away-lock helper)
- `RECEIVE_BOOT_COMPLETED` (re-arm pending alarm after a reboot — see "Edge cases")
- Battery optimization exemption (prompted via `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)
- `INTERNET` (normal, auto-granted)

## State machine

Six states, owned by the Lock Scheduler.

```
DISABLED ──(enable + credentials present)──► IDLE
   ▲                                          │
   │                                          │ AA connects
   │ master toggle off                        ▼
   │ or credentials cleared              CONNECTED ◄──────┐
   │                                          │           │
   │                                          │ AA        │ AA
   │                                          │ disconnects│ reconnects
   │                                          ▼           │
   │                                    PENDING_LOCK ─────┘
   │                                     (alarm armed,
   │                                     countdown in
   │                                     foreground-service
   │                                     notification)
   │                                          │
   │                                          │ alarm fires
   │                                          ▼
   │                                       LOCKING
   │                                       (API call,
   │                                        retry x3)
   │                                          │
   │                                          ▼
   │                                        DONE
   │                                  (success or failure
   │                                   notification)
   │                                          │
   └──────────────────────────────────────────┘
        back to IDLE/CONNECTED based on AA
```

### Cancellations during `PENDING_LOCK`

- AA reconnects → cancel alarm, transition to `CONNECTED`.
- User taps **Cancel** on notification → cancel alarm for **this cycle only**; transition to `IDLE`. Next disconnect arms a fresh timer.

### Failures during `LOCKING`

| Failure | Behavior |
|---|---|
| Network error | Retry with exponential backoff (≤3 attempts, ~30 s total). |
| `401 Unauthorized` | Re-run `login`, retry once. |
| Invalid PIN (Kia-specific error code) | Surface "fix PIN" failure notification; do not retry. |
| Vehicle unreachable | Single "couldn't reach vehicle" failure notification; do not retry. |

### Edge cases

- **Phone reboot while `PENDING_LOCK`:** a `BOOT_COMPLETED` receiver reads persisted state. If the original target time is still in the future and within 30 minutes from now, reschedule the alarm. If already past, fire the lock once immediately. If more than 30 minutes stale, give up — the user is likely already away from the car and this no longer matters.
- **Brief AA blip:** any reconnect resets to `CONNECTED`; a fresh disconnect starts a new full-duration timer. No partial credit.
- **User force-stops the app:** nothing fires. Expected behavior — they've explicitly opted out.

## Kia client (`:kia` module)

Pure-Kotlin JVM module. No Android dependencies. The `:app` module depends on `:kia`.

### Public API

```kotlin
interface KiaClient {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun vehicles(): Result<List<Vehicle>>
    suspend fun lock(vehicleId: String, pin: String): Result<Unit>
}

data class Vehicle(val id: String, val nickname: String, val vin: String)

/** Implemented by :app using EncryptedSharedPreferences; injected into KiaClient. */
interface TokenStorage {
    fun readAccessToken(): String?
    fun writeAccessToken(token: String, expiresAtEpochMs: Long)
    fun clear()
}
```

Four-method client surface plus a tiny storage SPI. The `:kia` module owns the protocol and HTTP wiring but is pure JVM, so it cannot use `EncryptedSharedPreferences` directly — it persists the access token through `TokenStorage`, which `:app` implements with Android Keystore-backed encrypted prefs. The cached `vehicleId` is owned entirely by `:app` (kept in encrypted prefs after the first `vehicles()` call) and passed back to `lock(...)`. The Scheduler only calls `lock(...)`; everything else is for first-run setup or transparent token refresh inside the client.

### HTTP flow

Mirrors what `hyundai_kia_connect_api` does for region Canada (region code 2). Base URL: `https://kiaconnect.ca/tods/api/`.

| Step | Endpoint | When called |
|---|---|---|
| 1. Login | `POST lgn` | First setup, and when access token expires (401). |
| 2. Vehicles | `POST vhcllst` | First setup only. Vehicle ID cached in `EncryptedSharedPreferences`. |
| 3. PIN preauth | `POST vrfypin` | On every lock — yields short-lived `pAuth` token. |
| 4. Lock | `POST drlck` | Uses `vehicleId`, `Accesstoken`, `pAuth` headers. |

### Stack

- Retrofit + OkHttp + kotlinx.serialization.
- `HttpLoggingInterceptor` enabled only in debug builds.
- Redaction interceptor strips `Accesstoken`, `pAuth`, and password fields from logs even in debug.

### Testing

- Capture one real successful `login` + `vehicles` + `vrfypin` + `drlck` cycle locally using a one-off debug capture mode.
- Sanitize tokens, IDs, VIN, and email out of the captured JSON.
- Commit fixtures under `kia/src/test/resources/fixtures/`.
- All `:kia` tests load fixtures into `MockWebServer`. **No live calls in CI.**
- Cover: happy path, 401 → re-login → retry, invalid PIN, network timeout, vehicle unreachable.

## Repo layout and pipelines

### Layout

```
ConnectWithKia/
├── .github/
│   ├── workflows/
│   │   ├── ci.yml
│   │   └── release.yml
│   ├── pull_request_template.md
│   └── dependabot.yml
├── .coderabbit.yaml
├── app/                              # Android module
├── kia/                              # pure-Kotlin Kia client
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── .gitignore
├── README.md
└── LICENSE                           # MIT
```

### CI (`.github/workflows/ci.yml`)

Triggers: every pull request, every push to `main`.

Steps:
1. Checkout, set up JDK 17, restore Gradle cache.
2. `./gradlew :kia:test :app:testDebugUnitTest ktlintCheck detekt :app:lintDebug :app:assembleDebug`
3. Upload the debug APK as a workflow artifact (90-day retention).

Required quality gates: unit tests pass, ktlint passes, detekt passes, Android Lint errors = 0.

### Release (`.github/workflows/release.yml`)

Trigger: push of a tag matching `v*.*.*`.

Steps:
1. Checkout, JDK 17, Gradle cache.
2. Decode signing keystore from base64 secret to disk.
3. `./gradlew :app:assembleRelease` with signing config wired to env vars.
4. Create a GitHub Release; attach the signed APK.

Repo secrets needed (added in Settings → Secrets → Actions):
- `SIGNING_KEYSTORE_BASE64`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`
- `SIGNING_STORE_PASSWORD`

### CodeRabbit (`.coderabbit.yaml`)

- CodeRabbit GitHub App installed on the repo (one-click, no repo secrets).
- Config:
  - `reviews.profile: chill` — low-noise reviews appropriate for a personal project.
  - `reviews.auto_review.enabled: true` on all PRs.
  - `reviews.poem: false`.
  - Path filters ignore `**/build/**`, `**/generated/**`, fixtures, and `gradle/wrapper/**`.
  - Summary posted in PR body.

### Dependabot (`.github/dependabot.yml`)

- Weekly grouped updates for Gradle dependencies.
- Weekly updates for GitHub Actions versions.

### Branch protection on `main`

- Require pull request before merging.
- Require the `ci` job to pass.
- Admin (you) may bypass — this is a personal project.

### App package id

`com.github.y3knik.connectwithkia`

### License

MIT.

## Security and privacy

- Email, password, PIN, access token, and vehicle ID stored in `EncryptedSharedPreferences` (AES-256 via Android Keystore-backed master key).
- No analytics. No crash reporting service. No telemetry.
- No data sent anywhere except `kiaconnect.ca`.
- Logs in debug builds redact `Accesstoken`, `pAuth`, and password fields.
- Release builds disable network logging entirely.

## Open questions / future work (out of scope for v1)

- Geofence-based trigger as an alternative to AA disconnect (lock when phone leaves a "parked at car" location). Useful when AA isn't connected (passenger in someone else's car).
- Multi-vehicle support if the Kia account has more than one car.
- Wear OS companion to surface lock status.
- F-Droid publication.

## Decisions log

| Decision | Choice | Rationale |
|---|---|---|
| Where API calls run | On-device, pure Kotlin | No backend, no recurring cost, credentials never leave the phone, one user / one car / one command keeps surface tiny. |
| Region | Canada (`kiaconnect.ca`) | User is in Canada. |
| AA detection method | `androidx.car.app.CarConnection` | Jetpack-supported, works for wired and wireless. |
| Timer mechanism | `AlarmManager.setExactAndAllowWhileIdle` + foreground service | Reliable under Doze; the foreground service keeps the notification persistent and the alarm wakelock honored. |
| Credentials storage | `EncryptedSharedPreferences` | Android Keystore-backed; standard pattern. |
| UI framework | Jetpack Compose, Material 3 | Modern default, less boilerplate than XML. |
| Min SDK | 29 (Android 10) | Covers nearly all devices in use; gives access to modern APIs (BiometricPrompt, scoped storage, etc.). |
| Modularization | `:app` + `:kia` | `:kia` is pure JVM = fast unit tests with `MockWebServer`. |
| License | MIT | Simplest for a personal-use tool. |
| Package id | `com.github.y3knik.connectwithkia` | Matches GitHub handle; personal project, not work. |
| Release pipeline | Included from day one | No cost to having it sit unused until first tag. |
