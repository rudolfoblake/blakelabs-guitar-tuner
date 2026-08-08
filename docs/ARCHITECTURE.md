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

Owns `AudioRecord`, selects a supported sample rate, reads mono PCM16 frames and feeds overlapping analysis windows to the pitch detector on a dedicated daemon thread.

Current capture source order:

1. `MIC`;
2. `UNPROCESSED`;
3. `DEFAULT`;
4. `VOICE_RECOGNITION`.

The regular microphone path is intentionally first. Physical Android testing showed why successful `AudioRecord` initialization is not enough: some vendor implementations can expose an `UNPROCESSED` path while delivering silence or heavily attenuated input. The standard microphone source is the most portable choice for an instrument tuner; the other sources remain fallbacks.

The engine publishes raw RMS signal level independently from pitch detection. This means the UI can answer two different questions:

- **SIGNAL:** is PCM audio actually arriving from the microphone?
- **LOCK:** did the pitch detector find a reliable periodic fundamental?

The current window is 4096 samples with a 2048-sample hop. At 48 kHz that contains enough cycles for E2 and Drop D while keeping the YIN workload and interactive latency reasonable on phones.

Runtime diagnostics use the `BlakeTunerAudio` logcat tag and report the selected input source, sample rate, capture start/stop and failures.

### `audio/PitchDetector.kt`

A dependency-free implementation of the YIN family of pitch detection:

1. remove DC offset;
2. reject only near-silent RMS input;
3. calculate the difference function;
4. calculate cumulative mean normalized difference (CMNDF);
5. find the first acceptable local minimum;
6. fall back to the global minimum only when confidence remains reasonable;
7. use parabolic interpolation around the selected lag;
8. convert lag to frequency.

The current useful range is 55–1200 Hz. That comfortably covers standard guitar, Drop D and useful upper notes while leaving headroom for chromatic mode.

Detector thresholds are deliberately separated from the UI's in-tune threshold. A pitch can be displayed before it is trusted strongly enough to produce the green in-tune lock.

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
- detector-confidence gates;
- independent raw-signal state;
- stale-reading clearing;
- guitar preset targeting;
- manual string lock;
- chromatic note targeting;
- ±3 cent in-tune classification.

### `ui/`

Jetpack Compose UI. The interface intentionally uses redundant cues: note name, cents number, gauge, color, plain-language direction, raw signal and detector lock.

## Branding boundary

The official Blake Labs logo is stored as a project-owned bitmap asset and reused by the in-app brand mark, adaptive launcher icon and Android 12+ splash. It is not reconstructed as a lookalike vector.

## Privacy boundary

There is no network permission in the manifest. Audio samples exist only in process memory during analysis and are discarded as buffers roll forward.

## Why no third-party pitch library?

Mature projects such as TarsosDSP provide YIN and other estimators, but pulling a framework into this app has licensing, repository and Android-compatibility consequences. The current pitch path is small enough to audit, benchmark and test directly while preserving the project's MIT-friendly, offline architecture.

That decision is not ideological. A third-party engine remains an option if physical-device benchmarks demonstrate a measurable accuracy or latency advantage that outweighs the dependency cost.

## Validation strategy

Automated DSP tests cover:

- all six standard guitar fundamentals with pure tones;
- explicit silence rejection;
- low-level, harmonic-rich plucked-string-like signals where the second harmonic is stronger than the fundamental.

Physical-device validation then checks the parts a synthetic test cannot reproduce: vendor microphone routing, AGC/noise processing, room noise, guitar position, attack/decay and real harmonic structure.

## Known engineering work before a production release

- benchmark CPU use on low/mid/high Android devices;
- test microphone DSP differences across additional vendors;
- measure pitch error against a calibrated signal source;
- test noisy rooms and different guitar body positions;
- profile attack/decay behavior and octave-error rate;
- add recorded licensed/self-produced audio fixtures;
- add release signing and reproducible release notes.
