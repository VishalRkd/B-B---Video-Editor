# Timeline and interactive preview performance report

**Engineering work and available verification complete (2026-10-29/30). Smooth-interaction signoff remains blocked by the measured tails and physical-device coverage gap.**

Investigation inputs: [foundation audit](EDITOR_ENGINE_FOUNDATION_AUDIT.md) and [foundation hardening report](EDITOR_ENGINE_FOUNDATION_FINAL_REPORT.md). Baseline and code were reverified before optimization.

# 1. Executive Summary

The scoped profile → attribute → fix → measure work is complete on the available emulator. The retained engine, document/history, layered rendering, original assets and export contracts are preserved. Timeline asset/layout invalidation, preview admission/cadence, paused clock polling, caption lookups and batched/final-coordinate input were improved without lowering the original gesture workload or correctness tolerances.

The primary identical-workload 400-clip profile case with assets/native ON changed **51.30/90.71 → 44.35/89.95 ms p50/p95**, **81 → 41 native seeks**, **228.21 → 162.00 MiB managed allocation**, **15 → 11 GCs**, and **1673.33 → 1358.90 ms Main CPU**. Assets/native-OFF p95 fell **54.59 → 37.65 ms**. Native player ownership stayed **1 create / 0 releases / 1 install / 1 prepare**. These are meaningful controlled-work reductions, but the worst native-on p95 barely improved, its maximum worsened, cold rows regressed and a later soak exposed an 878 ms/GC tail. The target professional smoothness is **not achieved or signed off**.

All 81 JVM checks passed. Full enabled profile regression reported 68 tests: 66 passed and two separate process-death protocol assumptions skipped, zero failures. The final harness-only corrections were separately verified unprofiled and traced. Debug/profile/minified unsigned release/test builds and lint passed. No physical phone was connected. The largest remaining observed costs are required Main framework/backend calls, Media3 allocation/decoder work and emulator/HWUI/driver/scheduling; evidence and limits are below. Hold broad feature expansion pending representative physical traces and resolution of reproduced severe tails.

# 2. Original Problem

The foundation retained one player but native-on scrubbing still allocated hundreds of MiB and suffered long frame tails. Historical debug 400-clip p50/p95 values were 20.48/35.18 ms (no assets/native off), 33.93/67.63 (assets/native off), 34.76/74.31 (no assets/native on), and 52.62/101.02 (assets/native on). Those historical values are separated from the clean rerun below.

# 3. Baseline Reproduction

Started at clean HEAD `f2f3ad4` (foundation work committed). Before production optimization: 44 core + 25 app JVM checks passed; debug/profile/test APKs built; the full enabled debug suite reported 60 tests with zero failures (two process-death assumptions skipped), 281.106 s. The untouched 16-case non-debuggable profile matrix passed in 139.724 s. Fixtures, clip counts and the original 120-event gesture schedule are preserved.

# 4. Performance Methodology

ARM64 Android 17/API37, 16 KiB-page `sdk_gphone16k_arm64`, explicitly selected as emulator-5554. JDK17, SDK37.0, pinned Media3 1.12.0-alpha02. The profile target is non-debuggable, shell-profileable, unminified and development-signed. Native tests and performance runs were serialized with heavy builds. Original matrix: four tracks, 10/25/50/100 clips per track, 1.5-second clips every two seconds, repeated real encoded video/audio and captions. Each variant runs assets off/on and native off/on. Test-idleness overhead is included. Different build variants and profiled/unprofiled timings are not pooled.

# 5. Perfetto / Trace Findings

Captured the four assets/native combinations before and after with Perfetto ftrace (gfx/view/input/dalvik, scheduler), plus real-clock gesture traces covering scrub, fling, zoom, drag, trim, follow and soak. Stable app slices/counters are guarded by Trace.isEnabled. The final four-case trace contains all four TimelineGesture spans (25.826 s combined), 86 NativeSeek slices and no paused PreviewClock polling. The final 79.29 MB real-clock trace covers all seven interaction paths and both soaks, with one verified fling start/end; Main totals include postAndWait 12405.90 ms, 190 NativeSeek calls / 1500.41 ms, timeline composition 328.31 ms, viewport pan 251.66 ms and waveform geometry 103.93 ms. Playback has 101 PreviewClock slices / 17.82 ms; paused matrix clock slices are absent. It has no recorded overrun/lost-data health flags. Stable trace names contain no project/media names or per-event formatted strings.

Perfetto CPU counters produced no usable userspace stacks on this emulator; a requested 1 ms sampled Java method trace supplied diagnostic call stacks instead. Method tracing changes runtime/JIT behavior, so its times are attribution evidence only. Separate ART allocation captures used 64 KiB sampling and 32 MiB shared memory. The initial 8 MiB capture overran and a long combined trace lost incremental heap state; those are excluded from complete allocation claims. The corrected before/after allocation traces retained callsite data with no heap-overrun/intern-loss flag. Linux perf sample skipping (3 before / 1092 after) makes CPU stack attribution unavailable; it does not invalidate the separate heapprofd allocation stream.

Raw artifacts are outside Git: baseline profile/full runner logs, before/after `.pftrace`, the method `.trace` and final runner logs under `/tmp/timeline-perf-*`. The repeatable guide records configs/commands. Trace-on measurements are kept separate from the unprofiled matrix.

# 6. Main Thread Breakdown

Ranked attribution is more specific than a generic Compose diagnosis. Untouched four-case Main trace: traversal 3904.9 ms, draw 3575.8, postAndWait 2503.2, recomposition 1697.2, measure/layout 1310.4, recorded View draw 1015.3 and text measurement 466.8. These spans nest. Espresso/Compose idle waiting was separately 15339.2 ms; it is not app CPU.

