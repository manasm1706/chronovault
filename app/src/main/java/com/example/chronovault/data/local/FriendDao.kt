package com.example.chronovault.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chronovault.data.local.entity.FriendEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {

    @Query("SELECT * FROM friends WHERE userId = :userId ORDER BY updatedAt DESC")
    fun observeFriends(userId: String): Flow<List<FriendEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(friend: FriendEntity)

    @Query("UPDATE friends SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)
}

