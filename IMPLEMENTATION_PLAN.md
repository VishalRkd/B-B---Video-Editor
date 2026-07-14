# Core Stabilization Implementation Plan

This implementation plan defines the step-by-step roadmap for stabilizing the B&B Video Editor codebase, ensuring that each phase builds upon a validated foundation.

---

## Global Constraints & Ground Rules
The following ground rules apply across all phases of the implementation:
- **One phase at a time**: A phase must be completed, building cleanly, and passing all unit/instrumented tests before starting the next.
- **Explain before implementing**: Prior to touching any code in a phase, analyze the existing state and explain the proposed approach in the chat.
- **Layering rules**: No Android framework classes in the domain layer (excluding `Uri`, `Bitmap`, and `Surface`). Feature fragments must delegate all business logic to ViewModels and code only against engine interfaces (never concrete classes).
- **Immutability**: Maintain functional, immutable model transformations throughout the mutation pipelines.
- **Verification**: Run `./gradlew assembleDebug` and `./gradlew test` at the end of each phase.
- **Real-time documentation reconciliation**: Keep `AGENTS.md`, `ROADMAP.md`, `DECISIONS.md`, and `CODE_AUDIT.md` fully updated as changes occur.

*References:*
- [PROJECT_SPEC.md#ground-rules](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#ground-rules)
- [PROJECT_SPEC.md#rule-one-phase-at-a-time](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#rule-one-phase-at-a-time)
- [PROJECT_SPEC.md#rule-explain-before-implementing](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#rule-explain-before-implementing)
- [PROJECT_SPEC.md#rule-architecture-rules](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#rule-architecture-rules)
- [PROJECT_SPEC.md#rule-room-migrations](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#rule-room-migrations)
- [PROJECT_SPEC.md#rule-immutability](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#rule-immutability)
- [PROJECT_SPEC.md#rule-verification-each-phase](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#rule-verification-each-phase)
- [PROJECT_SPEC.md#rule-update-docs-live](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#rule-update-docs-live)

---

## Phase 0: Ground-Truth Audit
### Objective
Establish the absolute, evidence-backed state of the codebase regarding layer structures, engines, use cases, models, persistence, user actions, and test status. Save findings to `CODE_AUDIT.md`. No functional code modifications are made during this phase.

### Features to Implement
- Mapping of all files under package root to their architecture layer.
- Engine status cataloging (existence, implementations, DI bindings, dead code, duplicates).
- Use case status analysis (existence, contract conformance, undocumented files).
- Domain model check (`Clip.kt`, `Track.kt`, `Timeline.kt`, `Project.kt` current fields).
- Persistence/migration status report (schema version, migration files, presence of destructive fallback flags).
- End-to-end trace documentation for:
  1. Trim handle drag
  2. Split clip
  3. Text overlay addition
  4. Project export
- Test suite run and manual testing logs.

### Files Likely to be Modified
- [CODE_AUDIT.md](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/CODE_AUDIT.md) [NEW]

### Dependencies
- None (purely analysis and verification).

### Expected Outcome
- A comprehensive `CODE_AUDIT.md` file resides at the root, laying out a complete mapping of code status, five contradiction checks, four action traces, test gaps, manual execution logs, and ranked root causes.

### Validation Checklist
- [ ] `CODE_AUDIT.md` is populated with all mandatory sections.
- [ ] Every Kotlin file under `app/src/main/java` has been mapped to its architectural layer.
- [ ] Five documentation contradictions are resolved and documented with exact code references.
- [ ] Four end-to-end execution paths are traced, highlighting any failures or gaps.
- [ ] Current test suite has been run and failure/coverage details recorded.

### Direct References to PROJECT_SPEC.md
- [PROJECT_SPEC.md#context-scope](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#context-scope)
- [PROJECT_SPEC.md#doc-alignment](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#doc-alignment)
- [PROJECT_SPEC.md#partial-implementations](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#partial-implementations)
- [PROJECT_SPEC.md#doc-contradictions](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#doc-contradictions)
- [PROJECT_SPEC.md#phase-0-audit](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-0-audit)
- [PROJECT_SPEC.md#audit-file-mapping](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#audit-file-mapping)
- [PROJECT_SPEC.md#audit-use-cases](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#audit-use-cases)
- [PROJECT_SPEC.md#audit-models-and-persistence](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#audit-models-and-persistence)
- [PROJECT_SPEC.md#audit-action-traces](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#audit-action-traces)
- [PROJECT_SPEC.md#audit-testing](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#audit-testing)

---

## Phase 1: Core Domain Model & Timeline Mutation Engine Stabilization
### Objective
Enforce exactly one canonical path for timeline mutations, consolidate domain models with structural invariants, secure Room database persistence with schema-safe serialization, and create a comprehensive unit test suite covering trim/split/delete/move/speed/history edge cases.

### Features to Implement
- **Canonical Timeline Path**: Choose between `TimelineEngine` and standalone use cases. Standardize the entire editing pipeline onto this single mechanism. Safely remove or deprecate the alternate code path.
- **Consolidated Models**: Unify fields inside `Clip`, `Track`, `Timeline`, and `Project` to ensure complete representation.
- **Timeline Validator**: Implement `TimelineValidator.validate(Timeline)` assessing clip IDs, durations, positive values, speed limits, and volume ranges. Wire this validator to run on every timeline modification (failing in debug; returning error in release).
- **Editing History Push**: Rectify `EditingHistory.push()` to execute using `.copy(...)` to maintain settings like `maxSize`, and resolve the unused `newState` parameter.
- **Update VM Selection Flow**: Adjust split/delete use case signatures to provide updated/surviving ID sets, allowing the ViewModel to update selection state accurately.
- **Persistence Correction**: Upgrade Room serialization of `Timeline` containing sealed classes (`VideoFilter`, `AnimatableProperty`) by registering explicit type adapters in Gson or migrating to `kotlinx.serialization` with polymorphic serialization support. Build JSON-level migrations if needed.
- **Migration Verification**: Establish automated migration tests simulating legacy schemas loading into updated models with fallback default values rather than null exceptions. Verify Hilt and Room configurations.

### Files Likely to be Modified
- [TimelineEngine.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/domain/engine/TimelineEngine.kt)
- [KotlinTimelineEngine.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/data/engine/KotlinTimelineEngine.kt)
- [Clip.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/domain/model/Clip.kt)
- [Track.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/domain/model/Track.kt)
- [Timeline.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/domain/model/Timeline.kt)
- [Project.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/domain/model/Project.kt)
- [EditingHistory.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/domain/model/EditingHistory.kt)
- [ProjectMapper.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/data/local/db/mapper/ProjectMapper.kt)
- [AppDatabase.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/data/local/db/AppDatabase.kt)
- [TrimClipUseCase.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/domain/usecase/timeline/TrimClipUseCase.kt)
- [SplitClipUseCase.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/domain/usecase/timeline/SplitClipUseCase.kt)
- [DeleteClipUseCase.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/domain/usecase/timeline/DeleteClipUseCase.kt)
- [MoveClipUseCase.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/domain/usecase/timeline/MoveClipUseCase.kt)
- [TimelineValidator.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/domain/model/TimelineValidator.kt) [NEW]
- Hilt modules and Use Case test files.

### Dependencies
- Phase 0 verification must be completed and logged.

### Expected Outcome
- The core timeline editing logic, domain models, history transitions, and serialization layers are verified and secured. Structural invariants are validated live on every mutation, preventing model corruption. Comprehensive tests are green.

### Validation Checklist
- [ ] Exactly one timeline mutation pipeline remains active; competing dead code is removed.
- [ ] `TimelineValidator` checks all structural invariants, with tests asserting invalid states are rejected.
- [ ] `EditingHistory.push` safely preserves history limits using `.copy()`.
- [ ] Gson/polymorphic serialization is verified to load legacy models with default values correctly.
- [ ] Database migration and schema safety are proved by automated unit tests.
- [ ] Full unit test suite for choice in 3.1 covers all edge cases (trim, split, delete, move, speed, empty timeline, missing/deleted backing assets).

### Direct References to PROJECT_SPEC.md
- [PROJECT_SPEC.md#contradiction-timeline-paths](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#contradiction-timeline-paths)
- [PROJECT_SPEC.md#contradiction-history-push](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#contradiction-history-push)
- [PROJECT_SPEC.md#contradiction-persistence-fragility](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#contradiction-persistence-fragility)
- [PROJECT_SPEC.md#phase-1-stabilization](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-1-stabilization)
- [PROJECT_SPEC.md#phase-1-canonical-path](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-1-canonical-path)
- [PROJECT_SPEC.md#phase-1-models-invariants](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-1-models-invariants)
- [PROJECT_SPEC.md#phase-1-persistence](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-1-persistence)
- [PROJECT_SPEC.md#phase-1-test-cases](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-1-test-cases)

---

## Phase 2: State-Management Spine Stabilization
### Objective
Establish `ProjectDetailViewModel` as the single source of truth for the active project. Force all changes to go through atomic validation, history logging, and database saving, while preventing race conditions on save actions.

### Features to Implement
- **Single VM Source of Truth**: Consolidate duplicate tracking references. Ensure all components read only from the exposed `StateFlow<ProjectDetailUiState>`.
- **Atomic Operations Flow**: Direct all edits to an atomic `applyEdit` pipeline. In one transaction: validate invariants via `TimelineValidator`, push history, save database state, and update UI state.
- **Dynamic Selection Tracking**: Guarantee that `selectedClipId` is dynamically verified or cleared when edits modify or destroy the selected clip's original ID (splits, deletions).
- **Auto-Save Concurrency Control**: Implement a mutex, channel-guarded sequential queue, or `mapLatest` collector to sequence Room operations, preventing overlapping save coroutines from writing out of order.
- **Safe Lifecycle Observing**: Standardize flow collections in Fragments to execute inside `repeatOnLifecycle(STARTED)` blocks.
- **Nullify Bindings**: Standardize binding lifecycle by nulling `_binding = null` only in `onDestroyView()`.

### Files Likely to be Modified
- [ProjectDetailViewModel.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/feature/projectdetail/ProjectDetailViewModel.kt)
- [ProjectDetailFragment.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/feature/projectdetail/ProjectDetailFragment.kt)
- UI and ViewModel unit tests.

### Dependencies
- Phase 1 must be completed and verified.

### Expected Outcome
- The state-management spine is safe against concurrency conflicts, out-of-order database writes, and state synchronization failures (e.g., stale clip selection).

### Validation Checklist
- [ ] ViewModel maintains only a single, authoritative project state.
- [ ] Mutex/queue guarantees serial execution of database auto-save operations.
- [ ] `selectedClipId` is safely updated or cleared on splits/deletions.
- [ ] Fragments use the correct `repeatOnLifecycle` lifecycle state.
- [ ] Unit tests verify concurrency protection, saving sequence, and VM error recovery.

### Direct References to PROJECT_SPEC.md
- [PROJECT_SPEC.md#contradiction-stale-selection](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#contradiction-stale-selection)
- [PROJECT_SPEC.md#phase-2-state-management](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-2-state-management)
- [PROJECT_SPEC.md#phase-2-single-source-of-truth](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-2-single-source-of-truth)
- [PROJECT_SPEC.md#phase-2-atomic-mutations](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-2-atomic-mutations)
- [PROJECT_SPEC.md#phase-2-state-sync](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-2-state-sync)
- [PROJECT_SPEC.md#phase-2-save-concurrency](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-2-save-concurrency)
- [PROJECT_SPEC.md#phase-2-lifecycle-safety](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-2-lifecycle-safety)
- [PROJECT_SPEC.md#phase-2-viewmodel-tests](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-2-viewmodel-tests)

---

## Phase 3: Preview and Render Pipeline Stabilization
### Objective
Confirm the compositing capability of the preview engine, establish the multi-track rendering strategy using Media3/OpenGL, gate unimplemented UI paths, and calculate timings relative to the actual consolidated Timeline.

### Features to Implement
- **Multi-Track Preview Composition**: Audit how the preview surface composites video tracks, audio, text, overlays, and filters. Migrate `ExoPlayerPreviewEngine` to Media3 `Composition` (e.g. `Composition` / `EditedMediaItemSequence` / `CompositionPlayer`) or wire standard OpenGL overlays as defined in ADR-011.
- **Architectural ADR**: Create a concrete ADR inside `DECISIONS.md` defining the chosen preview rendering pipeline.
- **Feature Gating**: Disable or hide UI entry points for tracks/filters that the player is not ready to render, keeping the interface clean and functional.
- **Seek and Duration Calculations**: Recalculate duration, seek parameters, and playhead positions from the entire timeline (accounting for trim modifications and clip speeds) rather than a single source media file.
- **Playback Integration Tests**: Verify seeking across clip boundaries, playing across boundaries, trimmed zones, and speed factor changes.

### Files Likely to be Modified
- [ExoPlayerPreviewEngine.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/data/engine/ExoPlayerPreviewEngine.kt)
- [PreviewEngine.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/domain/engine/PreviewEngine.kt)
- [DECISIONS.md](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/DECISIONS.md)
- [ProjectDetailFragment.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/feature/projectdetail/ProjectDetailFragment.kt)

### Dependencies
- Phase 2 must be completed and verified.

### Expected Outcome
- The preview player accurately renders the multi-track timeline, including trim boundaries and speed scaling, or safely gates non-rendering assets. Timings match the timeline data.

### Validation Checklist
- [ ] Preview engine utilizes a validated composition mechanism (e.g., Media3 Composition).
- [ ] ADR-015 is created in `DECISIONS.md` detailing the composition architecture.
- [ ] Unimplemented tracks/filters are safely gated in the UI.
- [ ] Timeline-based calculations govern seek boundaries and total duration.
- [ ] Integration tests verify playback stability across transitions, trims, and speed shifts.

### Direct References to PROJECT_SPEC.md
- [PROJECT_SPEC.md#contradiction-preview-mismatch](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#contradiction-preview-mismatch)
- [PROJECT_SPEC.md#phase-3-preview-pipeline](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-3-preview-pipeline)
- [PROJECT_SPEC.md#phase-3-compositing-audit](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-3-compositing-audit)
- [PROJECT_SPEC.md#phase-3-multi-track-strategy](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-3-multi-track-strategy)
- [PROJECT_SPEC.md#phase-3-feature-gating](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-3-feature-gating)
- [PROJECT_SPEC.md#phase-3-duration-calculations](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-3-duration-calculations)
- [PROJECT_SPEC.md#phase-3-playback-tests](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-3-playback-tests)

---

## Phase 4: Feature-by-Feature Re-Stabilization
### Objective
Individually stabilize and test each video editor feature in dependency order against the verified domain, state, and render foundations.

### Features to Implement
- **4.1 Media Import / Asset Library / Home**: Resolve edge cases regarding permission changes mid-session, duplicate file imports, and missing assets.
- **4.2 Timeline Core Editing**: Finalize core operations (trim/split/delete/move/speed/history) against the validated mutation engine.
- **4.3 Audio Engine (Waveforms, Volume, Mutes)**: Correct waveform rendering, mute actions, short/long audio file extraction, and immediate preview updates.
- **4.4 Text, Image Overlays, and Filters**: Validate styling, rendering, overlapping, and filter switches.
- **4.5 Export Engine**: Ensure WorkManager background operations survive app closures, clean up temporary segments on cancels, and handle storage limit issues.
- **4.6 OpenGL Effects / Keyframes**: Sort keyframes, handle missing parameters gracefully, and clamp keyframe limits relative to clip ranges.

### Files Likely to be Modified
- [MediaImportFragment.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/feature/mediaimport/MediaImportFragment.kt)
- [AndroidAudioEngine.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/data/engine/AndroidAudioEngine.kt)
- [AndroidEffectsEngine.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/data/engine/AndroidEffectsEngine.kt)
- [MediaCodecExportEngine.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/data/engine/MediaCodecExportEngine.kt)
- [ExportWorker.kt](file:///Users/vishalsingh/Desktop/DEV%20Android Projects/BBvideoeditor/app/src/main/java/com/beatsandbeyond/video_editor/core/data/local/worker/ExportWorker.kt)
- Feature ViewModels, use cases, and respective unit test files.

### Dependencies
- Phase 3 must be completed and verified.

### Expected Outcome
- Every editor feature (media, editing, audio, text, filters, export, effects) executes successfully, handles errors gracefully, and remains fully synchronized with the timeline and preview components.

### Validation Checklist
- [ ] Home and Import components degrade gracefully if permissions are modified or files deleted.
- [ ] Core editing actions execute accurately with invalid bounds rejected.
- [ ] Audio waveforms extract and volume changes reflect instantly in playback.
- [ ] Overlay rendering works for overlapping layers, and filters do not leak resources.
- [ ] Background exports survive process terminations and clean up temporary storage.
- [ ] Keyframe values remain clamped inside trim boundaries.

### Direct References to PROJECT_SPEC.md
- [PROJECT_SPEC.md#feat-media-import](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#feat-media-import)
- [PROJECT_SPEC.md#feat-timeline-core](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#feat-timeline-core)
- [PROJECT_SPEC.md#feat-audio](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#feat-audio)
- [PROJECT_SPEC.md#feat-overlays-filters](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#feat-overlays-filters)
- [PROJECT_SPEC.md#feat-export](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#feat-export)
- [PROJECT_SPEC.md#feat-opengl-keyframes](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#feat-opengl-keyframes)

---

## Phase 5: Regression and Lint Checks
### Objective
Execute static analysis over the codebase to clean up architectural violations, hardcoded resources, raw log references, long files, and illegal framework references in the domain layer.

### Features to Implement
- **Log Removal**: Replace all `android.util.Log` instances with the customized `Logger`.
- **Resource Hardcoding Cleanup**: Extract raw string values to `strings.xml` and hardcoded sizes to `dimens.xml`.
- **Dispatcher Clean Up**: Ensure all asynchronous scopes invoke injected `CoroutineDispatchers` (no hardcoded `Dispatchers.IO`/`Dispatchers.Main`).
- **File Length Reduction**: Refactor or split classes exceeding 300 lines of code.
- **Domain Layer Checks**: Erase illegal Android package imports from files inside the domain layer.
- **Fragment & VM Review**: Verify business logic is completely isolated in ViewModels, and flows are correctly exposed as read-only interfaces.
- **Dead Code Eradication**: Purge any remaining fragments of the deprecated timeline mutation path.

### Files Likely to be Modified
- All codebase files violating rules listed in [PROJECT_SPEC.md#phase-5-regression](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-5-regression).

### Dependencies
- Phase 4 must be completed and verified.

### Expected Outcome
- The codebase is clean, follows strict architectural specifications, contains no hardcoded assets, and enforces absolute layer boundaries.

### Validation Checklist
- [ ] No direct `android.util.Log` statements remain.
- [ ] All Kotlin files are clean of hardcoded dimensions and string literals.
- [ ] No class exceeds 300 lines of code without strong justification.
- [ ] Injected dispatchers are invoked in all async scopes.
- [ ] Domain layer imports are clean.
- [ ] Exposed flow interfaces are strictly read-only.
- [ ] No dead code remains.

### Direct References to PROJECT_SPEC.md
- [PROJECT_SPEC.md#phase-5-regression](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-5-regression)

---

## Phase 6: Documentation Reconciliation & Verification
### Objective
Reconcile all project documentation and execute a final validation pass ensuring compliance with all criteria, completing the stabilization directive.

### Features to Implement
- **Roadmap Verification**: Update `ROADMAP.md` status flags to represent the final code state.
- **Current State Sync**: Ensure `AGENTS.md` matches the final completed features.
- **Architectural ADRs**: Review and refine all new decisions added to `DECISIONS.md`.
- **Finalize Audit Trail**: Ensure `CODE_AUDIT.md` documents all before/after statuses, root causes, and resolving commits.
- **End-to-End Manual Run**: Verify project creation -> trim -> split -> undo -> redo -> add audio -> add text -> apply filter -> export executes correctly on a live device.

### Files Likely to be Modified
- [ROADMAP.md](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/ROADMAP.md)
- [AGENTS.md](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/AGENTS.md)
- [DECISIONS.md](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/DECISIONS.md)
- [CODE_AUDIT.md](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/CODE_AUDIT.md)

### Dependencies
- Phase 5 must be completed and verified.

### Expected Outcome
- The project documentation is accurate, all verification suites are green, and the target stabilization criteria are fully met.

### Validation Checklist
- [ ] `ROADMAP.md` and `AGENTS.md` reflect correct status tables.
- [ ] ADRs inside `DECISIONS.md` match architectural implementation.
- [ ] `CODE_AUDIT.md` outlines resolutions linked to commits.
- [ ] Automated tests and apk building complete successfully.
- [ ] Manual end-to-end execution behaves correctly without regressions.

### Direct References to PROJECT_SPEC.md
- [PROJECT_SPEC.md#phase-6-docs](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#phase-6-docs)
- [PROJECT_SPEC.md#definition-of-done](file:///Users/vishalsingh/Desktop/DEV%20Android%20Projects/BBvideoeditor/PROJECT_SPEC.md#definition-of-done)
