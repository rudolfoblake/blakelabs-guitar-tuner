# Architecture

## Goals

Blake Labs Guitar Tuner is deliberately small. The architecture optimizes for:

- predictable low-latency audio capture;
- auditable signal processing;
- no network dependency;
- easy physical-device debugging;
- clear separation between DSP, music theory, state and UI.

## Modules

### `audio/AudioEngine.kt`

Owns `AudioRecord`, selects a supported sample rate, reads mono PCM16 frames and feeds analysis windows to the pitch detector on a dedicated daemon thread.

Preferred capture source order:

1. `UNPROCESSED`;
2. `VOICE_RECOGNITION`;
3. `DEFAULT`.

This attempts to avoid vendor audio processing where possible while retaining compatibility.

### `audio/PitchDetector.kt`

A dependency-free implementation of the YIN family of pitch detection:

1. remove DC offset;
2. reject very low RMS input;
3. calculate the difference function;
4. calculate cumulative mean normalized difference (CMNDF);
5. find the first acceptable local minimum;
6. fall back to the global minimum only when confidence remains reasonable;
7. use parabolic interpolation around the selected lag;
8. convert lag to frequency.

The current useful range is 65–1200 Hz, comfortably covering normal guitar fundamentals and useful upper notes.

### `audio/MusicTheory.kt`

Pure functions for:

- MIDI note ↔ frequency conversion;
- nearest equal-tempered note;
- cents difference;
- note naming.

A4 is configurable and defaults to 440 Hz.

### `TunerViewModel.kt`

Owns application state and tuning decisions:

- five-sample median stabilization;
- confidence gates;
- stale-reading clearing;
- guitar preset targeting;
- manual string lock;
- chromatic note targeting;
- ±3 cent in-tune classification.

### `ui/`

Jetpack Compose UI. The interface intentionally uses redundant cues: note name, cents number, gauge, color and plain-language direction.

## Privacy boundary

There is no network permission in the manifest. Audio samples exist only in process memory during analysis and are discarded as buffers roll forward.

## Why no third-party pitch library?

A library can absolutely be justified later, but the initial YIN implementation is compact enough to audit and test directly. Avoiding a large DSP dependency also keeps the app size and attack surface down.

## Known engineering work before a production release

- benchmark CPU use on low/mid/high Android devices;
- test microphone DSP differences across vendors;
- measure pitch error against a calibrated signal source;
- test noisy rooms and different guitar body positions;
- profile attack/decay behavior and octave-error rate;
- expand automated tests with recorded fixtures;
- add release signing and reproducible release notes.
