# DECISIONS.md — Architectural Decision Log

This file records significant architectural decisions, the alternatives considered, and the rationale for each choice.

---

## ADR-001: Single-Module Architecture (Phase 1)

**Date**: Phase 1  
**Status**: Active

**Decision**: Use a single Gradle module (`:app`) with package-based separation.

**Alternatives Considered**:
- Multi-module from day one (e.g., `:core`, `:feature:home`, `:feature:mediaimport`)

**Rationale**:
- Feature boundaries are not yet proven stable
- Multi-module adds significant build complexity before it delivers value
- Premature module splitting causes constant merge conflicts across module boundaries
- Single module compiles faster during early development

**Future Plan**: Extract to multi-module when the codebase has ~5 stable features. Core domain will become `:core:domain`, features become individual `:feature:*` modules.

---

## ADR-002: MediaStore as Source of Truth (No Gallery Cache in Room)

**Date**: Phase 1  
**Status**: Active — **CRITICAL**

**Decision**: Device media (videos, images) is always fetched live from MediaStore via ContentResolver. It is NEVER cached or duplicated in Room.

**Alternatives Considered**:
- Mirror the gallery to Room for faster access

**Rationale**:
- MediaStore is already an optimized, indexed database managed by the OS
- Mirroring creates stale data problems (files deleted from gallery still show in app)
- Room should store only app-owned data, not user-managed device content
- MediaStore is the authoritative source — if the file is deleted, our view should reflect that immediately

**Implementation**: `MediaItem` is a transient domain type. `MediaStoreDataSource` queries live. `Asset` in Room stores only a URI reference + app metadata.

---

## ADR-003: Room for App-Specific Data Only

**Date**: Phase 1  
**Status**: Active

**Decision**: Room stores only: Projects, Assets (URI references + app metadata), and future: editing history, templates, settings.

**Schema**:
- `projects`: id, name, resolution, frame_rate, timeline_json, timestamps
- `assets`: id, project_id, media_uri (reference), display_name, metadata, imported_at

**Rationale**: Clearly separates device data (MediaStore) from app data (Room), preventing architectural confusion as the codebase grows.

---

## ADR-004: Timeline JSON Serialization (Phase 1)

**Date**: Phase 1  
**Status**: Active — planned for migration

**Decision**: The `Timeline` model is serialized as a JSON string in the `projects.timeline_json` column.

**Alternatives Considered**:
- Fully normalized: separate `tracks`, `clips`, `effects` tables with foreign keys
- JSON blob (chosen)

**Rationale**:
- Timeline schema is evolving — premature normalization leads to costly migrations
- For Phase 1 where the editing feature is not yet implemented, JSON is pragmatic
- Gson provides reliable serialization with graceful error handling

**Migration Plan**: Phase 3 or when project saving goes live — introduce `Timeline` normalization with a proper Room `Migration`.

---

## ADR-005: Engine Interface Architecture

**Date**: Phase 1  
**Status**: Active

**Decision**: Define 7 engine interfaces in `core/domain/engine/` with no implementations in Phase 1.

**Engines**:
`MediaEngine`, `TimelineEngine`, `PreviewEngine`, `RenderEngine`, `ExportEngine`, `AudioEngine`, `EffectsEngine`

**Rationale**:
- Establishes contracts before implementation, forcing correct design thinking
- All ViewModels that will use these engines code against interfaces, not concrete classes
- Enables easy substitution of implementations (e.g., software → hardware renderer)
- Allows parallel development of different engine implementations

**Note on Android Types in Domain**: Engine interfaces may reference `android.net.Uri`, `android.graphics.Bitmap`, and `android.view.Surface`. This is a deliberate pragmatic choice — these are not framework classes but foundational media primitives. Abstracting them would add complexity with no practical testability benefit for an Android-only app.

---

## ADR-006: Kotlin KSP over KAPT

**Date**: Phase 1  
**Status**: Active

