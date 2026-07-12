# PHASE_3_IMPLEMENTATION_GUIDE.md — Core Editing Engine
## Step-by-Step Implementation Guide for AI Agents

> **This file is the complete specification for Phase 3.**
> An AI agent should be able to implement Phase 3 entirely from this document.
> Follow ALL steps in order. Do not skip any step.

---

## 0. Pre-Implementation Checklist

Before writing ANY code, verify:
- [ ] `./gradlew assembleDebug` passes (Phase 2 is working)
- [ ] Read `ARCHITECTURE.md` completely
- [ ] Read `AGENTS.md` completely
- [ ] Read `DECISIONS.md` completely
- [ ] Understand `ProjectDetailFragment`, `ProjectDetailViewModel`, `ProjectDetailUiState`
- [ ] Understand `Clip.kt`, `Track.kt`, `Timeline.kt` domain models
- [ ] Understand `PreviewEngine` interface and `ExoPlayerPreviewEngine`

---

## 1. What Phase 3 Adds (Full List)

### New Functionality
1. **Timeline ruler** — scrollable time ruler above the track lanes
2. **Playhead** — white vertical line showing current playback position; draggable to seek
3. **Full multi-track layout** — Video + Audio + Text + Overlay tracks visible
4. **Track controls** — eye (visibility) + lock icons per track
5. **Trim clips** — drag left/right edge of any clip to trim
6. **Split clip** — tap "Split" button to cut clip at playhead
7. **Delete clip** — tap "Delete" to remove selected clip
8. **Reorder clips** — drag clip horizontally to new position on track
9. **Undo/Redo** — immutable history, up to 50 steps
10. **Speed control** — 0.25x to 4.0x per clip
11. **Clip thumbnail strip** — real video frames loaded via MediaMetadataRetriever
12. **Project auto-save** — saves to Room after every edit operation

### New Files (Domain)
- `core/domain/model/EditingHistory.kt`
- `core/domain/usecase/project/UpdateProjectUseCase.kt`
- `core/domain/usecase/timeline/TrimClipUseCase.kt`
- `core/domain/usecase/timeline/SplitClipUseCase.kt`
- `core/domain/usecase/timeline/DeleteClipUseCase.kt`
- `core/domain/usecase/timeline/MoveClipUseCase.kt`
- `core/domain/usecase/timeline/SetSpeedUseCase.kt`

### New Files (Data)
- `core/data/engine/KotlinTimelineEngine.kt`
- `core/data/engine/AndroidMediaEngine.kt`

### Modified Files (Feature)
- `feature/projectdetail/ProjectDetailViewModel.kt` — massive additions
- `feature/projectdetail/ProjectDetailFragment.kt` — massive additions
- `feature/projectdetail/model/ProjectDetailUiState.kt` — updated Content state
- `feature/projectdetail/adapter/TimelineClipAdapter.kt` — add trim handle support

### New UI Files
- `feature/projectdetail/view/TimelineRulerView.kt` (custom View)
- `feature/projectdetail/view/PlayheadView.kt` (custom View)
- `feature/projectdetail/view/WaveformView.kt` (custom View — Phase 4 prep)
- `res/layout/view_timeline_ruler.xml`
- `res/layout/layout_track_label.xml`
- `res/layout/item_timeline_clip_editable.xml`
- `res/layout/partial_editor_bottom_toolbar.xml`
- `res/layout/partial_editor_playback_controls.xml`

---

## 2. Domain Models — Create/Update

### Step 2.1: EditingHistory.kt
**Path**: `core/domain/model/EditingHistory.kt`

