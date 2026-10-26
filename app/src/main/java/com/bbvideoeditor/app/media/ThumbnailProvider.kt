package com.bbvideoeditor.app.media

import android.graphics.Bitmap
import com.bbvideoeditor.core.MediaReference

fun interface ThumbnailProvider {
    suspend fun thumbnail(media: MediaReference, sourceUs: Long, width: Int, height: Int): Bitmap?
}
