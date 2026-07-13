package com.beatsandbeyond.video_editor.core.di

import com.beatsandbeyond.video_editor.core.data.engine.ExoPlayerPreviewEngine
import com.beatsandbeyond.video_editor.core.data.engine.KotlinTimelineEngine
import com.beatsandbeyond.video_editor.core.data.engine.AndroidMediaEngine
import com.beatsandbeyond.video_editor.core.domain.engine.PreviewEngine
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.engine.MediaEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds engine interfaces to their concrete implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {

    @Binds
    @Singleton
    abstract fun bindPreviewEngine(
        impl: ExoPlayerPreviewEngine,
    ): PreviewEngine

    @Binds
    @Singleton
    abstract fun bindTimelineEngine(
        impl: KotlinTimelineEngine,
    ): TimelineEngine

    @Binds
    @Singleton
    abstract fun bindMediaEngine(
        impl: AndroidMediaEngine,
    ): MediaEngine
}
