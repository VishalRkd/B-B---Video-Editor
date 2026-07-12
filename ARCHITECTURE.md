# ARCHITECTURE.md — B&B Video Editor
## Complete Architecture Reference for AI Agents

> **Read this entire file before touching any code.**
> Every rule here is mandatory. Violations will require refactoring.

---

## 1. Guiding Philosophy

B&B Video Editor is built to be a **commercial-grade, CapCut-level video editor** on Android.
The architecture is designed to scale from Phase 2 (basic preview) to Phase 10+ (AI features,
cloud collaboration, plugin marketplace) without major refactoring. Every decision prioritizes
long-term maintainability over short-term speed.

---

## 2. Layer Map

```
╔═══════════════════════════════════════════════════════════════════╗
║                    FEATURE LAYER (Android)                        ║
║  Fragment → ViewModel → UiState → adapter → layout               ║
║  (one feature = one Fragment + one ViewModel + one UiState)       ║
╠═══════════════════════════════════════════════════════════════════╣
║                    DOMAIN LAYER (Pure Kotlin)                     ║
║  Entities · Use Cases · Repository interfaces · Engine interfaces ║
║  ZERO Android SDK imports (except Uri, Bitmap, Surface)           ║
╠═══════════════════════════════════════════════════════════════════╣
║                    DATA LAYER (Android SDK)                       ║
║  Room DB · MediaStore · Repository implementations                ║
║  Engine implementations · Mappers · DataSources                   ║
╚═══════════════════════════════════════════════════════════════════╝
```

**Dependency rule (MANDATORY):**
```
Feature Layer  →  Domain Layer  ←  Data Layer
```
Domain has NO dependency on either Feature or Data.
Feature imports only interfaces defined in Domain — never concrete Data classes.

---

## 3. Complete Package Structure (as of Phase 2)

