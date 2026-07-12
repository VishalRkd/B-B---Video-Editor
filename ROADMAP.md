# ROADMAP.md — B&B Video Editor
## Complete Implementation Guide for AI Agents

> This document is the **master build plan**. Each phase is self-contained.
> An AI agent should be able to read ONE phase section and implement it completely.
> Never skip phases. Never implement features from a future phase in an earlier one.

---

## Status Legend
- ✅ Complete
- 🔄 In Progress
- ⏳ Planned
- 💡 Future Consideration

---

## Phase 1 — Foundation ✅

**Goal**: Establish a scalable, production-ready foundation.
**Result**: Clean Architecture skeleton, domain models, engine interfaces, MediaStore browsing.

### Implemented
- ✅ Clean Architecture (Domain / Data / Feature layers)
- ✅ MVVM with StateFlow
- ✅ Hilt Dependency Injection
- ✅ Room Database (projects + assets tables)
- ✅ Navigation Component with slide animations
- ✅ Core domain models: Project, Asset, Timeline, Track, Clip, Transition, Effect
- ✅ 7 Engine interfaces (contracts only, no implementations)
- ✅ MediaStore data source (live queries, no Room caching)
- ✅ BaseFragment (ViewBinding lifecycle)
- ✅ BaseViewModel (safe coroutine launcher)
- ✅ AppResult<T> (unified result type)
- ✅ CoroutineDispatchers (injectable)
- ✅ Logger (centralized)
- ✅ Material3 dark theme (amber-orange #FFAF3F + electric purple #A97CFF)
- ✅ Media Import screen (browse, multi-select, filter chips, permission handling)
- ✅ Documentation: AGENTS.md, ARCHITECTURE.md, ROADMAP.md, DECISIONS.md

---

## Phase 2 — Project Management & Basic Preview ✅

**Goal**: Users can create projects, see a preview, and have their work saved.

### Implemented
- ✅ Project creation: MediaImport FAB → CreateProjectUseCase → ImportAssetsUseCase → Room
- ✅ Home screen: 2-column project grid (RecyclerView + DiffUtil)
- ✅ Project delete: long-press → AlertDialog → DeleteProjectUseCase
- ✅ Project detail screen (ProjectDetailFragment + ProjectDetailViewModel)
- ✅ ExoPlayer (Media3 1.6.1) via ExoPlayerPreviewEngine
- ✅ SurfaceView video preview + play/pause button
- ✅ Basic timeline clip strip (horizontal RecyclerView, read-only)
- ✅ Asset Library screen (all imported assets, grid view)
- ✅ Navigation Safe Args (type-safe projectId passing)
- ✅ AssetRepository interface + impl
- ✅ GetAllProjectsUseCase, GetProjectByIdUseCase, DeleteProjectUseCase
- ✅ ImportAssetsUseCase, GetAllAssetsUseCase
- ✅ EngineModule (Hilt @Binds for PreviewEngine)
- ✅ Unit tests: CreateProjectUseCaseTest (6 tests, FakeProjectRepository)
- ✅ ADR-011 (Media3), ADR-012 (Safe Args)

---

## Phase 3 — Core Editing Engine ⏳

**Goal**: Users can trim, split, delete, reorder clips, and scrub the playhead.
**This phase makes the app feel like a real video editor.**

### Visual Reference
The editor screen gains:
- A **scrollable timeline ruler** with time codes
- A draggable **white playhead line**
- **Trim handles** (drag left/right edges of a clip to trim)
- **Split** at playhead position
- **Reorder** (drag clip horizontally to new position)
- **Undo/Redo** buttons in toolbar (↩ ↪)
- **Playback seeking**: tap timeline ruler to jump to that time

### Files to Create

#### Domain Layer (create these first)

**`core/domain/usecase/project/UpdateProjectUseCase.kt`**
```kotlin
// Saves updated timeline state to Room
class UpdateProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
) {
    suspend operator fun invoke(project: Project): AppResult<Project> =
        projectRepository.updateProject(project)
}
```

**`core/domain/usecase/timeline/TrimClipUseCase.kt`**
```kotlin
// Pure function: returns new Project with updated Clip trim points
// Input: project, clipId, newTrimStartMs, newTrimEndMs
// Output: AppResult<Project> with the trimmed clip
// Validation: newTrimStartMs >= 0, newTrimEndMs > newTrimStartMs,
//             newTrimEndMs <= asset.durationMs
class TrimClipUseCase @Inject constructor() {
    operator fun invoke(
        project: Project,
        clipId: String,
        newTrimStartMs: Long,
        newTrimEndMs: Long,
    ): AppResult<Project> { ... }
}
```

**`core/domain/usecase/timeline/SplitClipUseCase.kt`**
```kotlin
// Splits clip at playhead position into two clips
// Input: project, clipId, splitPositionMs (timeline-absolute position)
// Output: AppResult<Project> with the clip replaced by two clips
class SplitClipUseCase @Inject constructor() {
    operator fun invoke(
        project: Project,
        clipId: String,
        splitAtTimelineMs: Long,
    ): AppResult<Project> { ... }
}
```

**`core/domain/usecase/timeline/DeleteClipUseCase.kt`**
```kotlin
class DeleteClipUseCase @Inject constructor() {
    operator fun invoke(project: Project, clipId: String): AppResult<Project> { ... }
}
```

**`core/domain/usecase/timeline/MoveClipUseCase.kt`**
```kotlin
// Reorders a clip within its track
class MoveClipUseCase @Inject constructor() {
    operator fun invoke(
        project: Project,
        clipId: String,
        newTimelinePositionMs: Long,
    ): AppResult<Project> { ... }
}
```

**`core/domain/usecase/timeline/SetSpeedUseCase.kt`**
```kotlin
class SetSpeedUseCase @Inject constructor() {
    operator fun invoke(
        project: Project,
        clipId: String,
        speedFactor: Float,   // 0.25f to 4.0f
    ): AppResult<Project> { ... }
}
```

#### Timeline Engine Implementation

**`core/data/engine/KotlinTimelineEngine.kt`**
```kotlin
// Pure Kotlin implementation of TimelineEngine
// No Android SDK imports — only pure logic
// Uses immutable Project state: returns new Project, never mutates
@Singleton
class KotlinTimelineEngine @Inject constructor() : TimelineEngine {
    override fun trimClip(project: Project, clipId: String, start: Long, end: Long): Project { ... }
    override fun splitClip(project: Project, clipId: String, positionMs: Long): Project { ... }
    override fun deleteClip(project: Project, clipId: String): Project { ... }
    override fun moveClip(project: Project, clipId: String, newPositionMs: Long): Project { ... }
}
```

#### Undo/Redo History System

**`core/domain/model/EditingHistory.kt`**
```kotlin
// Immutable stack-based history
data class EditingHistory(
    val past: List<Project> = emptyList(),     // stack of previous states
    val future: List<Project> = emptyList(),   // stack of undone states
    val maxSize: Int = 50,                     // max 50 undo steps
) {
    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()

    fun push(current: Project, newState: Project): EditingHistory = EditingHistory(
        past = (past + current).takeLast(maxSize),
        future = emptyList(),    // any new action clears redo stack
    )

    fun undo(current: Project): Pair<EditingHistory, Project>? {
        if (!canUndo) return null
        return Pair(
            EditingHistory(past = past.dropLast(1), future = listOf(current) + future),
            past.last()
        )
    }

    fun redo(current: Project): Pair<EditingHistory, Project>? {
        if (!canRedo) return null
        return Pair(
            EditingHistory(past = past + current, future = future.drop(1)),
            future.first()
        )
    }
}
```

#### MediaEngine Implementation (Thumbnail Generation)

**`core/data/engine/AndroidMediaEngine.kt`**
```kotlin
// Phase 3: generates clip thumbnails and extracts metadata
// Uses MediaMetadataRetriever for video frames
// Writes thumbnails to cache/thumbnails/{clipId}_{timeMs}.webp
class AndroidMediaEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatchers,
) : MediaEngine {
    override suspend fun extractThumbnail(
        uri: Uri,
        timeMs: Long,
        width: Int,
        height: Int,
    ): Bitmap? = withContext(dispatchers.io) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.getScaledFrameAtTime(
                timeMs * 1000L,                        // microseconds
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                width, height
            )
        } finally {
            retriever.release()
        }
    }
}
```

#### UI: ProjectDetailFragment Updates

**Add to `ProjectDetailFragment.kt`:**
- Timeline ruler view (custom `TimelineRulerView` extends View)
- Playhead indicator (vertical line drawn over timeline)
- Trim handles on selected clip (two draggable thumb views)
- Bottom context toolbar: Split, Trim, Speed, Delete buttons
- SeekBar or custom scrubber connected to PreviewEngine.seekTo()

**New layout files:**
- `view_timeline_ruler.xml` — custom ruler with time labels
- `view_trim_handle.xml` — left/right trim thumb
- `layout/partial_editor_bottom_toolbar.xml` — Split/Trim/Speed/Volume/Delete

#### UI: ProjectDetailUiState Updates
```kotlin
data class Content(
    val project: Project,
    val assets: Map<String, Asset>,   // keyed by assetId for O(1) lookup
    val playbackState: PlaybackState,
    val currentPositionMs: Long,
    val totalDurationMs: Long,
    val selectedClipId: String? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val editingHistory: EditingHistory = EditingHistory(),
) : ProjectDetailUiState()
```

### Tests to Write
- `TrimClipUseCaseTest` — valid trim, trim start > end error, trim beyond duration error
- `SplitClipUseCaseTest` — split at middle, split at start (error), split at end (error)
- `DeleteClipUseCaseTest` — delete existing clip, delete non-existent clip (error)
- `EditingHistoryTest` — push, undo, redo, max size clamping

### Dependencies to Add
None. Phase 3 uses only existing dependencies.

---

## Phase 4 — Audio Editing ⏳

**Goal**: Users can add background music, control volume, see waveforms.

### Visual Reference
- Audio track shows **green waveform visualization** of the audio file
- Track header shows: 🎵 icon + "Audio" label + eye + lock
- Clip shows audio file name + waveform
- Bottom toolbar adds: **Volume** slider control

### Files to Create

#### Domain

**`core/domain/model/AudioWaveform.kt`**
```kotlin
// Represents normalized waveform amplitude data
data class AudioWaveform(
    val samples: FloatArray,      // values 0.0f to 1.0f, one per pixel column
    val durationMs: Long,
)
```

**`core/domain/usecase/audio/ExtractWaveformUseCase.kt`**
```kotlin
// Extracts PCM samples from audio file using MediaExtractor + AudioDecoder
// Returns AudioWaveform with normalized samples
// Cache result in memory (LruCache) and disk (cache/waveforms/{assetId}.bin)
class ExtractWaveformUseCase @Inject constructor(
    private val audioEngine: AudioEngine,
) {
    suspend operator fun invoke(assetUri: String, widthPx: Int): AppResult<AudioWaveform>
}
```

**`core/domain/usecase/audio/AddAudioTrackUseCase.kt`**
```kotlin
// Adds a new AUDIO Track to the project timeline
class AddAudioTrackUseCase @Inject constructor() {
    operator fun invoke(project: Project, asset: Asset): AppResult<Project>
}
```

**`core/domain/usecase/audio/SetClipVolumeUseCase.kt`**
```kotlin
class SetClipVolumeUseCase @Inject constructor() {
    operator fun invoke(project: Project, clipId: String, volume: Float): AppResult<Project>
    // volume: 0.0f (silent) to 2.0f (200% boost)
}
```

#### Data: AudioEngine Implementation

**`core/data/engine/MediaCodecAudioEngine.kt`**
```kotlin
// Uses MediaExtractor to read audio frames
// Decodes PCM samples using MediaCodec (or AudioRecord for mic)
// Downsample to widthPx samples for waveform display
class MediaCodecAudioEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatchers,
) : AudioEngine { ... }
```

#### UI: Waveform View

**`ui/components/WaveformView.kt`** (custom View)
```kotlin
// Custom View that draws normalized FloatArray samples as a waveform
// Color: #00D4AA (teal / color_tertiary)
// Renders using Canvas.drawLine() for each sample column
// Must handle: setWaveform(AudioWaveform), scroll offset, zoom level
class WaveformView @JvmOverloads constructor(ctx: Context, ...) : View(ctx, ...) {
    fun setWaveform(waveform: AudioWaveform) { ... }
    fun setScrollOffsetMs(offsetMs: Long) { ... }  // synchronized with timeline scroll
    fun setZoomFactor(zoom: Float) { ... }
}
```

**`layout/item_audio_clip.xml`** — replaces the generic clip item for audio tracks:
- Background: #00D4AA (teal, semi-transparent)
- WaveformView fills the clip width
- Clip name text at bottom

### Tests to Write
- `ExtractWaveformUseCaseTest` — mock AudioEngine, verify sample normalization
- `AddAudioTrackUseCaseTest` — verify track added, assetId correct
- `SetClipVolumeUseCaseTest` — valid volume, clamp out-of-range

---

## Phase 5 — Text, Overlays & Filters ⏳

**Goal**: Users can add text, image overlays, and apply visual filters.

### Visual Reference
- Text track: brown/orange clip showing text preview
- Overlay track: purple clip showing image thumbnail
- Bottom toolbar adds: **Filter** button (opens filter picker bottom sheet)

### Text Overlay

**`core/domain/model/TextStyle.kt`**
```kotlin
data class TextStyle(
    val text: String,
    val fontFamily: String = "sans-serif",
    val fontSize: Float = 32f,
    val color: Int = 0xFFFFFFFF.toInt(),         // ARGB
    val backgroundColor: Int = 0x00000000,        // transparent
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val alignment: TextAlignment = TextAlignment.CENTER,
    val positionX: Float = 0.5f,                  // normalized 0-1
    val positionY: Float = 0.5f,                  // normalized 0-1
)
```

**`core/domain/model/Clip.kt` — Add optional textStyle**
```kotlin
data class Clip(
    val id: String,
    val assetId: String,          // null for text clips
    val textStyle: TextStyle? = null,   // non-null for TEXT track clips
    val timelinePositionMs: Long,
    val trimStartMs: Long,
    val trimEndMs: Long,
    ...
)
```

### Filter System

**`core/domain/model/VideoFilter.kt`**
```kotlin
sealed class VideoFilter {
    data object None : VideoFilter()
    data class Brightness(val value: Float) : VideoFilter()  // -1.0 to 1.0
    data class Contrast(val value: Float) : VideoFilter()    // 0.0 to 2.0
    data class Saturation(val value: Float) : VideoFilter()  // 0.0 to 2.0
    data class LUT(val lutPath: String) : VideoFilter()       // .cube file path
    data class Vintage(val intensity: Float) : VideoFilter()  // 0.0 to 1.0
    data class Vignette(val radius: Float) : VideoFilter()
}
```

**`core/domain/usecase/effects/ApplyFilterUseCase.kt`**
```kotlin
class ApplyFilterUseCase @Inject constructor() {
    operator fun invoke(
        project: Project,
        clipId: String,
        filter: VideoFilter,
    ): AppResult<Project>
}
```

### Bottom Sheet Filter Picker

**`feature/projectdetail/bottomsheet/FilterPickerBottomSheet.kt`**
- `BottomSheetDialogFragment`
- Horizontal RecyclerView of filter presets
- Each item: before/after thumbnail preview + filter name
- Selected filter gets a highlight border

### Layout: Filter Picker Item
**`layout/item_filter_preset.xml`**
- Square thumbnail (from video first frame with filter applied)
- Filter name below thumbnail
- Selected state: primary color border

---

## Phase 6 — Export Engine ⏳

**Goal**: Users can export the edited video to device gallery.

### Export Flow
```
User taps Export
    → ExportConfig dialog (resolution, quality, fps)
    → ExportViewModel.startExport()
        → ExportEngine.export(project, config)
            → MediaCodec (video encoder) + MediaMuxer
            → ExportEngine emits Flow<ExportProgress>
    → Progress notification (WorkManager for background)
    → On completion: MediaStore.insert() to save to gallery
    → "Export complete" snackbar with "Share" action
```

### ExportConfig Dialog

**`feature/export/ExportFragment.kt`** (full-screen modal or dialog)
- Resolution selector: 480p / 720p / 1080p / 4K
- Quality: Low / Medium / High / Lossless
- Frame rate: 24fps / 30fps / 60fps
- Estimated file size display
- Export button with progress bar

### ExportEngine Interface (already defined in Phase 1)
```kotlin
interface ExportEngine {
    fun export(project: Project, config: ExportConfig): Flow<ExportProgress>
    fun cancel()
}
```

### MediaCodecExportEngine Implementation

**`core/data/engine/MediaCodecExportEngine.kt`**
```kotlin
// Step 1: Create MediaCodec video encoder (H.264 or H.265)
// Step 2: For each frame at target FPS:
//           a. Load source frame from MediaMetadataRetriever
//           b. Apply effects (EffectsEngine.applyToFrame())
//           c. Encode via MediaCodec
//           d. Mux with MediaMuxer
// Step 3: If audio: MediaExtractor → MediaCodec decode → AAC encode
// Step 4: MediaMuxer.writeSampleData() for both video + audio
// Step 5: MediaMuxer.stop() → file written to private storage
// Step 6: MediaStore.insert() → visible in gallery
```

**`core/data/local/worker/ExportWorker.kt`** (WorkManager)
```kotlin
// Ensures export continues if app goes to background
// Shows foreground notification with progress
// On success: shows "Export complete" notification
// On failure: shows error notification with retry action
class ExportWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result { ... }
}
```

### Tests to Write
- `ExportConfigTest` — bitrate calculation, file size estimation
- `ExportWorkerTest` (instrumented) — worker completes successfully

---

## Phase 7 — Advanced Visual Features (OpenGL) ⏳

**Goal**: Professional-grade visual effects via OpenGL ES render pipeline.

### OpenGL Render Pipeline

**`core/data/engine/GlRenderEngine.kt`**
```kotlin
// EGL context creation → GLSurfaceView or SurfaceTexture
// Per-frame compositing:
//   Layer 1: Video frame (OES texture from SurfaceTexture)
//   Layer 2: Overlay images (GL_TEXTURE_2D)
//   Layer 3: Text (rendered to Bitmap → GL_TEXTURE_2D)
//   Layer 4: Effects GLSL fragment shader
//   Layer 5: Final composite → output Surface
class GlRenderEngine @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
) : RenderEngine { ... }
```

### GLSL Shaders

Store shaders in `assets/shaders/`:
- `base.vert` — vertex shader (pass-through UV)
- `base.frag` — fragment shader (sample OES texture)
- `brightness.frag` — brightness/contrast adjustment
- `saturation.frag` — HSL saturation
- `vintage.frag` — vintage color grade
- `vignette.frag` — radial vignette
- `lut.frag` — 3D LUT sampling (512×512 strip LUT texture)

### Keyframe Animation System

**`core/domain/model/Keyframe.kt`**
```kotlin
data class Keyframe<T>(
    val timeMs: Long,             // position on the clip timeline
    val value: T,                 // the animated value at this keyframe
    val interpolation: Interpolation = Interpolation.LINEAR,
)

enum class Interpolation { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT, HOLD }
```

**`core/domain/model/AnimatableProperty.kt`**
```kotlin
sealed class AnimatableProperty {
    data class Opacity(val keyframes: List<Keyframe<Float>>) : AnimatableProperty()
    data class Scale(val keyframes: List<Keyframe<Float>>) : AnimatableProperty()
    data class PositionX(val keyframes: List<Keyframe<Float>>) : AnimatableProperty()
    data class PositionY(val keyframes: List<Keyframe<Float>>) : AnimatableProperty()
    data class Rotation(val keyframes: List<Keyframe<Float>>) : AnimatableProperty()
}
```

**Adjustment Track Clip** (shown in reference image as "Color Grading ◆──◆──◆"):
- `Clip.animatableProperties: List<AnimatableProperty>` holds keyframes
- UI: diamonds (◆) drawn at keyframe positions on the timeline
- User taps a diamond → edits the value at that keyframe

---

## Phase 8 — Stickers, Templates & Advanced Overlays ⏳

- Sticker library (animated WebP + static PNG)
- Project templates (pre-built Timeline JSON)
- Green screen / chroma key (GLSL shader)
- PiP (picture-in-picture overlay with transform handles)

---

## Phase 9 — Performance & Background Processing ⏳

- Background export with WorkManager (foreground service)
- Export survives process death (WorkManager persistence)
- Render caching: pre-render timeline frames to disk
- Low-memory mode: reduce thumbnail cache size
- ProGuard/R8 optimization rules for production APK

---

## Phase 10 — AI Features 💡

- On-device auto-caption (Android SpeechRecognizer or ML Kit)
- Smart trim: silence detection + cut suggestions
- Auto beat sync: audio onset detection + clip boundary alignment
- Scene detection: visual change detection for automatic cuts
- AI color grading: ML model suggests color grade presets

---

## Phase 11 — Plugin Architecture 💡

- Plugin API: `EditorPlugin` interface (loadEffect, loadTransition)
- Plugin manifest: plugin.json describing capabilities
- Plugin host: loads plugins from signed APKs via PackageManager
- Effect marketplace: download and install plugin APKs

---

## Phase 12 — Cloud & Collaboration 💡

- Cloud project sync (Firebase Firestore or custom backend)
- Project export to cloud storage (Firebase Storage)
- Shared project links (read-only preview)
- Real-time collaborative editing (CRDT-based conflict resolution)

---

## Guiding Principles (Applies to ALL Phases)

1. **Never skip architecture for speed** — a shortcut now costs 10x later
2. **Immutable state everywhere** — Timeline operations return NEW objects
3. **Test use cases first** — domain logic must be tested before UI is built
4. **Engine interfaces are sacred** — feature layer NEVER imports data layer
5. **One feature per commit** — atomic, focused commits only
6. **Document decisions** — every major choice goes in DECISIONS.md
7. **No hardcoded anything** — strings in strings.xml, dims in dimens.xml
8. **Performance budget**: editor timeline must render at 60fps on mid-range devices
