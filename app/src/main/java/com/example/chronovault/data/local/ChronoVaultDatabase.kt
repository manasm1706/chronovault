package com.example.chronovault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.chronovault.data.local.entity.CapsuleEntity
import com.example.chronovault.data.local.entity.CommentEntity

/**
 * Room Database for ChronoVault
 * Manages local storage of capsules, comments, and related data
 */
@Database(
    entities = [CapsuleEntity::class, CommentEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ChronoVaultDatabase : RoomDatabase() {

    abstract fun capsuleDao(): CapsuleDao
    abstract fun commentDao(): CommentDao

    companion object {
        const val DATABASE_NAME = "chronovault_db"
    }
}