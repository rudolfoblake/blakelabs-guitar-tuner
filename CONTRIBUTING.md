# Contributing

Contributions are welcome.

## Principles

Please preserve the project's core constraints:

1. Pitch detection must remain useful on real Android hardware, not only synthetic test tones.
2. Audio processing should stay local by default.
3. Do not add advertising or tracking SDKs.
4. Keep dependencies intentional and explain non-obvious additions.
5. Prefer readable DSP code over clever DSP code.
6. UI changes should make tuning easier to understand at a glance.

## Development

- JDK 17+
- Android SDK 36
- Kotlin / Android Gradle Plugin versions are pinned in the root build configuration.

Run local unit tests with:

```bash
./tools/build-debug.sh :app:testDebugUnitTest
```

Build a debug APK with:

```bash
./tools/build-debug.sh
```

On Windows use `tools\build-debug.ps1` instead.

There is currently no CI workflow. Run relevant validation locally and describe it in the pull request.

## Pull requests

Keep PRs focused. Include:

- what changed;
- why it changed;
- how it was tested;
- any device/microphone used for physical validation;
- screenshots for meaningful UI changes.
