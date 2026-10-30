# 1. Executive Summary

The original admission-order defect is fixed in the implementation: an advertised codec-capability rejection no longer becomes a terminal preview failure before optimized media is considered. `PreviewSourcePreparer` connects declared evidence, verified cache lookup, bounded native decoder output, source-specific normalization, resource reassessment and retained-player installation.

Implementation and the available automated validation are complete: 160 JVM executions, the full emulator suite (69 passed, eight intentional opt-in skips), 25 final-APK native regressions, and six separately enabled performance methods passed. Debug, AndroidTest, profile and minified release builds passed; debug/release lint has zero errors. Physical-device and original-incident acceptance remain untested because only an emulator is connected and the original failing source/phone are unavailable. This report does not claim universal HEVC support.

Recovery resumed on 30 October 2026 from the surviving implementation and saved conversation, at HEAD `0411e25`. The previous session had already implemented most of this change; no architecture restart or repeat audit was performed. Its recorded baseline was 81 JVM checks and eight relevant instrumentation checks passing. That baseline is recovered historical evidence, separate from the final results below.

# 2. Original Failure

An imported 3840×2160 `video/hevc` source produced “A source format (3840×2160, video/hevc) is outside reported decoder capabilities.” This described an advertised platform predicate, not observed failure to decode the file.

# 3. Root Cause

`PreviewResourcePolicy.assess()` converted negative or failed capability queries to `UNSUPPORTED`. `PreviewEngine.compileProject()` immediately threw before `OptimizedPreviewMedia.resolve()`. Existing or newly generated optimized video could therefore never rescue this branch.

# 4. Final Compatibility Architecture

```text
import original (MediaImporter; original Project remains authoritative)
  → actual extracted format (AndroidMediaCompatibilityBackend)
  → declared capability evidence (MediaCompatibilityService)
  → PreviewSourcePreparer
      → default reported safe + resource safe: original
      → otherwise verified optimized cache: use prepared video
      → otherwise bounded MediaCodec output verification
          → suitable default hardware route + safe resource cost: original
          → alternate/software/expensive/HDR route: normalize, verify, substitute
          → retryable access/resource/deadline failure: typed retry
          → proven absent/protected/malformed or corroborated decode failure: terminal
  → reassess selected graph resources
  → existing CompositionCompiler / retained PreviewEngine
```

Mandatory comparison, using the implemented classes:

```text
BEFORE:
PreviewResourcePolicy capability query negative
→ UNSUPPORTED
→ PreviewEngine require/throw
→ OptimizedPreviewMedia skipped
→ technical error

AFTER:
AndroidMediaCompatibilityBackend / MediaCompatibilityService capability evidence
→ PreviewSourcePreparer direct-safe?
  YES → original preview
  NO / UNKNOWN → valid optimized source available?
    YES → verified OptimizedPreviewMedia source
    NO → bounded actual compatibility/recovery
      → direct route if safe
      or → optimized generation with Transformer decoder fallback
      or → evidence-backed terminal unsupported
```

# 5. Files Created

- `app/src/main/java/com/bbvideoeditor/app/media/MediaCompatibility.kt`: evidence, failure, preparation and lease/store contracts.
- `app/src/main/java/com/bbvideoeditor/app/media/AndroidMediaCompatibilityBackend.kt`: actual formats, candidate evidence and native output probe.
- `app/src/main/java/com/bbvideoeditor/app/media/MediaCompatibilityService.kt`: bounded queue, observed evidence cache and failure isolation.
- `app/src/main/java/com/bbvideoeditor/app/media/PreviewSourcePreparer.kt`: finite source routing and reassessment.
- `app/src/test/java/com/bbvideoeditor/app/media/MediaCompatibilityTest.kt`.
- `core/src/test/kotlin/com/bbvideoeditor/core/MediaMetadataCompatibilityTest.kt`.
- `app/src/androidTest/java/com/bbvideoeditor/app/render/MediaCompatibilityInstrumentedTest.kt`.
- This final report. The existing `MEDIA_COMPATIBILITY_DECODER_AUDIT.md` is preserved.

# 6. Files Modified

`EditorApplication.kt`, `MainActivity.kt`, `Media3RenderEngine.kt`, `MediaImporter.kt`, `OptimizedPreviewMedia.kt`, `EngineResourceBudget.kt`, `PreviewEngine.kt`, `PreviewRequests.kt`, `core/src/main/kotlin/com/bbvideoeditor/core/Project.kt`, and the instrumentation helper `PreviewTestSession.kt`.

