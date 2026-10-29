package com.bbvideoeditor.app.export

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.core.newId
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File

@androidx.test.filters.SdkSuppress(minSdkVersion=29)
class PublicationRecoveryTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val collection=MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private fun insert(name:String,pending:Boolean):Uri=context.contentResolver.insert(collection,ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME,name);put(MediaStore.MediaColumns.MIME_TYPE,"video/mp4")
        put(MediaStore.MediaColumns.RELATIVE_PATH,"Movies/Frame");put(MediaStore.MediaColumns.IS_PENDING,if(pending)1 else 0)
    })!!
    private fun exists(uri:Uri)=try{context.contentResolver.query(uri,arrayOf(MediaStore.MediaColumns._ID),null,null,null)?.use{it.moveToFirst()} ?: false}catch(_:SecurityException){false}
    @Test fun reservationRecoversCrashBetweenInsertAndUriJournal() {
        val directory=File(context.cacheDir,"publication-${newId()}").apply{mkdirs()}
        val journal=File(directory,"pending_uri.txt");val name="foundation-${newId()}.mp4"
        PublicationJournal.reserve(journal,name,collection,context)
        val uri=insert(name,true)
        try {
            assertFalse(PublicationJournal.recover(context,directory).retry)
            assertFalse(exists(uri));assertFalse(File(directory,"pending_name.json").exists())
        }finally{runCatching{context.contentResolver.delete(uri,null,null)};directory.deleteRecursively()}
    }
    @Test fun pendingUriIsRemovedAndCompletedPublicationSurvivesRecovery()=runBlocking {
        val root=File(context.cacheDir,"export-recovery-${newId()}").apply{mkdirs()}
        val id=newId();val directory=File(root,id).apply{mkdirs()};val journal=File(directory,"pending_uri.txt")
        val uri=insert("foundation-complete-${newId()}.mp4",false)
        try {
            PublicationJournal.writeUri(journal,uri)
            File(root,"status.json").writeText(Json.encodeToString(ExportState(jobId=id,phase=ExportPhase.PUBLISHING)))
            File(directory,"render.mp4").writeText("interrupted private intermediate")
            val coordinator=ExportCoordinator(context,root)
            coordinator.awaitRecovery()
            assertEquals(ExportPhase.SUCCEEDED,coordinator.state.value.phase)
            assertEquals(uri.toString(),coordinator.state.value.outputUri)
            assertTrue(exists(uri));assertFalse(journal.exists());assertFalse(File(directory,"render.mp4").exists())
            val pending=insert("foundation-pending-${newId()}.mp4",true)
            try {
                directory.mkdirs();PublicationJournal.writeUri(journal,pending)
                assertFalse(PublicationJournal.recover(context,directory).retry)
                assertFalse(exists(pending))
            }finally{runCatching{context.contentResolver.delete(pending,null,null)}}
        }finally{runCatching{context.contentResolver.delete(uri,null,null)};root.deleteRecursively()}
    }
}
