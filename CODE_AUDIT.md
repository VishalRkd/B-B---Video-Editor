# B&B Video Editor — Core Stabilization Audit Log (Phase 0)

This document contains the ground-truth audit of the codebase, tracking structural issues, document contradictions, persistence safety, use cases, engine states, and test coverage gaps.

---

## 1. Engine Status Table

The domain layer specifies 7 engines in `core/domain/engine/`. Their implementation and wiring states are as follows:

| Engine | Interface Exists? | Implementation Exists? | Bound in EngineModule? | Status / Dead Code / Competing Pipelines |
|---|---|---|---|---|
| `PreviewEngine` | Yes | Yes (`ExoPlayerPreviewEngine.kt`) | Yes | **Active but Limited**: Backed by a Media3 ExoPlayer. Loops over primary video and audio tracks, but completely ignores `TEXT`, `OVERLAY`, `EFFECT`, and `ADJUSTMENT` tracks in preview. |
| `TimelineEngine` | Yes | Yes (`KotlinTimelineEngine.kt`) | Yes | **Partially Bypassed**: Implementations for core edits (trim, split, delete, move) are used, but insertion use cases (`AddVideoClip`, `AddAudioTrack`, `AddOverlayClip`, `AddTextClip`, etc.) bypass it completely. |
| `MediaEngine` | Yes | Yes (`AndroidMediaEngine.kt`) | Yes | **Dead Code**: The application never invokes `MediaEngine`. Instead, `ProjectDetailViewModel` initializes and uses `MediaMetadataRetriever` directly inside its own methods (layer violation). |
| `AudioEngine` | Yes | Yes (`AndroidAudioEngine.kt`) | Yes | **Active**: Used by `ExtractWaveformUseCase` for waveform extraction. |
| `EffectsEngine` | Yes | Yes (`AndroidEffectsEngine.kt`) | Yes | **Dead Code**: Never injected or called. Shaders are loaded directly by export renderers or ignored in preview. |
| `RenderEngine` | Yes | No | No | **Dead/Placeholder Code**: Interface defined but has zero implementations or bindings. |
| `ExportEngine` | Yes | Yes (`MediaCodecExportEngine.kt`) | Yes | **Active**: Called inside `ExportWorker` for offline media encoding. |

---

## 2. Use Case Status Table

Use cases reside in `core/domain/usecase/`. The following table maps existence and dependencies:

| Use Case Family | Class Name | Exists? | Bypasses TimelineEngine? | Description / Contract Matching |
|---|---|---|---|---|
| **project** | `CreateProjectUseCase` | Yes | N/A | Creates new project with empty timeline. Matches contract. |
| | `DeleteProjectUseCase` | Yes | N/A | Deletes project by ID. Matches contract. |
| | `GetAllProjectsUseCase` | Yes | N/A | Fetches project list from repository. Matches contract. |
| | `GetProjectByIdUseCase` | Yes | N/A | Fetches project by ID. Matches contract. |
| | `UpdateProjectUseCase` | Yes | N/A | Saves updated project. Matches contract. |
| **timeline** | `AddTransitionUseCase` | Yes | No | Inserts visual transition between clips. |
| | `AddVideoClipUseCase` | Yes | **Yes** | Direct list manipulations on tracks. |
| | `DeleteClipUseCase` | Yes | No | Delegates to `TimelineEngine.removeClip`. |
| | `MoveClipUseCase` | Yes | No | Delegates to `TimelineEngine.moveClip`. |
| | `MoveClipsUseCase` | Yes | No | Delegates to `TimelineEngine.moveClips`. |
| | `RemoveTransitionUseCase` | Yes | No | Delegates to `TimelineEngine.removeTransition`. |
| | `RippleTrimUseCase` | Yes | No | Delegates to `TimelineEngine`. |
| | `SetSpeedUseCase` | Yes | **Yes** | Mutates clip directly by copying fields. |
| | `SplitClipUseCase` | Yes | No | Delegates to `TimelineEngine.splitClip`. |
| | `TrimClipUseCase` | Yes | No | Delegates to `TimelineEngine.trimClip`. |
| **audio** | `AddAudioTrackUseCase` | Yes | **Yes** | Direct track clips list manipulation. |
| | `ExtractWaveformUseCase` | Yes | N/A | Waveform extraction from local audio. |
| | `SetClipVolumeUseCase` | Yes | **Yes** | Directly copies clip volume field. |
| **effects** | `AddAdjustmentClipUseCase`| Yes | **Yes** | Directly modifies adjustment track clips. |
| | `AddEffectClipUseCase` | Yes | **Yes** | Directly modifies effect track clips. |
| | `AddOverlayClipUseCase` | Yes | **Yes** | Directly modifies overlay track clips. |
| | `AddTextClipUseCase` | Yes | **Yes** | Directly modifies text track clips. |
| | `ApplyFilterUseCase` | Yes | **Yes** | Directly copies clip filter field. |
| **media** | `GetMediaItemByIdUseCase` | Yes | N/A | Live query from MediaStore datasource. |
| | `GetMediaItemsUseCase` | Yes | N/A | Live query from MediaStore datasource. |

