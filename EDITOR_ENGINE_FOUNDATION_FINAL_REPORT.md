# Editor engine foundation hardening report

Investigation input: [EDITOR_ENGINE_FOUNDATION_AUDIT.md](EDITOR_ENGINE_FOUNDATION_AUDIT.md). Work was verified against the current implementation before changes. Measurement dates: 28–29 October 2026.

# 1. Executive Summary

The foundation-hardening implementation, runtime conformance, stress measurements and documentation are complete for the available emulator. Correctness and lifecycle gates passed. Timeline frame pacing remains a performance limitation, with worse p95 in the final debug comparison; broad physical-device readiness remains unverified.

The immutable Project/Timeline/Clip document, source-time semantics, command history and shared compiler remain the authority. The major execution change is a retained preview player with a bounded request mailbox and output-buffer acknowledgements. Ordinary modern-backend transport no longer rebuilds the composition/player. Structural graph edits and foreground lifecycle ownership remain explicit reasons for preparation/release.

Future-version project documents are protected against automatic backup rollback and stale autosaves. Source-aware preview admission, leased optimized media, bounded prioritized asset work, progressive waveform windows, caption prewarming, asset reachability, interrupted-publication recovery and measured output cadence now have concrete implementations and permanent tests. These are engineering foundations; they do not add transition/keyframe editing interfaces or a complete professional feature set.

# 2. Baseline

HEAD at start: `6bf570ab0c6bb4abec87321ee61136ef729067f9`. The investigation document was an existing staged addition and was preserved. Baseline JDK17 build, JVM checks and lint passed (39 core + 14 app JVM tests; 0 lint errors / 26 warnings). The audit's 36 instrumentation tests passed in its run; the baseline repetition in this phase exposed the existing intermittent natural-opacity boundary failure.

The audit's 120-request probe measured simple/layered/animated player counts **1/31/31**, with 30 detachments in each layered/animated case. Seek-body p95 was approximately **15.9/17.35/14.69 ms**; maxima were 34/103/114 ms. Small graph compilation took 8–12 ms. These were debug-emulator measurements, with short repeated sources.

For 400 clips, the audit's separate UI-only repeat measured no-assets **19.06/34.09/55.71 ms** p50/p95/max, 2576 ms Main CPU, 22.44 MiB allocation / 3 GCs; assets **18.73/41.35/177.58 ms**, 2358 ms CPU, 24.97 MiB / 5 GCs. Its session-like wrapper committed 119 times for 120 requests. Neither that experiment nor this phase establishes universal physical-device 60 Hz performance.

# 3. Final Architecture

`Project -> RenderPlan -> EvaluatedFrameState -> backend adapters` is the semantic boundary. Output/timeline/local/source time are explicit at evaluation. The compiler creates source-clipped/speed-mapped items, dedicated audio sequences, normalized stills, ordered visual inputs and a primary output clock. Preview source variants are compiler overrides; export still resolves the original document URIs.

`EditorViewModel.document` carries document/selection/history state; `.transport` carries position/play/scrub state. The preview controller owns Main-thread player calls, worker preparation, the latest request mailbox, runtime visual parameter binding and lifecycle epochs. Media3 owns native codecs, hardware buffers, GL resources and output writers. Application-owned caches and workers have explicit budgets; owned files/grants have conservative reachability collection.

# 4. Files Created

- `app/src/androidTest/java/com/bbvideoeditor/app/editor/TimelinePerformanceTest.kt`
- `app/src/androidTest/java/com/bbvideoeditor/app/export/ProcessDeathRecoveryTest.kt`
- `app/src/androidTest/java/com/bbvideoeditor/app/export/PublicationRecoveryTest.kt`
- `app/src/androidTest/java/com/bbvideoeditor/app/media/OwnedAssetStoreTest.kt`
- `app/src/androidTest/java/com/bbvideoeditor/app/media/ProjectRecoveryTest.kt`
- `app/src/androidTest/java/com/bbvideoeditor/app/render/FoundationPerformanceTest.kt`
- `app/src/androidTest/java/com/bbvideoeditor/app/render/PreviewTestSession.kt`
- `app/src/androidTest/java/com/bbvideoeditor/app/render/ResourcePressureTest.kt`
- `app/src/androidTest/java/com/bbvideoeditor/app/render/TemporalConformanceTest.kt`
- `app/src/androidTest/java/com/bbvideoeditor/app/render/TemporalFixtures.kt`
- `app/src/main/java/com/bbvideoeditor/app/export/OutputCadence.kt`
- `app/src/main/java/com/bbvideoeditor/app/export/PublicationJournal.kt`
- `app/src/main/java/com/bbvideoeditor/app/media/OptimizedPreviewMedia.kt`
- `app/src/main/java/com/bbvideoeditor/app/media/OwnedAssetStore.kt`
- `app/src/main/java/com/bbvideoeditor/app/media/PriorityWorkQueue.kt`
- `app/src/main/java/com/bbvideoeditor/app/playback/EngineResourceBudget.kt`
- `app/src/main/java/com/bbvideoeditor/app/playback/PreviewRequests.kt`
- `app/src/main/java/com/bbvideoeditor/app/render/ClipDurationAudioProcessor.kt`
- `app/src/main/java/com/bbvideoeditor/app/render/OutputFramePipeline.kt`
- `app/src/main/java/com/bbvideoeditor/app/render/RuntimeRenderBinding.kt`
- `app/src/test/java/com/bbvideoeditor/app/editor/EditorTransportTest.kt`
- `app/src/test/java/com/bbvideoeditor/app/media/PriorityWorkQueueTest.kt`
- `app/src/test/java/com/bbvideoeditor/app/playback/PreviewRequestsTest.kt`
- `app/src/test/java/com/bbvideoeditor/app/render/ClipDurationAudioProcessorTest.kt`
- `core/src/main/kotlin/com/bbvideoeditor/core/FrameEvaluation.kt`
- `core/src/main/kotlin/com/bbvideoeditor/core/GesturePreview.kt`
- `core/src/main/kotlin/com/bbvideoeditor/core/SnapIndex.kt`
- `core/src/test/kotlin/com/bbvideoeditor/core/FoundationContractsTest.kt`
- `tools/FOUNDATION_BENCHMARKS.md`
- `tools/generate_temporal_fixture.swift`