```
com.beatsandbeyond.video_editor/
│
├── BBVideoEditorApp.kt                    ← @HiltAndroidApp
├── MainActivity.kt                        ← Single-activity shell, NavHostFragment
│
├── core/
│   │
│   ├── common/
│   │   └── AppResult.kt                  ← sealed class AppResult<T>:
│   │                                          Success(data: T)
│   │                                          Error(exception: Throwable, message: String? = null)
│   │
│   ├── domain/
│   │   │
│   │   ├── model/                         ← Pure data. Zero Android SDK.
│   │   │   ├── Asset.kt                   ← Imported media ref (persisted in Room)
│   │   │   ├── AudioBuffer.kt             ← Raw PCM audio data for waveform
│   │   │   ├── Clip.kt                    ← Asset segment on a Track
│   │   │   ├── Effect.kt                  ← Non-destructive filter on a Clip
│   │   │   ├── ExportConfig.kt            ← Export settings (resolution, bitrate, fps)
│   │   │   ├── ExportProgress.kt          ← Progress value object
│   │   │   ├── MediaFilter.kt             ← Enum: ALL / VIDEO / IMAGE / AUDIO
│   │   │   ├── MediaItem.kt               ← Device media (from MediaStore, NEVER Room)
│   │   │   ├── MediaMetadata.kt           ← Video width/height/codec/bitrate
│   │   │   ├── MediaType.kt               ← Enum: VIDEO / IMAGE / AUDIO
│   │   │   ├── PlaybackState.kt           ← Sealed: Idle/Ready/Playing/Paused/Buffering/Ended/Error
│   │   │   ├── Project.kt                 ← Top-level editing container
│   │   │   ├── RenderFrame.kt             ← Single composited frame (for export)
│   │   │   ├── Resolution.kt              ← SD/HD/FHD/QHD/UHD_4K enum
│   │   │   ├── Timeline.kt                ← Ordered Track collection
│   │   │   ├── Track.kt                   ← Single editing lane
│   │   │   └── Transition.kt              ← Visual effect between Clips
│   │   │
│   │   ├── engine/                        ← Contracts. NO implementations here.
│   │   │   ├── AudioEngine.kt             ← Audio mixing, waveform data
│   │   │   ├── EffectsEngine.kt           ← GLSL filters, LUTs, color grading
│   │   │   ├── ExportEngine.kt            ← MediaCodec encoding
│   │   │   ├── MediaEngine.kt             ← Thumbnail + metadata extraction
│   │   │   ├── PreviewEngine.kt           ← Realtime playback (SurfaceView-based)
│   │   │   ├── RenderEngine.kt            ← Frame compositing
│   │   │   └── TimelineEngine.kt          ← Pure functional timeline operations
│   │   │
│   │   ├── repository/                    ← Interface contracts only
│   │   │   ├── AssetRepository.kt
│   │   │   ├── MediaRepository.kt
│   │   │   └── ProjectRepository.kt
│   │   │
│   │   └── usecase/                       ← One class = one operation
│   │       ├── asset/
│   │       │   ├── GetAllAssetsUseCase.kt
│   │       │   └── ImportAssetsUseCase.kt
│   │       ├── media/
│   │       │   ├── GetMediaItemByIdUseCase.kt
│   │       │   └── GetMediaItemsUseCase.kt
│   │       └── project/
│   │           ├── CreateProjectUseCase.kt
│   │           ├── DeleteProjectUseCase.kt
│   │           ├── GetAllProjectsUseCase.kt
│   │           └── GetProjectByIdUseCase.kt
│   │
│   ├── data/
│   │   ├── datasource/
│   │   │   ├── MediaStoreDataSource.kt           ← Interface
│   │   │   └── MediaStoreDataSourceImpl.kt       ← ContentResolver queries
│   │   │
│   │   ├── engine/
│   │   │   └── ExoPlayerPreviewEngine.kt         ← PreviewEngine via Media3
│   │   │
│   │   ├── local/db/
│   │   │   ├── AppDatabase.kt                    ← Room @Database
│   │   │   ├── dao/
│   │   │   │   ├── AssetDao.kt
│   │   │   │   └── ProjectDao.kt
│   │   │   ├── entity/
│   │   │   │   ├── AssetEntity.kt
│   │   │   │   └── ProjectEntity.kt
│   │   │   └── mapper/
│   │   │       ├── AssetMapper.kt
│   │   │       └── ProjectMapper.kt
│   │   │
│   │   └── repository/
│   │       ├── AssetRepositoryImpl.kt
│   │       ├── MediaRepositoryImpl.kt
│   │       └── ProjectRepositoryImpl.kt
│   │
│   ├── di/
│   │   ├── AppModule.kt
│   │   ├── DatabaseModule.kt
│   │   ├── EngineModule.kt
│   │   └── RepositoryModule.kt
│   │
│   └── utils/
│       ├── CoroutineDispatchers.kt
│       ├── FileUtils.kt
│       ├── Logger.kt
│       └── extensions/
│           ├── ContextExtensions.kt
│           └── FlowExtensions.kt
│
├── feature/
│   ├── assetlibrary/
│   │   ├── AssetLibraryFragment.kt
│   │   ├── AssetLibraryViewModel.kt
│   │   ├── adapter/AssetAdapter.kt
│   │   └── model/AssetLibraryUiState.kt
│   │
│   ├── home/
│   │   ├── HomeFragment.kt
│   │   ├── HomeViewModel.kt
│   │   ├── adapter/ProjectAdapter.kt
│   │   └── model/HomeUiState.kt
│   │
│   ├── mediaimport/
│   │   ├── MediaImportFragment.kt
│   │   ├── MediaImportViewModel.kt
│   │   ├── adapter/MediaItemAdapter.kt
│   │   └── model/MediaImportUiState.kt
│   │
│   └── projectdetail/              ← THE MAIN EDITOR SCREEN
│       ├── ProjectDetailFragment.kt
│       ├── ProjectDetailViewModel.kt
│       ├── adapter/TimelineClipAdapter.kt
│       └── model/ProjectDetailUiState.kt
│
└── ui/
    └── base/
        ├── BaseFragment.kt
        └── BaseViewModel.kt
```

---

## 4. Strict Layer Rules — NEVER VIOLATE THESE

### Domain Layer Rules
```kotlin
// ALLOWED in domain/model/
data class Clip(
    val id: String,
    val assetId: String,          // references Asset by ID — no Android type
    val timelinePositionMs: Long,
    val trimStartMs: Long,
    val trimEndMs: Long,
    val speedFactor: Float = 1.0f,
    val volume: Float = 1.0f,
    val effects: List<Effect> = emptyList(),
)

// ALLOWED in domain/engine/ — Uri/Bitmap/Surface are platform-native media types
interface PreviewEngine {
    suspend fun loadTimeline(timeline: Timeline)
    fun attach(surface: android.view.Surface)
    fun play()
    fun pause()
}

// FORBIDDEN in domain/
// import android.content.Context    NO
// import androidx.room.*            NO
// import dagger.hilt.*              NO
```