```kotlin
package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Immutable undo/redo history stack for project editing.
 *
 * Uses a copy-on-write approach: every edit creates a new [EditingHistory] instance.
 * Maximum [maxSize] past states are kept to limit memory usage.
 */
data class EditingHistory(
    val past: List<Project> = emptyList(),
    val future: List<Project> = emptyList(),
    val maxSize: Int = 50,
) {
    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()

    /**
     * Records [newState] as the current state, adding [current] to the past stack.
     * Clears the redo (future) stack.
     */
    fun push(current: Project, newState: Project): EditingHistory = copy(
        past = (past + current).takeLast(maxSize),
        future = emptyList(),
    )

    /**
     * Reverts to the previous state. Returns null if nothing to undo.
     * Returns [Pair] of (new history, reverted project state).
     */
    fun undo(current: Project): Pair<EditingHistory, Project>? {
        if (!canUndo) return null
        return Pair(
            copy(past = past.dropLast(1), future = listOf(current) + future),
            past.last(),
        )
    }

    /**
     * Re-applies the most recently undone state. Returns null if nothing to redo.
     */
    fun redo(current: Project): Pair<EditingHistory, Project>? {
        if (!canRedo) return null
        return Pair(
            copy(past = past + current, future = future.drop(1)),
            future.first(),
        )
    }
}
```

### Step 2.2: UpdateProjectUseCase.kt
**Path**: `core/domain/usecase/project/UpdateProjectUseCase.kt`

```kotlin
package com.beatsandbeyond.video_editor.core.domain.usecase.project

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.repository.ProjectRepository
import javax.inject.Inject

/** Saves an updated [Project] back to Room. Called after every editing operation. */
class UpdateProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
) {
    suspend operator fun invoke(project: Project): AppResult<Project> =
        projectRepository.updateProject(project)
}
```

### Step 2.3: TrimClipUseCase.kt
**Path**: `core/domain/usecase/timeline/TrimClipUseCase.kt`

```kotlin
package com.beatsandbeyond.video_editor.core.domain.usecase.timeline

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Project
import javax.inject.Inject

/**
 * Trims a clip by adjusting its [trimStartMs] and [trimEndMs].
 *
 * Rules:
 * - [newTrimStartMs] must be >= 0
 * - [newTrimEndMs] must be > [newTrimStartMs]
 * - Duration on timeline changes: clip end position shifts accordingly
 * - Adjacent clips are NOT automatically moved (gap may appear)
 */
class TrimClipUseCase @Inject constructor() {
    operator fun invoke(
        project: Project,
        clipId: String,
        newTrimStartMs: Long,
        newTrimEndMs: Long,
    ): AppResult<Project> {
        if (newTrimStartMs < 0) return AppResult.Error(
            IllegalArgumentException("Trim start must be >= 0")
        )
        if (newTrimEndMs <= newTrimStartMs) return AppResult.Error(
            IllegalArgumentException("Trim end must be > trim start")
        )

        val updatedTimeline = project.timeline.mapClip(clipId) { clip ->
            clip.copy(trimStartMs = newTrimStartMs, trimEndMs = newTrimEndMs)
        } ?: return AppResult.Error(NoSuchElementException("Clip $clipId not found"))

        return AppResult.Success(project.copy(
            timeline = updatedTimeline,
            updatedAt = System.currentTimeMillis(),
        ))
    }
}
```

> **Note**: `Timeline.mapClip()` is a helper extension to add in `Timeline.kt`:
> ```kotlin
> fun Timeline.mapClip(clipId: String, transform: (Clip) -> Clip): Timeline? {
>     val updatedTracks = tracks.map { track ->
>         if (track.clips.any { it.id == clipId }) {
>             track.copy(clips = track.clips.map { if (it.id == clipId) transform(it) else it })
>         } else track
>     }
>     val found = updatedTracks != tracks || tracks.any { it.clips.any { c -> c.id == clipId } }
>     return if (found) copy(tracks = updatedTracks) else null
> }
> ```

### Step 2.4: SplitClipUseCase.kt
**Path**: `core/domain/usecase/timeline/SplitClipUseCase.kt`

