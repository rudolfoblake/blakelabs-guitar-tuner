# Blake Labs Guitar Tuner

[![Android CI](https://github.com/rudolfoblake/blakelabs-guitar-tuner/actions/workflows/android.yml/badge.svg)](https://github.com/rudolfoblake/blakelabs-guitar-tuner/actions/workflows/android.yml)
[![iOS CI](https://github.com/rudolfoblake/blakelabs-guitar-tuner/actions/workflows/ios.yml/badge.svg)](https://github.com/rudolfoblake/blakelabs-guitar-tuner/actions/workflows/ios.yml)

**A precise guitar tuner for Android and iPhone. Free. Offline. No ads. No trackers. No account. No bullshit.**

Because somehow humanity managed to put a banner ad between a guitarist and an E string.

Blake Labs Guitar Tuner is a small, privacy-first tuner built for fast visual feedback and reliable pitch detection. The microphone signal is analyzed entirely on-device and is never uploaded or stored.

## What it does

- **Guitar mode** with automatic string detection or manual string locking.
- **Chromatic mode** for anything that makes a reasonably stable note.
- **Standard, Drop D and DADGAD** presets.
- **YIN-based pitch detection** implemented in-project, with no DSP black box.
- **Harmonic-aware low-string matching** so a strong E2 second harmonic remains E2.
- **Independent SIGNAL and LOCK meters** so microphone capture and pitch confidence are visible separately.
- **±3 cent in-tune lock** with clear flat / in-tune / sharp feedback.
- **Median smoothing** to keep the needle useful instead of caffeinated.
- **48 kHz preferred capture**, with the active device sample rate used by the shared detector.
- **A4 calibration from 430 to 450 Hz**.
- **Haptic confirmation** when tuning locks.
- **Fully offline** operation.
- **No ads, analytics, telemetry, sign-in, subscriptions or mysterious cloud "AI tuning".**

## Platforms

### Android

The Android app remains a native Jetpack Compose application. Microphone capture uses `AudioRecord`, while the pitch detector, music theory and guitar harmonic matcher now live in the shared Kotlin Multiplatform module.

### iPhone

The iPhone app uses a native SwiftUI interface and `AVAudioEngine` microphone capture. PCM windows are passed to the same Kotlin Multiplatform DSP core used by Android, so pitch detection does not fork into separate Android and iOS implementations.

See [`docs/IOS.md`](docs/IOS.md) for Xcode generation, simulator builds and physical-device validation.

## UI philosophy

The main screen answers three questions immediately:

1. **What note am I closest to?**
2. **Am I flat or sharp?**
3. **Am I actually in tune yet?**

The app shows the target note, detected frequency, cents offset, a large gauge, and an explicit status. The premium Blake Labs visual system uses true black, the alien-mark lime, a visible ±3-cent lock zone and redundant tuning feedback so nobody has to decode a tiny needle while holding a guitar in one hand.

The Android launcher icon, splash and in-app identity reuse the same **official Blake Labs logo asset** supplied for the project. The iPhone shell follows the same visual system while App Store icon packaging remains a release task.

Design system: [`docs/DESIGN.md`](docs/DESIGN.md).

## Architecture

```text
                         shared Kotlin Multiplatform core
                    ┌────────────────────────────────────┐
                    │ PitchDetector (YIN / CMNDF)        │
                    │ GuitarPitchMatcher                 │
                    │ MusicTheory                        │
                    │ TunerProcessor (iOS state machine) │
                    └────────────────────────────────────┘
                             ▲                 ▲
                             │ PCM16           │ PCM16
                             │                 │
                    Android AudioRecord   iOS AVAudioEngine
                             │                 │
                    Jetpack Compose         SwiftUI
```

On Android, the existing `TunerViewModel` retains its mature lifecycle and UI-state behavior while consuming the shared detector/matcher/theory classes. On iOS, `TunerProcessor` provides equivalent target hysteresis, confidence gating, median stabilization and tuning status through a narrow Swift bridge.

More detail: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Android build

Requirements:

- Android Studio with Android SDK 37
- JDK 17+
- no system Gradle installation; the checked-in wrapper pins and verifies Gradle 9.7

The project compiles against Android API 37, targets API 36, and supports Android 8.0 / API 26 and newer.

Windows PowerShell:

```powershell
.\tools\build-debug.ps1
```

macOS / Linux:

```bash
./tools/build-debug.sh
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## iPhone build

Requirements:

- macOS with Xcode
- JDK 17+
- XcodeGen

```bash
cd iosApp
xcodegen generate
open BlakeTuner.xcodeproj
```

The Xcode pre-build phase runs `:shared:embedAndSignAppleFrameworkForXcode`, keeping the Swift app connected to the current shared Kotlin framework. Full instructions and the physical-device checklist are in [`docs/IOS.md`](docs/IOS.md).

## Continuous integration

Every pull request and push to `main` runs the Android unit tests, lint, debug assembly and release assembly. iOS CI runs the shared Kotlin tests, links the iOS Simulator framework, generates the Xcode project and builds the iPhone Simulator app without code signing.

The full Android validation procedure is documented in [`docs/VALIDATION.md`](docs/VALIDATION.md).

## Tuning accuracy

The current detector is optimized for monophonic guitar-range signals. A clean pluck near the phone microphone works best. Accuracy depends on microphone hardware, ambient noise, harmonics, string attack and the physical instrument.

The UI considers a note **in tune at ±3 cents** with sufficient detector confidence. That threshold is intentionally stricter than "eh, close enough" but still practical for a phone microphone.

Simulator builds validate integration, not microphone quality. Android and iPhone releases should both receive physical-device acoustic validation before store release.

## Privacy

The app requests microphone access because, regrettably, phones have not yet invented telepathy.

Audio is processed in memory on the device. It is not recorded to disk, uploaded, transmitted, monetized, analyzed by a third party, or used to sell you guitar picks at 03:00.

See [`PRIVACY.md`](PRIVACY.md).

## Roadmap

Likely next steps:

- physical iPhone tuning validation and App Store packaging;
- strobe display mode;
- more alternate tunings and custom presets;
- configurable cents tolerance;
- landscape / tablet polish;
- optional reference tone generator;
- measured device-level latency and calibration work;
- accessibility and localization pass;
- release signing and store packaging.

## Contributing

Issues and pull requests are welcome. Keep the core promise intact: **fast, accurate, local and ad-free**.

See [`CONTRIBUTING.md`](CONTRIBUTING.md).

## License

MIT. See [`LICENSE`](LICENSE).

---

Built by **Blake Labs** for musicians who would like to tune a guitar without first closing three pop-ups.