**Decision**: Use KSP (Kotlin Symbol Processing) instead of KAPT for annotation processing.

**Rationale**:
- KSP is 2x faster than KAPT (incremental compilation support)
- Both Hilt and Room fully support KSP
- KAPT is in maintenance mode — KSP is the future

---

## ADR-007: Coil for Image Loading

**Date**: Phase 1  
**Status**: Active

**Decision**: Use Coil 2.x for all image and video thumbnail loading.

**Alternatives Considered**: Glide, Picasso

**Rationale**:
- Kotlin-first, coroutines-native API
- Built-in support for `MediaStore` content URIs
- `coil-video` extension handles video frame extraction natively
- Excellent cache management out of the box

---

## ADR-008: Dark-First Theme

**Date**: Phase 1  
**Status**: Active

**Decision**: The app is dark-first. Both `themes.xml` and `themes.xml (night)` use the same dark color palette.

**Rationale**:
- Video editors are universally dark-themed (CapCut, Premiere, DaVinci Resolve)
- Dark backgrounds reduce eye strain during extended editing sessions
- Thumbnail previews and video content stand out against dark backgrounds
- Light theme support can be added in a future phase if user demand warrants it

---

## ADR-009: @Binds over @Provides for Interface Binding

**Date**: Phase 1  
**Status**: Active

**Decision**: Use `@Binds` (abstract class) in Hilt modules when binding an interface to its implementation.

**Rationale**:
- `@Binds` generates a direct delegation at compile time — zero runtime overhead
- `@Provides` creates an unnecessary factory method for simple bindings
- More idiomatic Hilt pattern

---

## ADR-010: StateFlow over LiveData

**Date**: Phase 1  
**Status**: Active

**Decision**: ViewModels expose `StateFlow<UiState>`, not `LiveData<UiState>`.

**Rationale**:
- `StateFlow` is Kotlin-native, not Android-specific
- `StateFlow` always has a value (no null initial state issues)
- Coroutines-based collection integrates with `repeatOnLifecycle`
- LiveData is effectively in maintenance mode

---

## ADR-011: AndroidX Media3 (ExoPlayer) for Preview Engine

**Date**: Phase 2  
**Status**: Active

**Decision**: Use `androidx.media3:media3-exoplayer` (version `1.6.1`) as the implementation of `PreviewEngine`. The implementation is in `ExoPlayerPreviewEngine` in the data layer.

**Alternatives Considered**:
- Legacy standalone ExoPlayer (`com.google.android.exoplayer2`)
- Custom MediaCodec + MediaSync pipeline
- Device's default `MediaPlayer`

**Rationale**:
- `androidx.media3` is Google's official successor to the legacy ExoPlayer library
- Integrates with `MediaSession` for future background playback support (Phase N)
- Provides `Player.Listener` for reliable playback state callbacks
- `MediaPlayer` API is too low-level and lacks the composition capabilities needed for Phase 3 multi-track
- MediaCodec is the right long-term choice (Phase N export) but too complex for Phase 2 preview

**Architecture Impact**:
- `ExoPlayerPreviewEngine` lives in `core/data/engine/` — no ExoPlayer imports leak to feature or domain layers
- `PreviewEngine` interface is the contract — feature ViewModels are fully decoupled from ExoPlayer
- `EngineModule` in `core/di/` binds the interface to the implementation
- Engine is `@Singleton` — single ExoPlayer instance reused across the app lifecycle

**Migration Path**: When multi-track composition is needed (Phase 3+), replace `ExoPlayerPreviewEngine` with a `MediaCodecPreviewEngine` behind the same `PreviewEngine` interface — zero changes required in feature layer.

---

## ADR-012: Navigation Safe Args for Type-Safe Fragment Arguments

**Date**: Phase 2  
**Status**: Active

**Decision**: Use `androidx.navigation.safeargs.kotlin` plugin to generate type-safe argument classes for fragment navigation.