The retained executor, render plan, timeline model, request mailbox and shared composition compiler architecture are preserved. `MediaReference` gains optional metadata; Project/Timeline/Clip are not rewritten. No dependency, reflection, native codec backend or FFmpeg was introduced.

# 7. Source Format Model

`SourceFormatDescriptor` wraps stable `MediaReference` metadata, source version, CSD/HDR digest and nullable encryption/HDR observations. Encoded width/height remain distinct from rotated display dimensions. Track MIME, rotation, duration, size, frame rate/provenance, profile/level and color metadata come from extraction. Unknown transfer remains the existing unspecified value `0`; unknown FPS remains `0` with `UNKNOWN` provenance. Unknown profile, level, bit depth, color standard and range remain nullable. Main10 alone does not fabricate a bit depth.

Only stable metadata is persisted. Full `MediaFormat`, including decoder initialization data, is re-extracted when configuring a probe. Project schema remains 2, with backward-compatible defaults and existing future-schema safeguards.

# 8. Capability Evidence Model

`DeclaredSupport` distinguishes `SUPPORTED`, `UNSUPPORTED`, `UNKNOWN`, `QUERY_ERROR` and `NO_CANDIDATE`. Candidate evidence retains size support, rate support, full-format support, public hardware/software/vendor flags, profile-level lists and safe exception classes. Exceptions and missing FPS/capabilities do not become universal unsupported verdicts.

`DecoderObservation` separately records actual output, configuration/decode failure, no output, timeout, resource or access failure. Functional decoding is distinct from realtime suitability.

# 9. Decoder Candidate Selection

The backend enumerates regular, non-encoder MIME candidates. It uses public pinned Media3 format conversion and decoder sorting to identify the preferred preview route, and puts that route first when identifiable. Android-exposed software candidates remain eligible. API 29 classification flags are guarded; older systems conservatively normalize when direct-route confidence is weaker.

At most three candidates are probed per verification pass. Deterministically failed pages can advance on a later explicit attempt. A page with transient evidence restarts rather than falsely exhausting unverified candidates. Transformer selection uses a bounded candidate list with an observed decoder preferred where available; Media3 still applies its supported full-format sorting.

# 10. Actual Decoder Probe

`AndroidMediaCompatibilityBackend.probe()` uses the real selected `MediaExtractor` track and CSD, `MediaCodec.createByCodecName`, and a two-image PRIVATE `ImageReader` output surface. Success requires actual acquired image dimensions and a decoded output timestamp, rather than configure/start alone.

Bounds: one queued probe worker, 24 pending jobs, three candidates per pass, five-second coroutine deadline per candidate, four-second output-loop deadline, at most 96 input samples and an 8 MiB feeding threshold. An individual extractor sample still uses the codec's finite input buffer. A single-decoder working-set guard precedes configuration. Work runs on IO, checks cancellation between platform calls, and releases codec, reader and extractor through nested `finally` blocks.

Public native configure/start/release or provider calls cannot be forcibly interrupted safely if a vendor blocks inside them. Cancellation/deadlines are cooperative across such calls; the worker remains occupied until native work returns. This is a platform limitation, not a hard wall-clock kill guarantee.

# 11. Preview Admission Changes

`PreviewResourcePolicy` now owns graph and memory budgets rather than definitive codec judgments. `PreviewSourcePreparer` owns per-source routing before the selected graph is admitted. Only actual graph/peak hard limits can reject immediately; a negative format predicate alone cannot.

Original reported-safe input stays cheap. Negative, unknown, forced, previously failed or resource-expensive input reaches cache/probe/preparation. The player releases its codecs before cache verification, probing or normalization acquires another decoder; parameter-only edits retain their existing fast path.

# 12. Optimized-Media Routing

`OptimizedMediaStore.resolve(project, requiredMediaIds, preferredDecoders, onProgress)` prepares only the requested visual sources. The normalization project contains the entire unedited source, without original audio, trim, speed, effects or keyframes baked in. Prepared visuals are passed to existing compiler overrides.

Normalization explicitly requests the compiler's existing safe single-visual-input path. A real test exposed a black initial proxy frame when this operation used a transparent-clock composition; the single-source route fixes that observed defect. The regression checks both the decoded prepared first frame and visible preview pixels. Cache/policy identity is version 4 so earlier generated variants are not reused under new guarantees.

# 13. Cached Proxy Recovery

