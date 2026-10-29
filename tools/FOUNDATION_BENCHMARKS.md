# Foundation conformance and performance runs

Use JDK 17, the Android SDK including platform 37.0, and a connected device with an unlocked screen. Run from the repository root. Select the intended device explicitly with `adb -s SERIAL`; the examples use `emulator-5554`. Tests create fixtures in this application's storage and remove their published test outputs. Do not clear application data on a device containing projects you need.

## Standard regression

```sh
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease :app:lintDebug --console=plain
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -r com.bbvideoeditor.app.test/androidx.test.runner.AndroidJUnitRunner
```

The four performance methods skip in a normal regression run. Temporal conformance runs by default: actual encoded 24/30/60 FPS moving barcode sources; trim, split, speed and gaps; layered real-surface seek pixels; output-time animation; short AAC and longer sample-exact PCM pulse mapping. Pulse assertions use 50 ms tolerance and video identities allow one source frame. The AAC test measures the encoded source's own onset before applying timeline mappings.

## Engine, lifecycle and 4K measurements

```sh
adb -s emulator-5554 shell am instrument -w -r -e foundationPerformance true -e class com.bbvideoeditor.app.render.FoundationPerformanceTest com.bbvideoeditor.app.test/androidx.test.runner.AndroidJUnitRunner
```

The 120-request cases run simple, layered and animated projects. They assert final source pixels, one public player, one prepare/install, no release, and bounded observed native decoder initialization. Reported seek-offer latency measures the synchronous offer body on Main; native-call latency and pixel convergence are separate fields. No-request-recreation does not mean Media3 never reinitializes a decoder at source changes.

The 4K case requires the host fixture below. It measures one original source and two simultaneous layers, policy decisions, first output submission, final real pixels after 60 offers, PSS, a cold thumbnail, and actual dimensions/cadence of a bounded 2160-request export. It skips explicitly if the file is absent. This is a three-second workload, not sustained playback or thermal certification.

On macOS, generate a real 3840×2160 moving numbered H.264 source with AVFoundation:

```sh
swift tools/generate_temporal_fixture.swift /tmp/foundation-4k30.mp4 3840 2160 30 3 h264 3
adb -s emulator-5554 exec-in run-as com.bbvideoeditor.app sh -c 'cat > cache/foundation-4k30.mp4' < /tmp/foundation-4k30.mp4
```

Arguments are output path, width, height, FPS, seconds, `h264` or `hevc`, and barcode source ID (0–3). Choose a new output filename for another fixture. Copy under the expected cache filename when testing a different format, and keep the measured source FPS/dimensions/source ID consistent with the test assertions. `run-as` requires the debug target; install it and copy before replacing it with the identically signed profile target. Never use `adb uninstall` merely to switch variants.

## Timeline matrix and profile target

```sh
adb -s emulator-5554 shell am instrument -w -r -e foundationPerformance true -e class com.bbvideoeditor.app.editor.TimelinePerformanceTest com.bbvideoeditor.app.test/androidx.test.runner.AndroidJUnitRunner
./gradlew -PfoundationTestBuildType=profile :app:assembleProfile :app:assembleProfileAndroidTest --console=plain
adb -s emulator-5554 install -r app/build/outputs/apk/profile/app-profile.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/profile/app-profile-androidTest.apk
adb -s emulator-5554 shell am instrument -w -r -e foundationPerformance true -e class com.bbvideoeditor.app.editor.TimelinePerformanceTest com.bbvideoeditor.app.test/androidx.test.runner.AndroidJUnitRunner
```

The profile target is non-debuggable, shell-profileable, unminified, and signed with the development key. It is an engineering target, not a distributable release. Reinstall the debug application and debug test APK to return to standard regression.

The matrix has 10/25/50/100 clips per track, four tracks (video, overlay, audio, captions), assets off/on, and native preview off/on. It injects 120 paced finger updates through the actual Compose timeline. It records Window FrameMetrics p50/p95/max, >16.67/>33.33 ms counts, callback loss, Main CPU, ART allocations/GC, PSS, document-root commits, and player/native counters. Asset runs use real thumbnail and progressive waveform queues; native runs retain a layered preview graph. Compare equivalent rows on the same device/build. Injection/idleness overhead is included in CPU/allocation measurements.

Instrumentation emits `INSTRUMENTATION_STATUS: foundation_metrics=...` records. Redirect runner output outside the repository when preserving raw measurements. Capture device model/API, build type, fixture identity and run conditions alongside results. Window `GPU_DURATION` excludes the independent video SurfaceView's native GPU workload; it is not total GPU time. PSS is process-wide and includes shared/platform accounting. Emulator results do not establish physical-device frame pacing.

For long runs, capture output on the device so an interrupted host ADB stream does not lose the result. Start one invocation, poll its file, and pull it after `INSTRUMENTATION_CODE` appears. Wait for completion before installing another variant or starting another runner:

```sh
adb -s emulator-5554 shell "nohup sh -c 'am instrument -w -r -e foundationPerformance true -e foundationTimelineCounts 100 com.bbvideoeditor.app.test/androidx.test.runner.AndroidJUnitRunner > /data/local/tmp/frame-foundation-result.log 2>&1' </dev/null >/dev/null 2>&1 &"
adb -s emulator-5554 shell tail -n 20 /data/local/tmp/frame-foundation-result.log
adb -s emulator-5554 pull /data/local/tmp/frame-foundation-result.log /tmp/frame-foundation-result.log
```

This example runs the full regression with all four opt-in performance methods, focusing the timeline method on its four 400-clip combinations. The process-death protocol remains separate.

For a bounded repeat, add `-e foundationTimelineCounts 100` (or `10,25,50,100`), and optionally `-e foundationTimelineAssets true|false` / `-e foundationTimelineNative true|false`. Omitting these filters runs all 16 combinations. Run performance separately from heavy host builds; record contention if it occurs instead of merging those timings into an isolated comparison.

## Actual process-death recovery

Run the arm invocation in one terminal. After `foundation_process_death_ready` appears, force-stop only this application from another terminal, then run verification in a fresh process:

```sh
adb -s emulator-5554 shell am instrument -w -r -e foundationProcessDeath arm -e class com.bbvideoeditor.app.export.ProcessDeathRecoveryTest#armInterruptedExportAndSavedProject com.bbvideoeditor.app.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5554 shell am force-stop com.bbvideoeditor.app
adb -s emulator-5554 shell am instrument -w -r -e foundationProcessDeath verify -e class com.bbvideoeditor.app.export.ProcessDeathRecoveryTest#verifyInterruptedExportAndSavedProjectAfterFreshProcess com.bbvideoeditor.app.test/androidx.test.runner.AndroidJUnitRunner
```

Arming saves its own identifiable project, starts a real 120-second foreground export, writes a durable marker and waits at most 60 seconds for interruption. Its runner is expected to terminate when killed. Verification asserts the saved project survived, interrupted export became failed/retryable and partial render/request files were removed; it cleans only that test's project/media/marker. The two methods skip during ordinary regression. Do not run this protocol while another real export is active.

## Interpreting acknowledgement

`submittedPositionUs` means the final writer accepted the composed buffer and its synchronization fence. It does not prove display scanout or that the GPU fence has signalled. Correctness tests separately use PixelCopy to inspect the surface. API 26/27 uses the older backend and exposes the weaker applied/scheduled state; do not interpret it as a submitted-frame receipt. Source switches, lifecycle resume, output-size changes and structural graph changes have separate counters from ordinary transport.