**Rationale**:
- Eliminates runtime `Bundle` key string errors
- Generates `XxxFragmentArgs` (consumer) and `XxxFragmentDirections` (producer) at compile time
- Mandatory for `projectId: String` passed to `ProjectDetailFragment` — without Safe Args, a typo in the key string would only fail at runtime
- Consistent with the project's philosophy of catching errors at compile time

**Usage**:
- `ProjectDetailFragment` receives `projectId` via `navArgs()` delegate
- `HomeFragmentDirections.actionHomeFragmentToProjectDetailFragment(project.id)` is generated
- `MediaImportFragmentDirections.actionMediaImportFragmentToProjectDetailFragment(projectId)` is generated

---

## ADR-013: Drag Gesture Performance Optimization (Phase 3)

**Date**: Phase 3  
**Status**: Active

**Decision**: For highly interactive gestures (e.g. clip trimming, clip moving), the UI and preview engine are updated in real-time in memory (`isFinal = false`), but database updates (Room auto-save) and undo/redo history stack entries are deferred until the user releases the gesture (`isFinal = true` on `ACTION_UP` or `ACTION_CANCEL`).

**Rationale**:
- Storing to database and serialization/deserialization on every touch drag event (which triggers multiple times per millisecond) is extremely heavy and results in massive UI stutter and lag.
- Deferring the disk write to gesture completion maintains a smooth 60fps interaction during the scrub, while ensuring data integrity.
- Intermediate drag states do not pollute the undo/redo stack, so undoing a trim operation reverts the whole drag action in one step instead of multiple micro-frames.

---

## ADR-014: WorkManager for Background Export Execution

**Date**: Phase 3  
**Status**: Active

**Decision**: Integrate `androidx.work:work-runtime-ktx` to manage video export operations inside a WorkManager `CoroutineWorker` (`ExportWorker`).

**Alternatives Considered**:
- Run export inside ViewModel `viewModelScope` (status quo, process gets killed on backgrounding)
- Run export inside an Android Bound/Started Service

**Rationale**:
- Foreground Services are prone to strict OS limitations and require extensive boilerplate.
- WorkManager is the recommended Jetpack library for persistent work. It guarantees execution even if the app process is killed or the device restarts.
- By promoting the `ExportWorker` to a foreground service via `setForeground()`, it gets high-priority execution, survives backgrounding, and provides user progress updates via system notifications.
- Observable `WorkInfo` flows simplify tracking progress and handling cancellation directly from the ViewModel.

---

## ADR-015: Preview Composition Strategy

**Date**: Phase 3  
**Status**: Active

**Decision**: Implement a hybrid composition rendering strategy for player preview:
1. **Primary Video/Audio Tracks**: Rendered via Media3 ExoPlayer concatenating media sources.
2. **Visual Filters**: Rendered dynamically via a lightweight Android View overlay (`filterPreviewOverlay`) in the layout container on top of the player.
3. **Text, Overlays, and Adjustments**: Since dynamic real-time OpenGL overlay rendering on the player is highly complex and resource-heavy, rendering of Text, Image Overlays, and Adjustment track clips in the preview player is deferred. These features are fully rendered/composited on the final MP4 output during background video export using the OpenGL `GLFilterRenderer` and `MediaCodecExportEngine`.
4. **UI Gating**: Explicitly disable or grey out the UI plus/add buttons for Text, Overlay, Effect, and Adjustment tracks in this phase, showing a "coming soon" Toast.

**Alternatives Considered**:
- Real-time OpenGL composition on the preview surface (dismissed due to high implementation complexity and performance cost).
- Media3 Composition API (dismissed due to limited/unstable support for dynamic timeline changes like speed scaling and trim modifications).

**Rationale**:
- Ensures smooth 60fps video preview and scrub performance.
- Avoids premature rendering pipeline complexity.
- Keeps feature layer ViewModel fully decoupled.