### Feature Layer Rules
```kotlin
// Correct ViewModel pattern
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val getProjectByIdUseCase: GetProjectByIdUseCase,
    private val previewEngine: PreviewEngine,   // interface only — never ExoPlayerPreviewEngine
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<ProjectDetailUiState>(ProjectDetailUiState.Loading)
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()
    // NEVER expose _uiState (mutable) directly
}

// Correct Fragment pattern
class ProjectDetailFragment : BaseFragment<FragmentProjectDetailBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {   // ALWAYS STARTED, never CREATED/RESUMED
                viewModel.uiState.collect { renderState(it) }
            }
        }
    }
    // _binding is nulled by BaseFragment.onDestroyView() — never do it manually
}
```

---

## 5. State Management Pattern

```
MediaStore/Room → Repository → Flow<AppResult<T>> → ViewModel → StateFlow<UiState> → Fragment
```

### UiState Pattern (MANDATORY — every feature)
```kotlin
// feature/projectdetail/model/ProjectDetailUiState.kt
sealed class ProjectDetailUiState {
    data object Loading : ProjectDetailUiState()
    data class Content(
        val project: Project,
        val assets: List<Asset>,
        val playbackState: PlaybackState,
        val currentPositionMs: Long,
        val totalDurationMs: Long,
        val selectedClipId: String? = null,   // null = no clip selected
    ) : ProjectDetailUiState()
    data class Error(val message: String) : ProjectDetailUiState()
}
```

### One-Shot Navigation Events (MANDATORY pattern)
```kotlin
// In ViewModel
sealed class NavigationEvent {
    data class ToProjectDetail(val projectId: String) : NavigationEvent()
    data object ToHome : NavigationEvent()
}
private val _navigationEvent = MutableSharedFlow<NavigationEvent>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
)
val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

// In Fragment
repeatOnLifecycle(Lifecycle.State.STARTED) {
    viewModel.navigationEvent.collect { event ->
        when (event) {
            is NavigationEvent.ToProjectDetail ->
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeFragmentToProjectDetailFragment(event.projectId)
                )
        }
    }
}
```

---

## 6. Engine Architecture (Seven Engines)

All engines are defined as interfaces in `core/domain/engine/`.
Implementations live ONLY in `core/data/engine/`.

| Engine | Phase | Implementation Tech |
|---|---|---|
| `PreviewEngine` | Phase 2 ✅ | Media3 ExoPlayer |
| `TimelineEngine` | Phase 3 | Pure Kotlin (immutable data structures) |
| `MediaEngine` | Phase 3 | MediaMetadataRetriever + ThumbnailUtils |
| `AudioEngine` | Phase 4 | MediaExtractor + AudioTrack |
| `EffectsEngine` | Phase 5-7 | GLSL shaders via OpenGL ES 3.0 |
| `RenderEngine` | Phase 7 | OpenGL ES 3.0 compositing |
| `ExportEngine` | Phase 6 | MediaCodec + MediaMuxer |

**RULE**: Add new engine bindings ONLY in `core/di/EngineModule.kt`.
The feature layer never changes when an engine implementation changes.

---

## 7. Database Schema

### Room Tables
```sql
CREATE TABLE projects (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL,
    timelineJson TEXT NOT NULL,     -- Gson-serialized Timeline
    resolution TEXT NOT NULL,       -- Resolution.name()
    frameRate REAL NOT NULL,
    thumbnailUri TEXT               -- nullable; generated in Phase 3
);

CREATE TABLE assets (
    id TEXT PRIMARY KEY NOT NULL,
    projectId TEXT,                 -- nullable FK to projects.id
    mediaUri TEXT NOT NULL,         -- content://... from MediaStore
    mediaType TEXT NOT NULL,        -- MediaType.name()
    displayName TEXT NOT NULL,
    durationMs INTEGER NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    sizeBytes INTEGER NOT NULL,
    importedAt INTEGER NOT NULL,
    isFavorite INTEGER NOT NULL,
    customName TEXT,
    FOREIGN KEY (projectId) REFERENCES projects(id) ON DELETE CASCADE
);
```

### Migration Rules (MANDATORY)
- Every schema change = new `Migration` in `core/data/local/db/migration/`
- Class name: `MigrationXtoY` where X and Y are old/new version numbers
- Add to `AppDatabase.kt` in `.addMigrations(MigrationXtoY)`
- NEVER use `fallbackToDestructiveMigration()` in production builds

---

## 8. The Editor Screen — Visual Reference

Reference image shows a CapCut-style professional editor. Our target UI:

