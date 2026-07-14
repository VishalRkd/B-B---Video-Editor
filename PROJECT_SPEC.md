# Project Specification: B&B Video Editor Core Stabilization Directive

## Section 0: Context and Documentation Scope {#context-scope}

### 0.1 Architecture Documentation Alignment {#doc-alignment}
- Read all planning documents in order before writing code:
  1. [AGENTS.md](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/AGENTS.md)
  2. [ARCHITECTURE.md](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/ARCHITECTURE.md)
  3. [DECISIONS.md](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/DECISIONS.md)
  4. [ROADMAP.md](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/ROADMAP.md)
  5. [UI_DESIGN_SYSTEM.md](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/UI_DESIGN_SYSTEM.md)
  6. [PHASE_3_IMPLEMENTATION_GUIDE.md](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PHASE_3_IMPLEMENTATION_GUIDE.md)
- Do not trust the ✅/⏳ status markers in `ROADMAP.md` or `AGENTS.md`'s "Current Project State" table. Inspect actual Kotlin source code files (`.kt`) directly to verify the real state.

### 0.2 Rushed/Partial Implementations Identification {#partial-implementations}
- Identify and isolate partial/rushed implementations of later phases (audio, text/overlays, filters, export, OpenGL) built on top of an unvalidated Phase-3 timeline foundation.
- Enforce that the core Phase-3 foundation must be stabilized and verified before these later features are allowed to be active or expanded.

### 0.3 Resolution of Known Documentation Self-Contradictions {#doc-contradictions}

#### 0.3.1 Competing Timeline-Mutation Paths {#contradiction-timeline-paths}
- **Problem**: `ROADMAP.md` specifies `TimelineEngine` (`core/domain/engine/TimelineEngine.kt`) and `KotlinTimelineEngine` in the data layer with operations (`trimClip`, `splitClip`, `deleteClip`, `moveClip`). Separately, `PHASE_3_IMPLEMENTATION_GUIDE.md` specifies standalone use cases (`TrimClipUseCase`, `SplitClipUseCase`, `DeleteClipUseCase`, `MoveClipUseCase`, `SetSpeedUseCase`) that mutate models directly without any engine dependency.
- **Requirement**: Check which exists, if both exist, and how they are wired. Define and implement exactly one canonical timeline mutation path for every edit.

#### 0.3.2 EditingHistory.push() Incompatible Specifications {#contradiction-history-push}
- **Problem**: One specification suggests reconstructing `EditingHistory(past = ..., future = emptyList())` (which resets `maxSize` to its default value), while another uses `.copy(past = ..., future = emptyList())` (preserving `maxSize`).
- **Requirement**: Locate the implementation, verify it uses `.copy()`, check if `newState` is accepted but unused in the signature, and resolve the unused parameter.

#### 0.3.3 Stale Selection After Split/Delete {#contradiction-stale-selection}
- **Problem**: `SplitClipUseCase` deletes the original clip's ID and generates two new UUIDs. If the selected clip ID is carried over unchanged, subsequent operations on "the selected clip" fail.
- **Requirement**: Ensure `ProjectDetailViewModel.applyEdit()` or `refreshContent()` clears or reassigns the `selectedClipId` after any operation that changes or destroys the active clip ID (such as split or delete).

#### 0.3.4 Preview Engine vs. Timeline Model Mismatch {#contradiction-preview-mismatch}
- **Problem**: `ADR-011` planned to replace `ExoPlayerPreviewEngine` with a `MediaCodecPreviewEngine` when multi-track composition was needed (Phase 3+), with zero changes required in the feature layer.
- **Requirement**: Verify if `PreviewEngine`'s implementation is a bare single-asset ExoPlayer wrapper. If so, multi-track features (overlays, text, filters) cannot render correctly. Resolve this preview pipeline limitation.

