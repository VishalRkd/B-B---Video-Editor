package com.bbvideoeditor.app.media

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.app.projects.*
import com.bbvideoeditor.core.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ProjectRecoveryTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val repository=ProjectRepository(context)
    private suspend fun fixture(block:suspend(Project,File,File)->Unit) {
        val p=repository.create("recovery fixture")
        try {
            repository.save(p.copy(name="previous edit"));repository.save(p.copy(name="current edit"))
            block(p,File(context.filesDir,"projects/${p.id}.json"),File(context.filesDir,"projects/${p.id}.previous"))
        }finally{repository.delete(p.id)}
    }
    @Test fun validCurrentWinsOverValidPrevious()=runBlocking {
        fixture{p,current,previous -> val before=current.readBytes();val backup=previous.readBytes()
            assertEquals("current edit",repository.load(p.id).name)
            assertArrayEquals(before,current.readBytes());assertArrayEquals(backup,previous.readBytes())
        }
    }
    @Test fun corruptCurrentRecoversOnlyKnownGoodPrevious()=runBlocking {
        fixture{p,current,_ ->current.writeText("{broken")
            assertEquals("previous edit",repository.load(p.id).name)
            assertEquals(ProjectRecoveryKind.PREVIOUS_VERSION,repository.recoveries.value.last().kind)
        }
    }
    @Test fun futureCurrentIsNeverDowngradedByLoadListOrStaleAutosave()=runBlocking {
        fixture{p,current,previous ->
            current.writeText(Json.encodeToString(p.copy(name="future edit",schemaVersion=999)).dropLast(1)+",\"unknownFutureData\":[1,2,3]}")
            val bytes=current.readBytes();val backup=previous.readBytes()
            val failure=runCatching{repository.load(p.id)}.exceptionOrNull() as ProjectReadException
            assertEquals(ProjectReadFailure.UNSUPPORTED_SCHEMA,failure.reason)
            assertTrue(repository.list().none{it.id==p.id})
            assertTrue(repository.issues.value.any{it.contains("schema 999")})
            assertEquals(ProjectReadFailure.UNSUPPORTED_SCHEMA,(runCatching{repository.save(p)}.exceptionOrNull() as ProjectReadException).reason)
            assertArrayEquals(bytes,current.readBytes());assertArrayEquals(backup,previous.readBytes())
        }
    }
    @Test fun missingCurrentRecoversPrevious()=runBlocking {
        fixture{p,current,_ ->assertTrue(current.delete());assertEquals("previous edit",repository.load(p.id).name)}
    }
    @Test fun corruptBothFailsWithoutOverwritingEitherDocument()=runBlocking {
        fixture{p,current,previous ->current.writeText("bad-current");previous.writeText("bad-previous")
            assertEquals(ProjectReadFailure.CORRUPT_DOCUMENT,(runCatching{repository.load(p.id)}.exceptionOrNull() as ProjectReadException).reason)
            assertEquals("bad-current",current.readText());assertEquals("bad-previous",previous.readText())
        }
    }
    @Test fun interruptedAtomicBackupRecoversButCannotReplaceFutureCurrent()=runBlocking {
        fixture{p,current,_ ->
            val interrupted=File(current.path+".bak")
            interrupted.writeText(Json.encodeToString(p.copy(name="interrupted committed edit")))
            current.writeText("partial write")
            assertEquals("interrupted committed edit",repository.load(p.id).name)
            assertEquals(ProjectRecoveryKind.INTERRUPTED_WRITE,repository.recoveries.value.last().kind)
            interrupted.writeText(Json.encodeToString(p))
            current.writeText(Json.encodeToString(p.copy(schemaVersion=999)))
            val before=current.readBytes()
            assertEquals(ProjectReadFailure.UNSUPPORTED_SCHEMA,(runCatching{repository.load(p.id)}.exceptionOrNull() as ProjectReadException).reason)
            assertArrayEquals(before,current.readBytes())
        }
    }
    @Test fun unreadableCurrentIsStorageFailureNotCorruption()=runBlocking {
        fixture{p,current,_ ->
            try {
                check(current.setReadable(false,false))
                assertEquals(ProjectReadFailure.STORAGE_FAILURE,(runCatching{repository.load(p.id)}.exceptionOrNull() as ProjectReadException).reason)
            }finally{current.setReadable(true,true)}
        }
    }
}