Final four-case trace (31.949 s runner, including setup):

| Main scope | Calls | Inclusive wall ms | Max ms |
| --- | ---: | ---: | ---: |
| traversal | 503 | 2401.08 | 238.29 |
| postAndWait | 498 | 1531.23 | 232.21 |
| Recomposer:recompose | 959 | 1442.12 | 28.67 |
| NativeSeek | 86 | 712.13 | 45.53 |
| AndroidOwner measure/layout | 485 | 497.48 | 60.13 |
| TimelineComposition | 480 | 109.38 | 14.30 |
| TrackComposition | 72 | 49.99 | 7.11 |
| ClipComposition | 255 | 31.63 | 1.96 |
| VisibleRangeQuery | 1431 | 11.88 | 2.25 |
| ThumbnailDraw | 80 | 13.23 | 10.58 |
| WaveformGeometry | 12 | 1.88 | 0.31 |
| TextCanvasComposition | 14 | 2.15 | 0.39 |

The before and after traces have different frame/call counts and startup composition costs; their absolute totals are not normalized causal percentages. Same-workload unprofiled CPU/allocation is in sections 25–26.

For individual long tails, a 43.52 ms NativeSeek contained 3.89 ms guest Running and 39.50 ms runnable/preempted time; a 45.53 ms call contained 4.82 ms Running, 37.27 ms runnable/preempted and 3.44 ms sleeping. A 101.64 ms ViewportPan contained a **97.85 ms Choreographer#scheduleVsyncLocked** child. Required framework vsync scheduling and guest/host scheduling are therefore material; pure pan math is not assumed to consume those entire spans. Guest Running time can itself include host-vCPU descheduling. Before method stacks also identified MediaMetricsListener analytics, Timeline/Period comparisons and player getter/listener work. Removing paused clock reads and reducing native admissions addresses controllable frequency without violating Player Main ownership.

# 7. Allocation Breakdown

Matched **separate traced** 400-clip assets/native-on runs used the same fixture/filter/config; ART samples cover process startup, gesture and teardown. The before/after runner durations were 18.424/13.471 s. Sampled allocation is approximate and affected by tracing; no unprofiled frame-speedup claim is derived from these runs.

| Sampled site/accounting | Before bytes | After bytes |
| --- | ---: | ---: |
| All ART allocation accounting | 563051926 | 261316600 |
| NativeAllocationRegistry.registerNativeAllocation, inclusive | 282984448 | 98402304 |
| HardwareBuffer constructor, inclusive | 282263552 | 98336768 |
| DefaultExtractorInput constructor, inclusive | 33429228 | 23597088 |
| Timeline.equals, inclusive | 40637632 | 16449536 |
| AbstractConcatenatedTimeline.getPeriod, inclusive | 40178880 | 16056320 |

Each callsite/frame name is deduplicated within its stack; inclusive rows overlap and cannot be added. HardwareBuffer allocations flow through ImageWriter.WriterSurfaceImage.getHardwareBuffer → SurfaceHolderFrameWriter → the output pipeline/GL consumer. This is cumulative native registration accounting in the ART stream; it is not retained Java pixels or driver peak RAM. DefaultExtractorInput repeatedly allocates byte arrays during progressive source initialization; Timeline/Period/Window/UID construction repeats during Media3 timeline/getter/analytics work. Those managed sites shrink with reduced native work and paused getter polling. Extractor factory/source preparation remains a large residual category (about 59 MB inclusive under Loader in the final capture).

The traced runner's managed allocation counter fell 275.73 → 147.26 MiB and GCs 18 → 11; its native seeks fell 95 → 42. The separate primary unprofiled matrix reports 228.21 → 162.00 MiB and 81 → 41 seeks. Small app lists, viewport values, ruler labels and diagnostic events are not claimed to be zero; they did not dominate sampled allocation at this resolution. No unsafe shared mutable scratch objects, bitmap recycling or arbitrary VFR frame buckets were introduced.

# 8. Compose Findings

The old transport path recomposed viewport-dependent clip/asset subtrees and changed clipped dimensions. The final approach commits one small viewport snapshot in the frame composition phase and defers translation/drawing in stable child content. Regression tests verify unchanged clip/track composition during pan and drawing-only thumbnail completion. A fully deferred asynchronous viewport projection was rejected: it doubled measured UI frames and worsened pacing despite fewer compositions. Zero recompositions alone was not accepted as success.

# 9. Timeline Viewport Changes

Project/history remain authoritative. Timeline transport updates publish one viewport snapshot through rememberUpdatedState during frame composition. Stable clips translate through graphics layers; long clips use bounded three-screen windows that change at coarse viewport buckets. No enormous pixel scroll document or duplicate editable model was introduced. Zoom continues to preserve the focal timestamp through existing TimelineViewport math.

# 10. Native Preview Request Changes

The existing one-in-flight/one-pending mailbox remains. Interactive state is explicit from EditorViewModel through MainActivity and both performance harnesses. Expensive native admission runs from a display callback via a Main handler after UI traversal; Player calls retain their required Main ownership. Pointer-up bypasses adaptive delay and clears the backend debounce gate through an exact final request.

# 11. Native Seek Reduction

In the primary matched 400-clip profile rows, native seeks fell **74 → 37** without assets and **81 → 41** with assets. Observed native decoder initializations fell **28 → 11** and **37 → 11** respectively; public player/install/prepare ownership stayed 1/0/1/1. Coalescing and conservative equivalent-content suppression account for omitted native work; no pointer count was reduced. The already accepted same-output-time parameter-redraw path is retained. Different video milliseconds remain conservatively distinct because average FPS cannot establish VFR decoded-sample identity. Stills/gaps can reuse pixels while held; release always restores exact native target time.

