# AGENTS.md — AI Agent Rules for B&B Video Editor

This file contains **mandatory rules** that every AI agent working on this project must read and follow before making any changes.

---

## 0. Documentation Map (Read These First)

Before touching ANY code, read these documents in order:

| File | Purpose |
|---|---|
| `AGENTS.md` (this file) | Mandatory rules for all agents |
| `ARCHITECTURE.md` | Layer rules, package structure, code patterns |
| `DECISIONS.md` | Why each architectural decision was made |
| `ROADMAP.md` | Full implementation plan per phase with code specs |
| `UI_DESIGN_SYSTEM.md` | Colors, dimensions, layouts, component specs |
| `PHASE_3_IMPLEMENTATION_GUIDE.md` | Step-by-step Phase 3 guide |

---

## 1. Architecture First

- **Never sacrifice architecture for speed.** If a correct implementation takes more time, take that time.
- **Think before coding.** Understand the full impact of any change before writing a single line.
- **Every file must belong** to a clear layer: domain, data, feature, UI base, or utilities.
- **Read ARCHITECTURE.md and DECISIONS.md** before making any significant change.

---

## 2. Layer Rules (Strict)

### Domain Layer (`core/domain/`)
- **Zero Android SDK imports** except in engine interfaces (MediaEngine, PreviewEngine, etc.)
- Engine interfaces MAY reference `android.net.Uri`, `android.graphics.Bitmap`, `android.view.Surface` — these are platform-native media types, not framework dependencies
- **No Room, Hilt, or Retrofit** references — zero
- All business logic belongs here, in use cases
- Use cases are plain Kotlin classes: `class MyUseCase @Inject constructor(...) { operator fun invoke(...) }`

### Data Layer (`core/data/`)
- Android SDK allowed
- **Room is for app-owned data ONLY**: Projects, Assets, editing history tables
- **Do NOT cache MediaStore data in Room.** Device media is ALWAYS fetched live from MediaStore
- All MediaStore queries belong in `MediaStoreDataSourceImpl`
- Engine implementations (ExoPlayerPreviewEngine, etc.) live in `core/data/engine/`

### Feature Layer (`feature/`)
- Each feature: **exactly one Fragment**, **one ViewModel**, **one UiState sealed class**
- **No business logic in Fragments or Activities** — call a use case or ViewModel method
- ViewModels expose only `StateFlow<UiState>` — never expose MutableStateFlow
- Fragments use `PreviewEngine` interface — never import concrete engine classes

### UI Base Layer (`ui/`)
- `BaseFragment<VB>` must be used by ALL fragments — no exceptions
- `BaseViewModel` must be used by ALL ViewModels — no exceptions
- `_binding` is nulled by `BaseFragment.onDestroyView()` — never do it manually

---

## 3. Patterns to ALWAYS Follow

### AppResult<T>
```kotlin
// In repository or use case (any failable operation):
return try {
    AppResult.Success(result)
} catch (e: Exception) {
    Logger.e(TAG, "Operation failed", e)
    AppResult.Error(e)
}

// In ViewModel:
when (val result = useCase()) {
    is AppResult.Success -> _uiState.value = UiState.Content(result.data)
    is AppResult.Error -> _uiState.value = UiState.Error(result.exception.message ?: "Error")
}
```

### CoroutineDispatchers
```kotlin
// ALWAYS inject. NEVER use Dispatchers.IO, Dispatchers.Main directly.
class MyRepositoryImpl @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
) {
    suspend fun doWork() = withContext(dispatchers.io) { ... }
}
```

### Logger
```kotlin
private const val TAG = "MyClassName"
Logger.d(TAG, "Debug message")
Logger.e(TAG, "Error message", throwable)
Logger.w(TAG, "Warning message")
// NEVER: Log.d(), Log.e(), Log.w() — always use Logger
```

### Flow Collection in Fragments
```kotlin
// ALWAYS use repeatOnLifecycle(STARTED) — never CREATED or RESUMED
viewLifecycleOwner.lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            renderState(state)
        }
    }
}
```

### Navigation Events (one-shot)
```kotlin
// In ViewModel — use SharedFlow so it fires exactly once
private val _navigationEvent = MutableSharedFlow<NavigationEvent>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
)
val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()
```

### Room Migrations
```kotlin
// Every schema change MUST have a Migration class
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE projects ADD COLUMN thumbnailUri TEXT")
    }
}
// Add to AppDatabase.kt:
Room.databaseBuilder(...).addMigrations(MIGRATION_1_2).build()
// NEVER: .fallbackToDestructiveMigration() in production
```

---

## 4. Code Quality Non-Negotiables