```kotlin
package com.beatsandbeyond.video_editor.core.domain.usecase.timeline

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Project
import java.util.UUID
import javax.inject.Inject

/**
 * Splits a clip at [splitAtTimelineMs] (absolute timeline position).
 *
 * The original clip is replaced by two new clips:
 * - Left clip: same start, ends at split point
 * - Right clip: starts at split point, same end
 * - Right clip's [timelinePositionMs] is adjusted to [splitAtTimelineMs]
 */
class SplitClipUseCase @Inject constructor() {
    operator fun invoke(
        project: Project,
        clipId: String,
        splitAtTimelineMs: Long,
    ): AppResult<Project> {
        val clip = project.timeline.findClip(clipId)
            ?: return AppResult.Error(NoSuchElementException("Clip $clipId not found"))

        // splitAtTimelineMs must be within the clip bounds
        if (splitAtTimelineMs <= clip.timelinePositionMs ||
            splitAtTimelineMs >= clip.timelineEndMs
        ) {
            return AppResult.Error(IllegalArgumentException("Split position must be within clip bounds"))
        }

        // Calculate source position at split point
        val sourcePositionMs = clip.trimStartMs +
            ((splitAtTimelineMs - clip.timelinePositionMs) * clip.speedFactor).toLong()

        val leftClip = clip.copy(
            id = UUID.randomUUID().toString(),
            trimEndMs = sourcePositionMs,
        )
        val rightClip = clip.copy(
            id = UUID.randomUUID().toString(),
            timelinePositionMs = splitAtTimelineMs,
            trimStartMs = sourcePositionMs,
        )

        val updatedTimeline = project.timeline.replaceClip(clipId, listOf(leftClip, rightClip))
            ?: return AppResult.Error(IllegalStateException("Failed to split clip"))

        return AppResult.Success(project.copy(
            timeline = updatedTimeline,
            updatedAt = System.currentTimeMillis(),
        ))
    }
}
```

> Add `Timeline.findClip()` and `Timeline.replaceClip()` helpers to `Timeline.kt`.

### Step 2.5: DeleteClipUseCase.kt

```kotlin
class DeleteClipUseCase @Inject constructor() {
    operator fun invoke(project: Project, clipId: String): AppResult<Project> {
        val updatedTimeline = project.timeline.removeClip(clipId)
            ?: return AppResult.Error(NoSuchElementException("Clip $clipId not found"))
        return AppResult.Success(project.copy(
            timeline = updatedTimeline,
            updatedAt = System.currentTimeMillis(),
        ))
    }
}
```

### Step 2.6: SetSpeedUseCase.kt

```kotlin
class SetSpeedUseCase @Inject constructor() {
    operator fun invoke(project: Project, clipId: String, speedFactor: Float): AppResult<Project> {
        if (speedFactor < 0.25f || speedFactor > 4.0f)
            return AppResult.Error(IllegalArgumentException("Speed must be 0.25x to 4.0x"))

        val updatedTimeline = project.timeline.mapClip(clipId) { clip ->
            clip.copy(speedFactor = speedFactor)
        } ?: return AppResult.Error(NoSuchElementException("Clip $clipId not found"))

        return AppResult.Success(project.copy(
            timeline = updatedTimeline,
            updatedAt = System.currentTimeMillis(),
        ))
    }
}
```

---

## 3. Timeline.kt Helper Extensions

Add these helper functions to the bottom of `Timeline.kt` or in a new `TimelineExtensions.kt`:

```kotlin
/** Finds a clip by ID across all tracks. Returns null if not found. */
fun Timeline.findClip(clipId: String): Clip? =
    tracks.flatMap { it.clips }.firstOrNull { it.id == clipId }

/** Applies [transform] to the clip with [clipId]. Returns null if not found. */
fun Timeline.mapClip(clipId: String, transform: (Clip) -> Clip): Timeline? {
    var found = false
    val updatedTracks = tracks.map { track ->
        val updatedClips = track.clips.map { clip ->
            if (clip.id == clipId) { found = true; transform(clip) } else clip
        }
        track.copy(clips = updatedClips)
    }
    return if (found) copy(tracks = updatedTracks) else null
}

/** Replaces the clip with [clipId] with [newClips]. Returns null if not found. */
fun Timeline.replaceClip(clipId: String, newClips: List<Clip>): Timeline? {
    var found = false
    val updatedTracks = tracks.map { track ->
        val clipIndex = track.clips.indexOfFirst { it.id == clipId }
        if (clipIndex >= 0) {
            found = true
            track.copy(clips = track.clips.toMutableList().apply {
                removeAt(clipIndex)
                addAll(clipIndex, newClips)
            })
        } else track
    }
    return if (found) copy(tracks = updatedTracks) else null
}

/** Removes the clip with [clipId]. Returns null if not found. */
fun Timeline.removeClip(clipId: String): Timeline? {
    var found = false
    val updatedTracks = tracks.map { track ->
        val newClips = track.clips.filter { it.id != clipId }
        if (newClips.size < track.clips.size) { found = true; track.copy(clips = newClips) }
        else track
    }
    return if (found) copy(tracks = updatedTracks) else null
}
```