#### 0.3.5 Timeline Persistence Fragility {#contradiction-persistence-fragility}
- **Problem**: `ADR-004` stores the entire `Timeline` as a serialized JSON string in a single Room column (`projects.timelineJson`). Plain `Gson` fails to correctly round-trip Kotlin sealed classes (e.g., `VideoFilter`, `AnimatableProperty`) and silently nulls/defaults fields missing from older schemas.
- **Requirement**: Check if timeline serialization was normalized. If kept as JSON, implement custom polymorphic type adapters (e.g., `RuntimeTypeAdapterFactory` or `kotlinx.serialization` with polymorphic support) and handle schema versioning/migration of stored JSON data to prevent NPEs and silent corruption.

---

## Section 1: Ground Rules for Engagement {#ground-rules}

### 1.1 Strict Sequential Phase Execution {#rule-one-phase-at-a-time}
- Work on only one phase at a time. A phase must be fully complete, building cleanly, and passing all tests before moving to the next. Do not implement later features while the current foundation is broken.

### 1.2 Documentation of Audit Findings {#rule-explain-before-implementing}
- Produce a `CODE_AUDIT.md` file at the repository root and document the audit trail before writing any code. Keep updating it as changes are made.

### 1.3 Strict Architectural Layering Rules {#rule-architecture-rules}
- Adhere to `ARCHITECTURE.md` layering rules:
  - Domain layer must have zero Android SDK imports except `Uri`, `Bitmap`, and `Surface`.
  - Feature layer must never import concrete data-layer engine classes.
  - Exactly one Fragment, one ViewModel, and one UiState per feature.
  - Return `AppResult` for all failable operations.
  - Inject `CoroutineDispatchers` instead of using `Dispatchers.IO` or `Dispatchers.Main` directly.
  - Use custom `Logger` instead of `android.util.Log`.

### 1.4 Room Database Migrations {#rule-room-migrations}
- Every schema change must have a corresponding Room `Migration` class. Never use `fallbackToDestructiveMigration()`.

### 1.5 Immutable Data Transformations {#rule-immutability}
- Every timeline operation must return a new, copy-modified instance of the model (`Project`, `Timeline`, `Track`, `Clip`). No in-place mutations.

### 1.6 Verification Pass After Each Phase {#rule-verification-each-phase}
- Run `./gradlew assembleDebug` and `./gradlew test` at the end of each phase. Both must pass before starting the next phase.

### 1.7 Real-Time Documentation Reconciliation {#rule-update-docs-live}
- Update all project documents (`ROADMAP.md` status, `AGENTS.md` tables, `DECISIONS.md` ADRs) in the same phase and commit where the underlying features/fixes are completed.

---

## Section 2: Phase 0 — Ground-Truth Audit Requirements {#phase-0-audit}

### 2.1 File Layer Mapping and Engine Verification {#audit-file-mapping}
- List every file under `app/src/main/java/` (or the package root) and map it to its respective architecture layer.
- Audit the seven engines (`PreviewEngine`, `TimelineEngine`, `MediaEngine`, `AudioEngine`, `EffectsEngine`, `RenderEngine`, `ExportEngine`): check interface existence, implementation existence, DI binding in `EngineModule`, and usages. Identify dead code or competing pipelines.

### 2.2 Use Case Coverage Audit {#audit-use-cases}
- Audit use case families (`project`, `timeline`, `audio`, `effects`, `export` under `core/domain/usecase/`): confirm which exist, match them against documented contracts, and identify undocumented use cases.

### 2.3 Model and Persistence Inspection {#audit-models-and-persistence}
- Inspect `Clip.kt`, `Track.kt`, `Timeline.kt`, and `Project.kt` for their actual fields.
- Check `AppDatabase.kt` for version, migrations, and any use of `fallbackToDestructiveMigration()`.

### 2.4 User Action Traces {#audit-action-traces}
- Trace the following user actions end-to-end (View -> ViewModel -> UseCase/Engine -> Mutation -> Persistence -> UiState -> View/PreviewEngine):
  1. **Clip Trim Handle Drag**: User drags the right trim handle of a clip.
  2. **Split Clip**: User taps "Split".
  3. **Add Text Overlay**: User adds a text overlay.
  4. **Export Project**: User taps "Export".
