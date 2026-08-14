# iOS build and validation

The iPhone app is a native SwiftUI shell backed by the same Kotlin Multiplatform DSP core used by Android.

## Requirements

- macOS with Xcode and command-line tools
- JDK 17+
- XcodeGen (`brew install xcodegen`)

## Generate and open the project

```bash
cd iosApp
xcodegen generate
open BlakeTuner.xcodeproj
```

Choose an iPhone simulator or connected iPhone and run the `BlakeTuner` scheme. The Xcode pre-build phase runs:

```bash
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

so Swift always links the framework produced from the current shared Kotlin sources.

## Command-line validation

From the repository root:

```bash
./gradlew :shared:allTests :shared:linkDebugFrameworkIosSimulatorArm64
cd iosApp
xcodegen generate
xcodebuild \
  -project BlakeTuner.xcodeproj \
  -scheme BlakeTuner \
  -sdk iphonesimulator \
  -configuration Debug \
  CODE_SIGNING_ALLOWED=NO \
  build
```

## Device validation

Simulator compilation validates integration, but tuning quality must be checked on physical hardware because microphone routing, gain and acoustic response matter. Validate at least:

- E2, A2, D3, G3, B3 and E4 in Standard;
- D2 in Drop D;
- low-E second-harmonic behavior;
- A4 calibration at 430, 440 and 450 Hz;
- manual string lock and automatic targeting;
- repeated microphone start/stop;
- permission denial and re-launch;
- wired/Bluetooth audio route changes;
- foreground/background transitions;
- haptic lock only on entry into the in-tune state.

The privacy boundary remains unchanged: microphone PCM is processed in memory and is not persisted or transmitted.
