# Validation record

Validated on 27 October 2026 using JDK 17, Android SDK 36, and an ARM64 Pixel 5 emulator running Android 17 / API 37. Screenshots show generated test media in a project created only on the emulator; no sample project or fabricated preview ships in the APK.

| Check | Result |
| --- | --- |
| Core JVM tests | 25 passed |
| Android JVM tests | 10 passed |
| Emulator integration tests | 21 passed |
| Debug APK | Built and installed |
| Optimized release APK | Built, unsigned |
| Android lint | 0 errors; 26 warnings |

The integration suite covers three Compose gesture tests, four media/persistence tests, two existing composition tests, four compiler-path tests, two playback-continuity tests, four playback-lifecycle tests, one render test, and one foreground export test. Preview assertions inspect actual SurfaceView pixels. Render assertions inspect decoded frames, audio amplitude, frame counts, and packet timestamps. Export tests exercise MediaStore publication and cancellation cleanup.

The complete emulator suite passed in 78.591 seconds. The playback reliability update adds a single-input preview path for eligible edits, distinct transport intent during buffering, safer scrub cancellation, retained playhead on activity recreation, and selectable preview detail. Export capability fallbacks remain covered by four unit tests.

Continuity tests play generated four-second sequences through trimmed videos, gaps, 2x and 0.5x speed segments, and video/image cuts. They assert correct pixels, one retained player, no rebuffering after startup, monotonic time, bounded elapsed time, and repeated paused seeks. Compiler tests ensure transforms, overlapping layers, enabled effects, and audio-only projects retain the compositor path, while the export path remains unchanged. This is targeted evidence, not a long-project performance benchmark.

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
- [Preview quality controls](preview-quality.png)

## Evidence limits

This validates an editor foundation on one emulator. It does not establish commercial release readiness, sustained performance with long layered projects, or compatibility with all physical-device codecs. 4K/HEVC, Android 8–9 document output, and varied HDR/orientation sources need a physical-device matrix. Layered/transformed preview still recreates its graph at visual clip boundaries to avoid stale frames, which can briefly rebuffer. Preview detail affects output textures; source proxies are not implemented. See the root README for the full feature boundaries and architecture.
