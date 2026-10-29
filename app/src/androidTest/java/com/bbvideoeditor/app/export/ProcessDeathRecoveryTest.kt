package com.bbvideoeditor.app.export

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.app.EditorApplication
import com.bbvideoeditor.app.MainActivity
import com.bbvideoeditor.app.render.MediaFixtures
import com.bbvideoeditor.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Two separate runner invocations, with an external force-stop between them. Never terminates
 * an application itself. See tools/FOUNDATION_BENCHMARKS.md for the bounded protocol. */
@UnstableApi
class ProcessDeathRecoveryTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context=instrumentation.targetContext
    private val app=context.applicationContext as EditorApplication
    private val marker=File(context.filesDir,"foundation_process_death.txt")

    @Test fun armInterruptedExportAndSavedProject()=runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("foundationProcessDeath")=="arm")
        check(!marker.exists()){ "Verify/clean the previous process-death fixture first" }
        val image=MediaFixtures(context).image("process-death-fixture",Color.MAGENTA)
        val project=Project(name="Foundation process-death fixture",canvas=CanvasSettings(320,180),media=mapOf(image.id to image),
            timeline=Timeline(listOf(Track(name="V",type=TrackType.VIDEO,clips=listOf(
                Clip(mediaId=image.id,startUs=0,sourceRange=TimeRange(0,120_000_000)))))))
        app.projects.save(project)
        ActivityScenario.launch(MainActivity::class.java).use {
            instrumentation.runOnMainSync{app.exports.start(project,ExportSettings(resolution=720,bitrate=2_000_000))}
            val deadline=SystemClock.elapsedRealtime()+30_000
            while(app.exports.state.value.projectId!=project.id || app.exports.state.value.phase!=ExportPhase.RUNNING) {
                check(SystemClock.elapsedRealtime()<deadline){"Export did not start: ${app.exports.state.value}"};Thread.sleep(20)
            }
            val job=requireNotNull(app.exports.state.value.jobId)
            val status=File(context.filesDir,"exports/status.json")
            while(!runCatching{kotlinx.serialization.json.Json.decodeFromString<ExportState>(status.readText()).jobId==job}.getOrDefault(false)) {
                check(SystemClock.elapsedRealtime()<deadline){"Export status was not durably written"};Thread.sleep(10)
            }
            marker.outputStream().use{output->output.write("${project.id}\n$job\n${image.uri}\n".toByteArray());output.fd.sync()}
            instrumentation.sendStatus(2,Bundle().apply{putString("foundation_process_death_ready",job)})
            // The host force-stops only this app after seeing the durable ready marker.
            Thread.sleep(60_000)
            fail("Host did not interrupt the armed process within 60 seconds")
        }
    }

    @Test fun verifyInterruptedExportAndSavedProjectAfterFreshProcess()=runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("foundationProcessDeath")=="verify")
        val lines=marker.readLines();val projectId=lines[0];val job=lines[1]
        try {
            app.exports.awaitRecovery()
            val project=app.projects.load(projectId)
            assertEquals(120_000_000L,project.durationUs)
            assertEquals(lines[2],project.media.values.single().uri)
            assertEquals(job,app.exports.state.value.jobId)
            assertEquals(ExportPhase.FAILED,app.exports.state.value.phase)
            assertTrue(app.exports.state.value.error.orEmpty().contains("interrupted"))
            assertFalse(File(app.exports.jobDirectory(job),"render.mp4").exists())
            assertFalse(File(app.exports.jobDirectory(job),"request.json").exists())
        }finally {
            app.projects.delete(projectId)
            Uri.parse(lines[2]).path?.let{File(it).delete()}
            marker.delete()
        }
    }
}