- Document exactly where each trace breaks, has inconsistencies, or silently swallows errors.

### 2.5 Test Suite and Manual Verification {#audit-testing}
- Run `./gradlew test` and record test status and gaps (use cases with zero unit tests).
- Install debug build on emulator/device and manually test: create project, trim clip, split clip, undo, redo, add audio, add text, apply filter, export. Record observed vs. documented behaviors.
- Write the results into `CODE_AUDIT.md`.

---

## Section 3: Phase 1 — Core Domain Model & Timeline Mutation Engine Stabilization {#phase-1-stabilization}

### 3.1 Pick One Canonical Timeline Mutation Path {#phase-1-canonical-path}
- Select either `TimelineEngine`/`KotlinTimelineEngine` or standalone use cases as the single source of truth for timeline mutations.
- Remove the unused alternative path and clean up dead code.
- Update `ARCHITECTURE.md` and `ROADMAP.md` to reflect the chosen path.

### 3.2 Consolidate Domain Models and Implement Invariants {#phase-1-models-invariants}
- Consolidate `Clip`, `Track`, `Timeline`, and `Project` fields to support all phases (trim, speed, volume, textStyle, filter, keyframes, animatable properties) cleanly.
- Implement a pure function `TimelineValidator.validate(timeline: Timeline): List<TimelineValidationError>`.
  - Invariants to check: clip ID uniqueness, `trimStartMs >= 0`, `trimEndMs > trimStartMs`, `trimEndMs <= asset.durationMs`, valid `assetId` references, `timelinePositionMs >= 0`, `speedFactor` in `0.25f..4.0f`, volume in range, track order stability.
  - Execute validation at the end of every timeline mutation. Fail loudly in debug builds; log and return `AppResult.Error` in release builds.
- Resolve editing history and selection bugs:
  - Fix `EditingHistory.push()` to copy rather than reconstruct.
  - Update use case return values to supply modified clip IDs, enabling `ProjectDetailViewModel` to update `selectedClipId` appropriately after split/delete.

### 3.3 Normalize and Correct Database Persistence {#phase-1-persistence}
- Resolve JSON serialization fragility:
  - Switch to normalized SQLite tables for `Track`, `Clip`, `Effect`, `Transition` with foreign keys, OR
  - Switch to polymorphic serializers (e.g., `kotlinx.serialization` or `RuntimeTypeAdapterFactory` in Gson) to support sealed classes inside `timelineJson`.
  - Add schema versioning inside the serialized JSON blob with migration scripts.
- Write migration tests confirming older schema versions load correctly with sane defaults instead of null values.
- Verify migrations in `AppDatabase.kt` and remove any destructive fallbacks.

### 3.4 Exhaustive Timeline Unit Test Cases {#phase-1-test-cases}
- Implement unit tests for choice in 3.1 covering these scenarios:
  - **Trim Edge Cases**: start < 0; end <= start; end > asset duration; trim below minimum clip width; trim affecting downstream clips/caches.
  - **Split Edge Cases**: split before/at start; split at/after end; split 1-2ms clip; split exactly on boundary; split only clip on track; rapid double splits.
  - **Delete Edge Cases**: non-existent clip; last clip on track; selected clip (check selection clear); delete during thumbnail/waveform generation (race check).
  - **Move/Reorder Edge Cases**: negative position; position beyond track end; overlap behavior (gap-closing vs. rejection).
  - **Speed Edge Cases**: below 0.25x; above 4.0x; speed change on clip with keyframes (time rescaling check).
  - **Undo/Redo Edge Cases**: undo empty history; redo empty future; push after undo (future cleared); push past max size (preserves max size); auto-save interleave.
  - **Asset Deletion/Missing**: referenced asset missing from device (MediaStore URI invalid) – verify graceful failure with UI message, no crashes.
  - **Empty/Degenerate Timeline**: zero tracks; track with zero clips; all tracks hidden/locked; exporting empty timeline.