`EDITOR_ENGINE_FOUNDATION_AUDIT.md` was supplied/staged before implementation and is retained unchanged. This report is also a new requested deliverable.

# 5. Files Modified

- `README.md`
- `app/build.gradle.kts`
- `app/src/androidTest/java/com/bbvideoeditor/app/render/CompositionIntegrationTest.kt`
- `app/src/androidTest/java/com/bbvideoeditor/app/render/EffectRenderingTest.kt`
- `app/src/androidTest/java/com/bbvideoeditor/app/render/MediaFixtures.kt`
- `app/src/androidTest/java/com/bbvideoeditor/app/render/PreviewContinuityTest.kt`
- `app/src/androidTest/java/com/bbvideoeditor/app/render/TextPlaybackTest.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/bbvideoeditor/app/EditorApplication.kt`
- `app/src/main/java/com/bbvideoeditor/app/MainActivity.kt`
- `app/src/main/java/com/bbvideoeditor/app/editor/EditorScreen.kt`
- `app/src/main/java/com/bbvideoeditor/app/editor/EditorViewModel.kt`
- `app/src/main/java/com/bbvideoeditor/app/editor/TimelineView.kt`
- `app/src/main/java/com/bbvideoeditor/app/export/ExportCoordinator.kt`
- `app/src/main/java/com/bbvideoeditor/app/export/ExportModels.kt`
- `app/src/main/java/com/bbvideoeditor/app/export/ExportService.kt`
- `app/src/main/java/com/bbvideoeditor/app/export/Media3RenderEngine.kt`
- `app/src/main/java/com/bbvideoeditor/app/export/MediaPublisher.kt`
- `app/src/main/java/com/bbvideoeditor/app/media/MediaImporter.kt`
- `app/src/main/java/com/bbvideoeditor/app/media/ThumbnailCache.kt`
- `app/src/main/java/com/bbvideoeditor/app/media/ThumbnailProvider.kt`
- `app/src/main/java/com/bbvideoeditor/app/media/WaveformRepository.kt`
- `app/src/main/java/com/bbvideoeditor/app/playback/PreviewEngine.kt`
- `app/src/main/java/com/bbvideoeditor/app/projects/ProjectRepository.kt`
- `app/src/main/java/com/bbvideoeditor/app/projects/WorkspaceViewModel.kt`
- `app/src/main/java/com/bbvideoeditor/app/render/CompositionCompiler.kt`
- `app/src/main/java/com/bbvideoeditor/app/render/RenderEffectRegistry.kt`
- `app/src/main/java/com/bbvideoeditor/app/render/TextRasterizer.kt`
- `app/src/main/java/com/bbvideoeditor/app/render/TimelineTextOverlay.kt`
- `app/src/test/java/com/bbvideoeditor/app/render/RenderCapabilitiesTest.kt`
- `core/src/main/kotlin/com/bbvideoeditor/core/Commands.kt`
- `core/src/main/kotlin/com/bbvideoeditor/core/Project.kt`
- `core/src/main/kotlin/com/bbvideoeditor/core/RenderPlan.kt`
- `docs/validation/README.md`

# 6. Persistence Safety Fix

The former recovery code treated an unsupported newer schema like corrupt JSON and could overwrite it with an older backup. Reads now classify corruption, unsupported schema, missing files and storage failures. JSON/schema inspection occurs before deserialization/migration. Reads do not use AtomicFile's mutating backup-promotion path. The current file, AtomicFile backup, previous save and previous-save AtomicFile backup are protected against unsupported-version replacement, including stale autosave attempts.

Only corrupt/missing current files can recover from a supported valid backup. Unsupported versions and storage failures fail closed. Files are bounded at 64 MiB; recovery diagnostics are bounded. Seven device regressions verify future-version preservation, corruption recovery and associated save behavior. Asset scans also defer collection if a saved document/backup cannot be read safely.

# 7. Temporal Conformance Infrastructure

Device fixtures encode visible frame numbers, source IDs, sentinel/complement rows and a moving marker at 24/30/60 FPS. They do not use the editor compiler to manufacture expected pixels. Tests inspect actual PixelCopy preview surfaces and decoded exported frames. AAC pulse fixtures measure encoded-source onset before timeline mapping; the longer fixture uses sample-exact 48 kHz PCM pulses and mixed-cadence moving video.

Permanent temporal cases cover trim, split, speed, gaps, mixed rates, layered source switches, rapid latest-target seeks and source-timed position animation. The longer case spans 12.5 seconds with early/middle/later checks and two audio clips. Pixel identity tolerance is one source frame; audio onset tolerance is 50 ms. This is a useful regression duration, not hour-long drift certification.