# 12. Preview Cadence Policy

The UI consumes every pointer update. Native admission uses actual display refresh, source/output cadence, measured synchronous native cost and a replaceable latest target. It leaves display headroom between expensive calls. Playback polling now uses display frame callbacks at the slower of display/output cadence, with no paused polling; correctness remains gated by the real native clock and accepted output timestamp.

# 13. Fast-Scrub Behavior

Fast motion means more than one useful source-frame interval traversed per display interval, measured from target/time changes. It coalesces intermediate decoder work more aggressively. No project edits, layer omissions, source-quality change or input-rate reduction occurs. Pinned Media3 public scrubbing mode temporarily disables unnecessary audio/metadata processing while paused; its default seek tolerance remains exact. Leaving it explicitly flushes the final target without restoring captured native play intent.

# 14. Thumbnail Rendering Changes

One Canvas per clip draws stable ImageBitmap states; tile completion is read in drawing rather than causing strip/clip recomposition. Tile extraction size, 250 ms source buckets, visible/near priorities and crop/filter quality are preserved. Tiles are culled in viewport buckets with adjacent prefetch. Existing worker/cache bounds remain.

# 15. Waveform Rendering Changes

Progressive waveform requests use stable source/detail buckets. drawWithCache builds bounded path geometry for the clip window, trim/speed and zoom, and pan translates the containing layer. Three-dp sampling preserves the prior display density without rebuilding every line on every pan. Unknown/undecoded samples remain blank. Source mapping uses Clip.sourceTimeAt, including trim/speed rounding. The maximum geometry window is three screens rather than the duration of an hours-long source.

# 16. Asset Update / Invalidation Changes

Thumbnail state is observed only by its Canvas; a permanent test waits for a real magenta bitmap and verifies both its pixels and unchanged clip/strip composition counts. Waveform state invalidates its geometry/drawing cache. Assets remain visible; no asset mode was removed or changed to a warm-only workload.

# 17. Ruler / Clip Drawing Changes

Ruler labels use a bounded 128-entry cache keyed by time/step. Visible indexed clip queries and stable descriptions remain. Normal clip dimensions stay fixed while moving; exceptionally long clips use bounded windows. Labels/handles and semantics remain attached to the actual clip. Selected bounds and actual user input are verified in existing gesture tests.

# 18. Snap / Drag / Trim Changes

SnapIndex remains gesture-local and is not rebuilt by transport. Gesture-base previews and one-step commit/cancel history are preserved. Drag/trim run through the existing stable parent input owner, locks, collision/type checks and full final validation. No new document/command architecture was added. The final coordinate delivered only in ACTION_UP is applied before history commit and preview catch-up; a permanent UI test exercises that delivery path. Snap and full commit validation were not weakened. A new batched-input regression first failed on the old implementation with release velocity 0; velocity tracking now consumes the platform history through Compose's public `addPointerInputChange`, rather than losing intermediate samples when frames run late. Final verification is recorded below.

# 19. Allocation / GC Improvements

Primary matched 400-clip managed allocation/GC: no-assets/native-off 19.91 → 18.56 MiB (2 → 2 GCs); assets/native-off 26.25 → 17.00 MiB (2 → 1); no-assets/native-on 233.83 → 158.23 MiB (13 → 12); assets/native-on 228.21 → 162.00 MiB (15 → 11). This is a reduction, not elimination of native-on GC. The separately sampled matched allocation experiment is detailed in section 7; cumulative HardwareBuffer registrations are not retained Java heap or GPU peak memory. The long edit soak's allocation increased and remains explicitly reported.

# 20. Main Thread Improvements

Primary matched 400-clip Main CPU totals fell 664.61 → 476.55 ms (no assets/native off), 673.64 → 478.86 ms (assets/native off), 1567.31 → 1138.13 ms (no assets/native on) and 1673.33 → 1358.90 ms (assets/native on). CPU totals include test idling/injection overhead. The slowest wall-time frame can include host scheduling, framework vsync, native callbacks and renderer waits even when viewport math is cheap.

Internal 60 Hz review budget (engineering goals, not asserted emulator guarantees):

| Stage | Intended budget / policy | Observed evidence |
| --- | --- | --- |
| Pointer math/state publication | Prefer <1–2 ms | Real input stage means are reported; a traced 101.64 ms pan included 97.85 ms inside Choreographer scheduleVsyncLocked |
| Viewport/clip composition | A few ms; stable children skip | 480 viewport compositions total 109.38 ms; 72 track and 255 clip compositions include setup |
| Indexed visibility | Small fraction of frame | 1431 queries total 11.88 ms in the four-case trace |
| Asset geometry/draw | A few ms; geometry reused on pan | Waveform geometry 1.88 ms / 12 builds; thumbnail draw 13.23 ms / 80 calls in that trace |
| Preview offer | Sub-millisecond preferred | Foundation p95 0.091–0.411 ms; occasional longer wall-time outliers remain |
| Native operation | Replaceable latest target, at most one admitted per display interval, with display headroom | 400-clip native-call p95 16.24 / 25.47 ms; public Main calls can exceed the whole frame budget |
| HWUI/driver | Must leave room within 16.67 ms | Primary 400-clip window GPU mean 15.94–28.11 ms; target not achieved |

Nested trace spans are not additive. Guest Running time can include host-vCPU descheduling; it is not proof of application CPU executing continuously on physical hardware.

