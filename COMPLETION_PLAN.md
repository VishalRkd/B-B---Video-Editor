# Video Editor Complete Implementation & Architecture Master Plan

This document serves as the **definitive engineering roadmap** to take the `videoeditornew` Android project from its current extracted prototype state to a **100% production-ready, fully functional offline video editor app**.

---

## 1. Current State Audit & Diagnosis

### Why the App Felt "Not Even 10% Working" to the User:
1. **Unwired Toolbar Buttons**:
   In `videoeditornew/app/src/main/java/com/videoeditorpro/android/ui/EditorActivity.kt` (lines 228–242), all tool buttons (**Split**, **Speed**, **Music**, **Effects**, **Text**) are empty lambda stubs:
   ```kotlin
   ToolButton(icon = Icons.Default.ContentCut, label = "Split") { /* Empty */ }
   ToolButton(icon = Icons.Default.Speed, label = "Speed") { /* Empty */ }
   ToolButton(icon = Icons.Default.MusicNote, label = "Music") { /* Empty */ }
   ToolButton(icon = Icons.Default.AutoFixHigh, label = "Effects") { /* Empty */ }
   ToolButton(icon = Icons.Default.TextFields, label = "Text") { /* Empty */ }
   ```
2. **Timeline Density Bug**:
   In `TimelineView.kt`, pixel-to-dp conversions were hardcoded using `(leftOffsetPx / 2.7f).dp`. On devices with screen densities other than 2.7x (which is almost all real hardware), the trimming handles and selection boxes are severely misaligned, making trimming feel broken or unresponsive.
3. **Single-Clip Limitation**:
   `VideoEngineManager.kt` currently holds only a single `currentClip`. Splitting a video does not update the UI track or filmstrip, leaving the user with no visual feedback.
4. **Mocked Export Progress**:
   The export progress was driven by a simulated `Handler.postDelayed(..., 300)` ticker rather than reading actual frame-encoding progress from the native `QProducer`.
5. **No Gallery MediaStore Registration**:
   Exported MP4 files are written to app-specific storage (`getExternalFilesDir`) without publishing to Android's `MediaStore`, so the exported video does not appear in the user's Google Photos / Gallery.

---

## 2. Architecture & Native Engine Blueprint

The app runs on an extracted, fully offline native C++ engine (`libcesliveeditor.so`, `libcesrenderengine.so`, `libffmpeg.so`, `libx264.so`, `libasp.so`) accessed through 236 JNI bridge classes in the package `xiaoying.engine.*`.

```
                    ┌───────────────────────────────────────────────┐
                    │            Jetpack Compose UI Shell           │
                    │   MainActivity  •  EditorActivity  • Dialogs   │
                    └───────────────────────┬───────────────────────┘
                                            │
                                            ▼
                    ┌───────────────────────────────────────────────┐
                    │              VideoEngineManager               │
                    │     (High-level Kotlin Singleton Facade)      │
                    └───────┬───────────────┬───────────────┬───────┘
                            │               │               │
                            ▼               ▼               ▼
                     ┌────────────┐  ┌────────────┐  ┌────────────┐
                     │  QPlayer   │  │QStoryboard │  │ QProducer  │
                     │ (Playback) │  │ (Timeline) │  │  (Export)  │
                     └──────┬─────┘  └──────┬─────┘  └──────┬─────┘
                            │               │               │
                            ▼               ▼               ▼
                     ┌────────────────────────────────────────────┐
                     │          Native Engine C++ Core            │
                     │  libcesliveeditor.so • libcesrenderengine  │
                     │    libffmpeg.so • libx264.so • libasp.so   │
                     └────────────────────────────────────────────┘
```

### Key Native Classes & Roles:
- **`xiaoying.engine.QEngine`**: Root engine instance created with license file (`assets/xiaoying/ini/license.txt`).
- **`xiaoying.engine.storyboard.QStoryboard`**: The multi-track timeline container. Holds video clips (`QClip`), audio tracks, transitions, and overlays.
- **`xiaoying.engine.clip.QClip`**: Represents a media segment (video or photo). Supports `PROP_TRIM_RANGE`, `PROP_TIME_SCALE`, `PROP_AUDIO_VOLUME`, `PROP_ROTATION`.
- **`xiaoying.engine.player.QPlayer`**: Hardware-accelerated OpenGL ES preview renderer bound to Android `SurfaceView` via `QDisplayContext`.
- **`xiaoying.engine.producer.QProducer`**: Hardware-accelerated offline video exporter encoding MP4 (H.264 + AAC).

---

## 3. Step-by-Step Implementation Tasks for the AI