The macOS 4K generator encodes CFR numbered moving frames and independently decodes every output sample to compare the visible barcode to muxed PTS. Frame reordering is disabled for this conformance fixture. An earlier host fixture failed independent Android readback and was discarded; it is not used as backend correctness evidence.

# 8. Preview Request Scheduler

`offer()` changes the UI intent immediately and replaces one pending slot. There is at most one in-flight request plus the newest pending request. Choreographer admits native work after the installed graph's initial receipt and after worker caption preparation. If newer work arrives during preparation, the obsolete request is abandoned. A blocked obsolete in-flight request yields to the latest target on a 100 ms watchdog check instead of occupying the full failure timeout.

The graph revision and executor epoch guard every async completion. Natural playback is capped at the latest accepted composition time when the audio/player clock runs ahead; scrub intent remains immediate. A current request has a 2.5-second acknowledgement timeout and at most two same-player recovery attempts. Recovery uses the public scrubbing-mode exit contract to flush Media3's own pending-seek gate and can visit a distinct sample before returning to the exact target. The controller keeps play intent separate from request replacement. Failure is explicit and does not silently rebuild indefinitely.

# 9. Preview Executor

Modern preview uses one CompositionPlayer and the public experimental FrameProcessor API. The app's pipeline wraps Media3's DefaultGlFrameProcessor and observes final FrameWriter buffer/fence acceptance. Input admission checks composition item IDs, required active layers and video timestamp proximity before rendering. The adapter explicitly carries each packet's scheduled wall-clock display timestamp into the new GL output-buffer frame; the upstream GL consumer did not propagate that metadata, allowing early surface display relative to audio. Incorrect prewarmed inputs are released with their fences and recovered on the same player. The adapter preserves the established SDR RGB convention for BT.601-tagged buffers; HDR metadata is not relabelled.

