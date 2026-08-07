# Changelog

All notable changes to Blake Labs Guitar Tuner will be documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses semantic versioning where practical.

## [Unreleased]

### Planned

- Physical-device tuning validation across multiple Android microphones.
- Strobe tuning mode.
- Additional and custom tuning presets.
- Reference tone generator.

## [0.2.0] - 2026-08-07

### Added

- Premium Blake Labs visual identity inspired by the product concept.
- Blake Labs alien brand mark inside the tuner UI.
- Branded adaptive launcher icon.
- Branded Android 12+ splash screen.
- Premium tuner hero card with large note, frequency, cents, status and analog-style gauge.
- Dedicated manual-string and tuning-preset controls.
- Quick settings for Guitar / Chromatic mode and A4 calibration.
- Clear privacy messaging inside the product UI.

### Changed

- Reworked the color system around the Blake Labs black + acid-lime identity.
- Improved information hierarchy so note, tuning direction and lock state are readable at a glance.
- Refactored the screen entry point so the public `TunerScreen` contract remains stable while the premium implementation can evolve independently.

### Design principle

- Free. Offline. No ads. No trackers. No nonsense.
- A guitar tuner should not require closing a casino banner before tuning an E string.

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
