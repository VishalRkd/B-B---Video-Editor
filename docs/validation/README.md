# Validation record

Validated on 27 October 2026 using JDK 17, Android SDK 36, and an ARM64 Pixel 5 emulator running Android 17 / API 37. Screenshots show text entered into a project created only on the emulator; no sample project or fabricated preview ships in the APK.

| Check | Result |
| --- | --- |
| Core JVM tests | 32 passed |
| Android JVM tests | 10 passed |
| Emulator integration tests | 30 passed |
| Debug APK | Built and installed |
| Optimized release APK | Built, unsigned |
| Android lint | 0 errors; 26 warnings |

The integration suite covers seven Compose editing/gesture tests, four media/persistence tests, two composition tests, four compiler-path tests, four playback-continuity/text tests, four playback-lifecycle tests, four render/raster tests, and one foreground export test. Preview assertions inspect actual SurfaceView pixels and rendered Compose canvas pixels. Render assertions inspect decoded frames, audio amplitude, frame counts, and packet timestamps. Export tests exercise MediaStore publication and cancellation cleanup.

The complete emulator suite passed in 113.266 seconds. The text update adds native title/caption clips, schema migration, styling/timing controls, direct canvas manipulation, shared rasterization, timed Media3 overlays, and text-only export. Export reporting now accounts for MP4 rotation metadata when displaying portrait dimensions. The earlier playback reliability work and export capability fallback tests remain covered.

Continuity tests play generated sequences through trimmed videos, gaps, 2x and 0.5x speed segments, video/image cuts, and timed captions. They check pixels, player retention, buffering, time, and repeated paused seeks. Compiler tests retain the compositor for overlapping/transformed visuals and enabled effects. Audio-only/text-only projects use a single canvas input without compositor-only settings. This is targeted evidence, not a long-project performance benchmark.

Text tests verify under-finger model updates before release, one-step undo, pinch/rotation, locked tracks, left-trim extension, manual caption timing/style, and durable save/reopen. Export tests compare styled, rotated, partially transparent text layers and caption gaps against the actual Compose canvas. Font rasterization and H.264 chroma subsampling receive an explicit two-pixel edge tolerance, while bulk geometry and pixel-color differences are checked separately. Portrait captions are also verified over decoded video with audio, including output orientation and start/end timing.

The installed app was also checked manually: create a project, enter/style a title, save it to the timeline, move it on the native preview, undo the move, and play to the end. The preview retained the final text frame at the project end. The screenshots below come from that app session.

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

- [Text layer on the editor canvas and timeline](text-editor.png)
- [Text style and timing controls](text-style.png)

## Evidence limits

This validates an editor foundation on one emulator. It does not establish commercial release readiness, sustained performance with long layered projects, or compatibility with all physical-device codecs. 4K/HEVC, Android 8–9 document output, and varied HDR/orientation sources need a physical-device matrix. Layered/transformed preview still recreates its graph at visual clip boundaries to avoid stale frames, which can briefly rebuffer. Interactive text follows the project playback clock; export evaluates output-frame timestamps. Preview detail affects output textures; source proxies are not implemented. Captions are manually entered; automatic transcription, custom fonts, and text animation are future work. See the root README for the full feature boundaries and architecture.
