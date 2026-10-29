package com.bbvideoeditor.app.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.bbvideoeditor.app.export.ExportRequest
import com.bbvideoeditor.app.projects.ProjectRepository
import com.bbvideoeditor.core.CURRENT_PROJECT_SCHEMA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

data class AssetMaintenance(val removedFiles:Int=0,val removedBytes:Long=0,val releasedGrants:Int=0,val deferred:Boolean=false)

/** Conservative reachability collection: saved projects AND backups, active history sessions,
 * and durable export snapshots protect assets. A session lease excludes maintenance entirely,
 * including files added later to its undo history. Unknown/corrupt storage fails closed. */
class OwnedAssetStore(context:Context,private val projects:ProjectRepository,
    private val importedDirectory:File=File(context.filesDir,"imported_media"),
    private val exportDirectory:File=File(context.filesDir,"exports")) {
    private val context=context.applicationContext
    private val gate=Mutex()
    private val owners=ConcurrentHashMap<Any,Any>()
    private val json=Json{ignoreUnknownKeys=true}
    suspend fun acquire(owner:Any=Any()):AutoCloseable {
        val token=Any()
        try {
            withContext(Dispatchers.IO){gate.withLock{owners[token]=owner}}
            val closed=AtomicBoolean(false)
            return AutoCloseable{if(closed.compareAndSet(false,true))owners.remove(token)}
        }catch(error:Throwable){owners.remove(token);throw error}
    }
    suspend fun collectOrphans(nowMs:Long=System.currentTimeMillis()):AssetMaintenance=withContext(Dispatchers.IO) {
        gate.withLock {
            if(owners.isNotEmpty())return@withLock AssetMaintenance(deferred=true)
            val reachable=projects.protectedMediaUris()?.toMutableSet() ?: return@withLock AssetMaintenance(deferred=true)
            val exports=exportDirectory
            val exportFiles=exports.listFiles() ?: if(exports.exists())return@withLock AssetMaintenance(deferred=true) else emptyArray()
            val snapshots=mutableListOf<File>()
            for(directory in exportFiles.filter{it.isDirectory}) {
                val files=directory.listFiles() ?: return@withLock AssetMaintenance(deferred=true)
                snapshots+=files
            }
            for(file in snapshots
                .filter{it.name=="request.json" || it.name=="request.json.bak"}) {
                try {
                    require(file.length()<=64L*1024*1024)
                    val request=json.decodeFromString<ExportRequest>(file.readText())
                    require(request.project.schemaVersion<=CURRENT_PROJECT_SCHEMA)
                    request.project.upgradeSchema()
                    reachable+=request.project.media.values.map{it.uri}
                }catch(_:Exception){return@withLock AssetMaintenance(deferred=true)}
            }
            val directory=importedDirectory
            val root=directory.canonicalPath+File.separator
            val protectedPaths=reachable.mapNotNull{value->Uri.parse(value).takeIf{it.scheme=="file"}?.path?.let{File(it).canonicalPath}}.toSet()
            val cutoff=nowMs-30L*24*60*60*1000
            var removed=0;var bytes=0L;var grants=0
            for(file in directory.listFiles().orEmpty()) {
                if(!file.isFile || !file.canonicalPath.startsWith(root) || file.canonicalPath in protectedPaths || file.lastModified()>=cutoff)continue
                val size=file.length()
                if(file.delete()){removed++;bytes+=size}
            }
            for(permission in context.contentResolver.persistedUriPermissions) {
                if(permission.persistedTime>=cutoff || permission.uri.toString() in reachable)continue
                val flags=(if(permission.isReadPermission)Intent.FLAG_GRANT_READ_URI_PERMISSION else 0) or
                    (if(permission.isWritePermission)Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
                runCatching{context.contentResolver.releasePersistableUriPermission(permission.uri,flags)}.onSuccess{grants++}
            }
            AssetMaintenance(removed,bytes,grants)
        }
    }
}
