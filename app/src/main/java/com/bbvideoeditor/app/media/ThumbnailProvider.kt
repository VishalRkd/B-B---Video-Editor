package com.bbvideoeditor.app.media

import android.graphics.Bitmap
import com.bbvideoeditor.core.MediaReference

fun interface ThumbnailProvider {
    suspend fun thumbnail(request: ThumbnailRequest): Bitmap? = thumbnail(request.media,request.sourceUs,request.width,request.height)
    suspend fun thumbnail(media: MediaReference, sourceUs: Long, width: Int, height: Int): Bitmap?
}

data class ThumbnailRequest(val media:MediaReference,val sourceUs:Long,val width:Int,val height:Int,
    val priority:AssetPriority=AssetPriority.VISIBLE)