# 21. GPU / RenderThread Findings

The final four-case RenderThread trace ranks graphics work highly: Drawing 5927.2 ms (498 calls, maximum 308.8), flush commands 3427.9 (max 208.6), eglSwapBuffersWithDamageKHR 2230.6 (max 99.6), dequeueBuffer 972.3 (max 50.8), and waitForBufferRelease 962.7 (49 calls). They nest and are not additive. Initial EGL context creation took 451.8 ms in that diagnostic capture. This supports substantial emulator/HWUI/driver cost and cold initialization effects; it does not uniquely explain every outlier in another run.

Primary 400-clip window GPU means were 15.94/21.02/17.70/28.11 ms for off/off, off/on, on/off, on/on. GPU_DURATION excludes separate video SurfaceView/native GL work, and PSS is process-wide shared accounting. The real-clock run also reports command/swap means often around 15–20 ms while timeline layout means are far smaller. No physical GPU shader profile, scanout latency or hard driver-memory ceiling was established. API26/27 was unavailable for runtime validation; shared Compose changes apply there while its weaker native acknowledgement/recovery is unchanged.

# 22. SurfaceView Findings

The permanent matrix subclasses PlayerView only for counters. During every native gesture row, measured deltas were **0 measure, 0 layout, 0 attach and 0 detach**. A retained public Player was therefore accompanied by stable measured View ownership in this workload. This does not measure every SurfaceFlinger/driver operation or certify external displays. Ordinary pan never installs/prepares a graph; selection, asset completion and parameter-only edits retain their existing classification.

# 23. Player Lifecycle

The existing persistent executor is preserved. Stable graph transport must retain one player, zero releases and one composition install/prepare. Native item/decoder initializations are tracked separately. Structural edits/lifecycle releases retain their existing legitimate preparation paths. Resource policy and optimized-source probing remain in graph compilation; seeks use installed sources and leases. `RuntimeRenderBinding.evaluateFrame` retains its existing snapshot-identity/time memo, shared across compositor inputs; no alternate evaluator or unbounded frame cache was added. The existing 256-event diagnostics and 256 native-duration ring stay bounded; sampled evidence does not rank their allocation above Media3 extraction/timeline or buffer accounting, so telemetry was retained.

# 24. Correctness Verification

New permanent tests cover cadence at 60/90/120 Hz, final bypass, lifecycle reset, conservative sample reuse, animated-property exclusions, scrub lifecycle edges, bounded long-clip windows, asset invalidation/pixels, exact layered numbered source pixels after 120 Hz interactive requests and pause/play intent after final flush. Existing tolerance values remain unchanged. A held-gesture emulator screenshot was inspected for real thumbnail/layer/caption/ruler/playhead composition; this supplements the actual pixel/input assertions and does not establish human physical-touch latency or scanout. Final enabled profile regression: 68 reported, 66 passed and two paired process-death protocol checks skipped, zero failures, 406.295 s. All four foundation performance methods and both real-time interaction methods executed. The new batched-history regression failed before the fix (velocity 0) and passed afterwards; the targeted debug run passed all five checks. Existing correctness tolerances remained unchanged.

# 25. Final 16-Case Matrix

Final **non-debuggable profile** matrix, 114.798 s; all 16 combinations executed, zero failures. Four tracks, 10/25/50/100 clips per track and the original 120-move sinusoidal schedule are unchanged. Each native row includes one extra final flush intent (120 total observed requests in the 400-clip rows), not an extra synthetic pointer move. Zero callback drops and zero document-root commits in all rows. The existing matrix excludes FIRST_DRAW_FRAME/startup and nonpositive-duration records; every remaining delivered gesture frame is included. The separate real-time harness includes all delivered frames.

| Clips | Assets / native | Before p50 / p95 ms | Final frames | Final p50 / p95 / max ms | >16.67 / >33.33 ms |
| --- | --- | --- | --- | --- | --- |
| 40 | off / off | 17.43 / 34.09 | 118 | 41.07 / 95.92 / 115.62 | 118 / 78 |
| 40 | off / on | 38.10 / 83.75 | 123 | 71.19 / 170.64 / 335.73 | 123 / 108 |
| 40 | on / off | 28.16 / 50.85 | 118 | 17.49 / 34.08 / 143.70 | 118 / 21 |
| 40 | on / on | 50.90 / 84.87 | 124 | 27.29 / 55.24 / 101.65 | 124 / 58 |
| 100 | off / off | 17.33 / 18.38 | 118 | 17.51 / 34.40 / 35.51 | 118 / 27 |
| 100 | off / on | 39.77 / 73.69 | 123 | 34.49 / 56.25 / 80.93 | 123 / 82 |
| 100 | on / off | 35.97 / 68.00 | 118 | 17.48 / 34.06 / 35.30 | 118 / 19 |
| 100 | on / on | 50.68 / 94.82 | 123 | 33.95 / 62.56 / 100.30 | 123 / 66 |
| 200 | off / off | 17.67 / 41.46 | 118 | 17.75 / 34.68 / 51.18 | 118 / 36 |
| 200 | off / on | 37.63 / 69.16 | 122 | 33.86 / 67.15 / 89.72 | 122 / 65 |
| 200 | on / off | 33.74 / 51.23 | 118 | 28.90 / 35.64 / 50.61 | 118 / 55 |
| 200 | on / on | 51.08 / 96.14 | 125 | 34.97 / 68.10 / 95.52 | 125 / 70 |
| 400 | off / off | 18.03 / 37.67 | 118 | 17.47 / 34.52 / 50.59 | 118 / 25 |
| 400 | off / on | 41.79 / 72.92 | 123 | 34.91 / 67.68 / 122.36 | 123 / 69 |
| 400 | on / off | 33.84 / 54.59 | 118 | 33.90 / 37.65 / 52.29 | 118 / 75 |
| 400 | on / on | 51.30 / 90.71 | 126 | 44.35 / 89.95 / 252.66 | 126 / 97 |

