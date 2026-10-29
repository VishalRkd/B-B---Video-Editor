# Frame Studio

A native Android video editor built with Kotlin, Jetpack Compose, and Media3. The repository was empty at implementation start; no previous app implementation was replaced.

## Build and install

Requires JDK 17 and Android SDK 37.0 (target SDK remains 36). Set `sdk.dir` in your local, untracked `local.properties` or configure `ANDROID_HOME`.

```sh
./gradlew :app:assembleDebug :core:test :app:testDebugUnitTest :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On the development Mac, use:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :app:assembleDebug
```

The debug APK is installable. A commercial release still needs an owner-controlled signing key, release configuration, and a broader physical-device test matrix. `assembleRelease` builds an unsigned optimized release.

The current engineering results and limits are in [the foundation hardening report](EDITOR_ENGINE_FOUNDATION_FINAL_REPORT.md). Repeatable device/profile commands are in [the benchmark guide](tools/FOUNDATION_BENCHMARKS.md). Media3 is pinned to 1.12.0-alpha02; its packet-processing APIs are experimental.

## Editing workflow

1. Create a project, then choose **Media**, **Audio**, or **Overlay**.
2. Tap a clip to select it. Drag the timeline to scrub; pinch to zoom around the focal point.
3. Drag the selected clip's edge handles to trim. Hold a clip to move it or drag it onto a compatible track. Moving onto another clip in the main video track reorders the sequence.
4. Use Split, Delete, Duplicate, Earlier, Later, Adjust, Tracks, Canvas, Undo, and Redo.
5. **Adjust** controls speed, volume, scale, rotation, position, and opacity. Speed changes ripple subsequent clips on the same track. Delete leaves the removed clip's timeline space. Reordering closes internal gaps in that track.
6. Export a snapshot of the current project. Preview releases its decoders while export runs. Editing the project does not change an in-progress export.
7. Use the **Preview** menu in the preview corner to choose Fast, Balanced, or High detail. This preference changes preview rendering size; choose final output quality separately in Export.
8. Choose **Text** for a title or **Caption** for a bottom-positioned caption. Enter the words, start and duration, then choose font, alignment, color, outline, shadow, and background. Save creates a real text clip. Adjacent captions share a text track; overlapping titles create another text layer.
9. Drag text directly on the preview to move it. Pinch to resize/rotate, or use the corner and top handles. Each gesture commits one undo step. Tap selected text again or choose **Edit text** to edit its words/style/timing; **Adjust** provides numeric transform and opacity controls. Timeline trim, split, duplicate, delete, reorder, and compatible track moves also work on text.

Original media is never modified. Photo Picker/document permissions are persisted; providers without durable grants are copied incrementally into app storage. Projects autosave after committed edits and flush on backgrounding/closing. A separate prior snapshot supports recovery after project-file corruption.

## Architecture

```text
Compose editor / WorkspaceViewModel
              |
        EditorViewModel
              |
   EditorCommand + EditorHistory
              |
  immutable, serializable Project                 :core
  └─ CanvasSettings + MediaReference map
  └─ Timeline → Track → Clip → media source TimeRange OR native TextContent
                           └─ Transform / EffectSpec / AnimatedProperty
              |
       CompositionCompiler                       :app/render
        /                \
 PreviewEngine       Media3RenderEngine
 CompositionPlayer   Transformer → hardware codecs → MP4
                          |
                    ExportService → MediaPublisher → MediaStore
```

- **Core:** pure Kotlin; 64-bit microseconds; source ranges, track compatibility, collision and lock validation; bounded snapshot undo/redo. Gesture previews apply pure commands and commit a single undo step on release.
- **Timeline:** fixed playhead; time-based viewport rather than an enormous pixel scroll offset; focal zoom, inertial scrubbing, snapping, edge scrolling, lazy track rows, indexed visible clips. Bitmap and waveform work runs off the main thread.
- **Media:** metadata extraction using platform extractors/retrievers; bounded memory/disk thumbnail caches; streaming PCM peak decoding for waveforms. No fabricated thumbnails or waveforms.
- **Persistence:** versioned JSON, atomic writes, prior-good recovery, serialized IO, and explicit unsupported-schema rejection. Edit history is session-only.
- **Text:** schema 2 stores native `TextContent` inside clips; schema 1 media projects migrate without retiming. Text never creates synthetic media files. `TextRasterizer` uses Android font shaping and a canonical 1080-high coordinate system, with a bounded bitmap cache shared by the editor and renderer. The Compose canvas adapter draws those bitmaps and derives timing/transforms directly from the project. Media3 `TimelineTextOverlay` consumes the same content for native preview and export. Text-only edits with unchanged project duration keep the editor's native video decoder running. Rasterization runs off the UI thread; active GPU overlays retain their bitmap across cache evictions.
- **Render compiler:** the only translation of edits to media operations. Builds fresh stateful processors for each consumer. Visual and audio sequences derive from the same clips and timestamps; visual track gaps are transparent. A transparent primary image supplies output frame cadence. Audio-only sequences handle mixing and silence without mixed-track initialization races.
- **Preview:** a frame-paced latest-request mailbox retains one modern preview executor across ordinary transport, clip/gap boundaries and animated seeks. Transform/property updates use an atomic render binding; structural edits prepare a new composition on the retained player. Submitted-frame state means the final writer accepted the buffer/fence, not display scanout. Lifecycle release/resume and output-size changes have explicit ownership. A source-aware admission policy can lower output detail, create leased internal optimized visual media, or reject excessive inputs while retaining all required layers. Original audio and export sources remain authoritative. API 26/27 keeps a separate legacy compatibility path with weaker acknowledgement and boundary recovery.
- **Export:** immutable job snapshot, device capability queries with supported defaults, requested bitrate/FPS/codec, AAC audio, foreground processing service, notification cancellation, progress, atomic completion publication, interrupted-job reporting and partial-output cleanup. Android 10+ uses `Movies/Frame`; Android 8–9 uses a system-selected document location.

