# ROADMAP.md — B&B Video Editor

This document tracks the complete development roadmap from foundation to commercial-grade product.

---

## Status Legend
- ✅ Complete
- 🔄 In Progress  
- ⏳ Planned
- 💡 Future Consideration

---

## Phase 1 — Foundation ✅

**Goal**: Establish a scalable, production-ready foundation.

### Implemented
- ✅ Clean Architecture (Domain / Data / UI layers)
- ✅ MVVM pattern with StateFlow
- ✅ Hilt Dependency Injection
- ✅ Room Database (Projects, Assets)
- ✅ Navigation Component with slide animations
- ✅ Core domain models: Project, Asset, Timeline, Track, Clip, Transition, Effect
- ✅ 7 Engine interfaces: MediaEngine, TimelineEngine, PreviewEngine, RenderEngine, ExportEngine, AudioEngine, EffectsEngine
- ✅ MediaStore data source (device media, no Room caching)
- ✅ BaseFragment (ViewBinding lifecycle management)
- ✅ BaseViewModel (safe coroutine launcher)
- ✅ AppResult<T> (unified result type)
- ✅ CoroutineDispatchers (injectable for testing)
- ✅ Logger (centralized)
- ✅ FileUtils
- ✅ Material3 dark theme (amber-orange + electric purple palette)
- ✅ **Feature: Media Import** — browse device media, multi-select, permission handling
- ✅ Home screen with empty state
- ✅ Documentation: AGENTS.md, ARCHITECTURE.md, ROADMAP.md, DECISIONS.md, CONTRIBUTING.md

---

## Phase 2 — Project Management & Basic Preview ⏳

**Goal**: Users can create projects, see a preview, and have their work saved.

### Planned
- ⏳ Project creation flow (from selected media → new project)
- ⏳ Project list on Home screen (RecyclerView + grid)
- ⏳ Project detail screen with video preview
- ⏳ ExoPlayer integration for single-clip preview (implements PreviewEngine)
- ⏳ Basic timeline view (horizontal scroll, single video track)
- ⏳ Auto-save to Room on project open/close
- ⏳ Asset Library screen (all imported assets)

---

## Phase 3 — Core Editing Engine ⏳

**Goal**: Users can perform essential editing operations.

### Planned
- ⏳ TimelineEngine implementation (pure Kotlin)
- ⏳ Trim (drag start/end handles on clip)
- ⏳ Split clip at playhead position
- ⏳ Delete clip
- ⏳ Reorder clips (drag and drop on timeline)
- ⏳ Undo/Redo system (immutable state history)
- ⏳ Speed control (0.25x to 4x)

---

## Phase 4 — Audio ⏳

**Goal**: Full audio editing capabilities.

### Planned
- ⏳ AudioEngine implementation (MediaExtractor + MediaCodec)
- ⏳ Waveform visualization on audio tracks
- ⏳ Background music import and mixing
- ⏳ Voice-over recording
- ⏳ Volume control per clip
- ⏳ Mute/unmute tracks
- ⏳ Audio fade in/out

---

## Phase 5 — Visual Editing ⏳

**Goal**: Transform and enhance video visually.

### Planned
- ⏳ Crop and rotate
- ⏳ Flip horizontal/vertical
- ⏳ Basic filters (brightness, contrast, saturation) — EffectsEngine implementation
- ⏳ Color correction panel

---

## Phase 6 — Export Engine ⏳

**Goal**: High-quality hardware-accelerated video export.

### Planned
- ⏳ ExportEngine implementation (MediaCodec + MediaMuxer)
- ⏳ Hardware-accelerated encoding
- ⏳ Progress notification during export
- ⏳ Export presets (1080p30, 1080p60, 4K30, Instagram, TikTok, YouTube)
- ⏳ Save to gallery
- ⏳ Share to other apps

---

## Phase 7 — Advanced Visual Features ⏳

**Goal**: Professional-grade visual effects.

### Planned
- ⏳ OpenGL ES render pipeline (RenderEngine implementation)
- ⏳ LUT support (.cube format)
- ⏳ Full filter library
- ⏳ Keyframe animation system
- ⏳ Transition effects between clips
- ⏳ Overlay tracks (picture-in-picture)
- ⏳ Text overlay with font selection

---

## Phase 8 — Stickers, Templates & Advanced Overlays ⏳

- ⏳ Sticker library (animated and static)
- ⏳ Project templates
- ⏳ Green screen / chroma key
- ⏳ Motion tracking (basic)

---

## Phase 9 — Performance & Background Processing ⏳

- ⏳ Background export with WorkManager
- ⏳ Export survives process death
- ⏳ Render caching (pre-rendered preview frames)
- ⏳ Low-memory mode for older devices

---

## Phase 10 — AI Features 💡

- 💡 Auto-caption generation (on-device ASR)
- 💡 Smart trim suggestions
- 💡 Auto beat sync with music
- 💡 Scene detection
- 💡 AI-powered color grading presets
- 💡 Object removal

---

## Phase 11 — Plugin Architecture 💡

- 💡 Plugin API (3rd party effect providers)
- 💡 Effect marketplace
- 💡 Custom LUT packages

---

## Phase 12 — Cloud & Collaboration 💡

- 💡 Cloud project backup
- 💡 Cross-device sync
- 💡 Collaborative editing (shared projects)
- 💡 Asset cloud library

---

## Guiding Principles

Every phase must:
1. Build on the previous phase without major refactoring
2. Maintain all existing tests
3. Introduce no regressions
4. Follow the architecture rules in AGENTS.md
5. Document decisions in DECISIONS.md
