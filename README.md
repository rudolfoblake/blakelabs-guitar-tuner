# Blake Labs Guitar Tuner

[![Android CI](https://github.com/rudolfoblake/blakelabs-guitar-tuner/actions/workflows/android.yml/badge.svg)](https://github.com/rudolfoblake/blakelabs-guitar-tuner/actions/workflows/android.yml)

**A precise Android guitar tuner. Free. Offline. No ads. No trackers. No account. No bullshit.**

Because somehow humanity managed to put a banner ad between a guitarist and an E string.

Blake Labs Guitar Tuner is a small, privacy-first Android tuner built for fast visual feedback and reliable pitch detection. The microphone signal is analyzed entirely on-device and is never uploaded or stored.

## What it does

- **Guitar mode** with automatic string detection or manual string locking.
- **Chromatic mode** for anything that makes a reasonably stable note.
- **Standard, Drop D and DADGAD** presets.
- **YIN-based pitch detection** implemented in-project, with no DSP black box.
- **Harmonic-aware low-string matching** so a strong E2 second harmonic remains E2.
- **Independent SIGNAL and LOCK meters** so microphone capture and pitch confidence are visible separately.
- **±3 cent in-tune lock** with clear flat / in-tune / sharp feedback.
- **Median smoothing** to keep the needle useful instead of caffeinated.
- **48 kHz capture** with 44.1 kHz fallback.
- **A4 calibration from 430 to 450 Hz**.
- **Haptic confirmation** when tuning locks.
- **Fully offline** operation.
- **No ads, analytics, telemetry, sign-in, subscriptions or mysterious cloud "AI tuning".**

## UI philosophy

The main screen answers three questions immediately:

1. **What note am I closest to?**
2. **Am I flat or sharp?**
3. **Am I actually in tune yet?**

The app shows the target note, detected frequency, cents offset, a large gauge, and an explicit status. The premium Blake Labs visual system uses true black, the alien-mark lime, a visible ±3-cent lock zone and redundant tuning feedback so nobody has to decode a tiny needle while holding a guitar in one hand.

The launcher icon, Android splash and in-app identity all reuse the same **official Blake Labs logo asset** supplied for the project. No generated lookalike mark.

Design system: [`docs/DESIGN.md`](docs/DESIGN.md).

## Architecture

```text
Microphone
   │
   ├────► Raw RMS ────► SIGNAL meter
   │
   ▼
AudioRecord (mono PCM16)
   │
   ▼
PitchDetector (YIN / CMNDF)
   │
   ▼
GuitarPitchMatcher (fundamental / second harmonic)
   │
   ▼
Median stabilization + target hysteresis
   │
   ├────► Detector confidence ────► LOCK meter
   │
   ▼
MusicTheory (Hz ↔ MIDI ↔ cents)
   │
   ▼
TunerViewModel
   │
   ▼
Jetpack Compose UI
```

More detail: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Build

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

The standard Gradle wrapper downloads the pinned distribution and verifies its SHA-256 checksum.
Android Studio can also import the project normally.

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Every pull request and push to `main` runs unit tests, Android lint, debug assembly and release assembly.
The full validation procedure is documented in [`docs/VALIDATION.md`](docs/VALIDATION.md).

## Tuning accuracy

The current detector is optimized for monophonic guitar-range signals. A clean pluck near the phone microphone works best. Accuracy depends on microphone hardware, ambient noise, harmonics, string attack and the physical instrument.

The UI considers a note **in tune at ±3 cents** with sufficient detector confidence. That threshold is intentionally stricter than "eh, close enough" but still practical for a phone microphone.

For device debugging, Android audio routing is logged under the `BlakeTunerAudio` tag.

## Privacy

The app requests microphone access because, regrettably, Android has not yet invented telepathy.

Audio is processed in memory on the device. It is not recorded to disk, uploaded, transmitted, monetized, analyzed by a third party, or used to sell you guitar picks at 03:00.

See [`PRIVACY.md`](PRIVACY.md).

## Roadmap

Likely next steps:

- strobe display mode;
- more alternate tunings and custom presets;
- configurable cents tolerance;
- landscape / tablet polish;
- optional reference tone generator;
- measured device-level latency and calibration work;
- accessibility and localization pass;
- release signing and Play Store packaging if we ever feel like dealing with that circus.

## Contributing

Issues and pull requests are welcome. Keep the core promise intact: **fast, accurate, local and ad-free**.

See [`CONTRIBUTING.md`](CONTRIBUTING.md).

## License

MIT. See [`LICENSE`](LICENSE).

---

Built by **Blake Labs** for musicians who would like to tune a guitar without first closing three pop-ups.
