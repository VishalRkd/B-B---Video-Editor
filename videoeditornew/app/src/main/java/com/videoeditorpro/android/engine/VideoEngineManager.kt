package com.videoeditorpro.android.engine

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.view.SurfaceView
import com.videoeditorpro.android.domain.model.AudioClipItem
import com.videoeditorpro.android.domain.model.EditorProject
import com.videoeditorpro.android.domain.model.VideoClipItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Unified facade coordinating native engine subsystems.
 * Maintains backwards compatibility for single-clip calls while enabling
 * full multi-clip, Undo/Redo, and MediaStore capabilities via modular components.
 */
object VideoEngineManager {
    private const val TAG = "VideoEngineManager"

    val playbackController = PlaybackController()
    val storyboardBridge = StoryboardBridge()
    val exportEngine = ExportEngine()
    val thumbnailProvider = ThumbnailProvider

    private var activeProject: EditorProject? = null

    /**
     * Initializes native libraries and QEngine.
     */
    fun init(context: Context): Boolean {
        return EngineCore.initialize(context)
    }

    /**
     * Sets up a project session with a single video file.
     */
    fun openVideo(videoPath: String): Boolean {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durStr?.toIntOrNull() ?: 1000
            retriever.release()

            val initialClip = VideoClipItem(
                sourcePath = videoPath,
                sourceDurationMs = durationMs,
                trimStartMs = 0,
                trimEndMs = durationMs
            )

            val project = EditorProject(
                name = File(videoPath).nameWithoutExtension,
                videoClips = listOf(initialClip)
            )

            return openProject(project)
        } catch (e: Throwable) {
            Log.e(TAG, "openVideo failed for $videoPath", e)
            return false
        }
    }

    /**
     * Sets up a project session with a full multi-clip domain EditorProject.
     */
    fun openProject(project: EditorProject): Boolean {
        activeProject = project
        val built = storyboardBridge.buildStoryboard(project)
        if (built) {
            playbackController.refreshStream()
        }
        return built
    }

    fun getActiveProject(): EditorProject? = activeProject

    /**
     * Attaches playback to an Android SurfaceView.
     */
    fun attachSurface(surfaceView: SurfaceView, onAttached: () -> Unit) {
        val sb = storyboardBridge.getStoryboard() ?: return
        playbackController.attachSurface(surfaceView, sb, onAttached)
    }

    fun play() = playbackController.play()

    fun pause() = playbackController.pause()

    fun isPlaying(): Boolean = playbackController.isPlaying()

    fun seekTo(positionMs: Int) = playbackController.seekTo(positionMs)

    fun getDuration(): Int = storyboardBridge.durationMs

    fun getCurrentPosition(): Int = playbackController.getCurrentPosition()

    fun setProgressListener(listener: (currentMs: Int, totalMs: Int) -> Unit) {
        playbackController.setProgressListener(listener)
    }

    /**
     * Trims the primary clip (index 0).
     */
    fun trim(startMs: Int, endMs: Int) {
        if (storyboardBridge.updateClipTrim(0, startMs, endMs)) {
            playbackController.refreshStream()
            activeProject?.let { proj ->
                if (proj.videoClips.isNotEmpty()) {
                    val updated = proj.videoClips[0].copy(trimStartMs = startMs, trimEndMs = endMs)
                    activeProject = proj.copy(videoClips = listOf(updated) + proj.videoClips.drop(1))
                }
            }
        }
    }

    /**
     * Adjusts playback speed of primary clip.
     */
    fun setSpeed(speed: Float) {
        if (storyboardBridge.updateClipSpeed(0, speed)) {
            playbackController.refreshStream()
            activeProject?.let { proj ->
                if (proj.videoClips.isNotEmpty()) {
                    val updated = proj.videoClips[0].copy(speed = speed)
                    activeProject = proj.copy(videoClips = listOf(updated) + proj.videoClips.drop(1))
                }
            }
        }
    }

    /**
     * Splits clip 0 at splitTimeMs.
     */
    fun splitAt(splitTimeMs: Int): Boolean {
        val proj = activeProject ?: return false
        val clip = proj.videoClips.firstOrNull() ?: return false
        val success = storyboardBridge.splitClip(0, splitTimeMs, clip)
        if (success) {
            playbackController.refreshStream()
        }
        return success
    }

    /**
     * Inserts a background audio track.
     */
    fun addAudioTrack(audioPath: String, title: String = "Audio", volume: Int = 100): Boolean {
        val audioItem = AudioClipItem(
            sourcePath = audioPath,
            title = title,
            sourceDurationMs = 60000,
            volume = volume
        )
        val success = storyboardBridge.addAudioTrack(audioItem)
        if (success) {
            playbackController.refreshStream()
        }
        return success
    }

    /**
     * Sets volume/mute on the primary clip.
     */
    fun setOriginalVolume(volume: Int, isMuted: Boolean = false) {
        if (storyboardBridge.updateClipVolume(0, volume, isMuted)) {
            playbackController.refreshStream()
        }
    }

    /**
     * Sets rotation on the primary clip.
     */
    fun setRotation(rotationDegrees: Int) {
        if (storyboardBridge.updateClipRotation(0, rotationDegrees)) {
            playbackController.refreshStream()
        }
    }

    /**
     * Exports the project using native QProducer and saves to system MediaStore.
     */
    fun export(
        outputPath: String,
        context: Context? = null,
        onProgress: (percent: Int) -> Unit,
        onComplete: (success: Boolean, path: String) -> Unit
    ) {
        val sb = storyboardBridge.getStoryboard() ?: run {
            onComplete(false, "")
            return
        }

        pause()
        val appContext = context ?: surfaceContext ?: run {
            // Fallback export if context not passed
            onComplete(false, "")
            return
        }

        exportEngine.export(
            context = appContext,
            storyboard = sb,
            outputPath = outputPath,
            onProgress = onProgress,
            onComplete = { success, _, path ->
                onComplete(success, path)
            }
        )
    }

    private var surfaceContext: Context? = null

    fun setContext(context: Context) {
        surfaceContext = context.applicationContext
    }

    /**
     * Generates thumbnail filmstrip using cached ThumbnailProvider.
     */
    suspend fun generateFilmstripThumbnails(
        videoPath: String,
        count: Int = 10
    ): List<Bitmap> {
        val dur = getDuration().coerceAtLeast(1000)
        return thumbnailProvider.getFilmstripThumbnails(
            videoPath = videoPath,
            startMs = 0,
            endMs = dur,
            count = count
        )
    }

    fun release() {
        playbackController.releasePlayer()
        storyboardBridge.release()
        activeProject = null
        thumbnailProvider.clearCache()
        EngineCore.release()
    }
}