| Rule | Example |
|---|---|
| No class > 300 lines without justification | Split into multiple classes |
| No `TODO` without GitHub issue | `// TODO: #123 - implement waveform` |
| No hardcoded strings in Kotlin | `getString(R.string.my_string)` |
| No hardcoded dimensions in Kotlin | `resources.getDimensionPixelSize(R.dimen.spacing_md)` |
| No `context.startActivity()` in ViewModel | Use NavigationEvent SharedFlow |
| All new files: package declaration + KDoc | Required at minimum |
| `@Suppress` only with comment explaining why | Document the suppression reason |

---

## 5. Dependency Rules

- **New dependencies require approval.** Document in `DECISIONS.md` with rationale before adding.
- All library versions live in `gradle/libs.versions.toml`. No inline version strings in `build.gradle.kts`.
- Use `ksp` not `kapt` for annotation processing (KSP is faster, modern).
- Do NOT add duplicate functionality: check existing dependencies before adding new ones.
- Media processing: prefer AndroidX/Jetpack over third-party (less APK bloat).

---

## 6. Testing Rules

- **Every new use case must have a unit test** in `app/src/test/`
- Use `UnconfinedTestDispatcher` with `runTest` from `kotlinx-coroutines-test`
- Use **Fakes** (hand-written implementations) not Mockito for repositories
- Fake classes: name them `Fake<ClassName>` (e.g., `FakeProjectRepository`)
- Place fakes in the same test package as the test class (not in `main/`)
- Unit tests must be fast (<100ms per test), isolated (no real DB/network)

```kotlin
// Example test structure:
@OptIn(ExperimentalCoroutinesApi::class)
class CreateProjectUseCaseTest {
    private lateinit var fakeRepository: FakeProjectRepository
    private lateinit var useCase: CreateProjectUseCase

    @Before fun setUp() {
        fakeRepository = FakeProjectRepository()
        useCase = CreateProjectUseCase(fakeRepository)
    }

    @Test fun `creates project with trimmed name`() = runTest(UnconfinedTestDispatcher()) {
        val result = useCase("  My Project  ")
        assertTrue(result is AppResult.Success)
        assertEquals("My Project", (result as AppResult.Success).data.name)
    }
}
```

---

## 7. Git Hygiene

- One feature per commit. Atomic, focused commits.
- Format: `[scope]: short description`
  - `[mediaimport]: add filter chip selection`
  - `[editor]: implement trim and split`
  - `[audio]: add waveform visualization`
- Never commit broken code to `main`
- Never commit generated Safe Args files (they are in `build/`, already gitignored)

---

## 8. AI Agent Workflow (MANDATORY — Follow This Order)

For every task, strictly follow this sequence:

1. **Read** `ARCHITECTURE.md`, `DECISIONS.md`, and the relevant phase guide
2. **Explore** the existing code in the affected area (`grep_search`, `view_file`)
3. **Explain** the implementation approach before writing any code
4. **Implement** only the approved scope — one step at a time
5. **Run** `./gradlew assembleDebug` to verify the build
6. **Run** `./gradlew test` to verify unit tests pass
7. **Summarize** all changes made (files created, modified, deleted)
8. **Stop** and wait for approval before continuing to the next phase

**Never automatically continue to the next feature or phase.**
**Never implement Phase N+1 features when implementing Phase N.**

---

## 9. Current Project State

```
Phase 1: ✅ Complete — Foundation, domain models, engine interfaces
Phase 2: ✅ Complete — Project creation, ExoPlayer preview, asset library
Phase 3: ⏳ Next — Timeline editing: trim, split, delete, undo/redo
Phase 4: ⏳ Future — Audio editing, waveforms
Phase 5: ⏳ Future — Text, overlays, filters
Phase 6: ⏳ Future — Export engine
Phase 7: ⏳ Future — OpenGL rendering, keyframes
```

The next agent should implement **Phase 3** by following `PHASE_3_IMPLEMENTATION_GUIDE.md`.

---

## 10. Visual Design Reference

The target UI is a professional mobile video editor inspired by CapCut:
- **Multi-track timeline**: Video, Audio, Text, Overlay, Effect, Adjustment tracks
- **Amber-orange primary** (#FFAF3F) + **electric purple secondary** (#A97CFF) on **near-black** (#0D0C0E) background
- **Track lane colors**: Video (surface), Audio (teal #00D4AA), Text (brown/orange), Overlay (purple), Effect (cyan), Adjustment (dark teal)
- **Playhead**: white 2dp vertical line with a circular handle at top
- **Clip thumbnails**: extracted video frames (MediaMetadataRetriever)
- **Waveform**: green bars for audio clips
- **Keyframe diamonds**: ◆ shape for adjustment track keyframes

See `UI_DESIGN_SYSTEM.md` for complete specs.
