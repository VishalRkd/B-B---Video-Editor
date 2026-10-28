package com.bbvideoeditor.app.render

import android.graphics.Color
import androidx.media3.common.C
import androidx.media3.common.VideoCompositorSettings
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Effects
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.core.CanvasSettings
import com.bbvideoeditor.core.Clip
import com.bbvideoeditor.core.EffectSpec
import com.bbvideoeditor.core.Project
import com.bbvideoeditor.core.TimeRange
import com.bbvideoeditor.core.Timeline
import com.bbvideoeditor.core.Track
import com.bbvideoeditor.core.TrackType
import com.bbvideoeditor.core.Transform
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@UnstableApi
@RunWith(AndroidJUnit4::class)
class PreviewCompilationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun disjointVisualLayersKeepExactClipTimingAndAudioWhenFlattened() = runBlocking {
        val fixtures = MediaFixtures(context)
        val red = fixtures.image("single-input-red", Color.RED)
        val blue = fixtures.image("single-input-blue", Color.BLUE)
        val audio = fixtures.audio()
        val first = Clip(
            mediaId = red.id, startUs = 250_000, sourceRange = TimeRange(125_000, 1_000_000), speed = 2f,
            effects = listOf(EffectSpec(type = "disabled-effect", enabled = false)),
        )
        val second = Clip(mediaId = blue.id, startUs = 1_000_000, sourceRange = TimeRange(300_000, 500_000))
        val sound = Clip(mediaId = audio.id, startUs = 0, sourceRange = TimeRange(0, 2_000_000), volume = .25f)
        val project = Project(
            canvas = CanvasSettings(320, 180, 30),
            media = listOf(red, blue, audio).associateBy { it.id },
            timeline = Timeline(listOf(
                Track(name = "Main", type = TrackType.VIDEO, clips = listOf(first)),
                Track(name = "Upper", type = TrackType.OVERLAY, clips = listOf(second)),
                Track(name = "Sound", type = TrackType.AUDIO, clips = listOf(sound)),
            )),
        )
        val compiler = CompositionCompiler(context)
        val preview = compiler.compile(project, preferSingleVisualInput = true)
        val export = compiler.compile(project)

        assertFalse(preview.requiresCompositor)
        assertEquals(project.durationUs, preview.durationUs)
        assertEquals(2, preview.composition.sequences.size)
        assertEquals(VideoCompositorSettings.DEFAULT, preview.composition.videoCompositorSettings)
        assertTrue(preview.composition.effects.videoEffects.single() is Presentation)
        val visual = preview.composition.sequences[0]
        assertEquals(setOf(C.TRACK_TYPE_VIDEO), visual.trackTypes)
        assertEquals(listOf(250_000L, 500_000L, 250_000L, 500_000L, 500_000L),
            visual.editedMediaItems.map { it.presentationDurationUs })
        val firstItem = visual.editedMediaItems[1]
        val secondItem = visual.editedMediaItems[3]
        assertEquals(first.id, firstItem.mediaItem.mediaId)
        assertEquals(second.id, secondItem.mediaItem.mediaId)
        assertEquals(red.uri, firstItem.mediaItem.localConfiguration!!.uri.toString())
        assertEquals(0L, firstItem.mediaItem.clippingConfiguration.startPositionUs)
        assertEquals(first.durationUs, firstItem.mediaItem.clippingConfiguration.endPositionUs)
        assertEquals(first.durationUs, firstItem.durationUs) // ImageAssetLoader ignores source clipping.
        assertEquals(1f, firstItem.speedProvider.getSpeed(0), 0f)
        assertEquals(0L, secondItem.mediaItem.clippingConfiguration.startPositionUs)
        assertEquals(second.durationUs, secondItem.mediaItem.clippingConfiguration.endPositionUs)
        assertTrue(firstItem.removeAudio)
        assertTrue(secondItem.removeAudio)