---

## 3. Domain Model Reality Check

A review of the active fields in `Clip.kt`, `Track.kt`, `Timeline.kt`, and `Project.kt` shows:
- **`Clip`**: Correctly holds all required fields: `trimStartMs`, `trimEndMs`, `speedFactor`, `volume`, `textStyle` (nullable), `filter` (`VideoFilter`), and `animatableProperties` (keyframes list). No duplicate model exists.
- **`Track`**: Contains `isMuted`, `isLocked`, and `volume` fields.
- **`Timeline`**: Contains list of `tracks` and `transitions`.
- **`Project`**: Contains `resolution` (width, height), `frameRate`, `timeline`, and `thumbnailUri`.

---

## 4. Doc-Contradiction Resolutions

### 1. Competing timeline-mutation paths
- **Verification**: Confirmed. Four timeline use cases (`Trim`, `Split`, `Delete`, `Move`) call `TimelineEngine` methods. In contrast, all adding use cases (`AddVideoClip`, `AddAudioTrack`, `AddOverlayClip`, `AddTextClip`, etc.) and setting use cases (`SetSpeed`, `SetClipVolume`, `ApplyFilter`) bypass `TimelineEngine` and mutate the track list or clip objects directly.
- **Resolution Plan**: Move all insert, speed, volume, and filter mutations into the `TimelineEngine` contract. Clean up use cases to strictly delegate to the engine.

### 2. `EditingHistory.push()` specs
- **Verification**: Confirmed. The implementation in `EditingHistory.kt` (line 21) uses `.copy()`, which correctly preserves `maxSize`. However, the method signature includes `newState: Project` which is accepted but completely unused in the code block.
- **Resolution Plan**: Remove the unused `newState` parameter from the signature.

### 3. Stale selection after split/delete
- **Verification**: Confirmed. In `ProjectDetailViewModel.kt`, `splitAtPlayhead()` calls `applyEdit(result.data)` and then `selectClip(null)`. `applyEdit` emits `UiState` with the updated project (where the split clip ID has been replaced with two new IDs) while carrying the old selected clip ID. This causes a transient emission of an invalid state. A similar issue occurs in `deleteSelectedClip()`.
- **Resolution Plan**: Combine the state updates so that selection changes are atomic and emitted together with the project changes. After a split, automatically select the right-hand split clip.

### 4. Preview engine vs. timeline model mismatch
- **Verification**: Confirmed. `ExoPlayerPreviewEngine.kt` only reads the `primaryVideoTrack` and `AUDIO` track. It completely ignores `TEXT`, `OVERLAY`, `EFFECT`, and `ADJUSTMENT` tracks. As a result, overlays, text, and filters are never visible during playback.
- **Resolution Plan**: Refactor the preview engine to use a Media3 `Composition` or implement an OpenGL composition mechanism, documenting the choice in `DECISIONS.md`.

### 5. Timeline persistence fragility
- **Verification**: Confirmed. `ProjectEntity` saves the entire timeline as a JSON string. While `ProjectMapper.kt` registers `VideoFilterAdapter` to handle serialization of the `VideoFilter` sealed class, primitive fields added to `Track` (like `volume`) default to `0.0f` if missing in older JSON files, silently muting old projects.
- **Resolution Plan**: Update `sanitizeTimeline()` to handle default values for missing primitive fields (like track `volume` -> default `1.0f`) when deserializing old JSON.

---

## 5. Persistence/Migration Status

