package com.bbvideoeditor.app.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.media3.common.util.UnstableApi
import com.bbvideoeditor.app.export.*
import com.bbvideoeditor.app.render.CompositionCompiler
import com.bbvideoeditor.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

data class ResolvedPreviewMedia(val sources: Map<String,MediaReference>, private val release:()->Unit) : AutoCloseable {
    override fun close()=release()
}

/** Internal source optimization only. Original Project/media IDs/time ranges remain authoritative;
 * audio and export always open originals. One cancellable Transformer/image job owns generation.
 * Completed files are atomically promoted, identity-versioned, byte-bounded and leased by preview. */
@UnstableApi
class OptimizedPreviewMedia(context:Context,private val compiler:CompositionCompiler,
    private val budget:com.bbvideoeditor.app.playback.EngineResourceBudget=com.bbvideoeditor.app.playback.EngineResourceBudget.inspect(context)) {
    private val context=context.applicationContext
    private val folder=File(this.context.cacheDir,"optimized_preview_v1")
    private val generation=Mutex()
    private val ownership=Any()
    private val pins=mutableMapOf<File,Int>()
    suspend fun resolve(project:Project):ResolvedPreviewMedia {
        val acquired=mutableListOf<File>()
        try {
            val variants=mutableMapOf<String,MediaReference>()
            val ids=project.timeline.tracks.filter{it.type in listOf(TrackType.VIDEO,TrackType.OVERLAY)}
                .flatMap{it.clips}.mapNotNull{it.mediaId}.distinct()
            for(id in ids) {
                coroutineContext.ensureActive()
                val source=requireNotNull(project.media[id])
                if(source.kind==MediaKind.AUDIO)continue
                val (file,variant)=generation.withLock {
                    withContext(Dispatchers.IO){folder.mkdirs()}
                    val identity=withContext(Dispatchers.IO) {
                        val localVersion=Uri.parse(source.uri).takeIf{it.scheme=="file"}?.path?.let{File(it).lastModified()} ?: 0
                        cacheKey("v1:720:original-time|${source.uri}|${source.sizeBytes}|${source.durationUs}|${source.width}|${source.height}|${source.rotationDegrees}|${source.frameRate}|${source.colorTransfer}|$localVersion")
                    }
                    val file=File(folder,identity+if(source.kind==MediaKind.IMAGE)".png" else ".mp4")
                    var metadata=withContext(Dispatchers.IO){probe(file,source)}
                    if(metadata==null) {
                        val temporary=File(folder,"$identity-${newId()}.partial"+if(source.kind==MediaKind.IMAGE)".png" else ".mp4")
                        try {
                            if(source.kind==MediaKind.IMAGE)withContext(Dispatchers.IO){sampleImage(source,temporary)}
                            else {
                                val width=source.displayWidth/2*2;val height=source.displayHeight/2*2
                                val silentSource=source.copy(hasAudio=false)
                                val normalization=Project(canvas=CanvasSettings(width,height),media=mapOf(id to silentSource),
                                    timeline=Timeline(listOf(Track(name="Source normalization",type=TrackType.VIDEO,
                                        clips=listOf(Clip(mediaId=id,startUs=0,sourceRange=TimeRange(0,source.durationUs)))))))
                                val fps=listOf(24,25,30,50,60).minBy{kotlin.math.abs(it-(source.frameRate.takeIf{it>0} ?: 30f))}
                                // Legacy Transformer supports SDR normalization/HDR tone-map before
                                // the synchronized preview backend, which currently accepts SDR.
                                Media3RenderEngine(context,compiler,synchronizedFrames=false).render(normalization,
                                    ExportSettings(resolution=720,frameRate=fps,bitrate=5_000_000),temporary)
                            }
                            metadata=withContext(Dispatchers.IO) {
                                val verified=requireNotNull(probe(temporary,source)){"Optimized source could not be verified"}
                                require(temporary.renameTo(file)){"Could not commit optimized media"}
                                verified.copy(uri=Uri.fromFile(file).toString())
                            }
                        } finally { withContext(NonCancellable+Dispatchers.IO){temporary.delete()} }
                    }
                    synchronized(ownership){pins[file]=(pins[file] ?: 0)+1}
                    file to requireNotNull(metadata)
                }
                acquired+=file
                variants[id]=variant
            }
            withContext(Dispatchers.IO){trim()}
            val closed=AtomicBoolean(false)
            return ResolvedPreviewMedia(variants) { if(closed.compareAndSet(false,true))release(acquired) }
        } catch(error:Throwable){release(acquired);throw error}
    }
    private fun release(files:List<File>)=synchronized(ownership) {
        files.forEach{file->val count=(pins[file] ?: 1)-1;if(count==0)pins.remove(file) else pins[file]=count}
    }
    private fun probe(file:File,original:MediaReference):MediaReference?=runCatching {
        if(!file.isFile || file.length()==0L)return@runCatching null
        val dimensions=if(original.kind==MediaKind.IMAGE) {
            val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(file.path,bounds)
            Triple(bounds.outWidth,bounds.outHeight,0)
        } else {
            val reader=MediaMetadataRetriever()
            try {
                reader.setDataSource(file.path)
                val duration=(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0)*1000
                require(kotlin.math.abs(duration-original.durationUs)<=maxOf(100_000L,2_000_000L/(original.frameRate.toLong().coerceAtLeast(1)))) {
                    "Optimized media did not preserve source duration"
                }
                Triple(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0,
                    reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0,
                    reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0)
            }finally{reader.release()}
        }
        require(dimensions.first>0 && dimensions.second>0)
        file.setLastModified(System.currentTimeMillis())
        original.copy(uri=Uri.fromFile(file).toString(),width=dimensions.first,height=dimensions.second,rotationDegrees=dimensions.third,
            mimeType=if(original.kind==MediaKind.IMAGE)"image/png" else "video/mp4",codecMimeType=if(original.kind==MediaKind.IMAGE)"" else "video/avc",
            colorTransfer=3,sizeBytes=file.length(),hasAudio=false)
    }.getOrNull()
    private fun sampleImage(source:MediaReference,file:File) {
        val uri=Uri.parse(source.uri);val resolver=context.contentResolver
        var sample=1
        while(maxOf(source.width,source.height)/sample>1920)sample*=2
        val bitmap=requireNotNull(resolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply{inSampleSize=sample})})
        var oriented=bitmap
        try {
            val exif=resolver.openInputStream(uri)?.use{ExifInterface(it)}
            if(exif!=null && (exif.isFlipped || exif.rotationDegrees!=0)) {
                val matrix=Matrix().apply{if(exif.isFlipped)postScale(-1f,1f);postRotate(exif.rotationDegrees.toFloat())}
                oriented=Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
            }
            file.outputStream().use{check(oriented.compress(Bitmap.CompressFormat.PNG,100,it));it.fd.sync()}
        }finally{if(oriented!==bitmap)oriented.recycle();bitmap.recycle()}
    }
    private fun trim() {
        val files=folder.listFiles().orEmpty().sortedBy{it.lastModified()}
        var bytes=files.sumOf{it.length()}
        val cutoff=System.currentTimeMillis()-24*60*60*1000L
        synchronized(ownership) {
            for(file in files) {
                if(file in pins)continue
                if((file.name.contains(".partial") && file.lastModified()<cutoff) || bytes>budget.proxyDiskBytes) {
                    val size=file.length();if(file.delete())bytes-=size
                }
            }
        }
    }
}