Media3 is pinned to **1.12.0-alpha02**. Bounded experiments against 1.11.1 did not satisfy backward effect/source and speed-boundary conformance; the newer public packet pipeline plus source checks and controlled stream flags passed the relevant gates. Backend stream/clip flags are initialized once in Application before preview or export, so preview/export initialization order does not alter backend flags. The clipping-in-period experiment did not fix the audio duration defect and was removed. The upgrade adds upstream native effect helpers; there is no application C++/JNI implementation. Source: [official Media3 releases](https://developer.android.com/jetpack/androidx/releases/media3), [CompositionPlayer implementation](https://github.com/androidx/media/blob/release/libraries/transformer/src/main/java/androidx/media3/transformer/CompositionPlayer.java).

# 10. Player / Graph Lifecycle

Ordinary seek, scrub, play/pause, clip/gap traversal and animated transport retain the installed modern executor. Transform/property-only edits update an atomic binding and request redraw/seek without installing another composition. Naming, selection, locking and history timestamps do not rebuild rendered content. Text-only canvas edits retain the native graph when duration is stable.

Source/timing/effect/audio structural edits reinstall a composition and prepare the existing player. An output-size change replaces the writer/player because its format is fixed. Suspension/close releases codecs, frame processors and variant leases; resume installs a fresh executor at retained intent. Closed processor instances are removed from the pipeline's ownership list on structural replacement. API 26/27 retains the older compatibility graph and its explicit boundary recovery; persistent modern behavior is verified on API 37, not on those older devices.

# 11. Seek Architecture

`positionUs` is the newest UI intent. `appliedPositionUs` records issuance of a native seek/redraw API call; it does not assert decoder completion. `submittedPositionUs` is a final writer receipt. Request IDs and graph generations distinguish these stages. UI position stays at intent during buffering rather than snapping backward to an old player clock. After completion, requested play intent is reapplied. An already submitted identical paused position is reused instead of flushing a decoder that has no replacement frame to emit.

# 12. Frame-Presented Semantics

The strong modern signal means **the final writer accepted the composed output buffer and its synchronization fence**. It does not mean the fence has signalled, the display scanned out those pixels, or a human saw them. Tests independently read the real surface with PixelCopy. Legacy API 26/27 reports applied/scheduled metadata and never fabricates this stronger submission receipt. This report deliberately uses “submitted” for the production signal.

# 13. RenderPlan / Evaluated Frame State

`evaluateFrame()` produces active ordered visual layers, typed mapped times, evaluated transforms/opacity, effect references, audio mute/gain state, audio gain sum and derived two-input transition windows/progress. Runtime compositor settings evaluate against the primary output time. A revision/time cache avoids evaluating the same frame for every input. The legacy `frameAt` helper remains compatible.

# 14. Time Domains

Output/timeline/local/source microseconds have distinct wrapper types at the shared evaluator. Local time is clamped within the clip; existing speed, trim and fractional split-rounding mappings remain authoritative. Property keyframes remain source-timed. Media3 seek conversion to milliseconds happens at the native boundary; packet/output evaluation retains microseconds. Source sample cadence is accounted for in acknowledgement/input tolerances, rather than treating a decoder PTS as an exact display receipt.

# 15. Decoder Resource Policy

Admission examines original codec MIME, dimensions, FPS, HDR transfer, declared visual/video inputs, peak active videos and conservative source/output working estimates. Declared codec instance counts are not treated as proof of simultaneous safe decoding. Each video's source charge uses four RGBA-equivalent surfaces/reference allocations before output textures; stills have their own charge. These are conservative policy estimates, not driver telemetry.

Decisions are ORIGINAL, REDUCED_OUTPUT, OPTIMIZED_MEDIA_REQUIRED or UNSUPPORTED, with visible reasons. A layer-limit failure rejects the composition; no required layer disappears. Known initialization failures are bounded and influence subsequent admission. Sources are reassessed after optimization. Lowering output alone never changes the accounting for an original 4K decoder.

# 16. 4K Strategy

| Area | Implemented behavior / evidence |
|---|---|
| Import | Original dimensions, codec and stream metadata persist; verified 3840×2160 H.264 fixture |
| Source decode | Original-resolution retriever/thumbnail decode verified; no claim of reduced source buffers from a small output |
| Preview | Device budget can choose optimized source plus lower output; original document remains unchanged |
| Multilayer | Every layer retained; optimized sources reassessed before installation; excessive inputs rejected explicitly |
| Proxy | Internal cancellable, atomic, source-time-preserving visual variants with leases and bounded disk LRU |
| Export | Original URIs; actual dimensions/cadence inspected; this emulator fell back from 2160 request to 1080p |

No physical device was connected at the last device enumeration. HEVC/4K60/HDR results must be labelled separately from the executed H.26430 workload.

# 17. Proxy Architecture

One cancellable generation mutex owns source normalization. Keys include URI, size, duration, dimensions, rotation, FPS, transfer metadata, local mtime and profile version. Video normalizes the full original source timeline to approximately 720p AVC without applying edits or audio; stills are sampled with EXIF orientation. Actual encoded dimensions/rotation/duration are probed before atomic promotion. Active files are pinned; unpinned variants are trimmed to the central 2 GiB disk budget. Partial files are removed on cancellation and stale partials are cleaned later.

Clip IDs, edit ranges, source-time animation, audio originals and export originals are untouched. Long sources incur real preparation time; variants are not instantaneous. A provider that changes content behind an unchanged URI without updated observable metadata can evade the identity key; a provider version/hash policy remains future work.

# 18. GPU / Compositor Architecture

Media3's DefaultGlFrameProcessor imports hardware buffers, runs clip effects, composites ordered inputs and writes output using upstream JNI/EGL/fence helpers. The output adapter preserves the public scheduled display-time metadata through the buffer handoff to SurfaceHolderFrameWriter. Evaluated compositor settings come from current primary output time, so held secondary samples do not freeze transform/opacity animation at their old timestamp. The app owns no alternate renderer document or copied shader engine. Preview supplies RGB-compatible SDR frames; exports with animated properties use the same packet compositor, while static export retains the verified Transformer path.

# 19. Preview / Export Semantic Parity

Both compile the same validated edit model, effects, source mappings, geometry and audio sequences. Preview may substitute visual variants and render editor text in Compose from the shared rasterizer. Export always uses original source URIs and encoded overlays. Speed export selects the single-visual optimization where valid to preserve actual source-frame mapping; output cadence is measured separately. Existing effects/opacity/text/geometry and new moving-frame/audio tests protect adapter parity. Export normalizes each complete post-speed PCM clip to its document sample count, streaming silence for short output and consuming excess tails; this prevents cumulative drift at split and later speed-change boundaries without adding decoder lanes. Preview uses absolute stream timestamps and can terminate partial streams during seeks, so it deliberately excludes complete-item padding.

An experiment forcing the new virtual-FPS aggregator/all-export packet path stalled static effects export and regressed an opacity seek. It was removed. Requested frame rate is therefore not misrepresented as guaranteed CFR for every speed/source combination; the produced file's cadence is reported.

# 20. Timeline Performance Work

Transport no longer publishes a new document revision. Visible track indexes are reused by the actual clip-list semantics, clip labels are remembered, and snapping uses a gesture-local endpoint index. Gesture previews validate the base once and apply local move/trim/transform checks, with full validation retained at final history commit. Waveform detail and thumbnail cells use stable quantization. These reduce repeated command/index/allocation work; native and emulator GPU costs remain separately measured.

# 21. Compose State / Recomposition Work

EditorScreen/session subscribe to document state. Transport controls, split availability, timeline and the fast canvas overlay subscribe where needed. The native preview surface observes only player/buffering/error/effective-quality state. Playback ticks do not invalidate the full editor surface. The benchmark measures zero document-root commits through its 120 transport updates; the audit's analogous wrapper counted 119. This is a scope-specific measurement, not a claim that every timeline child has zero recompositions.

# 22. Snap / Index Work

SnapIndex snapshots endpoints once per gesture, uses binary-nearest searches, includes zero and the dynamic playhead, excludes the dragged clip and preserves tie behavior. A deterministic randomized JVM regression compares 4,000 queries with the original snap semantics. Track range lookup supports current/near caption prewarming. Gesture history still commits once, cancels without a commit and retains lock/collision/type rules.

# 23. Thumbnail Scheduling

A bounded deduplicated queue gives visible work priority over nearby/speculative/background work and drops displaced queued work. Caller cancellation removes unstarted work; an already blocking platform extraction retains its worker until it actually returns. Per-request cancellation does not terminate shared queue workers, and running thumbnail checks use the worker context rather than a captured, cancelled UI context. Native concurrency is one on low-memory/older devices and at most two otherwise. The UI and cache agree on 250 ms time buckets. Hashing, source opening, disk access and decode run off Main; the timeline never waits for a cold 4K extraction.

# 24. Waveform Scheduling

Visible source windows emit partial peak data during decoding, then cache bounded detail. Five-second chunks allow fresh visible work to run ahead of whole-source background analysis. One app-owned worker/decoder handles audio analysis; no full PCM file is retained. The UI adapts time-window and power-of-two bucket granularity to zoom, instead of restarting a new native producer for every pixel. Byte LRU and persistent window caches bound memory/disk data. Unsupported/absent audio produces empty data, never a fabricated waveform.

# 25. Text Raster Scheduling

The compiler prewarms current and near captions on workers before installation/seeks. Natural playback updates a conflated lookahead window; overlay transitions also prefetch upcoming content. Raster misses run through a bounded single raster worker rather than rasterizing on GL. The correctness fallback can wait on that worker, so cold fallback stalls remain possible; this is not a claim of zero GL waiting under all cache pressure. Active overlay/UI bitmap references survive LRU eviction and memory trimming without unsafe recycling.

# 26. Memory / Resource Budgets

The central budget derives thumbnail/text/waveform bytes, thumbnail worker count, conservative decoder working allowance and visual input limits from heap/memory class, low-RAM and 32-bit status. It also owns the proxy disk allowance. OnTrimMemory and onLowMemory drop cache ownership without recycling live consumer bitmaps. Thumbnail/waveform queues, raster queue, proxy generation and preview layer admission are independently bounded. Exact total GPU/decoder/driver allocation is not observable from these limits; Java LRU sizes are never called total engine memory.

# 27. Asset Ownership

Saved projects and all recovery backups, active editor/history sessions and durable export snapshots protect imported files and persisted grants. Live leases exclude collection, including assets introduced by later undo/redo edits. Collection only visits the app-owned imported-media directory and old unreferenced persisted permissions, uses a 30-day grace period and defers on unknown/corrupt/future/unreadable project/export state. Export/render leases protect originals while jobs consume immutable snapshots. Relink preserves media/clip identity and edits, validates kind/duration/audio compatibility and keeps undo references reachable; there is no relink UI in this phase.

# 28. Error / Capability Model

Persistence failures have typed causes. Render preflight rejects unsupported enabled effects, invalid/unknown parameters, unsupported animation bindings, multi-input effect stages without executors and transitions without a renderer. Disabled unknown serialized effects remain preserved. Built-in brightness/contrast/saturation IDs and schemas remain compatible. Custom registration declares defaults/ranges, capabilities, input count, geometry stage and texture cost; duplicate/built-in ID overrides fail. Visual factories returning audio processors are rejected explicitly because audio has a separate executor; they cannot silently lose their audio effect.

An export post-speed PCM processor enforces each complete clip duration to the nearest output sample, consumes excess decoder/stretch tails and emits bounded silence chunks for short output; the longer pulse test moved from roughly +88 ms at the later clip to approximately +8–12 ms. Audio >6-channel input is rejected before native execution; existing gains remain supported with headroom warnings. Preview admission and decoder failures are explicit. Export reports actual displayed dimensions including rotation, measured cadence and encoder fallback; observed dimension fallback removes unsupported advertised combinations across restarts, keyed by the pinned backend policy and device fingerprint. Observation loading/writing runs on workers; a backend/OS change invalidates old observations.

# 29. Lifecycle / Cancellation

Project revisions cancel preparation and reject stale completions. Close/suspend cancel request, seek-preparation, clock and acknowledgement jobs and release native ownership; resume recompiles at retained intent. Partial optimized files and temporary exports clean up in finally blocks. Native blocking thumbnail work is owned by its queue rather than pretending caller cancellation ended it. The combined scrolling/pressure run exposed worker-pool exhaustion: a cancelled operation could terminate a shared worker, and thumbnail work captured an obsolete caller's cancellation context. Operations now check their executing worker context, and only cancellation of that worker terminates its loop. A permanent JVM regression verifies subsequent visible work still executes. Existing Activity recreation/play-intent and service cancellation regressions remain; repeated suspend/resume and edit-storm tests inspect real source pixels and active player counts.

Publication reserves a unique owned MediaStore name before insert, then journals the URI atomically. Recovery queries pending rows including pending visibility, verifies exact name and owner, removes incomplete rows and preserves completed publications. Interrupted private renders/requests are removed and the export becomes an explicit failed/retry state. API 26 SAF cannot provide an atomic external-file publish transaction.

# 30. Diagnostics / Telemetry

A bounded 256-event in-process ring records revision/request/time stages, source mismatch, submission, admission and timeout events without media URI/name logging. Counters distinguish public player create/release, installs/prepares, native seek/redraw, parameter updates, submitted frames, buffering, coalescing and recoveries. Native decoder initialization/release totals come from actual DecoderCounters (CompositionPlayer does not forward every decoder-initialized analytics callback); retained counter identities and latency samples are bounded. Timing reports separate synchronous offer cost, native-call cost, install cost and final real-pixel convergence. No remote analytics service was added.

# 31. Performance Infrastructure

[tools/FOUNDATION_BENCHMARKS.md](tools/FOUNDATION_BENCHMARKS.md) documents reproducible commands and interpretation. The non-debuggable, shell-profileable, development-signed profile variant uses the same code with minification disabled. Opt-in device harnesses measure 120-seek source-pixel convergence, lifecycle/edit storms, one/two-layer 4K and a 16-row timeline matrix (10/25/50/100 clips per track × assets off/on × native off/on).

Timeline collection records Window FrameMetrics, slow-frame counts, listener loss, Main CPU, ART allocation/GC, PSS, document commits and native lifecycle counters. Its 120 paced Compose injections include test-idleness overhead. Window GPU_DURATION excludes separate native SurfaceView work. The harness is an equivalent repeatable measurement tool, not a physical-device Macrobenchmark certification.

# 32. Before vs After Measurements

All measurements below are on the same ARM64 API37 emulator, with no concurrent Gradle build during final measurement. Debug and non-debuggable profile numbers are separated. The timeline harness uses 1.5-second clips at two-second intervals; the audit used one-second clips. Cache state, fixture content and test scheduling also differ. These are representative comparisons, not a controlled universal speedup claim. Tables use the final post-cancellation-fix runs, including their slower results.

**120 requests, 25 clips per track.** Simple uses one visual track; layered/animated use video, overlay, audio and text tracks (100 clips total). Targets advance at 80 ms intervals, with 16 ms host pacing, then an explicit active numbered-frame target at 8.85 seconds. All final source barcodes are asserted. Startup includes Activity/harness setup.

| Mode | Before players / detachments | After players / releases / installs / prepares | After offer p50 / p95 / max ms | After native-call p95 / max ms | Final pixels ms | Native video codec init / release |
|---|---|---|---|---|---:|---|
| simple | 1 / 0 | 1 / 0 / 1 / 1 | 0.11 / 0.50 / 1.96 | 20.17 / 31.14 | 383 | 14 / 14 |
| layered | 31 / 30 | 1 / 0 / 1 / 1 | 0.11 / 0.87 / 3.84 | 18.02 / 18.55 | 291 | 26 / 25 |
| animated | 31 / 30 | 1 / 0 / 1 / 1 | 0.09 / 0.46 / 1.39 | 10.86 / 11.18 | 266 | 25 / 24 |

The audit measured old seek-body p95 at approximately 15.9/17.35/14.69 ms and max 34/103/114 ms. Its timings included native work inside the seek entry point; after measurements separate the cheap offer from actual admitted native calls. The distributions therefore have different populations. Native calls still exceed a 16.67 ms budget, and native codec/item/prewarm churn remains despite retaining one executor. Before installs/prepares were not separately counted; source trace calls prepare on rebuild, whereas after counts are instrumented. Final first-output times for simple/layered/animated were **2022/3270/2203 ms**.

**400-clip timeline comparison, debug, native preview off.**

| Assets | Audit p50 / p95 / max ms | Final p50 / p95 / max ms | Main CPU before → after ms | Allocation before → after MiB | GC before → after | Final frames / >33.33 ms |
|---|---|---|---|---|---|---|
| off | 19.06 / 34.09 / 55.71 | 20.48 / 35.18 / 45.18 | 2576 → 2383.25 | 22.44 → 20.44 | 3 → 2 | 118 / 9 |
| on | 18.73 / 41.35 / 177.58 | 33.93 / 67.63 / 103.08 | 2358 → 2349.87 | 24.97 → 26.84 | 5 → 2 | 184 / 108 |

Document-root commits changed from the audit's 119 to **0** for the final transport gesture. No-assets CPU/allocation fell, but p95 rose slightly; assets-on CPU was nearly unchanged, allocation rose and median/p95 worsened. Both maximum frame times fell in this repeat. Additional asynchronous asset frames make row frame counts differ. Final no-assets/assets-on window GPU means were **13.50/22.99 ms**. These results do not establish a frame-pacing improvement. The native-on rows below show substantial remaining allocation/GC and frame cost; a retained player does not eliminate it.

**Final 400-clip debug rows with native preview.**

| Assets | Frames | p50 / p95 / max ms | Main CPU ms | Allocated MiB / GC | Native seeks | Video codec init / release | Player / install / prepare |
|---|---:|---|---:|---|---:|---|---|
| false | 173 | 34.76 / 74.31 / 133.71 | 4652.76 | 243.76 / 20 | 93 | 43 / 42 | 1 / 1 / 1 |
| true | 232 | 52.62 / 101.02 / 167.48 | 5135.23 | 256.30 / 16 | 104 | 47 / 46 | 1 / 1 / 1 |

**Non-debuggable profile target, full 16-case matrix.** This is shell-profileable, unminified and development-signed, not a signed production release. Every row had zero listener drops and zero document-root commits. Every native row retained one player/install/prepare with no release. All measured frames exceeded the 16.67 ms window deadline; these results do not demonstrate universal 60 Hz rendering.

| Clips/track | Assets | Native | Frames | p50 / p95 / max ms | >33.33 ms | Main CPU ms | Allocated MiB / GC | PSS KiB |
|---:|---|---|---:|---|---:|---:|---|---:|
| 10 | false | false | 119 | 17.46 / 27.69 / 58.70 | 4 | 1120.89 | 25.83 / 1 | 84420 |
| 10 | false | true | 173 | 34.21 / 68.26 / 168.17 | 100 | 2399.57 | 223.14 / 23 | 101576 |
| 10 | true | false | 167 | 27.92 / 50.81 / 67.11 | 61 | 1017.58 | 24.69 / 2 | 100990 |
| 10 | true | true | 233 | 42.56 / 79.70 / 125.04 | 161 | 1925.29 | 229.00 / 24 | 105224 |
| 25 | false | false | 119 | 17.38 / 28.48 / 49.86 | 5 | 631.59 | 19.66 / 3 | 99570 |
| 25 | false | true | 170 | 34.49 / 73.27 / 106.44 | 101 | 1479.81 | 231.91 / 21 | 105418 |
| 25 | true | false | 168 | 33.93 / 51.33 / 68.54 | 101 | 830.15 | 23.38 / 1 | 113938 |
| 25 | true | true | 227 | 50.43 / 85.15 / 117.72 | 163 | 1805.29 | 232.39 / 17 | 111988 |
| 50 | false | false | 118 | 17.42 / 34.31 / 39.31 | 16 | 547.77 | 19.44 / 2 | 107964 |
| 50 | false | true | 173 | 34.95 / 67.78 / 100.35 | 103 | 1325.01 | 197.62 / 12 | 120579 |
| 50 | true | false | 167 | 34.37 / 53.51 / 181.35 | 120 | 814.12 | 23.69 / 1 | 116608 |
| 50 | true | true | 228 | 50.49 / 84.41 / 125.79 | 173 | 1784.58 | 249.12 / 15 | 121632 |
| 100 | false | false | 118 | 17.34 / 19.35 / 34.61 | 3 | 602.60 | 20.44 / 1 | 110736 |
| 100 | false | true | 174 | 39.44 / 68.20 / 209.62 | 117 | 1525.02 | 225.64 / 16 | 120016 |
| 100 | true | false | 163 | 34.38 / 53.53 / 70.79 | 132 | 645.49 | 24.28 / 1 | 120432 |
| 100 | true | true | 222 | 50.55 / 84.49 / 140.85 | 173 | 1874.96 | 273.85 / 19 | 119416 |

The final profile matrix passed in **140.234 seconds**. The earlier development debug matrix also executed all 16 cases; final debug measurement focused on the four 400-clip combinations. Profile/debug differences prevent treating their CPU delta as an isolated code-change improvement. Window GPU time excludes native SurfaceView processing.

**Lifecycle/edit storm:** 120 parameter/seek offers plus eight timing-revision offers and six suspend/resume cycles ended at correct current pixels. Counts: **7 player creates / 6 releases, 7 installs/prepares, 7 video codec initializations / 6 releases**, one active player, two parameter updates and one redraw; PSS **237,884 KiB**. Obsolete structural revisions coalesced before installation in this run. Preparations correspond to installed structural changes/resumes, not ordinary transport.

# 33. 4K Measurements

Executed on ARM64 Android 17/API37 16 KiB-page emulator `sdk_gphone16k_arm64`, debug build, three-second 3840×2160 H.26430 numbered source, independently read back.

| Case | First submitted output | Final pixels after 60 offers | PSS | Lifecycle | Policy |
|---|---:|---:|---:|---|---|
| One 4K source, cold variant | 18,761 ms | 474 ms | 236,816 KiB | 1 player / 1 prepare / 0 release | optimized visual sources, FAST output; all layers retained |
| Two simultaneous 4K layers, warm variant | 2,740 ms | 723 ms | 236,258 KiB | 1 player / 1 prepare / 0 release | optimized visual sources, FAST output; all layers retained |

The one-layer estimate was 147,456,000 bytes before substitution; two layers 283,852,800. Estimates describe requested originals, not measured native allocations. Cold 192×192 original-source thumbnail took **883 ms**, compared with audit **3003 ms** for its different one-second flat-red fixture; source/fixture differences prevent a controlled speedup claim. A one-second 3840×2160/30 export request produced **1920×1080**, 30 frames, measured **30.00002 FPS**, 33,333–33,334 us PTS deltas and 540,103 bytes. Original source URIs were used. The observed fallback is excluded from advertised supported settings, including after a fresh process. Physical device: not connected/tested at final enumeration. HEVC/4K60/HDR: not verified in this phase.

# 34. Multilayer Measurements

Final measured 120-request and timeline rows are in section 32. The 4K two-layer case independently decodes both visible source barcodes at distinct source times; a missing overlay cannot pass by acknowledging the base alone. Actual decoder counters distinguish Media3 item/prewarm codec churn from public executor recreation. Layer admission retains all required inputs, uses optimized sources where necessary and rejects excessive layer counts explicitly. Sustained four-layer 4K and device-specific codec resource exhaustion are outside the executed workload.

# 35. All Tests Executed

| Gate | Command / scope | Result |
|---|---|---|
| Baseline | JDK17 core/app JVM, debug/test/release assembly, lint | 39 + 14 JVM; build pass; lint 0 errors / 26 warnings |
| Final JVM | `./gradlew :core:test :app:testDebugUnitTest` | **44 core + 25 app pass**, including PCM duration boundaries, transport/history, queue cancellation, snap/time/effects |
| Final full device regression | Full AndroidJUnitRunner with `foundationPerformance=true`, timeline count 100 | **60 reported: 58 passed, 2 process-death assumptions skipped; zero failures**; includes all 36 existing tests and all four performance methods |
| Continuity/integration/temporal correction | Focused repeat after display-time forwarding and export-only PCM normalization | 11 pass / 67.757 s; subsequently included in passing final full run; four seek/play cycles on one player; unchanged tolerances |
| Final resource pressure | 48 retained thumbnails, 101 caption rasters, trim + low-memory callbacks after timeline/preview/4K work | PASS; live consumer bitmaps remain valid, bounded caches/queues; PSS 227,562 → 314,266 KiB; queue ends with 0 queued / 0 running |
| Final build/lint variants | Serialized debug, unsigned minified release, profile and profile test assembly plus JVM/lint | **PASS / 3m31s**, lint **0 errors / 35 warnings**; debug test APK also built and exercised |
| Final measured repeat | TimelinePerformanceTest + FoundationPerformanceTest + ResourcePressureTest, timeline count 100 | **5 methods pass / 127.963 s**, no assumption skips; three seek modes, one/two-layer 4K, lifecycle storm, four 400-clip rows and cache pressure |
| Final profile matrix | Non-debuggable profile target, TimelinePerformanceTest | **1 method / all 16 cases pass / 140.234 s**; debug development also exercised all 16 cases |
| Actual process death | Arm real foreground render, external app-only force-stop, fresh verification | **PASS / 1 verification test / 0.118 s**; saved project intact, interrupted export failed/retryable, partial render/request cleanup verified |

The final full run's host ADB stream disconnected before completion. Device TestRunner records independently confirm **60 tests, zero failures**, with exactly two process-death assumption skips. The runner continued through resource pressure, long temporal conformance and all text/export tests. Final measurements were then captured to files on the device before retrieval, avoiding reliance on the host stream. These files and build logs remain outside the change set. The process-death arm/verify protocol was separately executed, not counted as a passed assumption skip.

The final pressure repeat intentionally retained all 48 consumer bitmaps. Cache trimming relinquished cache ownership and did not recycle those live images; process PSS includes these retained consumers. The queue completed 54 operations and had no stranded work. An earlier run under concurrent heavy builds was terminated by LMK; graphics/driver limits remain qualified in section 36.

Development failures drove concrete corrections: future-schema recovery, source/speed/effect boundaries, frame/fence ownership, pending-publication recovery, SDR colour compatibility, lost paused replacement frames, cumulative audio tails, natural-play clock crossing and early opacity display. Scheduled display metadata is now carried through the GL output buffer; complete-item PCM normalization is export-only because preview streams can terminate at seeks. The subsequent combined scrolling/pressure failure identified cancelled shared workers and obsolete caller-context capture; both were corrected and the full regression passed. Rejected experiments are absent from final source. No existing assertion tolerance was widened. Native-codec checks distinguish bounded live codecs from legitimate total source-switch initialization; they do not equate codec initialization with player recreation.

# 36. Remaining Limitations

The highest remaining engineering risks are native/asset frame-pacing spikes, driver-memory behavior under pressure and reliance on an experimental backend. No data-loss or frame-correctness regression remains failing in the executed matrix; that evidence is limited to the coverage below.

The new packet APIs and pinned Media3 build are experimental; broader physical/API/driver coverage is required before a general production certification. API26/27 compatibility has weaker acknowledgement and retains boundary recovery. Surface acceptance is not scanout. Native player calls still execute on Main and can miss a frame budget; a cheap seek offer does not eliminate those native costs. Emulator Window GPU time is substantial and does not establish a physical rendering ceiling.

A run competing with concurrent heavy Gradle/R8 work produced timing misses and was killed by Android's low-memory killer, which reported about 1.5 GiB DMA-buffer PSS. The final isolated full run completed with zero failures; the post-workload pressure repeat also passed. This does not establish robustness under arbitrary graphics/host pressure, and Debug.getPss on this emulator reports Graphics as zero. Treat driver memory pressure as an unverified limit, not a solved hard bound. The conformance workload is bounded, not hours-long/VFR/HDR/HEVC/thermal certification. Long source normalization has real latency/storage cost. Caption cache misses can wait on the raster worker. Admission estimates cannot hard-cap unknown driver memory. SAF publication lacks transactionality. External providers need a stronger content-version signal for in-place mutation. Export cadence is inspected, but requested CFR is not guaranteed for every source/speed/backend path. Sections 32/35 separate successful correctness/lifecycle gates from the remaining frame-pacing and coverage limits.

# 37. What Is Now Easy To Extend

Typed effect registration and preflight provide one place to declare and reject capabilities. Evaluated frame state and time wrappers make new source-time properties possible without changing the edit document. Explicit geometry-stage slots distinguish decode orientation, source selection/crop, fit, clip effects, transform, composition and encoding; current geometry is preserved. Derived transitions already expose two evaluated inputs and progress, ready for a real executor plus an edit/handle policy. Relink and ownership now support a future repair interface. Proxy overrides and backend packet receipts permit future device-specific backends behind the same plan boundary.

# 38. Features Still Not Implemented

No transition renderer/UI, keyframe editing UI, arbitrary animated effect parameters, crop/pan/zoom UI, masks, LUT management, chroma key, adjustment layers, audio fades/automation/dynamic-range limiter/meters, reverse/speed ramps, automatic captions/SRT, recording, multiselect/advanced track management, relink UI, export queue/resume, cloud collaboration or release signing workflow was added. Geometry/transition registration data is not advertised as a working feature.

# 39. Future Work

Validate the same conformance/performance suite on physical devices, API26/27 and varied codec/GPU families. Add VFR/B-frame/HDR/HEVC/4K60 and substantially longer A/V fixtures with explicit source baselines. Investigate remaining native seek Main-thread tails using the now-separated timing/counter evidence and evaluate a dedicated execution-thread surface adapter if physical traces require it. Mature the experimental upstream packet API before broad rollout. Add provider content-version tracking, optional resumable normalization, stronger device budget calibration and feature-specific executors only with their own source/pixel/audio contracts.
