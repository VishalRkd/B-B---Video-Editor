package com.beatsandbeyond.video_editor.core.di

import com.beatsandbeyond.video_editor.core.data.datasource.MediaStoreDataSource
import com.beatsandbeyond.video_editor.core.data.datasource.MediaStoreDataSourceImpl
import com.beatsandbeyond.video_editor.core.data.repository.MediaRepositoryImpl
import com.beatsandbeyond.video_editor.core.data.repository.ProjectRepositoryImpl
import com.beatsandbeyond.video_editor.core.domain.repository.MediaRepository
import com.beatsandbeyond.video_editor.core.domain.repository.ProjectRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds repository and data source interfaces to their concrete implementations.
 *
 * Using @Binds instead of @Provides is more efficient — it generates less bytecode
 * and has no runtime overhead since no factory method is involved.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        impl: MediaRepositoryImpl,
    ): MediaRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(
        impl: ProjectRepositoryImpl,
    ): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindMediaStoreDataSource(
        impl: MediaStoreDataSourceImpl,
    ): MediaStoreDataSource
}
