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
