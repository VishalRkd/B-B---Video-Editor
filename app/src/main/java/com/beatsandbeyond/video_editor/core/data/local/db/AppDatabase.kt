package com.beatsandbeyond.video_editor.core.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.beatsandbeyond.video_editor.core.data.local.db.dao.AssetDao
import com.beatsandbeyond.video_editor.core.data.local.db.dao.ProjectDao
import com.beatsandbeyond.video_editor.core.data.local.db.entity.AssetEntity
import com.beatsandbeyond.video_editor.core.data.local.db.entity.ProjectEntity

/**
 * The single Room database for B&B Video Editor.
 *
 * Stores application-owned data only:
 * - [ProjectEntity]: Saved editing projects
 * - [AssetEntity]: Imported media references with app-specific metadata
 *
 * Device media (videos, images from the gallery) is NEVER stored here.
 * It is always queried live from MediaStore.
 *
 * Version history:
 * - Version 1 (Phase 1): projects + assets tables
 * - Version 2 (Phase N): normalize timeline into tracks/clips tables
 *
 * The database is provided as a singleton by [DatabaseModule].
 */
@Database(
    entities = [ProjectEntity::class, AssetEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun assetDao(): AssetDao

    companion object {
        const val DATABASE_NAME = "bb_video_editor.db"
    }
}