| Clips | Assets / native | Main CPU ms | MiB / GCs | PSS KiB | Native seeks | Create / release / install / prepare |
| --- | --- | --- | --- | --- | --- | --- |
| 40 | off / off | 1864.82 | 15.09 / 0 | 88940 | — | — / — / — / — |
| 40 | off / on | 3829.04 | 140.11 / 19 | 93190 | 58 | 1 / 0 / 1 / 1 |
| 40 | on / off | 632.07 | 16.72 / 3 | 91661 | — | — / — / — / — |
| 40 | on / on | 1215.08 | 126.50 / 22 | 100856 | 36 | 1 / 0 / 1 / 1 |
| 100 | off / off | 492.38 | 15.94 / 2 | 96642 | — | — / — / — / — |
| 100 | off / on | 1033.02 | 134.77 / 13 | 107823 | 37 | 1 / 0 / 1 / 1 |
| 100 | on / off | 496.70 | 16.22 / 2 | 99546 | — | — / — / — / — |
| 100 | on / on | 1105.62 | 149.30 / 15 | 105091 | 39 | 1 / 0 / 1 / 1 |
| 200 | off / off | 516.43 | 16.72 / 3 | 93616 | — | — / — / — / — |
| 200 | off / on | 1110.90 | 146.62 / 16 | 109046 | 39 | 1 / 0 / 1 / 1 |
| 200 | on / off | 447.07 | 17.09 / 1 | 106112 | — | — / — / — / — |
| 200 | on / on | 1150.88 | 139.65 / 15 | 107651 | 37 | 1 / 0 / 1 / 1 |
| 400 | off / off | 476.55 | 18.56 / 2 | 100144 | — | — / — / — / — |
| 400 | off / on | 1138.13 | 158.23 / 12 | 107702 | 37 | 1 / 0 / 1 / 1 |
| 400 | on / off | 478.86 | 17.00 / 1 | 112976 | — | — / — / — / — |
| 400 | on / on | 1358.90 | 162.00 / 11 | 113232 | 41 | 1 / 0 / 1 / 1 |

The cold first two 40-clip rows regressed materially: p95 95.92 and 170.64 ms, including a 335.73 ms native maximum. They are retained. The 100-clip no-assets/native-off row also rose from 18.38 to 34.40 ms p95. The 400-clip assets/native row has a 252.66 ms maximum. Run order, JIT/class/EGL initialization, persistent disk caches and emulator/host scheduling affect these single-run observations. No confidence interval or universal p95 improvement is claimed. Heavy Gradle/R8 work was serialized outside measurements; host sampling during diagnosis showed the emulator consuming about 525% CPU while other desktop processes remained present. No unrelated application was killed or device setting changed to improve the score.

# 26. 400-Clip Before / After

**Clean untouched profile rerun → primary final profile matrix**, identical device, fixture, build variant, event schedule and mode. Historical debug values in section 2 are a separate snapshot.

| Assets / native | p50 ms | p95 ms | max ms | >33.33 ms frames |
| --- | --- | --- | --- | --- |
| off / off | 18.03 → 17.47 | 37.67 → 34.52 | 53.04 → 50.59 | 37 → 25 |
| off / on | 41.79 → 34.91 | 72.92 → 67.68 | 104.30 → 122.36 | 115 → 69 |
| on / off | 33.84 → 33.90 | 54.59 → 37.65 | 87.36 → 52.29 | 101 → 75 |
| on / on | 51.30 → 44.35 | 90.71 → 89.95 | 128.98 → 252.66 | 186 → 97 |

| Assets / native | Main CPU ms | Managed MiB | GCs | Native seeks |
| --- | --- | --- | --- | --- |
| off / off | 664.61 → 476.55 | 19.91 → 18.56 | 2 → 2 | — → — |
| off / on | 1567.31 → 1138.13 | 233.83 → 158.23 | 13 → 12 | 74 → 37 |
| on / off | 673.64 → 478.86 | 26.25 → 17.00 | 2 → 1 | — → — |
| on / on | 1673.33 → 1358.90 | 228.21 → 162.00 | 15 → 11 | 81 → 41 |

Assets/native-on median fell 13.5%, Main CPU 18.8%, managed allocation 29.0%, seeks 49.4% and >33 ms frames 47.8%; p95 improved only 0.8% and maximum worsened. UI-only assets p95 fell 31.0%, CPU 28.9% and allocation 35.2%, while median was essentially unchanged. The other final unprofiled four-row run inside the full regression measured 30.45/65.16 ms p50/p95 for assets/native-on (178.05 MiB, 9 GCs, 38 seeks). Its different run order/cache history prevents substituting that more favorable sample for this primary final matrix. The first full matrix remains the headline, with the variability visible.

# 27. Scrub Results

The original paced 120-event matrix is the primary matched comparison. The additional ActivityScenario harness delivers wall-clock MotionEvents without Compose idling between moves. Its final source revision passed both methods unprofiled in **71.822 s** and under ftrace in **74.049 s**. It reads actual unmerged Compose geometry on Main, avoiding accessibility merging/cached handle nodes. This setup refinement and explicit immediate-lift timestamp affect instrumentation and are disclosed; the original matrix is unchanged.

