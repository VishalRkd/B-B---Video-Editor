package com.bbvideoeditor.app.media

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.app.projects.ProjectRepository
import com.bbvideoeditor.core.MediaKind
import com.bbvideoeditor.core.MediaReference
import com.bbvideoeditor.core.Project
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class MediaPersistenceInstrumentedTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun projectSaveReopenBackupRecoveryAndDelete() = runBlocking {
        val repository = ProjectRepository(context)
        val initial = repository.create("Persistence test")
        try {
            val updated = initial.copy(name = "Renamed project", updatedAt = initial.updatedAt + 1)
            repository.save(updated)
            assertEquals(updated, ProjectRepository(context).load(initial.id))
            assertTrue(repository.list().any { it.id == initial.id })
            File(context.filesDir, "projects/${initial.id}.json").writeText("{broken")
            assertEquals(initial, repository.load(initial.id))
            assertTrue(repository.issues.value.any { it.contains("Recovered") })
        } finally {
            repository.delete(initial.id)
        }
        assertFalse(repository.list().any { it.id == initial.id })
    }

    @Test fun imageImportIsDurableAndProducesActualCachedPixels() = runBlocking {
        val source = File.createTempFile("bb-image-import", ".png", context.cacheDir)
        val image = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        source.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
        var importedFile: File? = null
        try {
            val result = MediaImporter(context).importMedia(listOf(Uri.fromFile(source)))
            assertTrue(result.errors.toString(), result.errors.isEmpty())
            val media = result.media.single()
            importedFile = File(requireNotNull(Uri.parse(media.uri).path))
            assertEquals(MediaKind.IMAGE, media.kind)
            assertEquals(80, media.width)
            assertEquals(40, media.height)
            assertTrue(importedFile.exists())
            assertTrue(source.delete())
            val first = ThumbnailCache(context).thumbnail(media, 0, 40, 20)
            assertNotNull(first)
            assertEquals(Color.RED, first!!.getPixel(20, 10))
            val cached = ThumbnailCache(context).thumbnail(media, 0, 40, 20)
            assertNotNull(cached)
            assertEquals(40, cached!!.width)
            assertEquals(20, cached.height)
            assertEquals(Color.RED, cached.getPixel(20, 10))
        } finally {
            source.delete()
            importedFile?.delete()
        }
    }

    @Test fun realAudioImportAndDecodeDistinguishSilenceFromSignal() = runBlocking {
        val source = File.createTempFile("bb-waveform", ".wav", context.cacheDir)
        writeWave(source)
        var importedFile: File? = null
        try {
            val result = MediaImporter(context).importMedia(listOf(Uri.fromFile(source)))
            assertTrue(result.errors.toString(), result.errors.isEmpty())
            val media = result.media.single()
            importedFile = File(requireNotNull(Uri.parse(media.uri).path))
            assertEquals(MediaKind.AUDIO, media.kind)
            assertTrue(media.hasAudio)
            assertEquals(1_000_000L, media.durationUs)
            val waveform = WaveformRepository(context).waveform(media, 32)
            assertEquals(32, waveform.size)
            assertTrue(waveform.take(14).all { it < 0.01f })
            assertTrue(waveform.takeLast(14).all { it > 0.6f && it < 0.8f })
            assertTrue(WaveformRepository(context).waveform(media, 32).contentEquals(waveform))
        } finally {
            source.delete()
            importedFile?.delete()
        }
    }

    @Test fun missingMediaIsReportedAndCorruptMediaIsRejected() = runBlocking {
        val missing = MediaReference(uri = Uri.fromFile(File(context.cacheDir, "does-not-exist.mp4")).toString(), displayName = "Missing", kind = MediaKind.VIDEO, durationUs = 1_000_000)
        val importer = MediaImporter(context)
        assertEquals(listOf(missing), importer.unavailable(Project(media = mapOf(missing.id to missing))))
        val corrupt = File.createTempFile("bb-corrupt", ".mp4", context.cacheDir)
        try {
            corrupt.writeText("This is not a media file")
            val result = importer.importMedia(listOf(Uri.fromFile(corrupt)))
            assertTrue(result.media.isEmpty())
            assertEquals(1, result.errors.size)
        } finally { corrupt.delete() }
    }

    private fun writeWave(file: File) {
        val sampleRate = 48_000
        val dataLength = sampleRate * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(dataLength + 36).put("WAVE".toByteArray())
            .put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(sampleRate).putInt(sampleRate * 2).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(dataLength)
        file.outputStream().use { output ->
            output.write(header.array())
            val samples = ByteBuffer.allocate(dataLength).order(ByteOrder.LITTLE_ENDIAN)
            repeat(sampleRate) { index ->
                val value = if (index < sampleRate / 2) 0 else (sin(2 * PI * 440 * index / sampleRate) * 0.75 * 32767).toInt()
                samples.putShort(value.toShort())
            }
            output.write(samples.array())
        }
    }
}
