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

## Releases

This is a personal sideload, so the `release` workflow publishes a debug-signed APK — no keystore secrets required. Tag a version and push only that tag to trigger the build:

```bash
git tag v0.1.0
git push origin v0.1.0
```

The workflow runs `:app:assembleDebug` and attaches `app-debug.apk` to a GitHub Release for the tag. Debug APKs install on any Android device with "Install unknown apps" enabled for your file manager.

## License

MIT — see [`LICENSE`](LICENSE).
