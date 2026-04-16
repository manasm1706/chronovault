package com.example.chronovault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.chronovault.data.local.entity.CapsuleEntity
import com.example.chronovault.data.local.entity.CommentEntity
import com.example.chronovault.data.local.entity.FriendEntity
import com.example.chronovault.data.local.entity.NotificationEntity

/**
 * Room Database for ChronoVault
 * Manages local storage of capsules, comments, and related data
 */
@Database(
    entities = [CapsuleEntity::class, CommentEntity::class, NotificationEntity::class, FriendEntity::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ChronoVaultDatabase : RoomDatabase() {

    abstract fun capsuleDao(): CapsuleDao
    abstract fun commentDao(): CommentDao
    abstract fun notificationDao(): NotificationDao
    abstract fun friendDao(): FriendDao

    companion object {
        const val DATABASE_NAME = "chronovault_db"

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notifications (
                        id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isRead INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        typeCategory TEXT NOT NULL,
                        capsuleId TEXT,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notifications (
                        id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isRead INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        typeCategory TEXT NOT NULL,
                        capsuleId TEXT,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS friends (
                        id TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        friendUserId TEXT NOT NULL,
                        status TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE capsules ADD COLUMN isPublic INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}