**Latest unprofiled profile-target results:** every delivered frame is retained, with callback loss reported. Each row includes reset/convergence overhead. Native counts are per-path deltas.

| Path | Frames | p50 / p95 / max ms | >33 ms | Main CPU ms | MiB / GCs | Native seeks | Drops / blocking-GC ms |
| --- | --- | --- | --- | --- | --- | --- | --- |
| ScrubCold | 62 | 92.52 / 150.67 / 191.78 | 61 | 816.92 | 108.50 / 7 | 19 | 0 / 0 |
| ScrubWarm | 73 | 84.98 / 119.17 / 131.48 | 71 | 805.81 | 85.64 / 5 | 21 | 0 / 0 |
| Fling | 42 | 91.83 / 127.04 / 150.41 | 41 | 381.99 | 41.81 / 3 | 8 | 0 / 0 |
| PinchZoom | 19 | 88.17 / 309.74 / 319.26 | 18 | 485.36 | 48.32 / 3 | 6 | 0 / 0 |
| ClipDrag | 39 | 93.14 / 184.28 / 210.90 | 38 | 485.76 | 32.74 / 3 | 3 | 0 / 0 |
| ClipTrim | 45 | 86.73 / 112.15 / 123.27 | 44 | 275.41 | 5.58 / 0 | 0 | 0 / 0 |
| PlaybackFollow | 21 | 75.97 / 139.22 / 167.86 | 19 | 193.53 | 25.51 / 1 | 0 | 0 / 0 |
| GestureSoak30s | 408 | 83.99 / 134.20 / 878.47 | 392 | 3990.20 | 619.64 / 38 | 128 | 1 / 231 |
| EditPlaybackSoak | 45 | 71.65 / 119.53 / 168.81 | 43 | 438.37 | 141.15 / 7 | 6 | 0 / 0 |

`ScrubCold` means first interaction in the session; graph preparation can already warm visible assets and disk caches remain. The 4K test separately exercises cold normalization. The untouched unprofiled path harness encountered an accessibility setup failure before input; the successful early all-path capture had substantial profiling overhead. Neither is used to fabricate a controlled seven-path speedup. The older valid unprofiled soak and final full-suite soak are retained alongside this latest observation in section 33. One callback was lost in the latest soak, so its percentiles cover 408 delivered frames and are not a complete account of all frames.

# 28. Fling Results

Batched historical motion is consumed through Compose's public velocity helper. A deterministic regression first failed on original code with velocity 0 and passed after the fix. The final unprofiled real-time path measured -598.60 px/s, one fling start and 27 animation frames; the traced repeat measured -588.65 px/s and 31 frames. Final preview became current. All 45 moves remain; the last eight follow a 600 px/s wall-time trajectory. UP carries the final hardware-sample timestamp to represent immediate lift, rather than an accidental hold caused by injector return latency (29 ms observed in the latest unprofiled run). No fling threshold or correctness tolerance was lowered.

Earlier new-harness failures included a below-threshold generated velocity and a trace-on run that lost release velocity after a delayed lift. Those are retained as diagnostic/setup failures; the production batched-history bug was separately reproduced and fixed. The table includes all captured fling frames and its emulator p95 remains unacceptable for a 60 Hz smoothness claim.

# 29. Zoom Results

Actual two-pointer input passed. Indexed visibility and continuous focal zoom remain; waveform/thumbnail detail changes are bucketed while geometry scales. During latest cold/warm pan, track/clip commits stayed at 6/12 while viewport commits advanced 65 → 139 and waveform geometry stayed at 7. During zoom, waveform geometry increased 9 → 54 and clip commits 24 → 169 across visible clips, a legitimate structural zoom cost. The latest zoom p95 was **309.74 ms**, maximum **319.26 ms**; those poor results are retained. A prior same-code full-suite path measured 118.44 ms p95, illustrating variability without replacing the latest result. Long-clip layout is bounded to three screens, including the tested two-hour/max-zoom case.

# 30. Drag Results

Long-press drag used 60 real move events. Committed source/model placement changed, undo restored the exact original clip, and redo/undo remained valid. Existing cross-track continuous-gesture tests also passed. The measured row is in section 27; no comparable unprofiled before-path result is claimed. Gesture-local SnapIndex and existing local preview validation remain; full validation still occurs on commit. Dragging legitimately publishes transient document revisions and recomposes affected tracks.

# 31. Trim Results

The real trim-handle path used 60 move events, changed source start, preserved source end and undid exactly. Existing trim/split/speed pixel and audio contracts passed with their original tolerances. Section 27 includes all trim frames: 0 new decoder seeks during that measured path does not mean trimming can never require a graph install; intermediate structural preparations can be superseded by the final undo. Final timing/source changes still use the existing compile path. ACTION_UP-only coordinates are applied before commit.

# 32. Playback-Follow Results

Playback-follow advanced real native position beyond 11 seconds during the 1.8-second observation, then paused. Frame-callback sampling runs only while playing/current/not busy at the slower of display/output cadence. Paused native getter polling was removed; actual native time and accepted output constrain position, without an invented interpolated receipt. Natural play/end, opacity and A/V timing tests passed. Latest follow p95/max were 139.22/167.86 ms; the prior full-suite sample had a 233.02 ms maximum, also retained. Logical 60/90/120 Hz policies are unit-tested; runtime measurement used only the emulator's 60 Hz display mode (dumpsys confirmed 60.000004 Hz).

# 33. Long Gesture Soak

At least 30 seconds of alternating scrub/reversal and pinch passed. The new harness was made robust after initial setup failures; these three unprofiled session observations are retained rather than selecting the best:

