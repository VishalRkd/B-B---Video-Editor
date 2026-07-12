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