        val previewAudio = preview.composition.sequences[1]
        val exportAudio = export.composition.sequences.last()
        assertEquals(setOf(C.TRACK_TYPE_AUDIO), previewAudio.trackTypes)
        assertEquals(exportAudio.editedMediaItems.map { it.mediaItem }, previewAudio.editedMediaItems.map { it.mediaItem })
        assertEquals(exportAudio.editedMediaItems.map { it.presentationDurationUs },
            previewAudio.editedMediaItems.map { it.presentationDurationUs })
        assertEquals(exportAudio.editedMediaItems.single().effects.audioProcessors.map { it.javaClass },
            previewAudio.editedMediaItems.single().effects.audioProcessors.map { it.javaClass })
        assertTrue(previewAudio.editedMediaItems.single().removeVideo)

        // Default compilation remains the export graph: clock, both original layers, and audio.
        assertTrue(export.requiresCompositor)
        assertEquals(4, export.composition.sequences.size)
        assertTrue(export.composition.effects.videoEffects.isEmpty())
        assertEquals(second.id, export.composition.sequences[1].editedMediaItems[1].mediaItem.mediaId)
        assertEquals(first.id, export.composition.sequences[2].editedMediaItems[1].mediaItem.mediaId)
    }

    @Test fun simultaneousLayersAndNonidentityTransformsRetainCompositor() = runBlocking {
        val media = MediaFixtures(context).image("single-input-eligibility", Color.RED)
        val first = Clip(mediaId = media.id, startUs = 0, sourceRange = TimeRange(0, 1_000_000))
        val second = first.copy(id = "upper-clip", startUs = 500_000)
        val overlapping = Project(
            canvas = CanvasSettings(320, 180, 30), media = mapOf(media.id to media),
            timeline = Timeline(listOf(
                Track(name = "Main", type = TrackType.VIDEO, clips = listOf(first)),
                Track(name = "Upper", type = TrackType.OVERLAY, clips = listOf(second)),
            )),
        )
        val compiler = CompositionCompiler(context)
        assertTrue(compiler.compile(overlapping, preferSingleVisualInput = true).requiresCompositor)
        val transformed = overlapping.copy(timeline = Timeline(listOf(
            overlapping.timeline.tracks.first().copy(clips = listOf(first.copy(transform = Transform(opacity = .5f)))),
        )))
        assertTrue(compiler.compile(transformed, preferSingleVisualInput = true).requiresCompositor)
        val touching = overlapping.copy(timeline = Timeline(listOf(
            overlapping.timeline.tracks.first(),
            overlapping.timeline.tracks.last().copy(clips = listOf(second.copy(startUs = first.endUs))),
        )))
        assertFalse(compiler.compile(touching, preferSingleVisualInput = true).requiresCompositor)
    }

    @Test fun enabledEffectsKeepOriginalGraphEvenWhenTheRegisteredEffectIsANoOp() = runBlocking {
        val media = MediaFixtures(context).image("single-input-effect", Color.BLUE)
        val clip = Clip(mediaId = media.id, startUs = 0, sourceRange = TimeRange(0, 1_000_000),
            effects = listOf(EffectSpec(type = "registered-effect")))
        val project = Project(canvas = CanvasSettings(320, 180, 30), media = mapOf(media.id to media),
            timeline = Timeline(listOf(Track(name = "Main", type = TrackType.VIDEO, clips = listOf(clip)))))
        val registry = RenderEffectRegistry(mapOf("registered-effect" to RenderEffectFactory { _, _, _ ->
            Effects(emptyList(), emptyList())
        }))
        assertTrue(CompositionCompiler(context, registry).compile(project, preferSingleVisualInput = true).requiresCompositor)
    }

    @Test fun audioOnlyProjectRetainsItsCanvasClock() = runBlocking {
        val audio = MediaFixtures(context).audio()
        val clip = Clip(mediaId = audio.id, startUs = 0, sourceRange = TimeRange(0, 1_000_000))
        val project = Project(canvas = CanvasSettings(320, 180, 30), media = mapOf(audio.id to audio),
            timeline = Timeline(listOf(Track(name = "Sound", type = TrackType.AUDIO, clips = listOf(clip)))))
        val compiled = CompositionCompiler(context).compile(project, preferSingleVisualInput = true)
        assertFalse(compiled.requiresCompositor)
        assertEquals(2, compiled.composition.sequences.size)
        assertEquals(setOf(C.TRACK_TYPE_VIDEO), compiled.composition.sequences.first().trackTypes)
        assertEquals(project.durationUs, compiled.composition.sequences.first().editedMediaItems.single().presentationDurationUs)
    }
}
