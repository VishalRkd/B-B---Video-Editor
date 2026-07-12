package com.beatsandbeyond.video_editor.core.di

import android.content.ContentResolver
import android.content.Context
import com.beatsandbeyond.video_editor.core.utils.CoroutineDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides application-wide dependencies that are not tied to a specific feature.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCoroutineDispatchers(): CoroutineDispatchers = CoroutineDispatchers()

    @Provides
    @Singleton
    fun provideContentResolver(
        @ApplicationContext context: Context,
    ): ContentResolver = context.contentResolver
}
