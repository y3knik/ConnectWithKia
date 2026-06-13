# ConnectWithKia

Android app that locks a Kia EV9 about five minutes after Android Auto disconnects from it. Built for personal use in Canada (Kia Connect Canada / `kiaconnect.ca`).

- **Trigger:** Android Auto disconnect from the car (wired or wireless)
- **Delay:** 5 minutes (1–15 min, configurable in Settings)
- **Cancel:** Android Auto reconnects within the window, or you tap **Cancel** on the countdown notification
- **Lock:** sent via Kia Connect Canada — your email, password, and 4-digit vehicle PIN are stored encrypted on-device

This is a personal-use sideload. Not affiliated with Kia.

## Design and plan

- Design: [`docs/superpowers/specs/2026-06-12-connectwithkia-design.md`](docs/superpowers/specs/2026-06-12-connectwithkia-design.md)
- Implementation plan: [`docs/superpowers/plans/2026-06-12-walk-away-lock.md`](docs/superpowers/plans/2026-06-12-walk-away-lock.md)

## Building

Requires JDK 17 and the Android SDK.

```bash
./gradlew :app:assembleDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Signed releases

The `release` workflow signs the APK using these repository secrets (Settings → Secrets and variables → Actions):

- `SIGNING_KEYSTORE_BASE64` — `base64` of an Android signing keystore
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`
- `SIGNING_STORE_PASSWORD`

Create the keystore once locally:

```bash
keytool -genkey -v -keystore release.keystore -alias release \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w 0 release.keystore > release.keystore.b64
```

Copy the contents of `release.keystore.b64` into the `SIGNING_KEYSTORE_BASE64` secret. Tag a release with `git tag v0.1.0 && git push --tags` to trigger the build.

## License

MIT — see [`LICENSE`](LICENSE).
