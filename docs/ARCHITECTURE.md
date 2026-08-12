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

Capture ownership is atomic and cleanup is idempotent. Normal lifecycle stops and unexpected read
failures converge on the same release path, preventing a dead audio thread from retaining the
microphone. Irregular vendor read lengths are preserved in the rolling analysis buffer.

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

Scratch buffers are allocated once per detector and reused for each analysis hop. This keeps repeated
array allocation and avoidable garbage collection out of the real-time audio path.

### `audio/GuitarPitchMatcher.kt`

Resolves a detected pitch against the configured guitar strings. It compares both the fundamental and
second harmonic, then folds a valid harmonic match back to the string fundamental before cents are
calculated.

This handles the common low-E failure mode where the phone hears E3, the second harmonic of E2, more
strongly than E2 itself. Only the second harmonic is considered; fitting arbitrary higher harmonics
would make unrelated room sounds too easy to misclassify.

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
- harmonic-aware guitar preset targeting;
- consecutive-frame confirmation before automatic target switches;
- manual string lock;
- chromatic note targeting;
- ±3 cent in-tune classification.

Audio callbacks originate on the capture thread but are serialized through `viewModelScope` before
they touch UI state. Each engine start receives a session token, so queued callbacks from an old
microphone session cannot corrupt a newer session.

### `MainActivity.kt`

Derives one `captureEnabled` state from lifecycle, permission, splash completion and the user's MIC
toggle. A single effect starts or stops capture from that state. Permission callbacks and lifecycle
observers do not manipulate the microphone independently, which removes start/stop ordering races.

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
- low-level, harmonic-rich plucked-string-like signals where the second harmonic is stronger than the fundamental;
- low-E second-harmonic folding across realistic pitch drift;
- fundamental-versus-harmonic ambiguity in Drop D;
- invalid detector configuration and DC-offset rejection;
- rejection of guitar-mode candidates outside configured string neighborhoods.

Physical-device validation then checks the parts a synthetic test cannot reproduce: vendor microphone routing, AGC/noise processing, room noise, guitar position, attack/decay and real harmonic structure.

## Known engineering work before a production release

- benchmark CPU use on low/mid/high Android devices;
- test microphone DSP differences across additional vendors;
- measure pitch error against a calibrated signal source;
- test noisy rooms and different guitar body positions;
- profile attack/decay behavior and octave-error rate;
- add recorded licensed/self-produced audio fixtures;
- add release signing and reproducible release notes.