| Measure | Untouched profile soak | Final full-suite soak | Latest final harness |
| --- | ---: | ---: | ---: |
| Frames | 476 | 430 | 408 |
| p50 / p95 / max ms | 84.22 / 128.30 / 181.84 | 84.08 / 119.15 / 209.87 | 83.99 / 134.20 / 878.47 |
| Main CPU ms | 6024.81 | 3685.33 | 3990.20 |
| Managed MiB / GCs | 735.28 / 50 | 714.24 / 47 | 619.64 / 38 |
| Native seeks | 152 | 138 | 128 |
| End PSS KiB | 119039 | 126103 | 114992 |
| Callback losses | 0 | 0 | 1 |
| Blocking-GC counter delta ms | 0 | 0 | 231 |

CPU/allocation/native counts improved as session observations; median barely changed, latest p95 worsened and maximum/GC tail materially worsened. Dynamic reset/convergence changes completed cycles within the wall-time interval; the final geometry-lookup setup also changed, so these are not per-identical-cycle causal estimates. The latest percentiles omit one undelivered callback, explicitly counted above. The 878 ms delivered frame is retained. A 231 ms blocking-GC runtime delta does not by itself prove that GC alone caused the entire longest frame, but it is a serious observed stall. Native identity recovery count was 4 in the latest soak, and final preview converged. This does not certify steady 60 Hz or leak-free long physical operation.

# 34. Memory / Queue Stability

Six repeated play/pause, trim/move, undo/redo sequences passed after the gesture soak. Latest edit/playback p95/max were 119.53/168.81 ms, allocation 141.15 MiB / 7 GCs; the untouched edit observation was 126.05 ms p95 and 115.60 MiB / 7 GCs. The earlier final full-suite edit sample was 156.11 MiB / 8 GCs. Edit-session allocation did not uniformly improve and remains a limitation.

At settled final preview: one player, zero releases, one install/prepare, 469 intents, 134 native seeks, 341 coalesced requests and 61 submissions. Thumbnail queue returned to 0 queued / 0 running / 14 completed; waveform queue 0 / 0; zero live TextRaster workers. Thumbnail cache held 1,734,656 bytes, text 309,504 bytes; waveform memory was 0 in this disk-cache-hit run (waveform geometry executed 174 times). End PSS was 124,711 KiB. Existing resource-pressure checks preserved 48 live consumer thumbnails and 101 text rasters through eviction/memory callbacks, with queues idle.

Bounds remain 96 thumbnail jobs/device-budgeted workers, 32 waveform jobs/one worker, 32 queued text-raster tasks/one worker, one pending plus one in-flight preview request and bounded diagnostic rings. No accumulating player or worker count was observed. Optimized-source lease release/cancellation was inspected and exercised by 4K sessions; no direct lease counter or long physical retained-heap study was added. Short soaks/PSS establish observed bounded work, not every native/driver allocation's lifetime.

# 35. Physical Device Results

**PHYSICAL DEVICE NOT TESTED.** Final `adb devices -l` enumerated only emulator-5554 (`sdk_gphone16k_arm64`, API37/Android17). No phone or additional display mode was available. No unrelated application or personal media was accessed. API26/27, physical 90/120 Hz modes and varied vendor codecs/GPUs remain runtime coverage gaps.

# 36. Remaining Platform-Bound Cost

The evidence separates app-controlled reductions from remaining backend/device cost:

1. App-controlled: per-tick clip/asset/text invalidation, changing clip dimensions, native request frequency, redundant paused-content seeks, paused player getter polling and lost batched velocity. These were reduced or fixed, with meaningful CPU/allocation/native-count improvements and permanent correctness tests.
2. Pinned Media3: progressive source initialization/byte arrays, timeline/analytics object churn, buffer wrapping, public seek/redraw/current-position behavior and final accepted-output recovery. These still allocate and some public Main calls exceed a display interval. Runtime parameter redraw remains safe for same-output-time parameter changes. Cross-time animation uses a real clock/seek because redraw at the old timestamp would freeze the property curve; no fabricated output receipt was introduced.
3. Framework/driver/emulator: measured Choreographer vsync scheduling tail, RenderThread command flushing/EGL swap/dequeue waits, cold EGL initialization and guest/host scheduling. Video SurfaceView work is outside Window GPU_DURATION. A physical trace is required to distinguish hardware-independent app cost from emulator limits.

CompositionPlayer's pinned builder exposes a looper, but PlayerView requires its player on Main. Moving those calls illegally is not an optimization. No public switch was found for its internal ExoPlayer platform media-metrics listener. No hidden API, reflection, backend fork, dropped layer, quality reduction or casual dependency upgrade was used. A dedicated execution-thread surface adapter or upstream backend change would be a separately scoped engineering change with its own conformance tests, not an unverified tweak in this phase.

# 37. Remaining High-Severity Performance Issues

**High-severity performance target remains open.** The primary 400-clip assets/native p95 is 89.95 ms, essentially unchanged from 90.71; its maximum rose to 252.66 ms. Cold 40-clip rows regress; continuous interaction still has roughly 84–100 ms medians on this emulator. Allocation/GC persists under the pinned backend, and the edit soak did not uniformly improve allocation. A later soak also exhibited a blocking-GC/callback-loss tail, detailed in section 33. These observations prevent a professional-smoothness or production-ready signoff.

The largest measured controllable hot paths were substantially reduced and the next large observed costs lie in required Main framework/backend calls and graphics/host scheduling. This is the task's evidence-based stopping condition for the current architecture/environment, rather than a claim that every performance goal was met. Physical-driver validation and a scoped response to any reproduced native/GC tail are release blockers. API26/27, 90/120 Hz hardware, varied codecs, long thermal runs and complete driver-memory lifetimes remain unverified.

