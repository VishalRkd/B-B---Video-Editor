package com.beatsandbeyond.video_editor.core.di

import com.beatsandbeyond.video_editor.core.data.engine.ExoPlayerPreviewEngine
import com.beatsandbeyond.video_editor.core.domain.engine.PreviewEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds engine interfaces to their concrete implementations.
 *
 * Phase 2: Only [PreviewEngine] has an implementation — [ExoPlayerPreviewEngine].
 * Remaining engines (TimelineEngine, RenderEngine, ExportEngine, AudioEngine,
 * EffectsEngine) will be bound here in future phases as they are implemented.
 *
 * Engine implementations are singletons so a single ExoPlayer instance is reused
 * across the app's lifecycle. This avoids the cost of creating/destroying players
 * on every navigation event.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {

    @Binds
    @Singleton
    abstract fun bindPreviewEngine(
        impl: ExoPlayerPreviewEngine,
    ): PreviewEngine
}
