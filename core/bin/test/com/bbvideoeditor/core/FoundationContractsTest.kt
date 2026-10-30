package com.bbvideoeditor.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class FoundationContractsTest {
    private val media=MediaReference(id="media",uri="file:/original",displayName="original",kind=MediaKind.VIDEO,durationUs=60_000_000,hasAudio=true)
    private val clip=Clip(id="a",mediaId=media.id,startUs=1_000_000,sourceRange=TimeRange(3_000_000,8_000_000),speed=2f,
        properties=listOf(AnimatedProperty("opacity",1f,listOf(Keyframe(3_000_000,0f),Keyframe(11_000_000,1f)))))
    private fun project()=Project(media=mapOf(media.id to media),timeline=Timeline(listOf(Track(id="v",name="V",type=TrackType.VIDEO,clips=listOf(clip)))))
    @Test fun outputEvaluationPreservesSourceDomainAcrossSplitAndHeldCadences() {
        val original=project()
        val split=SplitClip(clip.id,2_750_000,"b").apply(original)
        for(time in listOf(1_000_000L,1_500_000,2_749_999,2_750_000,4_999_999)) {
            val before=RenderPlan.derive(original).evaluateFrame(time).visuals.single()
            val after=RenderPlan.derive(split).evaluateFrame(time).visuals.single()
            assertEquals(before.time.source,after.time.source)
            assertEquals(before.transform.opacity,after.transform.opacity,.00001f)
            assertEquals(TimelineTime(time),after.time.timeline)
            assertEquals(GeometryStage.DECODE_ORIENTATION,canonicalGeometryStages.first())
            assertEquals(GeometryStage.OUTPUT_ENCODING,canonicalGeometryStages.last())
        }
        assertTrue(RenderPlan.derive(split).evaluateFrame(5_000_000).visuals.isEmpty())
    }
    @Test fun snapIndexMatchesExistingSemanticsAcrossRandomPositionsAndDynamicPlayheads() {
        val clips=(0..399).map { i -> clip.copy(id="$i",startUs=i*200_000L,sourceRange=TimeRange(0,100_000),speed=1f) }
        val project=project().copy(timeline=Timeline(listOf(Track(name="V",type=TrackType.VIDEO,clips=clips))))
        val random=Random(437)
        for(excluded in listOf(null,"2","200","399")) {
            val index=SnapIndex(project,excluded)
            repeat(1000) {
                val time=random.nextLong(0,81_000_000);val playhead=random.nextLong(0,81_000_000)
                val targets=TimelineSnapper.targets(project,excluded,playhead)
                assertEquals(TimelineSnapper.snap(time,targets,80_000),index.snap(time,80_000,playhead))
                assertEquals(TimelineSnapper.snapMove(time,100_000,targets,80_000),index.snapMove(time,100_000,80_000,playhead))
            }
        }
    }
    @Test fun localGestureEditsMatchCommittedCommandsAndRejectCollisions() {
        val base=project().copy(timeline=Timeline(listOf(Track(id="v",name="V",type=TrackType.VIDEO,clips=listOf(clip)),
            Track(id="o",name="O",type=TrackType.OVERLAY))))
        val gesture=GesturePreview(base)
        for(command in listOf<EditorCommand>(MoveClip("a","o",500_000),TrimClip("a",TimeRange(4_000_000,4_000_000),1_500_000),
                UpdateClip("a",transform=Transform(translationX=.4f)))) {
            assertEquals(command.apply(base),gesture.apply(command))
            gesture.apply(command).validate()
        }
        val occupied=base.copy(timeline=base.timeline.copy(tracks=base.timeline.tracks.map { if(it.id=="o")it.copy(clips=listOf(clip.copy(id="occupied"))) else it }))
        assertThrows(IllegalArgumentException::class.java){GesturePreview(occupied).apply(MoveClip("a","o",1_500_000))}
    }
    @Test fun relinkPreservesAllEditsAndHistoryProtectsBothSources() {
        val project=project();val history=EditorHistory(project)
        history.execute(RelinkMedia(media.id,media.copy(id="new",uri="file:/replacement",displayName="replacement")))
        assertEquals(project.timeline,history.project.timeline)
        assertEquals(setOf("file:/original","file:/replacement"),history.referencedMedia)
        assertTrue(history.undo());assertEquals(media,history.project.media[media.id])
        assertThrows(IllegalArgumentException::class.java) { RelinkMedia(media.id,media.copy(durationUs=1_000_000)).apply(project) }
        assertThrows(IllegalArgumentException::class.java) { RelinkMedia(media.id,media.copy(hasAudio=false)).apply(project) }
    }
    @Test fun transitionEvaluationCarriesTwoInputsAndExplicitProgress() {
        val first=clip.copy(id="out",startUs=0,sourceRange=TimeRange(0,3_000_000),speed=1f)
        val second=clip.copy(id="in",startUs=2_000_000,sourceRange=TimeRange(0,3_000_000),speed=1f)
        val spec=TransitionSpec(type="future",durationUs=1_000_000,fromClipId=first.id,toClipId=second.id)
        val project=project().copy(timeline=Timeline(listOf(Track(name="out",type=TrackType.VIDEO,clips=listOf(first)),
            Track(name="in",type=TrackType.OVERLAY,clips=listOf(second))),listOf(spec)))
        val transition=RenderPlan.derive(project).evaluateFrame(2_500_000).transitions.single()
        assertEquals("out",transition.outgoing.clip.id);assertEquals("in",transition.incoming.clip.id)
        assertEquals(.5f,transition.progress,0f)
    }
}