---

## 4. Updated ProjectDetailUiState

**Path**: `feature/projectdetail/model/ProjectDetailUiState.kt`

```kotlin
sealed class ProjectDetailUiState {
    data object Loading : ProjectDetailUiState()

    data class Content(
        val project: Project,
        val assets: Map<String, Asset>,         // keyed by assetId for O(1) lookups
        val playbackState: PlaybackState,
        val currentPositionMs: Long,
        val totalDurationMs: Long,
        val selectedClipId: String? = null,     // null = no selection
        val canUndo: Boolean = false,
        val canRedo: Boolean = false,
    ) : ProjectDetailUiState()

    data class Error(val message: String) : ProjectDetailUiState()
}
```

---

## 5. ProjectDetailViewModel Updates

Add these to `ProjectDetailViewModel.kt`:

```kotlin
// Inject new use cases
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProjectByIdUseCase: GetProjectByIdUseCase,
    private val updateProjectUseCase: UpdateProjectUseCase,      // NEW
    private val previewEngine: PreviewEngine,
    private val trimClipUseCase: TrimClipUseCase,                // NEW
    private val splitClipUseCase: SplitClipUseCase,             // NEW
    private val deleteClipUseCase: DeleteClipUseCase,           // NEW
    private val setSpeedUseCase: SetSpeedUseCase,               // NEW
) : BaseViewModel() {

    // History for undo/redo
    private var editingHistory = EditingHistory()

    // Current project state (latest)
    private var currentProject: Project? = null

    fun trimSelectedClip(newTrimStartMs: Long, newTrimEndMs: Long) {
        val content = currentContent ?: return
        val clipId = content.selectedClipId ?: return
        val project = currentProject ?: return

        when (val result = trimClipUseCase(project, clipId, newTrimStartMs, newTrimEndMs)) {
            is AppResult.Success -> applyEdit(result.data)
            is AppResult.Error -> showError(result.exception.message ?: "Trim failed")
        }
    }

    fun splitAtPlayhead() {
        val content = currentContent ?: return
        val clipId = content.selectedClipId ?: return
        val project = currentProject ?: return
        val positionMs = content.currentPositionMs

        when (val result = splitClipUseCase(project, clipId, positionMs)) {
            is AppResult.Success -> applyEdit(result.data)
            is AppResult.Error -> showError(result.exception.message ?: "Split failed")
        }
    }

    fun deleteSelectedClip() {
        val clipId = currentContent?.selectedClipId ?: return
        val project = currentProject ?: return

        when (val result = deleteClipUseCase(project, clipId)) {
            is AppResult.Success -> applyEdit(result.data)
            is AppResult.Error -> showError(result.exception.message ?: "Delete failed")
        }
    }

    fun undo() {
        val project = currentProject ?: return
        val (newHistory, revertedProject) = editingHistory.undo(project) ?: return
        editingHistory = newHistory
        currentProject = revertedProject
        saveAndRefreshUi(revertedProject)
    }

    fun redo() {
        val project = currentProject ?: return
        val (newHistory, redoneProject) = editingHistory.redo(project) ?: return
        editingHistory = newHistory
        currentProject = redoneProject
        saveAndRefreshUi(redoneProject)
    }

    /** Applies an edit: pushes history, updates state, auto-saves to Room. */
    private fun applyEdit(newProject: Project) {
        val old = currentProject ?: return
        editingHistory = editingHistory.push(old, newProject)
        currentProject = newProject
        saveAndRefreshUi(newProject)
    }

    private fun saveAndRefreshUi(project: Project) {
        launchSafely {
            updateProjectUseCase(project)   // auto-save to Room
            refreshContent(project)
        }
    }
}
```

---

## 6. Custom View: TimelineRulerView

**Path**: `feature/projectdetail/view/TimelineRulerView.kt`

