# Validation

This project has two quality gates. Neither replaces the other:

1. deterministic build, unit-test and lint checks;
2. physical validation of Android microphone behavior and real guitar transients.

## Automated gate

Requirements:

- JDK 17 or newer;
- Android SDK 36;
- network access for the first dependency resolution.

Run the complete CI-equivalent gate:

```bash
./gradlew --no-daemon \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug \
  :app:assembleRelease
```

The Gradle wrapper distribution is pinned in `gradle/wrapper/gradle-wrapper.properties` and protected
by `distributionSha256Sum`. CI separately validates the wrapper before executing it.

## Physical-device gate

Use a quiet room first, then repeat the noise checks with normal conversation or a fan in the room.
Keep the phone 20–40 cm from the guitar sound hole or electric-guitar speaker.

### Startup and lifecycle

- cold-start the app ten times;
- grant microphone permission on a fresh install;
- background and foreground the app five times;
- turn MIC off and on five times;
- open Android permission settings, revoke permission, return, then grant again;
- confirm no crash, frozen SIGNAL meter or retained microphone indicator after leaving the app.

### Standard tuning

- test E2, A2, D3, G3, B3 and E4 from at least ±30 cents;
- pluck each string softly and firmly;
- confirm the target does not switch on a single outlier frame;
- confirm the needle enters the ±3-cent zone without visual overshoot;
- confirm haptics fire once per transition into tune.

### Low E regression

- test open E2 at approximately 82.41 Hz;
- pluck near the bridge and near the neck to vary harmonic balance;
- pluck firmly enough to emphasize the attack and second harmonic;
- confirm a detected E3 harmonic remains targeted as E2;
- confirm the needle does not slam to +50 cents and return;
- repeat with E2 manually locked and with automatic string selection.

### Alternate tunings

- verify D2 in Drop D and DADGAD;
- verify a real D3 remains D3 rather than being folded into the D2 second harmonic;
- verify preset and manual-string changes clear stale readings immediately.

## Diagnostics

Capture audio lifecycle logs with:

```bash
adb logcat -s BlakeTunerAudio
```

Record the device model, Android version, selected audio source, sample rate and reproduction steps for
every physical failure. A synthetic test proves deterministic math; it does not prove vendor microphone
routing, AGC, noise suppression or attack/decay behavior.

## Release decision

A release is eligible only when:

- CI is green;
- the low-E regression checklist passes on at least one physical device;
- startup/lifecycle checks pass;
- version and changelog are updated;
- no Internet permission, analytics or audio persistence has been introduced.