Follow these tasks sequentially to complete the video editor.

---

### Task 1: Fix Timeline Density & Interaction in `TimelineView.kt`

**Target File**: `videoeditornew/app/src/main/java/com/videoeditorpro/android/ui/components/TimelineView.kt`

1. **Remove hardcoded `2.7f` density divisor**:
   Inject `LocalDensity.current` and use density-aware conversions:
   ```kotlin
   val density = LocalDensity.current
   val leftOffsetDp = with(density) { leftOffsetPx.toDp() }
   val trimWidthDp = with(density) { trimWidthPx.toDp() }
   ```
2. **Expand Touch Area for Handles**:
   Enlarge the handle touch target to at least `36.dp` width using invisible touch bounds while keeping the yellow visual handle at `12.dp` width.
3. **Smooth Scrubbing**:
   In `pointerInput(timelineWidthPx, durationMs)`, debounce fast drag updates to 60fps and ensure seek time is clamped between `0` and `durationMs`.

---

### Task 2: Implement Tool Bottom Sheets in `EditorActivity.kt`

**Target File**: `videoeditornew/app/src/main/java/com/videoeditorpro/android/ui/EditorActivity.kt`

Create an active tool state in `EditorScreen`:
```kotlin
enum class ActiveTool { NONE, SPLIT, SPEED, MUSIC, EFFECTS, TEXT }
var currentTool by remember { mutableStateOf(ActiveTool.NONE) }
```

#### 2.1 Split Action
When user taps **Split**:
1. Check if `currentPositionMs > 500` and `currentPositionMs < durationMs - 500`.
2. Call `VideoEngineManager.splitAt(currentPositionMs)`.
3. Reload filmstrip and update clip duration.
4. Show confirmation Toast: `"Clip split at ${formatTimecode(currentPositionMs)}"`.

#### 2.2 Speed BottomSheet (`ActiveTool.SPEED`)
Display a modal bottom sheet containing:
- Preset buttons: `0.25x`, `0.5x`, `0.75x`, `1.0x`, `1.5x`, `2.0x`, `4.0x`.
- Continuous Slider from `0.25f` to `4.0f`.
- Action:
  ```kotlin
  VideoEngineManager.setSpeed(selectedSpeed)
  durationMs = VideoEngineManager.getDuration()
  ```

#### 2.3 Audio / Music BottomSheet (`ActiveTool.MUSIC`)
1. Provide options:
   - **Pick Local Audio**: Use `ActivityResultContracts.GetContent()` with `"audio/*"`.
   - **Track Volume Slider**: `0%` to `200%`.
   - **Mute Original Video**: Checkbox to toggle original video track volume.
2. In `VideoEngineManager.kt`, implement `addAudioTrack(audioPath: String, volume: Int)`.

#### 2.4 Filters & Effects BottomSheet (`ActiveTool.EFFECTS`)
Provide built-in color adjustments:
- Brightness (-100 to 100)
- Contrast (-100 to 100)
- Saturation (-100 to 100)
- Presets (Warm, Cool, Vintage, B&W, Vivid)
Apply using `QClip.setProperty()` or shader filter effects.

#### 2.5 Text Overlay Modal (`ActiveTool.TEXT`)
1. Show an input dialog with:
   - Text input field
   - Text color picker (White, Black, Yellow, Red, Neon Blue, Pink)
   - Font size slider (12sp to 48sp)
2. Render text onto a bitmap and apply as overlay or call `VideoEngineManager.addTextOverlay()`.

---

### Task 3: Complete `VideoEngineManager.kt` Native Implementations

**Target File**: `videoeditornew/app/src/main/java/com/videoeditorpro/android/engine/VideoEngineManager.kt`

Implement the following missing methods:

```kotlin
/**
 * Inserts a background audio track into the storyboard.
 */
fun addAudioTrack(audioPath: String, volume: Int = 100): Boolean {
    val sb = storyboard ?: return false
    val eng = engine ?: return false
    try {
        val audioSource = QMediaSource(QMediaSource.TYPE_FILE, false, audioPath)
        val audioClip = QClip()
        val ret = audioClip.init(eng, audioSource)
        if (ret == 0) {
            // Set audio volume
            audioClip.setProperty(QClip.PROP_AUDIO_VOLUME, volume)
            // Insert into storyboard audio track (track index 1)
            sb.insertClip(audioClip, sb.clipCount)
            player?.autoRefreshStream()
            return true
        }
    } catch (e: Throwable) {
        Log.e("VideoEngineManager", "addAudioTrack error", e)
    }
    return false
}

/**
 * Sets original video audio volume or mutes it.
 */
fun setOriginalVolume(volume: Int) {
    currentClip?.let { clip ->
        clip.setProperty(QClip.PROP_AUDIO_VOLUME, volume)
        player?.autoRefreshStream()
    }
}

/**
 * Sets video rotation in degrees (0, 90, 180, 270).
 */
fun setRotation(rotationDegrees: Int) {
    currentClip?.let { clip ->
        clip.setProperty(QClip.PROP_ROTATION, rotationDegrees)
        player?.autoRefreshStream()
    }
}
```

