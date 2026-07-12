package com.beatsandbeyond.video_editor.core.di

import android.content.Context
import androidx.room.Room
import com.beatsandbeyond.video_editor.core.data.local.db.AppDatabase
import com.beatsandbeyond.video_editor.core.data.local.db.dao.AssetDao
import com.beatsandbeyond.video_editor.core.data.local.db.dao.ProjectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides Room database and DAO instances.
 *
 * The database is a singleton — there should only ever be one instance per process.
 * DAOs are derived from the database and share its lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME,
    ).build()

    @Provides
    @Singleton
    fun provideProjectDao(database: AppDatabase): ProjectDao =
        database.projectDao()

    @Provides
    @Singleton
    fun provideAssetDao(database: AppDatabase): AssetDao =
        database.assetDao()
}
