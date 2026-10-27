# Frame Studio

A native Android video editor built with Kotlin, Jetpack Compose, and Media3. The repository was empty at implementation start; no previous app implementation was replaced.

## Build and install

Requires JDK 17 and Android SDK 36. Set `sdk.dir` in your local, untracked `local.properties` or configure `ANDROID_HOME`.

```sh
./gradlew :app:assembleDebug :core:test :app:testDebugUnitTest :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On the development Mac, use:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :app:assembleDebug
```

The debug APK is installable. A commercial release still needs an owner-controlled signing key, release configuration, and a broader physical-device test matrix. `assembleRelease` builds an unsigned optimized release.

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
- **Preview:** lifecycle releases decoders and GL resources, and activity recreation retains the playhead. Recompilation coalesces drag edits. The shared compiler can flatten non-overlapping visual clips with identity transforms and no enabled effects into a single visual sequence for preview. This path retains one player across cuts, images, gaps, speed changes, and paused seeks. Layered/transformed projects retain a safe decoder reset at visual boundaries to avoid stale frames in Media3 1.11.1's legacy compositor. Playback intent is separate from actual playback so Pause also works during buffering. Preview detail limits the longer edge to 640, 1280, or 1920 pixels without enlarging smaller canvases; this reduces render size, not source decode resolution.
- **Export:** immutable job snapshot, device capability queries with supported defaults, requested bitrate/FPS/codec, AAC audio, foreground processing service, notification cancellation, progress, atomic completion publication, interrupted-job reporting and partial-output cleanup. Android 10+ uses `Movies/Frame`; Android 8–9 uses a system-selected document location.

`RenderEffectRegistry` is the extension point for new Media3/GPU effects. Transitions and animated properties are modeled, but unsupported operations fail explicitly instead of disappearing from preview/export. Text tracks form a title/caption stack above video and overlay tracks; later text tracks render on top. Text-only and audio-only exports use a single canvas input without compositor-only settings.

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

See the [validation record and device screenshots](docs/validation/README.md) for the completed test counts and evidence limits.

## Current boundaries

- Media3 CompositionPlayer is experimental. The single-input preview optimization is limited to non-overlapping, untransformed visual clips without enabled effects. Layered/transformed compositions can still briefly rebuffer at visual boundaries; seamless multitrack playback needs further backend work. Decode accuracy is constrained by source frames, the millisecond player seek API, and device codecs.
- Lower preview detail reduces output texture work; proxy-media generation is not implemented, so heavy source videos can still exceed a device's decoding capacity.
- 4K, HEVC, and high frame rates are offered based on encoder capability. They require separate physical-device verification; advertised capabilities cannot guarantee a device has sufficient memory for every layered project.
- HDR sources are tone-mapped to SDR. HDR output and color-managed mastering are not implemented.
- Effects, transitions, keyframe editing, speed ramps, masks, chroma key, and LUT tools are future work. Unknown render features are rejected explicitly.
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