---

### Task 4: Complete Production Video Exporter with MediaStore

**Target File**: `videoeditornew/app/src/main/java/com/videoeditorpro/android/engine/VideoEngineManager.kt` & `EditorActivity.kt`

1. **Wire Real Native Export Progress**:
   Configure `QProducer` to report export progress via `IQSessionStateListener`:
   ```kotlin
   producer.setProperty(QSession.PROP_SESSION_STATE_LISTENER, object : IQSessionStateListener {
       override fun onSessionStateChanged(session: QSession, state: QSessionState): Int {
           val progress = state.progress // 0 to 100
           Handler(Looper.getMainLooper()).post { onProgress(progress) }
           return 0
       }
   })
   ```

2. **Save Directly to System MediaStore Gallery**:
   When export succeeds, insert the file into `MediaStore.Video.Media.EXTERNAL_CONTENT_URI` so the user sees it in their Gallery immediately:
   ```kotlin
   fun saveToGallery(context: Context, videoFile: File): Uri? {
       val values = ContentValues().apply {
           put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
           put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
           put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/VideoEditorPro")
           put(MediaStore.Video.Media.IS_PENDING, 1)
       }
       val resolver = context.contentResolver
       val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
       resolver.openOutputStream(uri)?.use { out ->
           videoFile.inputStream().use { it.copyTo(out) }
       }
       values.clear()
       values.put(MediaStore.Video.Media.IS_PENDING, 0)
       resolver.update(uri, values, null, null)
       return uri
   }
   ```

---

### Task 5: Multi-Clip Video Import in `MainActivity.kt`

**Target File**: `videoeditornew/app/src/main/java/com/videoeditorpro/android/ui/MainActivity.kt`

1. Upgrade picker contract from `GetContent()` to `GetMultipleContents()`:
   ```kotlin
   val videoPickerLauncher = rememberLauncherForActivityResult(
       contract = ActivityResultContracts.GetMultipleContents()
   ) { uris: List<Uri> ->
       if (uris.isNotEmpty()) {
           val paths = uris.mapNotNull { copyUriToInternalStorage(it) }
           val intent = Intent(this, EditorActivity::class.java).apply {
               putStringArrayListExtra("EXTRA_VIDEO_PATHS", ArrayList(paths))
               putExtra("EXTRA_VIDEO_PATH", paths.first())
           }
           startActivity(intent)
       }
   }
   ```
2. In `EditorActivity.kt`, support concatenating all picked clips into `QStoryboard`:
   ```kotlin
   for ((index, path) in videoPaths.withIndex()) {
       val clip = QClip()
       clip.init(engine, QMediaSource(QMediaSource.TYPE_FILE, false, path))
       storyboard.insertClip(clip, index)
   }
   ```

---

## 4. Quality Verification & Test Checklist

After completing the tasks above, run the following verification steps on device or emulator:

| Feature | Test Case | Expected Result |
| :--- | :--- | :--- |
| **Video Import** | Pick 1 or multiple videos from gallery | Videos load into project without crash, filmstrip renders |
| **Preview** | Tap Play/Pause button | Video plays smoothly with audio, playhead moves |
| **Scrubbing** | Drag red playhead across timeline | Video preview seeks instantaneously to exact frame |
| **Trimming** | Drag yellow left/right trim handles | Video duration updates, preview respects trim range |
| **Split** | Position playhead and tap Split | Clip splits into two independent segments on timeline |
| **Speed** | Select 2.0x speed in speed dialog | Playback speeds up, audio pitch remains natural |
| **Music** | Add background audio track from device | Music plays alongside video, volume adjustable |
| **Export** | Tap Export and choose 1080p | Native engine renders MP4, progress reaches 100%, video appears in Gallery |

---

## 5. Build & Deployment Commands

Run from `/Users/vishalsingh/Desktop/DEV Android Projects/BBvideoeditor/videoeditornew`:

```bash
# Clean and assemble debug APK
./gradlew assembleDebug

# Install directly to USB connected phone or running emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Monitor runtime logs for engine messages
adb logcat -s VideoEngineManager VideoEditorApp QPlayer QProducer
```
