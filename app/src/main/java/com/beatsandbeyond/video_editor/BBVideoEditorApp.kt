package com.beatsandbeyond.video_editor

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for B&B Video Editor.
 *
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation,
 * which creates a component that is attached to the Application lifecycle
 * and provides dependencies to all other Android components.
 *
 * Future responsibilities:
 * - Initialize crash reporting (e.g., Firebase Crashlytics)
 * - Initialize performance monitoring
 * - Initialize analytics
 * - Set up global exception handling
 */
@HiltAndroidApp
class BBVideoEditorApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Future: Initialize Timber for logging in debug builds
        // Future: Initialize Firebase, analytics, etc.
    }
}
