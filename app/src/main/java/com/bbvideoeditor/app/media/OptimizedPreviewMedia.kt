package com.bbvideoeditor.app.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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

/** Internal source optimization only. Original Project/media IDs/time ranges remain authoritative;
 * audio and export always open originals. One cancellable Transformer/image job owns generation.
 * Completed files are atomically promoted, identity-versioned, byte-bounded and leased by preview. */
@UnstableApi
class OptimizedPreviewMedia(context:Context,private val compiler:CompositionCompiler,
    private val budget:com.bbvideoeditor.app.playback.EngineResourceBudget=com.bbvideoeditor.app.playback.EngineResourceBudget.inspect(context)) : OptimizedMediaStore {
    private val context=context.applicationContext
    private val folder=File(this.context.cacheDir,"optimized_preview_v4")
    private val generation=Mutex()
    private val ownership=Any()
    private val pins=mutableMapOf<File,Int>()
    private val compatibility:MediaCompatibilityService = (this.context as? com.bbvideoeditor.app.EditorApplication)?.mediaCompatibility
        ?: MediaCompatibilityService(AndroidMediaCompatibilityBackend(this.context,budget))
    val liveLeases:Int get()=synchronized(ownership){pins.values.sum()}
    private suspend fun cacheFile(source:MediaReference):File=withContext(Dispatchers.IO) {
        val identity=if(source.kind==MediaKind.IMAGE) {
            val localVersion=Uri.parse(source.uri).path?.let{File(it).lastModified()} ?: 0
            compatibilityHash("v4:image|${source.uri}|${source.sizeBytes}|${source.durationUs}|${source.width}|${source.height}|${source.rotationDegrees}|$localVersion")
        } else compatibility.key(compatibility.inspect(source))
        File(folder,identity+if(source.kind==MediaKind.IMAGE)".png" else ".mp4")
    }
    override suspend fun invalidate(project:Project,mediaId:String) = generation.withLock {
        val source=requireNotNull(project.media[mediaId]);val file=cacheFile(source)
        withContext(Dispatchers.IO) {
            synchronized(ownership) {
                if(file in pins)throw sourceFailure(source,MediaFailureKind.RESOURCE,"Prepared source is still leased",MediaRetry.RESOURCES)
                if(file.exists() && !file.delete())throw sourceFailure(source,MediaFailureKind.STORAGE,"Could not discard failed prepared source",MediaRetry.PREPARE)
            }
        }
    }
    override suspend fun cached(project:Project,mediaId:String):ResolvedPreviewMedia? = generation.withLock {
        val source=requireNotNull(project.media[mediaId]);val file=cacheFile(source)
        if(!withContext(Dispatchers.IO){file.isFile && file.length()>0})return@withLock null
        val verified=try{verify(file,source)}catch(cancelled:CancellationException){throw cancelled}catch(_:Exception){return@withLock null}
        pin(file);ResolvedPreviewMedia(mapOf(mediaId to verified)){release(listOf(file))}
    }
    suspend fun resolve(project:Project):ResolvedPreviewMedia=resolve(project,project.timeline.tracks.filter{it.type in listOf(TrackType.VIDEO,TrackType.OVERLAY)}
        .flatMap{it.clips}.mapNotNull{it.mediaId}.toSet())
    override suspend fun resolve(project:Project,requiredMediaIds:Set<String>,preferredDecoders:Map<String,String>,
        onProgress:suspend (MediaPreparation)->Unit):ResolvedPreviewMedia {
        val acquired=mutableListOf<File>()
        try {
            val variants=mutableMapOf<String,MediaReference>()
            for(id in requiredMediaIds) {
                coroutineContext.ensureActive()
                val source=requireNotNull(project.media[id]);if(source.kind==MediaKind.AUDIO)continue
                val (file,variant)=generation.withLock {
                    withContext(Dispatchers.IO){folder.mkdirs()}
                    val file=cacheFile(source)
                    var metadata=if(withContext(Dispatchers.IO){file.isFile})try{verify(file,source)}
                        catch(cancelled:CancellationException){throw cancelled}catch(_:Exception){null} else null
                    if(metadata==null) {
                        val temporary=File(folder,"${file.nameWithoutExtension}-${newId()}.partial.${file.extension}")
                        try {
                            if(source.kind==MediaKind.IMAGE)withContext(Dispatchers.IO){sampleImage(source,temporary)}
                            else {
                                val width=source.displayWidth/2*2;val height=source.displayHeight/2*2
                                val silentSource=source.copy(hasAudio=false)
                                val normalization=Project(canvas=CanvasSettings(width,height),media=mapOf(id to silentSource),
                                    timeline=Timeline(listOf(Track(name="Source normalization",type=TrackType.VIDEO,
                                        clips=listOf(Clip(mediaId=id,startUs=0,sourceRange=TimeRange(0,source.durationUs)))))))
                                val fps=listOf(24,25,30,50,60).minBy{kotlin.math.abs(it-(source.frameRate.takeIf{it>0} ?: 30f))}
                                onProgress(MediaPreparation(PreparationStage.GENERATING,id,source.displayName))
                                coroutineScope {
                                    withTimeout((source.durationUs/1_000*4).coerceIn(90_000,1_800_000)) {
                                        // A source variant has one unedited visual stream. The existing
                                        // single-input compiler avoids clock-compositor startup gaps.
                                        Media3RenderEngine(context,compiler,synchronizedFrames=false,preferredDecoderName=preferredDecoders[id],preferSingleVisualInput=true).render(normalization,
                                            ExportSettings(resolution=720,frameRate=fps,bitrate=5_000_000),temporary,
                                            onProgress={ progress->launch { onProgress(MediaPreparation(PreparationStage.GENERATING,id,source.displayName,progress)) }})
                                    }
                                }
                            }
                            val verified=verify(temporary,source)
                            withContext(Dispatchers.IO) {
                                coroutineContext.ensureActive()
                                if(!temporary.renameTo(file))throw sourceFailure(source,MediaFailureKind.STORAGE,"Could not promote prepared media",MediaRetry.PREPARE)
                            }
                            metadata=verified.copy(uri=Uri.fromFile(file).toString())
                        } catch(timeout:TimeoutCancellationException) {
                            throw sourceFailure(source,MediaFailureKind.PREPARATION_TIMEOUT,"Normalization deadline",MediaRetry.PREPARE,timeout)
                        } finally { withContext(NonCancellable+Dispatchers.IO){temporary.delete()} }
                    }
                    pin(file);file to requireNotNull(metadata)
                }
                acquired+=file;variants[id]=variant
            }
            withContext(Dispatchers.IO){trim()}
            return ResolvedPreviewMedia(variants){release(acquired)}
        } catch(error:Throwable){
            release(acquired)
            if(error is CancellationException || error is MediaCompatibilityException || error !is Exception)throw error
            val source=requiredMediaIds.firstNotNullOfOrNull{project.media[it]} ?: throw error
            val kind=when {
                error is SecurityException->MediaFailureKind.ACCESS
                error.message?.contains("ENOSPC")==true || error.message?.contains("No space left")==true->MediaFailureKind.STORAGE
                else->MediaFailureKind.UNKNOWN
            }
            throw sourceFailure(source,kind,error.javaClass.simpleName,if(kind==MediaFailureKind.ACCESS)MediaRetry.RECHECK else MediaRetry.PREPARE,error)
        }
    }
    private fun pin(file:File)=synchronized(ownership){pins[file]=(pins[file] ?: 0)+1}
    private fun release(files:List<File>)=synchronized(ownership) {
        files.forEach{file->val count=(pins[file] ?: 1)-1;if(count<=0)pins.remove(file) else pins[file]=count}
    }
    private suspend fun verify(file:File,original:MediaReference):MediaReference=withContext(Dispatchers.IO) {
        try {
            require(file.isFile && file.length()>0){"Empty prepared file"}
            if(original.kind==MediaKind.IMAGE) {
                val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(file.path,bounds)
                require(bounds.outWidth>0 && bounds.outHeight>0)
                return@withContext original.copy(uri=Uri.fromFile(file).toString(),width=bounds.outWidth,height=bounds.outHeight,
                    rotationDegrees=0,mimeType=bounds.outMimeType ?: "image/png",sizeBytes=file.length())
            }
            val actual=MediaImporter(context).readAudioVideo(Uri.fromFile(file),original.displayName,"video/mp4",file.length()).copy(id=original.id)
            require(actual.kind==MediaKind.VIDEO && actual.codecMimeType.isNotBlank())
            require(actual.rotationDegrees.mod(90)==0)
            val referenceFps=original.frameRate.takeIf{it>0} ?: actual.frameRate.takeIf{it>0}
            val tolerance=maxOf(100_000L,referenceFps?.let{(2_000_000/it).toLong()} ?: 100_000L)
            require(kotlin.math.abs(actual.durationUs-original.durationUs)<=tolerance){"Prepared duration changed"}
            if(original.displayWidth>0 && original.displayHeight>0) {
                val a=original.displayWidth.toDouble()/original.displayHeight
                val b=actual.displayWidth.toDouble()/actual.displayHeight
                require(kotlin.math.abs(a-b)/a<.02){"Prepared display geometry changed"}
            }
            val extractor=android.media.MediaExtractor()
            val times=mutableListOf<Long>()
            try {
                extractor.setDataSource(file.path)
                val video=(0 until extractor.trackCount).first{extractor.getTrackFormat(it).getString(android.media.MediaFormat.KEY_MIME).orEmpty().startsWith("video/")}
                extractor.selectTrack(video)
                repeat(128) { if(extractor.sampleTime>=0){times+=extractor.sampleTime;extractor.advance()} }
            }finally{extractor.release()}
            require(times.isNotEmpty() && times.min()>=0 && times.min()<=100_000){"Prepared timestamps missing or offset"}
            val ordered=times.sorted();require(ordered.zipWithNext().all{it.second>it.first}){"Duplicate prepared timestamps"}
            val measured=if(ordered.size>1 && ordered.last()>ordered.first())
                ((ordered.size-1)*1_000_000.0/(ordered.last()-ordered.first())).toFloat() else actual.frameRate
            val metadata=actual.copy(frameRate=measured,frameRateProvenance=if(ordered.size>1)FrameRateProvenance.MEASURED else actual.frameRateProvenance)
            val observed=compatibility.verify(compatibility.inspect(metadata))
            require(observed.success!=null){"Prepared file did not produce decoded output"}
            file.setLastModified(System.currentTimeMillis())
            metadata
        } catch(cancelled:CancellationException){throw cancelled}
        catch(error:MediaCompatibilityException){throw error}
        catch(error:Exception){throw sourceFailure(original,MediaFailureKind.OUTPUT_VERIFICATION,error.javaClass.simpleName,MediaRetry.PREPARE,error)}
    }
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