# 38. Files Modified

Nineteen commit-worthy files; no dependency, core domain, persistence or export implementation changes.

| Files | Purpose |
| --- | --- |
| `MainActivity.kt`, `EditorViewModel.kt` | Interactive-state edges and lifecycle wiring |
| `TimelineView.kt` | Stable viewport/clip drawing, bounded asset display, ruler cache, final-coordinate and historical-velocity input fixes |
| `TimelineClipWindow.kt`, `TimelineRenderCounters.kt` | Bounded long-clip geometry and optional test counters |
| `TextCanvasLayer.kt` | Indexed, narrow caption invalidation |
| `PreviewEngine.kt`, `PreviewCadence.kt` | Display-aware adaptive latest-target admission, exact final catch-up, paused-content reuse, frame-clock playback sampling |
| `diagnostics/EngineTrace.kt` | Guarded stable Perfetto slices/counters |
| `TimelinePerformanceTest.kt` | Original workload retained; scrub-state wiring, surface counters and optional method attribution |
| `TimelineInvalidationTest.kt` | Four draw/input contract regressions |
| `TimelineInteractionPerformanceTest.kt` | Two real-clock gesture/soak methods and profiling metrics |
| `InteractivePreviewAdmissionTest.kt` | Two real-source layered/pause-intent regressions |
| `EditorTransportTest.kt`, `TimelineClipWindowTest.kt`, `PreviewCadenceTest.kt` | Twelve additional JVM contracts |
| `TIMELINE_INTERACTIVE_PERFORMANCE_FINAL_REPORT.md`, `tools/FOUNDATION_BENCHMARKS.md`, `README.md` | Requested report, repeatable commands and current-result link |

Paths above are under their existing app main/test/androidTest source trees unless explicitly shown. Raw logs, APKs, generated media, sampled method traces and Perfetto binaries/captures remain in `/tmp`, ignored build directories or the test application's cache; none is in Git changes. The two foundation investigation reports remain unchanged. No file deletion or commit was performed.

# 39. Tests Added

Twenty new tests: 8 cadence/content-reuse JVM checks, 3 bounded clip-window JVM checks and 1 scrub-edge/document-identity JVM check; 4 Compose draw/input regressions, 2 native interactive pixel/play-intent checks and 2 real-clock gesture/soak methods. The original matrix method gained observation/wiring only; its clips, tracks, assets and 120-event input are preserved. Existing source identity, audio tolerance, output receipt and history assertions were not widened. The batched-history test was observed failing before the production fix, then passing afterwards. Real-clock coordinate lookup uses the public unmerged Compose test tree without advancing a virtual frame clock.

# 40. Tests / Build Results

All 44 core + 37 app debug JVM tests passed (81 total). Debug, AndroidTest debug, profile, AndroidTest profile, and unsigned minified release APKs built with JDK17. Final lint: 0 errors, 35 warnings (dependency-update notices and existing platform/KTX recommendations included); the trace-pair helper has a narrowly documented lint suppression because its end is at the call site. The full enabled profile run passed in 406.295 s, 68 reported / 66 passed / 2 process-death protocol skips. The debug targeted run passed five checks in 50.255 s, including real input. The earlier full debug run reported one new fling failure before the history fix, with all other checks successful; it is retained as failure evidence, not described as a passing final run. No legacy assertion was weakened.

Foundation 120-offer results on final profile: simple/layered/animated offer p95 0.091/0.411/0.286 ms; final numbered pixels 234/436/415 ms; 28/22/20 native seeks; all one create, zero release, one install/prepare. Original 4K normalization measured 16,170 ms for the first layer and 2,574 ms for the reused two-layer case; final pixels 416/523 ms. The 4K host fixture was present and the test executed. Original-source thumbnail took 1,030 ms. A 2160/30 request produced the device fallback 1920×1080, 30 frames, 30.00002 FPS, 33,333–33,334 us spacing and 544,636 bytes; the capability contract correctly excludes the observed unsupported setting. These are measured limits, not a new 4K60/HDR claim. Lifecycle storm retained seven creations/six releases across intentional suspend/resume, with one live player; resources returned queues to idle. API26/27, physical drivers, extended thermal runs and the separate forced-process-death protocol were not rerun in this phase.

The final 16-case matrix passed in 114.798 s. After the full-suite run, two harness-only issues were corrected: explicit immediate-lift sample time and unmerged semantic coordinate lookup. Both changed methods then passed again on the same final production binary, unprofiled (71.822 s) and with ftrace (74.049 s). The earlier trace-on fling failure and handle-lookup failure are retained as diagnostic setup evidence; no product assertion or workload was weakened. All raw artifacts remain outside tracked changes. The final all-variant/JVM/lint handoff invocation passed in 37 s; profile AndroidTest was built separately. Lint remains 0 errors / 35 warnings. All 19 changed files are implementation, tests or final documentation. `git diff HEAD --check` passed; no temporary/log/APK/trace files appear in changes, and no files were deleted or committed.

# 41. Final Recommendation

The implementation and available validation are complete; **the smooth-interaction target is not signed off**. Preserve this working architecture and its regression suite. Hold broad feature expansion until representative physical-device traces establish whether the remaining native/framework/GC/driver tails reproduce, then scope any backend/surface work from that evidence. Focused feature work can use the strengthened command/render foundation, but it should not be marketed as completing performance hardening. Do not replace the primary mixed results with a favorable warm or trace-on sample.