```kotlin
package com.beatsandbeyond.video_editor.feature.projectdetail.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.beatsandbeyond.video_editor.R

/**
 * Custom View that draws a scrollable timeline ruler with time labels.
 *
 * Time labels appear every second (major tick) and every 250ms (minor tick).
 * The view is synchronized with the timeline horizontal scroll position.
 *
 * Usage: call [setScrollOffsetMs] when the timeline scrolls.
 * Call [setDurationMs] with the total project duration.
 * Call [setPixelsPerMs] when zoom changes.
 */
class TimelineRulerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    private var durationMs: Long = 0L
    private var scrollOffsetMs: Long = 0L
    private var pixelsPerMs: Float = 0.15f   // default: 0.15px per ms = 150px per second

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.color_on_surface_variant)
        textSize = resources.getDimension(R.dimen.media_duration_badge_text_size)
        textAlign = Paint.Align.CENTER
    }

    private val tickPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.color_outline)
        strokeWidth = 1f
    }

    fun setDurationMs(ms: Long) { durationMs = ms; invalidate() }
    fun setScrollOffsetMs(ms: Long) { scrollOffsetMs = ms; invalidate() }
    fun setPixelsPerMs(pxPerMs: Float) { pixelsPerMs = pxPerMs; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val visibleStartMs = scrollOffsetMs
        val visibleEndMs = scrollOffsetMs + (width / pixelsPerMs).toLong()

        // Draw major ticks every 1000ms
        var tickMs = (visibleStartMs / 1000L) * 1000L
        while (tickMs <= visibleEndMs) {
            val x = (tickMs - scrollOffsetMs) * pixelsPerMs
            canvas.drawLine(x, h * 0.4f, x, h, tickPaint)
            canvas.drawText(formatTime(tickMs), x, h * 0.35f, textPaint)
            tickMs += 1000L
        }

        // Draw minor ticks every 250ms (no labels)
        tickMs = (visibleStartMs / 250L) * 250L
        while (tickMs <= visibleEndMs) {
            if (tickMs % 1000L != 0L) {  // skip major ticks
                val x = (tickMs - scrollOffsetMs) * pixelsPerMs
                canvas.drawLine(x, h * 0.7f, x, h, tickPaint)
            }
            tickMs += 250L
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return "%02d:%02d".format(mins, secs)
    }
}
```

---

## 7. Layout Files

### `res/layout/partial_editor_bottom_toolbar.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<HorizontalScrollView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/contextToolbarScroll"
    android:layout_width="match_parent"
    android:layout_height="@dimen/editor_context_toolbar_height"
    android:background="@color/color_surface"
    android:scrollbars="none">

    <LinearLayout
        android:layout_width="wrap_content"
        android:layout_height="match_parent"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:paddingHorizontal="@dimen/spacing_sm">

        <!-- Split -->
        <LinearLayout android:id="@+id/btnSplit"
            style="@style/Widget.Editor.ContextButton">
            <ImageView android:src="@drawable/ic_split"
                style="@style/Widget.Editor.ContextButton.Icon" />
            <TextView android:text="@string/editor_split"
                style="@style/Widget.Editor.ContextButton.Label" />
        </LinearLayout>

        <!-- Trim -->
        <LinearLayout android:id="@+id/btnTrim"
            style="@style/Widget.Editor.ContextButton">
            <ImageView android:src="@drawable/ic_trim"
                style="@style/Widget.Editor.ContextButton.Icon" />
            <TextView android:text="@string/editor_trim"
                style="@style/Widget.Editor.ContextButton.Label" />
        </LinearLayout>

        <!-- Speed -->
        <LinearLayout android:id="@+id/btnSpeed"
            style="@style/Widget.Editor.ContextButton">
            <ImageView android:src="@drawable/ic_speed"
                style="@style/Widget.Editor.ContextButton.Icon" />
            <TextView android:text="@string/editor_speed"
                style="@style/Widget.Editor.ContextButton.Label" />
        </LinearLayout>

        <!-- Delete -->
        <LinearLayout android:id="@+id/btnDelete"
            style="@style/Widget.Editor.ContextButton.Danger">
            <ImageView android:src="@drawable/ic_delete"
                style="@style/Widget.Editor.ContextButton.Icon" />
            <TextView android:text="@string/editor_delete"
                style="@style/Widget.Editor.ContextButton.Label" />
        </LinearLayout>

    </LinearLayout>

