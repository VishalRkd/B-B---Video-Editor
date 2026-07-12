# ARCHITECTURE.md — B&B Video Editor

## Overview

B&B Video Editor uses **Clean Architecture** with an **MVVM presentation layer**, organized within a single Gradle module. This document describes every layer, its responsibilities, and the rules governing it.

---

## Layer Map

```
┌─────────────────────────────────────────────────────────┐
│                     UI Layer (Android)                   │
│    Activities · Fragments · ViewModels · UiState         │
├─────────────────────────────────────────────────────────┤
│               Domain Layer (Pure Kotlin)                 │
│    Entities · Use Cases · Repository Interfaces          │
│    Engine Interfaces                                     │
├─────────────────────────────────────────────────────────┤
│               Data Layer (Android SDK)                   │
│    Room DB · MediaStore · Repository Implementations     │
│    Data Sources · Mappers                                │
└─────────────────────────────────────────────────────────┘
```

**Dependency Rule**: Inner layers know nothing about outer layers.

```
UI → Domain ← Data
```

Domain has NO dependency on Data or UI.

---

## Package Structure

```
com.beatsandbeyond.video_editor/
├── BBVideoEditorApp.kt            ← Hilt @HiltAndroidApp
├── MainActivity.kt                ← Single Activity (navigation shell only)
│
├── core/
│   ├── common/
│   │   └── AppResult.kt           ← Shared result type (Success/Error/Loading)
│   │
│   ├── domain/
│   │   ├── model/                 ← Pure domain entities
│   │   │   ├── MediaItem.kt       ← Device media (from MediaStore, NOT in Room)
│   │   │   ├── Asset.kt           ← Imported media (in Room, references MediaItem by URI)
│   │   │   ├── Project.kt         ← Top-level editing container
│   │   │   ├── Timeline.kt        ← Ordered track collection
│   │   │   ├── Track.kt           ← Single editing lane (VIDEO/AUDIO/OVERLAY/TEXT)
│   │   │   ├── Clip.kt            ← Asset segment on a track
│   │   │   ├── Transition.kt      ← Visual effect between clips
│   │   │   ├── Effect.kt          ← Non-destructive clip filter/effect
│   │   │   ├── Resolution.kt      ← Output dimensions
│   │   │   └── ...supporting      ← MediaMetadata, PlaybackState, ExportConfig, etc.
│   │   │
│   │   ├── engine/                ← Engine interfaces (no implementations in Phase 1)
│   │   │   ├── MediaEngine.kt     ← Load media, extract metadata, generate thumbnails
│   │   │   ├── TimelineEngine.kt  ← Pure functional timeline editing operations
│   │   │   ├── PreviewEngine.kt   ← Playback and seek (Surface-based)
│   │   │   ├── RenderEngine.kt    ← Frame-by-frame compositing
│   │   │   ├── ExportEngine.kt    ← Final video encoding and export
│   │   │   ├── AudioEngine.kt     ← Audio mixing, waveform extraction
│   │   │   └── EffectsEngine.kt   ← Filters, LUTs, color grading
│   │   │
│   │   ├── repository/            ← Data access contracts (interfaces only)
│   │   │   ├── MediaRepository.kt
│   │   │   └── ProjectRepository.kt
│   │   │
│   │   └── usecase/               ← Single-responsibility business operations
│   │       ├── media/
│   │       │   ├── GetMediaItemsUseCase.kt
│   │       │   └── GetMediaItemByIdUseCase.kt
│   │       └── project/
│   │           └── CreateProjectUseCase.kt
│   │
│   ├── data/
│   │   ├── datasource/
│   │   │   ├── MediaStoreDataSource.kt        ← Interface
│   │   │   └── MediaStoreDataSourceImpl.kt    ← ContentResolver queries
│   │   │
│   │   ├── local/db/
│   │   │   ├── AppDatabase.kt                 ← Room database (app data only)
│   │   │   ├── entity/ProjectEntity.kt
│   │   │   ├── entity/AssetEntity.kt
│   │   │   ├── dao/ProjectDao.kt
│   │   │   ├── dao/AssetDao.kt
│   │   │   ├── mapper/ProjectMapper.kt
│   │   │   └── mapper/AssetMapper.kt
│   │   │
│   │   └── repository/
│   │       ├── MediaRepositoryImpl.kt         ← Delegates to MediaStoreDataSource
│   │       └── ProjectRepositoryImpl.kt       ← Delegates to Room DAOs
│   │
│   ├── di/
│   │   ├── AppModule.kt           ← ContentResolver, CoroutineDispatchers
│   │   ├── DatabaseModule.kt      ← AppDatabase, DAOs
│   │   └── RepositoryModule.kt    ← @Binds interfaces to implementations
│   │
│   └── utils/
│       ├── Logger.kt              ← Centralized logging wrapper
│       ├── CoroutineDispatchers.kt← Injectable dispatcher set
│       ├── FileUtils.kt           ← File/format utilities
│       └── extensions/
│           ├── ContextExtensions.kt
│           └── FlowExtensions.kt
│
├── feature/
│   ├── home/
│   │   ├── HomeFragment.kt
│   │   ├── HomeViewModel.kt
│   │   └── model/HomeUiState.kt
│   │
│   └── mediaimport/
│       ├── MediaImportFragment.kt
│       ├── MediaImportViewModel.kt
│       ├── model/MediaImportUiState.kt
│       └── adapter/
│           ├── MediaItemAdapter.kt
│           └── MediaItemViewHolder.kt
│
└── ui/
    └── base/
        ├── BaseFragment.kt        ← ViewBinding lifecycle management
        └── BaseViewModel.kt       ← Safe coroutine launcher
```

---

## Engine Architecture

Seven engine interfaces define the contracts for all major processing systems:

| Engine | Status | Future Implementation |
|---|---|---|
| `MediaEngine` | Interface only | MediaMetadataRetriever + ThumbnailUtils |
| `TimelineEngine` | Interface only | Pure Kotlin functional engine |
| `PreviewEngine` | Interface only | ExoPlayer-based implementation |
| `RenderEngine` | Interface only | OpenGL ES / Vulkan compositing |
| `ExportEngine` | Interface only | MediaCodec + MediaMuxer hardware encoding |
| `AudioEngine` | Interface only | AudioTrack + MediaExtractor |
| `EffectsEngine` | Interface only | GLSL shader pipeline |

---

## Data Storage Strategy

### MediaStore (Device Media)
- Source of truth for all device videos, images, and audio
- Queried live via `ContentResolver` — never cached in Room
- `MediaItem` is the domain type — transient, not persisted

### Room (App-Owned Data)
- `projects` table: Saved editing projects
- `assets` table: Imported media references + app-specific metadata
- Future tables: editing history, templates, user settings

### File System (App Private)
- `files/exports/`: Exported videos
- `cache/thumbnails/`: Generated project thumbnails

---

## State Management Pattern

```
Repository → Flow<AppResult<T>> → ViewModel → StateFlow<UiState> → Fragment
```

1. Repository emits Loading → Success/Error via Kotlin Flow
2. ViewModel maps to a feature-specific UiState sealed class
3. Fragment collects UiState with `repeatOnLifecycle(STARTED)`
4. Fragment renders state with exhaustive `when` expression

---

## Dependency Injection

Hilt is used for all DI. The component hierarchy:
- `SingletonComponent` — app-wide singletons (DB, ContentResolver, dispatchers)
- `ViewModelComponent` — ViewModel-scoped dependencies (use cases, repositories)
- `FragmentComponent` — Fragment-scoped if needed (none in Phase 1)