`cached()` validates the derived file, metadata, timestamps and actual decoder output before leasing it. A verified cache hit can rescue even a source with no currently reported original decoder, without an original probe or regeneration. Source access is still required to establish a trustworthy original fingerprint. Corrupt cache files are never leased as valid and are replaced during regeneration.

# 14. Transformer Decoder Fallback

Pinned Media3 **1.12.0-alpha02** APIs are used directly:

```kotlin
DefaultDecoderFactory.Builder(context)
    .setEnableDecoderFallback(true)
    .setMediaCodecSelector(selector)
    .setListener { name, failures -> /* bounded diagnostics */ }
    .build()
DefaultAssetLoaderFactory(context, decoder, Clock.DEFAULT, null)
Transformer.Builder(context).setAssetLoaderFactory(loader)
```

This applies to normalization and original-source export. Existing `DefaultEncoderFactory.Builder.setEnableFallback(true)` remains separate. A device test supplies a nonexistent first video decoder followed by a real platform candidate, and requires successful encoded output plus recorded initialization-failure evidence.

Signatures/default behavior were checked against locally available pinned dependency source; the [public decoder-factory API](https://developer.android.com/reference/androidx/media3/transformer/DefaultDecoderFactory.Builder) also documents the supported injection controls.

# 15. CompositionPlayer Limitation

No unsupported decoder selector, reflection or renderer-factory setter was added to `CompositionPlayer`. Alternate-only observed success leads to normalization. A successful functional probe is insufficient to promise smooth direct software decoding.

# 16. Proxy Metadata Verification

Output is re-read using `MediaImporter`, not labeled from requested encoder settings. Actual MIME, encoded size, rotation, audio presence, duration and color metadata populate the variant. Up to 128 sample timestamps establish initial offset, distinct sorted presentation timestamps and measured cadence. Duration must remain within two source-frame periods or 100 ms, whichever is larger; display aspect ratio must remain within 2%. These are explicit verification contracts, not widened existing test assertions.

Actual decode output is required before atomic promotion. Bad temporary files are removed. No full-file pixel scan or VFR/HDR equivalence certification is claimed.

# 17. Source-Time / Audio Preservation

Source-range edits and keyframes continue using original source time. The compiler uses optimized media only for visual sequences; audio sequences continue referencing originals. Export compiles the original project without preview overrides. Instrumentation checks original audio URIs, export URI isolation, trim/speed duration and untouched original project ranges.

# 18. Resource Accounting

Static visual graph inputs, peak simultaneously active videos and retained source-buffer costs are distinct. Sequential clip count is not decoder concurrency. Per-track maximum source cost conservatively represents retained sequence pressure; overlapping sources and output textures remain charged. Low-resource policy tests normalize necessary inputs without dropping layers. Hard graph/peak limits remain intact.

# 19. Typed Failure Model

`PreviewFailure` carries kind, source ID, user message, technical reason, retry action, candidate IDs and underlying cause. Categories include access, malformed/protected media, no decoder, decoder init/decode, probe/preparation timeout, resource/graph, encoder/storage/output verification and ambiguous graph failure. `PreviewState.error` is a derived UI string, not a second error authority.

Native attribution first uses item/format identity and then matches the selected original/proxy format. Ambiguous errors retain candidate IDs and do not poison every source. Probe failure and normalization failure must corroborate deterministic decoding failure before the all-decoders-failed terminal route is selected.

# 20. Retry Behavior

If verified prepared media still exceeds the fixed graph-memory budget, the state requests smaller sources/fewer layers and offers no identical retry. Explicit retry invalidates source-specific observed/failure evidence and performs bounded verification or preparation. Failure of a prepared source during native preview records a regeneration request: after lease release, `OptimizedPreviewMedia.invalidate()` deletes that derived file and a later retry regenerates it. It cannot silently select the same failed proxy. Files still leased by another preview are protected and produce a resource retry state.

An attributable original decoder-init failure has one automatic recovery transition per graph/source preparation cycle. Terminal failures do not loop through Play, Resume or Retry. A changed source/graph clears an older terminal UI even when the engine is suspended, allowing the changed document to be assessed on resume. Candidate-query and transient failures are not permanently cached as unsupported.

# 21. User-Facing UX

Preparation displays the relevant source and real stage; progress percentages appear only when Transformer supplies them. Failures use friendly source-specific messages. “Try again” appears only for a typed retry; technical evidence is available through Details. No URI is emitted into general compatibility events.

# 22. Cancellation / Lifecycle

New revisions cancel old preparation. Suspension cancels compilation and releases player, pipeline and optimized leases. The probe queue uses caller-owned cancellable operations; service shutdown cancels active work and prevents a racing queued operation from registering after close. Partial normalization output is deleted in non-cancellable IO cleanup. Stale completion cannot install a prior graph generation. Native error callbacks also require the installed revision to match the current generation, so an older player cannot attribute an error to a newer graph being prepared.

Thread ownership remains: compatibility/extraction/probe on IO, graph compilation on existing workers, Transformer/player native operations on Main as required, UI state on Main. No nested `runBlocking` was added to production.

# 23. Compatibility Cache / Invalidation

The SHA-256 key includes source identity/URI internally, version/size/mtime where available, MIME, encoded dimensions/rotation, FPS/provenance, profile/level/bit depth/color, duration, bounded CSD/HDR digest and encryption observations. Device/build/SDK/manufacturer/model, pinned Media3 and policy version are included through the backend key. Raw URIs are not used as log identities.

Observed successes have a five-minute lifetime with at most 64 entries. Source-specific native failure hints expire after one minute with at most 128 entries. Observation/queue state is bounded; explicit retry invalidates relevant evidence. Some content providers cannot expose reliable modification times, so same-size in-place changes with unchanged extracted configuration are a remaining source-version limitation.

# 24. Export Behavior

Final export retains original URIs and enables supported decoder fallback independently of preview variants. Errors preserve native causes and typed source/candidate information where attribution is possible. Decoder fallback covers candidate initialization; it is not an arbitrary midstream alternate-decoder recovery backend. A genuinely unreadable original can still fail export truthfully. Preview-proxy quality is never silently used for final export.

# 25. Test Coverage Added

Twenty compatibility JVM tests cover direct, reported-negative actual success, alternate success, query errors, cache rescue, deterministic failure, source isolation, low resources, overlapping graph limits, transient pages, timeout/cancellation/concurrency, metadata identity, cache expiry and failed-proxy regeneration. Two core tests cover older metadata JSON and metadata round trips. Nine compatibility instrumentation methods exercise actual codec output, fallback, prepared pixel output, cached/corrupt variants, source audio/export mapping, cancellation, terminal retry behavior, twelve sources and the explicitly enabled four-format matrix.

# 26. Reported-NO / Actual-Success Test

The JVM regression requires `ORIGINAL_OBSERVED` after a negative declared predicate and actual default hardware output. The instrumented regression injects negative capability evidence while retaining real extraction/decoding, forces the safe optimized route, checks the prepared first-frame color and visible preview, and verifies twenty transport offers add no compatibility queries/probes or player installs.

# 27. Alternate Decoder / Proxy Test

JVM injection makes the primary candidate fail and the alternate succeed, requiring a decoder preference and only the difficult source's variant. Real Transformer injection verifies candidate initialization fallback, not merely an enabled flag. Forced negative/alternate routing additionally exercises real normalization and visible output.

# 28. All-Decoders-Fail Test

The deterministic JVM case requires all supplied candidates plus normalization decoding to fail before a non-retryable source-specific result. The device terminal-state test proves no player creation and no repeat inspection from Play/Resume/Retry. A genuinely undecodable physical HEVC fixture was not supplied; this is controlled failure-path evidence.

# 29. Cached Proxy Test

The real test generates a variant, compares its metadata against independent extraction, uses it despite injected absence of original candidates, corrupts its own derived file, requires rejection/regeneration, and explicitly invalidates/regenerates it again. Lease counts return to baseline and original files are preserved.

# 30. 12+ Sequential Clips Test

Twelve distinct file identities on one video track include one injected difficult source. JVM and device tests require only that source to be normalized, peak active videos = 1, original project identities unchanged, a single native player, and correct final visible pixels after seeking into clip twelve. Both the full suite and final APK compatibility run passed this case; no twelve-decoder requirement or project URI mutation occurred.

# 31. Simultaneous Layer Test

Injected budgets test overlapping high-resolution inputs and retain both layers after normalization. Separate hard-limit coverage rejects excess static inputs before probe/generation. The final native temporal suite and the existing 4K one/two-layer performance test passed. Two simultaneous layers retained both numbered source-time identities, with peak active videos = 2 and one retained player.

# 32. Existing 4K AVC Regression

The four-format matrix passed the independently host-verified numbered 3840×2160 AVC fixture. `FoundationPerformanceTest.original4kAndLayeredOptimizedPreviewPreserveNumberedSourceFrames` also passed one/two visual layers, numbered pixel convergence and thumbnail work. A one-second 3840×2160/30 export request used the original and truthfully reported the device encoder fallback: 1920×1080 AVC, 30 frames, measured 30.00002 FPS, 540,103 bytes. The observed fallback is excluded from advertised supported settings; no proxy URI was used for export.

# 33. HEVC Results

The final API 37 run passed all four controlled formats. Each row required actual decoder output, visible source/frame identity at 850 ms, and the same numbered identity in decoded original-source export. All four exports produced 1280×720 AVC, 45 frames and approximately 30 FPS for the requested 1.5-second/720p workload.

| Source | Advertised overall | Actual default probe | Selected route | Proxy generated | Preview / export pixels | Time to verified preview |
| --- | --- | --- | --- | --- | --- | --- |
| AVC 1920×1080/30 | Supported | OUTPUT | Original | No | PASS / PASS | 5.036 s |
| AVC 3840×2160/30 | Supported | OUTPUT | Verified optimized video | Yes | PASS / PASS | 17.981 s |
| HEVC 1920×1080/30 | Supported | OUTPUT | Original | No | PASS / PASS | 4.491 s |
| HEVC 3840×2160/30 | Supported | OUTPUT | Verified optimized video | Yes | PASS / PASS | 16.857 s |

These times include Activity launch/preparation and a seek to the checked 850 ms frame; they are upper bounds on time to playable preview, not isolated decoder/startup benchmarks. Earlier full-suite measurements were 2.888 / 19.520 / 2.830 / 13.770 s respectively, illustrating run-to-run emulator variation.

The working defaults were `c2.goldfish.h264.decoder` and `c2.goldfish.hevc.decoder`. For 4K HEVC, `c2.android.hevc.decoder` and `OMX.google.hevc.decoder` reported unsupported while the Goldfish candidate reported support and produced output. The mandatory overall reported-NO regression is therefore supplied by controlled evidence injection with real platform decoding, not mislabeled as the emulator's native overall HEVC verdict.

Both 4K rows emitted actual generation/success/substitution events. All rows retained one native player and one composition install. Emulator support for these low-complexity numbered fixtures does not establish phone support, sustained playback, high-bitrate camera-file behavior, or HDR correctness.

# 34. Main10/HDR Results

Metadata/fingerprint tests distinguish Main/Main10, unspecified bit depth and HDR10/HLG transfer values. HDR/static information routes conservatively toward normalization. **No real Main10/HDR/HLG or 4K60 fixture was validated in this run.** No pixel/color or throughput claim is made for those cases.

# 35. Physical Device Results

**PHYSICAL DEVICE NOT TESTED.** Only `emulator-5554`, `sdk_gphone16k_arm64`, API 37 is connected. No device result is fabricated from its advertised codecs.

# 36. Original Incident Reproduction

The original phone and exact failing HEVC source are unavailable. The causal negative-query branch is covered permanently using injected evidence with real decoder/proxy output; the four-format matrix uses controlled files. Acceptance on the affected phone remains outstanding.

# 37. Performance Regression Check

All six opted-in performance methods passed against the final APK, without skips, in 159.653 seconds:

- 120 paced seek offers in each of simple, layered and animated projects; correct final pixels; one player, zero releases, one install/prepare; bounded native decoder churn.
- Original 4K and two-layer optimized preview, source-time pixels, thumbnail work and original-source export.
- Repeated structural/parameter/lifecycle storms converging to the newest graph.
- Four-track timeline smoke with 10 clips per track, assets enabled and native preview enabled.
- Continuous scrub/fling/zoom/drag/trim/playback-follow checks with 100 clips per track.
- Thirty-second gesture/edit/playback soak with the same 100-clip-per-track workload.

| Preview workload | Seek-offer p95 | Final-pixel convergence |
| --- | --- | --- |
| Simple | 1.208 ms | 419 ms |
| Layered | 0.775 ms | 335 ms |
| Animated | 1.064 ms | 438 ms |

The 4K first-output measurements were 15.890 s for a cold one-layer preparation and 3.352 s for the following cached two-layer case; final seeks converged in 561 / 732 ms. The timeline smoke retained one player/install/prepare, zero document-root commits and zero preview remeasure/layout/attach/detach events. Its window-frame p95 was 67.807 ms; this is **not** evidence of universal 60 FPS rendering. Synchronous native seek tails also remain (up to 131 ms in the simple fixture). These existing platform/backend timing limits are measured separately from seek-offer latency.

This is a functional/performance smoke regression, not a controlled latency speedup claim. Measurements are debug-emulator/build/cache-specific and do not establish sustained physical-device thermal behavior. The dedicated compatibility transport test additionally asserts no new inspections/probes during twenty ordinary seeks.

# 38. Memory / Codec / Lease Cleanup

Real probe tests require zero live codec/reader/extractor counters. Normalization cancellation checks partial-file removal and lease baseline; sequential and cache tests check lease release. Idempotent combined leases release previously prepared sources if a later source fails. Existing memory-pressure and lifecycle tests are included. The final changes retain implementation, meaningful tests and requested reports; raw logs/host fixtures remain outside the repository.

# 39. Build / Lint / Test Results

Final source build/JVM/lint gates passed (7m 7s for the final combined invocation). Verification used JDK 17, offline dependencies, two Gradle workers, Kotlin compilation in-process and a 2 GiB Gradle heap to limit host RAM pressure. Existing test tolerances were not changed.

| Gate | Final result |
| --- | --- |
| `:core:test` | 46 tests, zero failures/errors |
| `:app:testDebugUnitTest` | 57 tests, zero failures/errors |
| `:app:testReleaseUnitTest` | 57 tests, zero failures/errors |
| Debug / AndroidTest assembly | PASS |
| Profile assembly | PASS |
| Release assembly with R8 minification | PASS; unsigned artifact, no release signing claim |
| `:app:lintDebug` | Zero errors; 36 warnings |
| `:app:lintRelease` | Zero errors; 32 warnings |
| Full instrumentation / four-format matrix | 77 discovered: 69 passed, 8 intentional opt-in skips; zero failures; 381.094 s |
| Final revision-guard native regression | 25 passed, zero failures/skips; 226.285 s |
| Performance smoke | Six methods passed, zero failures/skips; 159.653 s |

The full run preceded the final native-error revision-ownership guard. The final APK then passed all 25 relevant compatibility/temporal/lifecycle/resource methods without skips. The eight opt-in skips comprise six performance methods (subsequently run separately and all passed) and the two host-controlled process-death protocol methods (not rerun in this phase). The 160 JVM executions include the same app suite in two variants. Lint suggestions/deprecations remain; no lint baseline or suppression was added to hide errors. The earlier indentation error was fixed in source.

Reproducible build gate:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew \
  :core:test :app:testDebugUnitTest :app:testReleaseUnitTest \
  :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleProfile :app:assembleRelease \
  :app:lintDebug :app:lintRelease --offline --no-daemon --max-workers=2 \
  -Pkotlin.compiler.execution.strategy=in-process -Dorg.gradle.parallel=false \
  '-Dorg.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8' --console=plain
```

For instrumentation, install the generated debug target/test APKs and use the existing SDK runner with `-e compatibilityMatrix true`. Matrix fixtures must be explicitly supplied in the target app cache under the four names in sections 32–33; missing fixtures fail the opted-in test. `tools/generate_temporal_fixture.swift` generates and independently decodes the numbered host fixtures. No user library media is needed.

# 40. Remaining Limitations

- No physical-phone or original-source incident reproduction; no real Main10/HDR/HLG/4K60 validation.
- A probe proves bounded functional output, not realtime throughput or correctness throughout a whole source.
- Native platform/provider calls can delay cooperative cancellation; no unsafe native-thread termination is used.
- CompositionPlayer has no supported app decoder-selector injection in the pinned version. Alternate-only viability uses normalization.
- The static composition must prepare required future media before installation. A difficult future source can delay graph availability; the timeline/project remains editable and a usable recovery path replaces early policy rejection.
- Candidate probes and Transformer candidates are bounded. More candidates can require an explicit retry; normalization cannot supply a codec absent from Android.
- Provider source versions can be incomplete; output validation samples bounded timestamps/decoded output rather than all frames. Broad arbitrary VFR/rotation/HDR coverage remains a device/media validation gap.
- Existing alpha Media3/backend and unrelated editor production limitations remain outside this focused compatibility correction.

# 41. Production Readiness Assessment

The demonstrated negative-capability/admission defect is corrected and end-to-end recovery is implemented, including a first-frame normalization defect discovered during validation. All requested available build/test/lint gates passed, with no observed implementation blocker in this scope. Physical-phone/original-incident acceptance, API 26/27 runtime, Main10/HDR/HLG/4K60 and varied high-bitrate/VFR camera media remain validation gaps before broad production certification. Original-source export and all required layers remain authoritative. This change must not be described as support for every 4K HEVC video.