</HorizontalScrollView>
```

Add to `res/values/styles.xml`:
```xml
<style name="Widget.Editor.ContextButton">
    <item name="android:layout_width">60dp</item>
    <item name="android:layout_height">match_parent</item>
    <item name="android:gravity">center</item>
    <item name="android:orientation">vertical</item>
    <item name="android:paddingVertical">8dp</item>
</style>
<style name="Widget.Editor.ContextButton.Icon">
    <item name="android:layout_width">24dp</item>
    <item name="android:layout_height">24dp</item>
    <item name="android:tint">@color/color_on_surface</item>
</style>
<style name="Widget.Editor.ContextButton.Label">
    <item name="android:layout_width">wrap_content</item>
    <item name="android:layout_height">wrap_content</item>
    <item name="android:layout_marginTop">4dp</item>
    <item name="android:textAppearance">@style/TextAppearance.Material3.LabelSmall</item>
    <item name="android:textColor">@color/color_on_surface_variant</item>
</style>
<style name="Widget.Editor.ContextButton.Danger" parent="Widget.Editor.ContextButton">
    <!-- inherits, just icon/label use color_error -->
</style>
```

---

## 8. Testing Phase 3

### Unit Tests to Write (all in `src/test/`)

**`TrimClipUseCaseTest.kt`**
- Valid trim → new project has updated trimStartMs/trimEndMs
- Trim start < 0 → AppResult.Error
- Trim end <= trim start → AppResult.Error
- Non-existent clip ID → AppResult.Error

**`SplitClipUseCaseTest.kt`**
- Split at middle → two clips, IDs are different, durations sum to original
- Split at start position → AppResult.Error (out of bounds)
- Split at end position → AppResult.Error (out of bounds)

**`DeleteClipUseCaseTest.kt`**
- Delete existing clip → project has one fewer clip
- Delete non-existent clip → AppResult.Error

**`EditingHistoryTest.kt`**
- Push states → `canUndo=true`, `canRedo=false`
- Undo → returns previous state, `canRedo=true`
- Redo → returns redone state
- Push after undo → clears redo stack
- Push maxSize+1 states → oldest state dropped (maxSize enforced)

**`SetSpeedUseCaseTest.kt`**
- Valid speed 1.5x → clip.speedFactor == 1.5f
- Speed 0.24x → AppResult.Error (out of range)
- Speed 4.1x → AppResult.Error (out of range)

---

## 9. Strings to Add (strings.xml)

```xml
<!-- Editor toolbar -->
<string name="editor_undo">Undo</string>
<string name="editor_redo">Redo</string>
<string name="editor_export">Export</string>

<!-- Context bottom toolbar -->
<string name="editor_split">Split</string>
<string name="editor_trim">Trim</string>
<string name="editor_speed">Speed</string>
<string name="editor_volume">Volume</string>
<string name="editor_filter">Filter</string>
<string name="editor_adjust">Adjust</string>
<string name="editor_delete">Delete</string>

<!-- Dialogs -->
<string name="dialog_delete_clip_title">Delete Clip?</string>
<string name="dialog_delete_clip_message">This clip will be removed from the timeline.</string>

<!-- Speed selector -->
<string name="speed_0_25x">0.25×</string>
<string name="speed_0_5x">0.5×</string>
<string name="speed_1x">1×</string>
<string name="speed_1_5x">1.5×</string>
<string name="speed_2x">2×</string>
<string name="speed_4x">4×</string>
```

---

## 10. Commit Checklist

After implementing Phase 3, verify:
- [ ] `./gradlew test` passes (all unit tests green)
- [ ] `./gradlew assembleDebug` passes (no errors, no warnings)
- [ ] Trim handles work (drag both edges of a clip)
- [ ] Split works (tap Split, clip divides at playhead)
- [ ] Delete works (selected clip is removed)
- [ ] Undo/Redo works (← ↩ in toolbar)
- [ ] Timeline ruler shows correct time labels
- [ ] Playhead moves during playback
- [ ] Dragging playhead seeks ExoPlayer
- [ ] DECISIONS.md updated with any new architectural decisions
- [ ] ROADMAP.md Phase 3 marked ✅

**Commit message**: `[editor]: implement timeline editing — trim, split, delete, undo/redo`