```
┌────────────────────────────────────────┐
│  [←] [↩] [↪]    [1080P▼]  [EXPORT]  [⋮]  ← Toolbar
├────────────────────────────────────────┤
│                                         │
│         VIDEO PREVIEW AREA              │
│         (SurfaceView, 16:9 ratio)       │  ← Black background
│                                         │
│  [▶] 00:06 / 00:24             [⛶]     │  ← Overlay: pos/time + fullscreen
├────────────────────────────────────────┤
│  [+] [⏮] [⏪] [▶/⏸] [⏩] [↻] [⟷] [⛶] │  ← Playback controls
├────────────────────────────────────────┤
│ 00:00   00:04  |00:06|  00:08   00:12  │  ← Timeline ruler (scrollable)
│                  🔴                    │  ← Playhead (white line)
├─────────┬──────────────────────────────┤
│🎬 Video  │ [thumbnail][thumbnail][+]   │  ← Video track (thumbnail strip)
│eye🔒     │                             │
├─────────┼──────────────────────────────┤
│🎵 Audio  │ [~~waveform~~Adventure.mp3]│  ← Audio track (waveform vis)
│eye🔒     │                             │
├─────────┼──────────────────────────────┤
│𝐓 Text   │ [EXPLORE MORE]      [+]     │  ← Text track
│eye🔒     │                             │
├─────────┼──────────────────────────────┤
│📷 Overlay│ [Mountains.png]     [+]     │  ← Overlay track (PiP)
│eye🔒     │                             │
├─────────┼──────────────────────────────┤
│✨ Effect  │ [Glow]              [+]     │  ← Effects track
│eye🔒     │                             │
├─────────┼──────────────────────────────┤
│🎨 Adjust │ [Color Grading ◆──◆──◆]   │  ← Adjustment track (keyframes)
│eye🔒     │                             │
├────────────────────────────────────────┤
│[Split][Trim][Speed][Volume][Filter][🗑]│  ← Context-sensitive bottom toolbar
└────────────────────────────────────────┘
```

Track left panel (label column):
- Track icon + type name
- Eye icon = visibility toggle
- Lock icon = lock/unlock track editing

Track right area (clip lane):
- Clip represented as colored rectangle
- Video clips: thumbnail strip (extracted frames)
- Audio clips: waveform visualization (green)
- Text clips: preview of text content (brown/orange)
- Overlay clips: thumbnail (purple)
- Effect clips: effect name + icon (teal)
- Adjustment clips: keyframe diamonds on a line (dark teal)

---

## 9. Key Utilities — Always Use These

### AppResult<T>
```kotlin
// In repository / use case:
return try {
    AppResult.Success(dao.getById(id) ?: throw NoSuchElementException("Not found"))
} catch (e: Exception) {
    AppResult.Error(e)
}

// In ViewModel:
when (val result = useCase()) {
    is AppResult.Success -> _uiState.value = UiState.Content(result.data)
    is AppResult.Error   -> _uiState.value = UiState.Error(result.exception.message ?: "Error")
}
```

### CoroutineDispatchers
```kotlin
// ALWAYS inject. NEVER hardcode Dispatchers.IO.
class AssetRepositoryImpl @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
) : AssetRepository {
    override suspend fun getAll() = withContext(dispatchers.io) { dao.getAll() }
}
```

### Logger
```kotlin
private const val TAG = "ClassName"
Logger.d(TAG, "message")
Logger.e(TAG, "error message", throwable)
Logger.w(TAG, "warning")
// NEVER: Log.d(TAG, "...") — direct android.util.Log is forbidden
```

---

## 10. Code Quality Requirements (Non-Negotiable)

| Rule | Why |
|---|---|
| No class > 300 lines without justification | God class anti-pattern |
| No hardcoded strings in Kotlin | Use `getString(R.string.key)` |
| No hardcoded dimensions in Kotlin | Use `resources.getDimensionPixelSize(R.dimen.key)` |
| No direct `Dispatchers.IO` | Inject `CoroutineDispatchers` for testability |
| No `Log.d/e/w` directly | Use `Logger` wrapper (respects BuildConfig.DEBUG) |
| No `TODO` without GitHub issue | Technical debt must be tracked |
| All new files: package declaration + KDoc | Required at minimum |
| Room migrations for every schema change | Never destroy user data |
| `repeatOnLifecycle(STARTED)` in Fragments | Prevents lifecycle leaks |
| Tests use `FakeRepository` not Mockito | Fakes are more maintainable |
