package com.bbvideoeditor.app.export

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.AtomicFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Reserve a unique owned name BEFORE insert, closing the crash window between MediaStore insert
 * and URI journaling. Recovery only touches exact-name, package-owned pending rows. */
internal object PublicationJournal {
    @Serializable private data class Reservation(val name:String,val collection:String,val owner:String)
    data class Recovery(val completedUri:Uri?=null,val retry:Boolean=false)
    private val json=Json{ignoreUnknownKeys=true}
    private fun reservation(journal:File)=File(journal.parentFile,"pending_name.json")
    fun reserve(journal:File?,name:String,collection:Uri,context:Context) {
        if(journal!=null)write(reservation(journal),json.encodeToString(Reservation(name,collection.toString(),context.packageName)))
    }
    fun writeUri(journal:File?,uri:Uri){if(journal!=null)write(journal,uri.toString())}
    fun clear(journal:File?){if(journal!=null){AtomicFile(journal).delete();AtomicFile(reservation(journal)).delete()}}
    fun recover(context:Context,directory:File):Recovery {
        val journal=File(directory,"pending_uri.txt")
        if(Build.VERSION.SDK_INT<29){clear(journal);return Recovery()}
        return try {
            var completed:Uri?=null
            if(journal.exists() || File(journal.path+".bak").exists()) {
                val uri=Uri.parse(AtomicFile(journal).openRead().bufferedReader().use{it.readText()})
                require(uri.authority==MediaStore.AUTHORITY)
                context.contentResolver.query(uri,arrayOf(MediaStore.MediaColumns.IS_PENDING,MediaStore.MediaColumns.OWNER_PACKAGE_NAME),null,null,null)?.use { cursor ->
                    if(cursor.moveToFirst() && cursor.getString(1)==context.packageName) {
                        if(cursor.getInt(0)==1)context.contentResolver.delete(uri,
                            "${MediaStore.MediaColumns.IS_PENDING}=1 AND ${MediaStore.MediaColumns.OWNER_PACKAGE_NAME}=?",arrayOf(context.packageName))
                        else completed=uri
                    }
                }
            }
            val reserved=reservation(journal)
            if(reserved.exists() || File(reserved.path+".bak").exists()) {
                val record=json.decodeFromString<Reservation>(AtomicFile(reserved).openRead().bufferedReader().use{it.readText()})
                require(record.owner==context.packageName)
                val collection=Uri.parse(record.collection);require(collection.authority==MediaStore.AUTHORITY)
                @Suppress("DEPRECATION")
                val includingPending=MediaStore.setIncludePending(collection)
                val selection="${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.IS_PENDING}=1 AND ${MediaStore.MediaColumns.OWNER_PACKAGE_NAME}=?"
                val ids=mutableListOf<Long>()
                requireNotNull(context.contentResolver.query(includingPending,arrayOf(MediaStore.MediaColumns._ID),
                    selection,arrayOf(record.name,record.owner),null)).use { cursor ->
                    while(cursor.moveToNext())ids+=cursor.getLong(0)
                }
                for(id in ids)check(context.contentResolver.delete(android.content.ContentUris.withAppendedId(collection,id),null,null)==1)

            }
            clear(journal)
            Recovery(completed)
        }catch(_:Exception){Recovery(retry=true)}
    }
    private fun write(file:File,text:String) {
        val atomic=AtomicFile(file);val stream=atomic.startWrite()
        try{stream.write(text.toByteArray(Charsets.UTF_8));atomic.finishWrite(stream)}
        catch(error:Throwable){atomic.failWrite(stream);throw error}
    }
}
