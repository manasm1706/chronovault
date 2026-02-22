package com.example.chronovault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.chronovault.data.local.entity.CapsuleEntity

/**
 * Room Database for ChronoVault
 * Manages local storage of capsules and related data
 */
@Database(
    entities = [CapsuleEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class) // ← Add this line
abstract class ChronoVaultDatabase : RoomDatabase() {

    abstract fun capsuleDao(): CapsuleDao

    companion object {
        const val DATABASE_NAME = "chronovault_db"
    }
}