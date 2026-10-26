# Validation record

Validated on 26 October 2026 using JDK 17, Android SDK 36, and an ARM64 Pixel 5 emulator running Android 17 / API 37. Screenshots show generated test media in a project created only on the emulator; no sample project or fabricated preview ships in the APK.

| Check | Result |
| --- | --- |
| Core JVM tests | 25 passed |
| Android JVM tests | 6 passed |
| Emulator integration tests | 11 passed |
| Debug APK | Built and installed |
| Optimized release APK | Built, unsigned |
| Android lint | 0 errors; 25 warnings |

The integration suite covers three Compose gesture tests, four media/persistence tests, two preview tests, one render test, and one foreground export test. Preview assertions inspect actual SurfaceView pixels. Render assertions inspect decoded frames, audio amplitude, frame counts, and packet timestamps. Export tests exercise MediaStore publication and cancellation cleanup.

The complete emulator suite passed in 129.381 seconds. Subsequent changes only adjusted system-bar appearance, bottom insets, and export close-icon contrast; those changes were rebuilt, linted, and inspected on the emulator.

## Reproduce

```sh
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

Generated reports live in `core/build/reports/tests`, `app/build/reports/tests`, `app/build/reports/androidTests`, and `app/build/reports/lint-results-debug.html`. Direct ADB instrumentation also works after installing the debug and Android test APKs:

```sh
adb shell am instrument -w -r com.bbvideoeditor.app.test/androidx.test.runner.AndroidJUnitRunner
```

## Screens

- [Project library](projects.png)
- [Editor and real timeline media](editor.png)
- [Export settings](export.png)

## Evidence limits

This validates an editor foundation on one emulator. It does not establish commercial release readiness, sustained performance with long layered projects, or compatibility with all physical-device codecs. 4K/HEVC, Android 8–9 document output, and varied HDR/orientation sources need a physical-device matrix. Preview currently recreates its graph at visual clip boundaries to avoid stale frames, which can briefly rebuffer. See the root README for the full feature boundaries and architecture.
