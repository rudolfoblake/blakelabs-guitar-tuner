# Changelog

All notable changes to Blake Labs Guitar Tuner will be documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses semantic versioning where practical.

## [Unreleased]

### Planned

- Additional physical-device tuning validation across multiple Android microphones.
- Strobe tuning mode.
- Additional and custom tuning presets.
- Reference tone generator.

## [0.2.2] - 2026-08-08

### Fixed

- Reframed the official Blake Labs artwork from the original source so the full alien contour remains visible instead of being cropped at the top and bottom.
- Added dedicated safe-area wrappers for adaptive launcher icons and the Android 12+ system splash.
- Added second-harmonic recovery in guitar mode so a low E whose strong octave is detected near E3 is normalized back to the E2 fundamental.
- Added automatic-string hysteresis so short room tones and transients cannot immediately steal the currently tracked guitar string.
- Rejected guitar-mode pitch candidates that are too far from any plausible tuning target instead of letting unrelated room noise yank the needle.
- Tightened the low-confidence YIN fallback after physical testing showed that the 0.2.1 fallback was too accepting of noisy, weakly periodic input.

### Tested

- Added deterministic coverage for low-E second-harmonic normalization.
- Added checks that D3 and high E fundamentals remain assigned to the correct strings after harmonic recovery was introduced.

### Changed

- Increased accepted-pitch confidence from 0.45 to 0.50 and in-tune confidence from 0.60 to 0.65.
- Bumped Android application to version 0.2.2 / versionCode 4.

## [0.2.1] - 2026-08-08

### Fixed

- Replaced the reconstructed placeholder alien artwork with the official Blake Labs logo asset supplied by the project owner.
- Updated the in-app logo, adaptive launcher icon and Android 12+ system splash to use the same official mark.
- Prioritized the standard Android `MIC` input path before vendor-sensitive `UNPROCESSED` capture.
- Separated raw microphone signal measurement from pitch detection so the SIGNAL meter can prove that PCM audio is arriving even when pitch has not locked yet.
- Relaxed detector gates for real acoustic-guitar input while retaining confidence gating before a note is accepted.
- Kept a 4096-sample rolling analysis window with a 2048-sample hop: enough low-string cycles for E2/Drop D without paying the CPU/latency cost of an oversized window.
- Added `BlakeTunerAudio` logcat diagnostics with selected input source, sample rate and capture failures.

### Tested

- Added deterministic low-level, harmonic-rich plucked-string style tests where the second harmonic is stronger than the fundamental.
- Kept pure-tone coverage for all six standard guitar strings and explicit silence rejection.

### Changed

- Bumped Android application to version 0.2.1 / versionCode 3.

## [0.2.0] - 2026-08-07

### Added

- Blake Labs alien mark rebuilt as a scalable Android vector and used by the installed launcher icon and in-app branding.
- Premium branded launch screen with the project's free/offline/no-ads promise.
- Dedicated settings view for A4 calibration, tuner mode, haptic feedback and privacy information.
- Signal and detector-lock meters on the main tuning surface.
- Scroll-safe layout for shorter Android displays.
- Branded Android 12+ system splash so the visual identity is present from the first frame.

### Changed

- Reworked the tuner visual system around OLED black, Blake Labs lime, quieter surfaces and stronger information hierarchy.
- Enlarged and refined the tuning gauge with a visible ±3-cent lock zone, smoother needle treatment and clearer cents readout.
- Rebuilt string selection, tuning presets, mode selection and microphone controls to follow the premium Blake Labs UI direction.
- Updated launcher and system theme colors to match the Blake Labs mark.
- Updated the installed Android label to the full product name: Blake Labs Guitar Tuner.
- Bumped the Android application to version 0.2.0 / versionCode 2.

### Product principle

- Free. Offline. No ads. No trackers. No nonsense.
- A guitar tuner should not make you close a casino banner before tuning an E string.

## [0.1.0] - 2026-08-07

### Added

- Initial public Android application.
- Offline YIN/CMNDF pitch detector implemented in Kotlin.
- Mono PCM microphone capture with 48 kHz and 44.1 kHz fallback.
- Guitar mode with Standard, Drop D and DADGAD presets.
- Automatic guitar-string targeting and manual string lock.
- Chromatic tuner mode.
- A4 reference calibration from 430 Hz to 450 Hz.
- Median pitch stabilization and detector-confidence gating.
- ±3 cent in-tune state with flat/sharp guidance.
- Haptic feedback when the note locks in tune.
- Blake Labs dark UI and adaptive launcher icon.
- Privacy documentation, architecture notes, contribution guide and unit tests.

### Privacy

- No network permission.
- No ads.
- No analytics.
- No telemetry.
- No account system.
- No audio persistence or upload.
