package com.bbvideoeditor.app.render

import android.graphics.Color
import android.os.Bundle
import android.os.Debug
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.app.EditorApplication
import com.bbvideoeditor.app.media.AssetPriority
import com.bbvideoeditor.app.media.ThumbnailRequest
import com.bbvideoeditor.core.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ResourcePressureTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val app=instrumentation.targetContext.applicationContext as EditorApplication

    @Test fun cacheEvictionAndMemoryCallbacksPreserveLiveConsumerBitmaps()=runBlocking {
        val image=MediaFixtures(app).image("resource-pressure",Color.CYAN)
        val before=Debug.getPss()
        val bitmaps=withTimeout(60_000) {
            coroutineScope { (0 until 48).map { i -> async(Dispatchers.Default) {
                app.thumbnails.thumbnail(ThumbnailRequest(image,0,512-i,512,
                    if(i>=40)AssetPriority.VISIBLE else AssetPriority.PREFETCH))
            }}.awaitAll().filterNotNull() }
        }
        assertTrue(bitmaps.isNotEmpty())
        assertTrue(app.thumbnails.memoryBytes<=app.resourceBudget.thumbnailBytes)
        assertTrue(app.thumbnails.queueMetrics().running<=app.resourceBudget.thumbnailWorkers)
        val text=TextBitmapCache(1024*1024)
        val canvas=CanvasSettings(1920,1080)
        val first=withContext(Dispatchers.Default){text.get(TextContent("Retained caption",fontSize=160f),canvas,720)}
        withContext(Dispatchers.Default){repeat(100){text.get(TextContent("Caption $it with unique raster",fontSize=160f),canvas,720)}}
        assertTrue(text.memoryBytes<=1024*1024)
        assertTrue(text.rasterCount.get()>=100)
        text.clearMemory()
        instrumentation.runOnMainSync{app.onTrimMemory(android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL);app.onLowMemory()}
        assertEquals(0,app.thumbnails.memoryBytes);assertEquals(0,text.memoryBytes)
        assertFalse(first.bitmap.isRecycled)
        first.bitmap.getPixel(0,0)
        bitmaps.forEach{assertFalse(it.isRecycled);it.getPixel(0,0)}
        instrumentation.sendStatus(2,Bundle().apply{putString("foundation_metrics",
            "resource-pressure {pssBeforeKb=$before, pssAfterKb=${Debug.getPss()}, liveThumbnails=${bitmaps.size}, textRasters=${text.rasterCount.get()}, thumbnailQueue=${app.thumbnails.queueMetrics()}}")})
    }
}
