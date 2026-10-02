package com.videoeditorpro.android

import android.app.Application
import android.util.Log
import com.videoeditorpro.android.engine.VideoEngineManager

class VideoEditorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val success = VideoEngineManager.init(this)
        Log.i("VideoEditorApp", "QuVideo Engine initialized: $success")
    }
}
