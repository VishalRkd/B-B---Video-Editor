package com.bbvideoeditor.app.media

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.app.export.ExportRequest
import com.bbvideoeditor.app.export.ExportSettings
import com.bbvideoeditor.app.projects.ProjectRepository
import com.bbvideoeditor.core.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class OwnedAssetStoreTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun collectionProtectsProjectsBackupsSessionsAndExportSnapshots()=runBlocking {
        val root=File(context.cacheDir,"ownership-${newId()}").apply{mkdirs()}
        val imports=File(root,"imports").apply{mkdirs()};val exports=File(root,"exports").apply{mkdirs()}
        val repo=ProjectRepository(context,File(root,"projects"))
        val store=OwnedAssetStore(context,repo,imports,exports)
        val now=System.currentTimeMillis()
        fun asset(name:String)=File(imports,name).apply{writeText(name);setLastModified(now-31L*24*60*60*1000)}
        val original=asset("original");val replacement=asset("replacement");val orphan=asset("orphan")
        val reference=MediaReference(uri=Uri.fromFile(original).toString(),displayName="source",kind=MediaKind.IMAGE,durationUs=5_000_000)
        val project=Project(media=mapOf(reference.id to reference),timeline=Timeline(listOf(Track(name="V",type=TrackType.VIDEO,
            clips=listOf(Clip(mediaId=reference.id,startUs=0,sourceRange=TimeRange(0,5_000_000)))))))
        try {
            repo.save(project)
            val changed=RelinkMedia(reference.id,reference.copy(uri=Uri.fromFile(replacement).toString())).apply(project)
            repo.save(changed)
            val lease=store.acquire()
            assertTrue(store.collectOrphans(now).deferred)
            assertTrue(orphan.exists());lease.close()
            val collected=store.collectOrphans(now)
            assertEquals(1,collected.removedFiles)
            assertTrue(original.exists());assertTrue(replacement.exists());assertFalse(orphan.exists())
            val job=File(exports,newId()).apply{mkdirs()}
            File(job,"request.json").writeText(Json.encodeToString(ExportRequest(job.name,project,ExportSettings())))
            repo.delete(project.id)
            store.collectOrphans(now)
            assertTrue("Export snapshot lost its source",original.exists())
            assertFalse(replacement.exists())
            job.deleteRecursively();store.collectOrphans(now)
            assertFalse(original.exists())
        }finally{root.deleteRecursively()}
    }
    @Test fun unreadableOrFutureStorageDefersAllDeletion()=runBlocking {
        val root=File(context.cacheDir,"ownership-future-${newId()}").apply{mkdirs()}
        val directory=File(root,"projects").apply{mkdirs()};val imports=File(root,"imports").apply{mkdirs()}
        val repo=ProjectRepository(context,directory);val store=OwnedAssetStore(context,repo,imports,File(root,"exports"))
        val project=Project();val orphan=File(imports,"orphan").apply{writeText("owned");setLastModified(1)}
        try {
            repo.save(project)
            val current=File(directory,"${project.id}.json")
            val future=current.readText().replace("\"schemaVersion\":2","\"schemaVersion\":999")
            current.writeText(future)
            assertTrue(store.collectOrphans().deferred);assertTrue(orphan.exists());assertEquals(future,current.readText())
            current.writeText(Json.encodeToString(project))
            assertTrue(directory.setReadable(false,false))
            assertTrue(store.collectOrphans().deferred);assertTrue(orphan.exists())
        }finally{directory.setReadable(true,false);root.deleteRecursively()}
    }
}
