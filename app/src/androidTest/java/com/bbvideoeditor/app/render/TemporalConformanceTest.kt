package com.bbvideoeditor.app.render

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.app.export.*
import com.bbvideoeditor.app.media.MediaImporter
import com.bbvideoeditor.app.media.WaveformRepository
import com.bbvideoeditor.core.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

@UnstableApi
@RunWith(AndroidJUnit4::class)
class TemporalConformanceTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val fixtures=TemporalFixtures(context)
    private fun assertIdentity(bitmap:Bitmap,source:Int,frame:Int,left:Float=0f,top:Float=0f,width:Float=1f,height:Float=1f) {
        val actual=fixtures.decode(bitmap,left,top,width,height)
        assertNotNull("No valid source/frame barcode for source $source frame $frame",actual)
        assertEquals(source,actual!!.source)
        assertTrue("Expected frame $frame, got $actual",abs(actual.frame-frame)<=1)
    }
    @Test fun encodedFixturesIdentifyRealFramesAt24_30_60Fps() {
        for(fps in listOf(24,30,60)) {
            val media=fixtures.video(fps=fps)
            val reader=MediaMetadataRetriever()
            try {
                reader.setDataSource(context,Uri.parse(media.uri))
                for(time in listOf(250_000L,1_150_000L,2_650_000L)) {
                    val frame=requireNotNull(reader.getFrameAtTime(time,MediaMetadataRetriever.OPTION_CLOSEST))
                    try{assertIdentity(frame,1,(time*fps/1_000_000).toInt())}finally{frame.recycle()}
                }
            }finally{reader.release()}
        }
    }
    @Test fun trimmedSplitSpeedGapsAndAudioPulsesRemainAlignedInExport()=runBlocking {
        val media=fixtures.video(audio=true)
        val original=Clip(mediaId=media.id,startUs=200_000,sourceRange=TimeRange(500_000,2_000_000),speed=2f)
        val later=Clip(mediaId=media.id,startUs=1_500_000,sourceRange=TimeRange(0,1_500_000))
        val base=Project(canvas=CanvasSettings(320,180),media=mapOf(media.id to media),timeline=Timeline(listOf(Track(name="Video",type=TrackType.VIDEO,clips=listOf(original,later)))))
        val project=SplitClip(original.id,700_000).apply(base)
        val output=File(context.cacheDir,"temporal-export-${newId()}.mp4")
        try {
            withTimeout(120_000){Media3RenderEngine(context,CompositionCompiler(context)).render(project,ExportSettings(resolution=720,bitrate=4_000_000),output)}
            val reader=MediaMetadataRetriever()
            try {
                reader.setDataSource(output.absolutePath)
                for(time in listOf(350_000L,650_000L,850_000L,1_650_000L,2_650_000L)) {
                    val state=RenderPlan.derive(project).frameAt(time).visuals.single()
                    val frame=requireNotNull(reader.getFrameAtTime(time,MediaMetadataRetriever.OPTION_CLOSEST))
                    try{assertIdentity(frame,1,(state.sourceTimeUs*30/1_000_000).toInt())}finally{frame.recycle()}
                }
                val gap=requireNotNull(reader.getFrameAtTime(1_350_000,MediaMetadataRetriever.OPTION_CLOSEST))
                try{assertNull(fixtures.decode(gap))}finally{gap.recycle()}
            }finally{reader.release()}
            val imported=MediaImporter(context).importMedia(listOf(Uri.fromFile(output))).media.single()
            val envelope=WaveformRepository(context).waveform(imported,2048)
            fun maximum(time:Long,radius:Long):Float {
                val start=((time-radius).coerceAtLeast(0).toDouble()/imported.durationUs*envelope.size).toInt().coerceIn(envelope.indices)
                val end=((time+radius).toDouble()/imported.durationUs*envelope.size).toInt().coerceIn(start,envelope.lastIndex)
                return (start..end).maxOf{envelope[it]}
            }
            // AAC fixture encoding can add codec priming. Map the independently decoded SOURCE
            // pulse positions through the real edit, instead of assuming compressed audio begins
            // at a PCM sample that the fixture encoder did not actually produce.
            val sourceEnvelope=WaveformRepository(context).waveform(media,4096)
            val markers=sourceEnvelope.indices.filter { sourceEnvelope[it]>.1f && (it==0 || sourceEnvelope[it-1]<=.1f) }
                .map { it*media.durationUs/sourceEnvelope.size }
            assertTrue("Fixture contains no deterministic source pulses",markers.size>=3)
            val mapped=project.timeline.clips.flatMap { clip -> markers.filter { it in clip.sourceRange }.map(clip::timelineTimeAt) }
            assertEquals("Expected both speed-adjusted pulses and the later source's pulse",3,mapped.size)
            for(time in mapped)assertTrue("Missing A/V pulse near $time. Peaks: ${envelope.indices.filter { envelope[it]>.1f }.map { it*imported.durationUs/envelope.size }.filterIndexed { i,_ -> i%8==0 }}",maximum(time,50_000)>.1f)
            assertTrue("Gap contains unexpected audio",maximum(1_350_000,50_000)<.02f)
        }finally{output.delete()}
    }
    @Test fun longerMixedCadenceSourcesKeepPcmPulsesAlignedThroughSpeedSplitAndLaterClips()=runBlocking {
        val video=fixtures.video(seconds=12,source=1)
        val later=fixtures.video(fps=24,seconds=4,source=2)
        val pcm=fixtures.pulsesWav(16)
        val first=Clip(mediaId=video.id,startUs=200_000,sourceRange=TimeRange(500_000,10_000_000),speed=1.25f)
        val next=Clip(mediaId=later.id,startUs=8_500_000,sourceRange=TimeRange(200_000,3_000_000),speed=.75f)
        val soundFirst=first.copy(id=newId(),mediaId=pcm.id)
        val soundNext=next.copy(id=newId(),mediaId=pcm.id)
        var project=Project(canvas=CanvasSettings(320,180),media=listOf(video,later,pcm).associateBy{it.id},timeline=Timeline(listOf(
            Track(name="numbered video",type=TrackType.VIDEO,clips=listOf(first,next)),
            Track(name="sample-exact audio",type=TrackType.AUDIO,clips=listOf(soundFirst,soundNext)))))
        project=SplitClip(first.id,3_700_000).apply(project)
        project=SplitClip(soundFirst.id,3_700_000).apply(project)
        val output=File(context.cacheDir,"longer-temporal-${newId()}.mp4")
        try {
            val result=withTimeout(180_000){Media3RenderEngine(context,CompositionCompiler(context)).render(project,
                ExportSettings(resolution=720,bitrate=4_000_000),output)}
            assertTrue(result.cadence!!.frameCount>200)
            val reader=MediaMetadataRetriever()
            try {
                reader.setDataSource(output.path)
                for(time in listOf(900_000L,3_300_000L,4_250_000L,7_700_000L,9_250_000L,11_200_000L)) {
                    val layer=RenderPlan.derive(project).evaluateFrame(time).visuals.single()
                    val source=project.media.getValue(layer.clip.mediaId!!)
                    val image=requireNotNull(reader.getFrameAtTime(time,MediaMetadataRetriever.OPTION_CLOSEST))
                    try{assertIdentity(image,if(source.id==video.id)1 else 2,(layer.time.source.us*source.frameRate/1_000_000).toInt())}
                    finally{image.recycle()}
                }
            }finally{reader.release()}
            val exported=MediaImporter(context).importMedia(listOf(Uri.fromFile(output))).media.single()
            val envelope=WaveformRepository(context).waveform(exported,4096)
            val audible=project.timeline.tracks.first{it.type==TrackType.AUDIO}.clips
            val expected=audible.flatMap { clip -> (0 until 16).map{it*1_000_000L+500_000}.filter{it in clip.sourceRange}.map(clip::timelineTimeAt) }
            InstrumentationRegistry.getInstrumentation().sendStatus(2,android.os.Bundle().apply{putString("temporal_audio",expected.associateWith { target ->
                envelope.indices.filter{envelope[it]>.1f}.map{it*exported.durationUs/envelope.size}.minByOrNull{kotlin.math.abs(it-target)}
            }.toString())})
            for(time in expected) {
                val a=((time-50_000).coerceAtLeast(0)*envelope.size/exported.durationUs).toInt().coerceIn(envelope.indices)
                val b=((time+50_000)*envelope.size/exported.durationUs).toInt().coerceIn(a,envelope.lastIndex)
                assertTrue("Missing later-form A/V pulse at $time; nearby=${envelope.indices.filter{envelope[it]>.1f && kotlin.math.abs(it*exported.durationUs/envelope.size-time)<400_000}.map{it*exported.durationUs/envelope.size}.filterIndexed{i,_->i%8==0}}",(a..b).maxOf{envelope[it]}>.1f)
            }
            val gap=(8_300_000L*envelope.size/exported.durationUs).toInt().coerceIn(envelope.indices)
            assertTrue(envelope[gap]<.02f)
        }finally{output.delete()}
    }
    @Test fun rapidLayeredSeeksConvergeToSourceFramesAcrossGapsAndSwitches() {
        val base=fixtures.video(source=1)
        val overlay=fixtures.video(fps=24,source=2)
        val first=Clip(mediaId=base.id,startUs=0,sourceRange=TimeRange(300_000,1_500_000))
        val second=Clip(mediaId=base.id,startUs=2_000_000,sourceRange=TimeRange(1_000_000,1_500_000))
        val upper=Clip(mediaId=overlay.id,startUs=500_000,sourceRange=TimeRange(200_000,800_000),transform=Transform(scaleX=.35f,scaleY=.35f))
        val project=Project(canvas=CanvasSettings(320,180),media=listOf(base,overlay).associateBy{it.id},timeline=Timeline(listOf(
            Track(name="base",type=TrackType.VIDEO,clips=listOf(first,second)),Track(name="overlay",type=TrackType.OVERLAY,clips=listOf(upper)))))
        PreviewTestSession(project).use { session ->
            for(target in listOf(850_000L,2_650_000L,250_000L,1_750_000L,1_100_000L)) {
                session.main{repeat(50){session.engine.seek((it*97_000L)%project.durationUs)};session.engine.seek(target)}
                val state=RenderPlan.derive(project).frameAt(target)
                session.await("correct source frames at $target") {
                    check(session.engine.state.value.error==null){session.engine.state.value.error.orEmpty()}
                    val frame=session.frame() ?: return@await false
                    try {
                        if(state.visuals.isEmpty())fixtures.decode(frame)==null
                        else {
                            val lower=state.visuals.last()
                            val a=fixtures.decode(frame)
                            val upperState=state.visuals.firstOrNull{it.clip.id==upper.id}
                            val b=upperState?.let{fixtures.decode(frame,.325f,.325f,.35f,.35f)}
                            session.observation="base=$a overlay=$b expectedBase=${lower.sourceTimeUs} expectedOverlay=${upperState?.sourceTimeUs}"
                            a?.source==1 && abs(a.frame-(lower.sourceTimeUs*30/1_000_000).toInt())<=1 &&
                                (upperState==null || (b?.source==2 && abs(b.frame-(upperState.sourceTimeUs*24/1_000_000).toInt())<=1))
                        }
                    }finally{frame.recycle()}
                }
            }
        }
    }
    @Test fun sourceTimedPositionAnimationUsesOutputTimeAcrossMixedCadenceSeeks() {
        val media=fixtures.video(fps=24)
        val clip=Clip(mediaId=media.id,startUs=0,sourceRange=TimeRange(300_000,2_000_000),transform=Transform(scaleX=.5f,scaleY=.5f),
            properties=listOf(AnimatedProperty("translationX",0f,listOf(Keyframe(300_000,-.4f),Keyframe(2_300_000,.4f)))))
        val project=Project(canvas=CanvasSettings(320,180,60),media=mapOf(media.id to media),timeline=Timeline(listOf(Track(name="animated",type=TrackType.VIDEO,clips=listOf(clip)))))
        PreviewTestSession(project).use{session ->
            for(target in listOf(250_000L,850_000L,1_550_000L,450_000L)) {
                session.main{session.engine.seek(target)}
                val state=RenderPlan.derive(project).frameAt(target).visuals.single()
                val left=.25f+state.transform.translationX/2
                session.await("animated source frame/geometry at $target") {
                    val frame=session.frame() ?: return@await false
                    try{fixtures.decode(frame,left,.25f,.5f,.5f)?.let{it.source==1 && abs(it.frame-(state.sourceTimeUs*24/1_000_000).toInt())<=1}==true}
                    finally{frame.recycle()}
                }
            }
        }
    }
}
