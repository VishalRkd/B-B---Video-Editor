package com.bbvideoeditor.app.render

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.app.MainActivity
import com.bbvideoeditor.app.playback.PreviewEngine
import com.bbvideoeditor.core.Project
import kotlinx.coroutines.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Shared real-surface conformance harness, also used by repeatable engine performance tests. */
@UnstableApi
internal class PreviewTestSession(project: Project, readyTimeoutMs:Long=25_000,
    engineFactory:(()->PreviewEngine)?=null) : AutoCloseable {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    val engine=main {engineFactory?.invoke() ?: PreviewEngine(instrumentation.targetContext,CompositionCompiler(instrumentation.targetContext))}
    private val scenario=ActivityScenario.launch(MainActivity::class.java)
    private lateinit var view:PlayerView
    var observation: String = ""
    private var binding:Job?=null
    init {
        scenario.onActivity { activity ->
            view=PlayerView(activity).apply{useController=false};activity.setContentView(view)
            binding=CoroutineScope(Dispatchers.Main.immediate).launch{engine.state.collect{view.player=it.player}}
        }
        await("attached native preview surface") {
            main{(view.videoSurfaceView as? SurfaceView)?.holder?.surface?.isValid==true}
        }
        main{engine.updateProject(project)}
        await("native preview ready",readyTimeoutMs) {
            check(engine.state.value.error==null){engine.state.value.error.orEmpty()}
            main{engine.state.value.player?.playbackState==Player.STATE_READY}
        }
    }
    fun <T> main(block:()->T):T {
        var value:T?=null;instrumentation.runOnMainSync{value=block()}
        @Suppress("UNCHECKED_CAST") return value as T
    }
    fun frame():Bitmap? {
        val surface=main{view.videoSurfaceView as SurfaceView}
        val size=main{surface.width to surface.height}
        if(size.first<=0 || size.second<=0 || !surface.holder.surface.isValid)return null
        val raw=Bitmap.createBitmap(size.first,size.second,Bitmap.Config.ARGB_8888)
        val latch=CountDownLatch(1);var result=-1
        PixelCopy.request(surface,raw,{result=it;latch.countDown()},Handler(Looper.getMainLooper()))
        check(latch.await(5,TimeUnit.SECONDS)){"PixelCopy timed out"}
        if(result!=PixelCopy.SUCCESS){raw.recycle();return null}
        val width=minOf(raw.width,raw.height*16/9);val height=width*9/16
        val cropped=Bitmap.createBitmap(raw,(raw.width-width)/2,(raw.height-height)/2,width,height)
        if(cropped!==raw)raw.recycle()
        return cropped
    }
    fun await(label:String,timeoutMs:Long=25_000,condition:()->Boolean) {
        val deadline=SystemClock.elapsedRealtime()+timeoutMs
        while(!condition()) {
            check(SystemClock.elapsedRealtime()<deadline){"Timed out: $label; $observation; ${engine.state.value}"}
            Thread.sleep(20)
        }
    }
    override fun close() {
        main{binding?.cancel();view.player=null;engine.close()};scenario.close()
    }
}