---

## Section 4: Phase 2 — State-Management Spine Stabilization {#phase-2-state-management}

### 4.1 ViewModel Source of Truth Clean Up {#phase-2-single-source-of-truth}
- Audit `ProjectDetailViewModel` for duplicate project caching variables. Consolidate them into a single mutable source of truth: `StateFlow<UiState>`'s `Content.project`. Maintain `EditingHistory` based on states returned by actions.

### 4.2 Atomic Mutation Operation {#phase-2-atomic-mutations}
- Channel all project mutations through an atomic function (e.g., `applyEdit(newProject: Project)`) that validates invariants, registers history, performs auto-save (persists), and updates `UiState` simultaneously.

### 4.3 UI State Synchronization {#phase-2-state-sync}
- Derive `selectedClipId`, `canUndo`, `canRedo`, `playbackState`, and `currentPositionMs` directly from the single source of truth upon state emission to prevent stale references (e.g., post-split/delete).

### 4.4 Auto-Save Race Prevention {#phase-2-save-concurrency}
- Debounce/serialize database write calls. Use a channel/mutex-guarded single coroutine or a `mapLatest` flow collector to guarantee sequential Room writes and prevent out-of-order writes.

### 4.5 Lifecycle and View Binding Safety {#phase-2-lifecycle-safety}
- Ensure fragments collect StateFlows/SharedFlows inside `repeatOnLifecycle(Lifecycle.State.STARTED)` blocks.
- Explicitly nullify view bindings (`_binding = null`) only in `BaseFragment.onDestroyView()`.

### 4.6 ViewModel Verification Unit Tests {#phase-2-viewmodel-tests}
- Implement unit tests using repository fakes verifying: rapid sequential edits, undo during background saves, edit overlapping a save, and graceful error handling on failures (e.g., missing asset).

---

## Section 5: Phase 3 — Preview and Render Pipeline Stabilization {#phase-3-preview-pipeline}

### 5.1 Multi-Layer Rendering Capability Investigation {#phase-3-compositing-audit}
- Determine if the preview pipeline composites multiple layers (video track + audio + text + overlays + filters) onto the preview surface. If it is only a single-asset ExoPlayer wrapper, fix the pipeline.

### 5.2 Implement Multi-Track Composition Strategy {#phase-3-multi-track-strategy}
- Implement composition rendering using a defined strategy (e.g., Media3 `Composition`/`EditedMediaItemSequence`/`CompositionPlayer` API or an OpenGL-based `RenderEngine`). Document this architecture in a new ADR in `DECISIONS.md`.
- Keep the `PreviewEngine` interface contract unchanged for the feature layer.

### 5.3 UI Gating of Unimplemented Render Targets {#phase-3-feature-gating}
- Explicitly gate UI controls for any track or filter types the preview cannot render yet (e.g., grey out or disable controls with "coming soon"). Do not present non-rendering features as functional.

### 5.4 Timeline Time Calculations {#phase-3-duration-calculations}
- Calculate playhead seeking, current positions, and total project duration from the consolidated `Timeline` (taking trim points and speed factor into account) rather than a single raw source video's length.

### 5.5 Player Integration Tests {#phase-3-playback-tests}
- Verify player mechanics: scrubbing/playing across clip boundaries, seeking inside trimmed regions, and immediate execution of playback speed adjustments.

---

## Section 6: Phase 4 — Feature-by-Feature Re-Stabilization {#phase-4-feature-stabilization}

*Note: Work on each feature below sequentially. Verify the build and unit tests pass before moving to the next feature. Write exception/edge case tests for each feature.*

### 6.1 Media Import / Asset Library / Home {#feat-media-import}
- **Edge cases**: zero media on device; permission denials/grants mid-session; permission revoked during import; asset deleted from device after import; duplicate file imports; list rendering performance under a large media library.

### 6.2 Timeline Core Editing Operations {#feat-timeline-core}
- Verify core editing operations (trim, split, delete, move, speed, undo, redo) against the stabilized core mutation engine. Correct any remaining failures.

