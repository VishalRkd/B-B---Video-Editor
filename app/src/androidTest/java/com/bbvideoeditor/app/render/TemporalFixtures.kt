package com.bbvideoeditor.app.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.opengl.GLES20
import android.os.SystemClock
import com.bbvideoeditor.core.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/** Real encoded source identity: 10 frame bits, 2 source bits, 1010 sentinel, complemented row.
 * The pattern uses large cells, allowing H.264/chroma/display tolerances without OCR. Audio pulses
 * start at .5s, 1.5s, ... in the SAME source clock. No editor compiler generates these fixtures.
 */
internal class TemporalFixtures(private val context: Context) {
    data class Identity(val source: Int, val frame: Int)
    /** Sample-exact PCM reference for longer A/V tests, avoiding compressed-source priming. */
    fun pulsesWav(seconds:Int):MediaReference {
        val rate=48_000;val samples=seconds*rate
        val file=File(context.cacheDir,"temporal-pcm-${newId()}.wav")
        file.outputStream().buffered().use { output ->
            val header=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray()).putInt(36+samples*2).put("WAVEfmt ".toByteArray()).putInt(16)
                .putShort(1).putShort(1).putInt(rate).putInt(rate*2).putShort(2).putShort(16)
                .put("data".toByteArray()).putInt(samples*2)
            output.write(header.array())
            val buffer=ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN)
            for(sample in 0 until samples) {
                val pulse=sample%rate in rate/2 until rate/2+rate/40
                buffer.putShort(if(pulse)(sin(sample*2*PI*1000/rate)*20_000).toInt().toShort() else 0)
                if(!buffer.hasRemaining()){output.write(buffer.array());buffer.clear()}
            }
            if(buffer.position()>0)output.write(buffer.array(),0,buffer.position())
        }
        return MediaReference(uri=Uri.fromFile(file).toString(),displayName=file.name,kind=MediaKind.AUDIO,
            durationUs=seconds*1_000_000L,hasAudio=true,mimeType="audio/wav",sizeBytes=file.length(),audioChannels=1,audioSampleRate=rate)
    }
    fun video(fps: Int = 30, seconds: Int = 4, source: Int = 1, audio: Boolean = false): MediaReference {
        require(fps in listOf(24,30,60) && source in 0..3)
        val video = MediaFixtures(context).video("time-$source-$fps", .08f,.12f,.18f, fps, fps*seconds) { frame ->
            val code = (frame % 1024) or (source shl 10) or (0b1010 shl 12)
            GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
            for (bit in 0..15) for (row in 0..1) {
                val white = ((code shr bit) and 1) == (if (row == 0) 1 else 0)
                GLES20.glScissor(bit*20, if(row==0) 108 else 18,20,54)
                val value=if(white)1f else 0f
                GLES20.glClearColor(value,value,value,1f);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            }
            // A second independently moving marker catches a frozen/non-updated source texture.
            GLES20.glScissor((frame*7)%300,83,20,14)
            GLES20.glClearColor(1f,0f,0f,1f);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
        }
        return if(audio) muxPulses(video,seconds) else video
    }
    fun decode(bitmap: Bitmap, left: Float=0f, top: Float=0f, width: Float=1f, height: Float=1f): Identity? {
        var code=0
        for(bit in 0..15) {
            fun value(y:Float):Int {
                val color=bitmap.getPixel(((left+width*(bit+.5f)/16)*bitmap.width).toInt().coerceIn(0,bitmap.width-1),
                    ((top+height*y)*bitmap.height).toInt().coerceIn(0,bitmap.height-1))
                return (Color.red(color)+Color.green(color)+Color.blue(color))/3
            }
            val a=value(.25f);val b=value(.75f)
            if(kotlin.math.abs(a-b)<90)return null
            if(a>b)code=code or (1 shl bit)
        }
        if(code ushr 12 != 0b1010)return null
        return Identity((code ushr 10) and 3,code and 1023)
    }
    private fun muxPulses(video:MediaReference,seconds:Int):MediaReference {
        val file=File(context.cacheDir,"temporal-av-${video.id}.mp4")
        val input=MediaExtractor();input.setDataSource(context,Uri.parse(video.uri),null)
        val encoder=MediaCodec.createEncoderByType("audio/mp4a-latm")
        val muxer=MediaMuxer(file.absolutePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var started=false
        try {
            input.selectTrack(0);val videoTrack=muxer.addTrack(input.getTrackFormat(0))
            val rate=48_000
            encoder.configure(MediaFormat.createAudioFormat("audio/mp4a-latm",rate,1).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE,128_000)
            },null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);encoder.start()
            var sample=0;var ended=false;var audioTrack=-1;val info=MediaCodec.BufferInfo()
            val deadline=SystemClock.elapsedRealtime()+30_000
            while(!ended) {
                check(SystemClock.elapsedRealtime()<deadline){"Temporal fixture audio encoder timeout"}
                if(sample<=seconds*rate) {
                    val index=encoder.dequeueInputBuffer(1000)
                    if(index>=0) {
                        val buffer=encoder.getInputBuffer(index)!!.order(ByteOrder.LITTLE_ENDIAN);buffer.clear()
                        val begin=sample
                        while(buffer.remaining()>=2 && sample<seconds*rate) {
                            val local=sample%rate
                            val pulse=local in rate/2 until rate/2+rate/40
                            buffer.putShort(if(pulse)(sin(sample*2*PI*1000/rate)*20_000).toInt().toShort() else 0)
                            sample++
                        }
                        val eos=sample==begin
                        encoder.queueInputBuffer(index,0,buffer.position(),begin*1_000_000L/rate,if(eos)MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                        if(eos)sample++
                    }
                }
                val index=encoder.dequeueOutputBuffer(info,1000)
                if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    audioTrack=muxer.addTrack(encoder.outputFormat);muxer.start();started=true
                    val data=ByteBuffer.allocate(512*1024)
                    while(true) {
                        data.clear();val size=input.readSampleData(data,0);if(size<0)break
                        val packet=MediaCodec.BufferInfo().apply{set(0,size,input.sampleTime,input.sampleFlags)}
                        muxer.writeSampleData(videoTrack,data,packet);if(!input.advance())break
                    }
                } else if(index>=0) {
                    if(info.size>0 && info.presentationTimeUs>=0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG==0) {
                        check(started);muxer.writeSampleData(audioTrack,encoder.getOutputBuffer(index)!!,info)
                    }
                    ended=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0
                    encoder.releaseOutputBuffer(index,false)
                }
            }
        } finally {
            runCatching{encoder.stop()};encoder.release();input.release()
            if(started)muxer.stop();muxer.release()
        }
        return video.copy(id=newId(),uri=Uri.fromFile(file).toString(),displayName=file.name,hasAudio=true,sizeBytes=file.length())
    }
}
