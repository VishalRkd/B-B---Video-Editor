package com.bbvideoeditor.app.render

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Pair
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.OverlaySettings
import androidx.media3.common.VideoCompositorSettings
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.audio.ToInt16PcmAudioProcessor
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.bbvideoeditor.core.Clip
import com.bbvideoeditor.core.MediaKind
import com.bbvideoeditor.core.MediaReference
import com.bbvideoeditor.core.Project
import com.bbvideoeditor.core.Track
import com.bbvideoeditor.core.RenderPlan
import com.bbvideoeditor.core.RenderTrack
import com.bbvideoeditor.core.TrackType
import com.bbvideoeditor.core.Transform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

@UnstableApi
data class CompiledComposition(
    val composition: Composition,
    val durationUs: Long,
    val warnings: List<String>,
    val width: Int,
    val height: Int,
    val frameRate: Int,
    val requiresCompositor: Boolean = true,
    val plan: RenderPlan,
)

/**
 * Compiles immutable edit decisions into a Media3 render graph. This is the only translation of
 * the project into playback/render operations. Compilations never mutate a project or cache
 * stateful audio processors, so preview and a simultaneous export have independent resources.
 */
@UnstableApi
class CompositionCompiler(
    context: Context,
    private val effects: RenderEffectRegistry = RenderEffectRegistry(),
    private val textBitmaps: TextBitmapCache = TextBitmapCache(),
) {
    private val context = context.applicationContext
    private val clockAssetMutex = Mutex()

    suspend fun compile(
        project: Project,
        outputWidth: Int = project.canvas.width,
        outputHeight: Int = project.canvas.height,
        frameRate: Int = project.canvas.frameRate,
        preferSingleVisualInput: Boolean = false,
        renderText: Boolean = true,
    ): CompiledComposition = compile(
        withContext(Dispatchers.Default) { RenderPlan.derive(project) },
        outputWidth, outputHeight, frameRate, preferSingleVisualInput, renderText,
    )

    /** Reuse immutable indexes across boundary recovery; runtime effects remain independently owned. */
    suspend fun compile(
        plan: RenderPlan,
        outputWidth: Int = plan.project.canvas.width,
        outputHeight: Int = plan.project.canvas.height,
        frameRate: Int = plan.project.canvas.frameRate,
        preferSingleVisualInput: Boolean = false,
        renderText: Boolean = true,
    ): CompiledComposition = withContext(Dispatchers.Default) {
        val project = plan.project
        effects.preflight(project)
        require(project.durationUs > 0) { "Add media or text before previewing or exporting." }
        require(outputWidth in 2..8192 && outputHeight in 2..8192 && outputWidth % 2 == 0 && outputHeight % 2 == 0)
        require(frameRate in 1..120)
        val usedMedia = project.timeline.clips.mapNotNull { it.mediaId }.toSet()
        withContext(Dispatchers.IO) {
            for (id in usedMedia) {
                coroutineContext.ensureActive()
                val source = requireNotNull(project.media[id])
                try {
                    context.contentResolver.openAssetFileDescriptor(Uri.parse(source.uri), "r")?.use { }
                        ?: throw IllegalStateException("Cannot open media")
                } catch (error: Exception) {
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    throw IllegalArgumentException("'${source.displayName}' is unavailable. Relink or remove it.", error)
                }
            }
        }
        val textTracks = plan.text.map { it.track }
        for (track in textTracks) {
            // Prewarm a small window off the render thread. Later captions are cached on demand.
            if (renderText) track.clips.sortedBy { it.startUs }.take(2).forEach { textBitmaps.get(requireNotNull(it.text), project.canvas, outputHeight) }
        }
        val visualTracks = plan.visuals.map { it.track }
        val visualClips = plan.visualClips
        // With no simultaneous layers or compositor-only edits, all visual clips can share one
        // decoder sequence. Keep this an explicit preview optimization: export still uses the
        // clock-backed graph, and projects needing composition retain their original layer order.
        val useSingleVisualInput = preferSingleVisualInput && (visualClips.isNotEmpty() || textTracks.isNotEmpty()) &&
            visualClips.all { it.transform == Transform() && it.properties.isEmpty() &&
                it.effects.filter { effect -> effect.enabled }.all { effect -> effects.capability(effect.type)?.singleInput == true } } &&
            plan.disjointVisuals
        val compiledVisualTracks = if (useSingleVisualInput && visualTracks.isNotEmpty()) {
            listOf(visualTracks.first().copy(clips = visualClips))
        } else visualTracks
        // Decode audio in dedicated sequences, even when it belongs to a video clip. This avoids
        // Media3's force-audio initialization race for mixed image/video/silent sequences and
        // gives every audio stream explicit gaps and the same project timestamps.
        val audioTracks = plan.audio.map { it.track }
        val sequenceTracks = compiledVisualTracks + audioTracks
        val sequences = mutableListOf<EditedMediaItemSequence>()
        if (!useSingleVisualInput || visualTracks.isEmpty()) sequences.add(clockSequence(ensureClockAsset(), project.durationUs, frameRate))
        val warnings = mutableListOf<String>()
        for (track in sequenceTracks) {
            coroutineContext.ensureActive()
            val trackTypes = if (track.type == TrackType.AUDIO) setOf(C.TRACK_TYPE_AUDIO) else setOf(C.TRACK_TYPE_VIDEO)
            val sequence = EditedMediaItemSequence.Builder(trackTypes)
            var cursorUs = 0L
            for (clip in track.clips.sortedBy { it.startUs }) {
                coroutineContext.ensureActive()
                if (clip.startUs > cursorUs) sequence.addGap(clip.startUs - cursorUs)
                val media = requireNotNull(project.media[clip.mediaId])
                sequence.addItem(compileClip(clip, media, track, outputWidth, outputHeight, frameRate))
                cursorUs = clip.endUs
            }
            if (cursorUs < project.durationUs) sequence.addGap(project.durationUs - cursorUs)
            sequences.add(sequence.build())
        }
        if (audioTracks.isEmpty()) {
            sequences.add(EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO)).addGap(project.durationUs).build())
        }
        if (!useSingleVisualInput && visualTracks.size > 3) {
            warnings += "This project has ${visualTracks.size} visual tracks; playback depends on the device's decoder capacity."
        }
        val compositionBuilder = Composition.Builder(sequences)
            .setHdrMode(Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL)
        val compositionEffects = mutableListOf<androidx.media3.common.Effect>()
        val requiresCompositor = !useSingleVisualInput && visualTracks.isNotEmpty()
        if (!requiresCompositor) {
            // Native video gaps decode as small black bitmaps. Apply the output canvas after
            // every item, including gaps, without requiring a second visual clock input.
            compositionEffects += Presentation.createForWidthAndHeight(outputWidth, outputHeight, Presentation.LAYOUT_SCALE_TO_FIT)
        } else {
            compositionBuilder.setVideoCompositorSettings(ProjectCompositor(plan.visuals, outputWidth, outputHeight))
        }
        // Text tracks form the title/caption stack above the visual composition. Chunking avoids
        // exceeding texture-unit limits on older GPUs while retaining the track stacking order.
        textTracks.filter { renderText }.map { TimelineTextOverlay(it, project.canvas, outputHeight, textBitmaps) }.chunked(8)
            .forEach { compositionEffects += OverlayEffect(it) }
        compositionBuilder.setEffects(Effects(emptyList(), compositionEffects))
        CompiledComposition(
            compositionBuilder.build(), project.durationUs, warnings, outputWidth, outputHeight, frameRate,
            requiresCompositor = requiresCompositor, plan = plan,
        )
    }

    private fun compileClip(
        clip: Clip,
        media: MediaReference,
        track: Track,
        width: Int,
        height: Int,
        frameRate: Int,
    ): EditedMediaItem {
        val item = MediaItem.Builder()
            .setUri(media.uri)
            .setMediaId(clip.id)
        if (media.mimeType.isNotBlank()) item.setMimeType(media.mimeType)
        val isImage = media.kind == MediaKind.IMAGE
        // ImageAssetLoader emits durationUs directly and does not apply source clipping.
        // Normalize stills to exact timeline duration, including split rounding and speed.
        // Original virtual source time remains in RenderPlan for property evaluation.
        val sourceDurationUs = if (isImage) clip.durationUs else media.durationUs
        if (isImage) item.setImageDurationMs((sourceDurationUs + 999L) / 1000L)
        item.setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionUs(if (isImage) 0 else clip.sourceRange.startUs)
                .setEndPositionUs(if (isImage) clip.durationUs else clip.sourceRange.endUs)
                .build(),
        )
        val audioEffects = mutableListOf<androidx.media3.common.audio.AudioProcessor>()
        if (track.type == TrackType.AUDIO || (!track.muted && media.hasAudio)) {
            audioEffects += ToInt16PcmAudioProcessor()
            audioEffects += ChannelMixingAudioProcessor().apply {
                for (channels in 1..6) {
                    putChannelMixingMatrix(ChannelMixingMatrix.createForConstantPower(channels, 2).scaleBy(clip.volume))
                }
            }
        }
        val videoEffects = mutableListOf<androidx.media3.common.Effect>()
        if (track.type != TrackType.AUDIO) {
            videoEffects += Presentation.createForWidthAndHeight(width, height, Presentation.LAYOUT_SCALE_TO_FIT)
        }
        for (effect in clip.effects.filter { it.enabled && track.type != TrackType.AUDIO }) {
            val rendered = effects.create(effect, clip, media)
            audioEffects += rendered.audioProcessors
            videoEffects += rendered.videoEffects
        }
        val builder = EditedMediaItem.Builder(item.build())
            .setDurationUs(sourceDurationUs)
            .setFrameRate(frameRate)
            .setEffects(Effects(audioEffects, videoEffects))
        if (!isImage) builder.setSpeed(ClipSpeedProvider(clip))
        if (track.type == TrackType.AUDIO) builder.setRemoveVideo(true)
        else builder.setRemoveAudio(true)
        return builder.build()
    }

    /** The transparent primary sequence supplies deterministic frame cadence and black empty canvas. */
    private fun clockSequence(uri: Uri, durationUs: Long, frameRate: Int): EditedMediaItemSequence {
        val item = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(MimeTypes.IMAGE_PNG)
            .setImageDurationMs((durationUs + 999) / 1000)
            .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder().setEndPositionUs(durationUs).build())
            .build()
        return EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_VIDEO))
            .addItem(EditedMediaItem.Builder(item).setDurationUs(durationUs).setFrameRate(frameRate).setRemoveAudio(true).build())
            .build()
    }

    private suspend fun ensureClockAsset(): Uri = withContext(Dispatchers.IO) {
        clockAssetMutex.withLock {
            val folder = File(context.filesDir, "render").apply { mkdirs() }
            val file = File(folder, "transparent-clock.png")
            if (!file.exists()) {
                val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
                try {
                    val temporary = File(folder, "transparent-clock.tmp")
                    temporary.outputStream().use { stream ->
                        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                    }
                    check(temporary.renameTo(file)) { "Cannot create the render canvas." }
                } finally {
                    bitmap.recycle()
                }
            }
            Uri.fromFile(file)
        }
    }
}

/** Input 0 is the clock; visual inputs 1..n are registered from topmost layer to bottommost. */
@UnstableApi
private class ProjectCompositor(private val tracks: List<RenderTrack>, private val width: Int, private val height: Int) : VideoCompositorSettings {
    private val hidden = object : OverlaySettings { override fun getAlphaScale(): Float = 0f }

    override fun getOutputSize(inputSizes: MutableList<Size>): Size = Size(width, height)

    override fun getOverlaySettings(inputId: Int, presentationTimeUs: Long): OverlaySettings {
        if (inputId == 0) return hidden
        val state = tracks.getOrNull(inputId - 1)?.stateAt(presentationTimeUs) ?: return hidden
        val transform = state.transform
        return object : OverlaySettings {
            override fun getAlphaScale(): Float = transform.opacity
            override fun getScale(): Pair<Float, Float> = Pair.create(transform.scaleX, transform.scaleY)
            override fun getRotationDegrees(): Float = transform.rotationDegrees
            override fun getBackgroundFrameAnchor(): Pair<Float, Float> = Pair.create(transform.translationX, transform.translationY)
        }
    }
}