### 6.3 Audio Engine (Waveform, Volume, Mute) {#feat-audio}
- **Edge cases**: audio-only clips; distinguishing muted clip vs. muted track vs. volume=0.0; extracting waveforms for short (<100ms) or long (>10min) tracks; graceful handling of corrupt audio extraction; real-time audio volume changes reflected in the preview player.

### 6.4 Text, Image Overlays, and Filters {#feat-overlays-filters}
- **Edge cases**: empty string text overlays; text overflow rendering; overlapping overlays at identical positions; filters applied to missing/deleted clips; corrupt LUT files; rapid filter switching resource release (leak prevention).

### 6.5 Export Engine {#feat-export}
- **Edge cases**: cleanup of partial file on user cancellation; background survival via `WorkManager` (using `foregroundService` notification updates); storage full errors; exporting missing assets; exporting empty timelines; resolution/bitrate codec limitations; concurrent export execution attempts.

### 6.6 OpenGL Effects / Keyframe Adjustment Track {#feat-opengl-keyframes}
- **Edge cases**: fallback values for animatable properties with zero keyframes; chronological sorting of keyframes; keyframes extending past modified clip boundaries (post-trim/speed scaling).

---

## Section 7: Phase 5 — Regression and Lint Checks {#phase-5-regression}
- Conduct a static analysis check over the codebase and resolve these issues:
  - Eliminate direct `android.util.Log` references, replacing them with `Logger`.
  - Remove hardcoded Kotlin string literals; migrate them to `strings.xml`.
  - Replace hardcoded dimensions with `dimens.xml` resource calls.
  - Replace direct calls to `Dispatchers.IO`/`Dispatchers.Main` with injected `CoroutineDispatchers`.
  - Split files/classes exceeding 300 lines of code (or supply written architectural justification).
  - Resolve or delete `TODO` statements without linked tracking issues.
  - Verify that domain layer modules are free of framework-specific Android SDK references (excluding allowed `Uri`, `Bitmap`, `Surface`).
  - Move business logic out of Fragment UI files into ViewModels.
  - Restrict exposed ViewModel flows to read-only `StateFlow`/`SharedFlow` interfaces (keep mutable structures private).
  - Enforce `repeatOnLifecycle(Lifecycle.State.STARTED)` for flow collections in Fragments.
  - Delete all leftover or dead code from deprecated timeline-mutation paths.

---

## Section 8: Phase 6 — Documentation Reconciliation {#phase-6-docs}
- Update `ROADMAP.md` status flags accurately matching completed phases.
- Align `AGENTS.md` §9 ("Current Project State") table with the audited state.
- Create and add ADRs inside `DECISIONS.md` covering choices made (mutation engine selection, serialization strategy, composition playback technology, concurrency logic).
- Finish updating `CODE_AUDIT.md` highlighting found issues and their respective resolution commits.

---

## Section 9: Definition of Done {#definition-of-done}
- [ ] `CODE_AUDIT.md` is complete, lists all root causes, and maps them to resolving commits.
- [ ] There is exactly one canonical path for timeline mutations, and the deprecated path is deleted.
- [ ] `TimelineValidator` executes on every mutation and has unit test coverage of the edge-case matrix in 3.4.
- [ ] Database schema migrations work, and old project versions deserialize cleanly without throwing nulls or NPEs (proven by tests).
- [ ] `ProjectDetailViewModel` has a single source of truth for the project state; selections remain valid after splits and deletes.
- [ ] Preview player renders all active media tracks, or unimplemented tracks are explicitly gated in the UI.
- [ ] Both `./gradlew assembleDebug` and `./gradlew test` compile and pass without failures.
- [ ] End-to-end user manual test path (create -> trim -> split -> undo -> redo -> add audio -> add text -> apply filter -> export) works without crashes or silent failures.
- [ ] Documentation files (`ROADMAP.md`, `AGENTS.md`, `DECISIONS.md`) match the project state.