- **Real Database Version**: `1` in `AppDatabase.kt`.
- **Destructive fallback**: Destructive migrations are **NOT** enabled in `DatabaseModule.kt` (safe).
- **Schema Migration files**: No migration classes exist yet.
- **Vulnerabilities**: Gson deserializer will silently default missing fields to `false` or `0.0f` without warning, risking silent corruption when deserializing old JSON blobs containing newer database fields.

---

## 6. Four End-to-End Traces

### Trace 1: Clip Trim Handle Drag (Right trim handle)
1. **View**: User drags right trim handle.
2. **ViewModel**: UI triggers `trimSelectedClip(..., isFinal = false)`.
3. **Engine/UseCase**: Calls `trimClipUseCase` -> computes new timeline state -> updates `currentProject` in VM memory -> seeks player.
4. **Touch Release**: Triggers `trimSelectedClip(..., isFinal = isFinal = true)` -> pushes baseline project to history -> saves to Room -> reloads player -> seeks player.
5. **Issues**: Drag gesture depends on VM keeping `preEditProject` snapshot. It avoids UI updates during drag to prevent gesture listener resetting, but this creates brief timeline/ui misalignment.

### Trace 2: User taps "Split"
1. **View**: User taps Split button.
2. **ViewModel**: Calls `splitAtPlayhead()`.
3. **Engine/UseCase**: Calls `splitClipUseCase` -> delegates to `TimelineEngine.splitClip` (generates two new UUIDs) -> returns updated project.
4. **VM persistence**: Saves project to Room via `applyEdit()`.
5. **Selection update**: ViewModel clears selection using `selectClip(null)`.
6. **Issues**: Race condition between the project update emission (which contains the old clip ID) and selection clearing. The selection should be updated atomically with the split.

### Trace 3: User adds a text overlay
1. **View**: User taps "Add Text".
2. **ViewModel**: Calls `addDefaultTextAtPlayhead()`.
3. **UseCase**: Calls `addTextClipUseCase` -> Directly adds a text clip to the track -> saves to DB.
4. **Preview**: `PreviewEngine` reloads the timeline, but ignores the text track. No text is rendered.
5. **Issues**: Total break in visual preview.

### Trace 4: User taps "Export"
1. **View**: User taps export with configurations.
2. **ViewModel**: Calls `startExport(config)`.
3. **Worker**: Enqueues `ExportWorker` via WorkManager. Worker runs as a foreground service.
4. **Engine**: Worker delegates to `MediaCodecExportEngine.export()`.
5. **Transcoding**: Loops over video clips -> decodes to frames -> applies filter/overlays/text using OpenGL renderer -> encodes -> muxes mixed PCM audio track.
6. **Issues**: Export works and successfully composites text/overlays on disk, but this highlights a massive mismatch: the exported MP4 contains overlays that the user was never able to preview.

---

## 7. Test Coverage Gaps

- **Timeline Invariant Validation**: No tests exist since `TimelineValidator` is not yet implemented.
- **Import/Insert use cases**: Use cases mutating tracks directly (`AddVideoClipUseCase`, `AddAudioTrackUseCase`, `AddOverlayClipUseCase`, etc.) have zero or limited tests.
- **Migration Tests**: No tests verifying older JSON compatibility or schema migration logic.

---

## 8. Manual Testing Log

- **Create Project**: Works correctly.
- **Trim/Split Clip**: Functional, but selection is dropped, and UI flashes.
- **Audio Waveform**: Extracted waveform renders on track, but updates occasionally lag.
- **Text & Filters**: Do not show up in the player preview.
- **Export Video**: Generates valid MP4 with composited text and overlays.

---

## 9. Ranked List of Root Causes

1. **Root Cause 1: Incomplete Preview Rendering (PreviewEngine)**: Bypassing the text/overlay tracks in `ExoPlayerPreviewEngine` prevents all visual feedback of overlay edits.
2. **Root Cause 2: Split timeline-mutation paths**: Bypassing `TimelineEngine` in multiple use cases makes validation and consistency hard to enforce.
3. **Root Cause 3: Non-atomic UI state updates**: Emitting project state before resetting selected clip IDs causes transient invalid states.
4. **Root Cause 4: Silent Gson defaulting**: Deserializing missing JSON primitives defaults `volume` to `0.0f`, muting tracks on older projects.