`RenderEffectRegistry` is the extension point for new Media3/GPU effects. Brightness, contrast and saturation render real pixels; source-timed media transform/opacity properties have runtime adapters. Transition execution and unsupported bindings fail explicitly instead of disappearing from preview/export. Text tracks form a title/caption stack above video and overlay tracks; later text tracks render on top. Text-only and audio-only exports use a single canvas input without compositor-only settings.

## Validation

Pure Kotlin tests cover source-time calculations, repeated fractional-speed splits, trim constraints, track moves, duplication/reordering, locks, history invalidation, serialization, indexed visibility, long timelines, focal zoom, and snapping.

Android unit tests cover PCM sample formats/timestamps and exact Media3 speed-duration conversion. Instrumentation tests generate their own H.264 video, PNG images, and WAV audio, then exercise:

- Saved project reopening, corruption recovery, deletion, missing and corrupt media.
- Durable import, actual thumbnail pixels, audio waveform silence versus signal.
- Toolbar edits, continuous trim gestures, cross-track movement, scrubbing, and undo.
- Preview pixels, overlays, clip order, seeking, playback position, and lifecycle release/recreation.
- Continuous preview across video/image/gap boundaries and speed changes, with player identity, buffering, pixel colors, elapsed time, and repeated paused seeks checked together.
- Pending play cancellation, pause after seek, clearing playback intent on empty projects, and playhead retention through actual activity recreation.
- MP4 colors/order/gaps, speed, overlay placement, frame count, stream timestamp ordering, and A/V end alignment.
- Foreground export publication and cancellation cleanup.
- Text/caption creation, style and timing, continuous canvas movement, pinch/rotation, locks, one-step undo, and durable save/reopen.
- Text-only native playback across captions/gaps and paused seeks; text edits preserve the underlying decoder/playhead.
- Styled/rotated text layers compared between the actual Compose canvas and decoded MP4 frames, with an explicit two-pixel tolerance for font smoothing and H.264 chroma edges. Portrait captions are also rendered over real video with audio.

```sh
./gradlew :app:connectedDebugAndroidTest
```

Tests use an emulator/device connected through ADB. They do not require access to personal media. Pixel tests capture the video SurfaceView and sample inside the composition's letterboxed region.

See the [current foundation report](EDITOR_ENGINE_FOUNDATION_FINAL_REPORT.md) for current test counts, measured before/after results and limits. The [earlier validation record](docs/validation/README.md) is historical.

## Current boundaries

- Media3 CompositionPlayer and the new packet API are experimental. Modern transport retains one executor, but native calls, codec item switches, buffering and GPU frame tails still cost time. API 26/27 compatibility and broad physical-device behavior need separate validation. Decode accuracy is constrained by source cadence, the millisecond seek API and codecs.
- Output detail and original source decoding are accounted for separately. Internal optimized media is implemented, but cold preparation takes real time; layer/codec/resource limits can still reject preview explicitly. Cache budgets are not a hard bound on unknown driver memory.
- Encoder capability is a reported possibility. Actual export dimensions/cadence are inspected, and observed size fallbacks persist by backend/device version. The tested emulator produced 1080p from a 2160 request. 4K HEVC/60 FPS and physical-device coverage remain distinct from the verified H.26430 optimized-preview workload.
- HDR sources are tone-mapped to SDR. HDR output and color-managed mastering are not implemented.
- Transition execution, keyframe editing, speed ramps, masks, chroma key and LUT tools remain future work. Basic colour effects and source-timed transform evaluation are implemented; unsupported features are rejected explicitly.
- Captions are manually entered/timed. Automatic transcription, SRT import/export, custom font import, word highlighting, and animated text presets are not implemented. The four font choices use Android system families; exact glyphs can differ across devices. Text is limited to 2000 characters and bounded raster dimensions; overly tall/large text reports an error. Captions are burned into MP4 frames rather than exported as a selectable subtitle track.
- Interactive text preview follows the project playback clock (approximately 33 ms updates); export evaluates text at each output frame timestamp. Font smoothing, H.264 compression, and preview scaling can produce small edge differences. All text layers currently sit above visual layers; arbitrary text/video interleaving is not supported.
- Clips cannot overlap within one track; use overlay tracks for simultaneous visuals. Tracks can be added, locked, and muted; arbitrary track-stack reordering and track deletion are future tools.
- Missing media is reported and can be removed; a dedicated relink UI is not implemented. Undo history is not persisted across reopening.
- A killed export is reported as interrupted and can be started again. It does not resume midway through encoding. Android imposes background processing time limits.
- Android 8–9's document output path and physical-device codec/orientation combinations still need dedicated compatibility testing.

## Engine references

- [Media3 compositions](https://developer.android.com/media/media3/transformer/composition)
- [CompositionPlayer preview](https://developer.android.com/media/media3/transformer/compositionplayer)
- [Foreground media processing services](https://developer.android.com/develop/background-work/services/fgs/service-types)

The Gradle dependencies are pinned. The preview/export adapters contain the Media3-specific APIs so the editing model stays independent of backend upgrades.
