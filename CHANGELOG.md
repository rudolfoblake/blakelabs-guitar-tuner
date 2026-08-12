# Changelog

All notable changes to Blake Labs Guitar Tuner will be documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses semantic versioning where practical.

## [Unreleased]

### Planned

- Additional physical-device tuning validation across multiple Android microphones.
- Strobe tuning mode.
- Additional and custom tuning presets.
- Reference tone generator.

## [0.3.3] - 2026-08-12

### Fixed

- Separated the launcher foreground from the shared brand asset and placed the official logo inside Android's 72 dp adaptive-icon viewport.
- Prevented OEM launcher masks from scaling and clipping the face while retaining the full-proportion logo inside the app and splash.

### Changed

- Bumped Android application to version 0.3.3 / versionCode 10.

## [0.3.2] - 2026-08-12

### Fixed

- Restored the official Blake Labs alien eye proportions in the shared launcher, splash and in-app brand asset.
- Rebuilt the source-derived WebP at 512 × 512 so Android scaling no longer amplifies the previous low-resolution reconstruction.

### Changed

- Bumped Android application to version 0.3.2 / versionCode 9.

## [0.3.1] - 2026-08-12

### Changed

- Updated the Compose BOM to 2026.06.01, Kotlin/Compose toolchain to 2.4.10, Lifecycle to 2.11.0 and Gradle Wrapper to 9.7.0.
- Raised `compileSdk` to API 37 while retaining `targetSdk` 36 and `minSdk` 26, so runtime compatibility and supported devices do not change.
- Grouped related Lifecycle and Kotlin toolchain updates in Dependabot to avoid duplicate, independently failing pull requests.
- Bumped Android application to version 0.3.1 / versionCode 8.

## [0.3.0] - 2026-08-12

### Fixed

- Restored low-E second-harmonic folding as an isolated runtime change, without restoring the branding/resource changes associated with the failed 0.2.2 build.
- Added automatic-target hysteresis so a single detector outlier cannot switch guitar strings.
- Serialized audio callbacks onto the ViewModel scope and rejected callbacks from stale capture sessions.
- Replaced competing permission/lifecycle microphone controls with one derived capture state.
- Made `AudioRecord` shutdown idempotent so unexpected read failures cannot retain the microphone.
- Preserved samples from irregular vendor read sizes instead of assuming every read equals the configured hop.

### Changed

- Added dedicated confidence gates for low strings and stricter gates for normal strings.
- Reused YIN scratch buffers to reduce allocation and garbage collection in the real-time audio path.
- Changed the gauge animation to critical damping so UI motion cannot overshoot the measured cents value.
- Disabled Android backup and cleartext traffic as defense-in-depth for the offline privacy boundary.
- Bumped Android application to version 0.3.0 / versionCode 7.

### Added

- Checked-in Gradle 9.5 wrapper with distribution SHA-256 verification.
- GitHub Actions quality gate for unit tests, lint, debug assembly and release assembly.
- Dependabot configuration for Gradle and GitHub Actions dependencies.
- Regression coverage for low-E harmonic drift, Drop D ambiguity, DC offset and invalid detector configuration.
- A repeatable physical-device and release validation checklist.

## [0.2.4] - 2026-08-09

### Fixed

- Restored the complete application tree from the last physical-device build proven to launch and tune successfully (`0.2.1`).
- Removed the `0.2.2` low-E / automatic-target experiment from the runtime after the same device began failing immediately after launch.
- Restored the exact `0.2.1` Blake Labs logo/resource path instead of continuing to patch the broken `0.2.2` branding state.

### Changed

- Bumped Android application to version 0.2.4 / versionCode 6 so this rollback build is unambiguous on-device and cannot be confused with the failed `0.2.3` experiment.

### Validation note

- This release intentionally prioritizes returning to a known-good launch/tuning baseline. Low-E and room-noise improvements will be reapplied separately only after startup is physically validated again.

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
