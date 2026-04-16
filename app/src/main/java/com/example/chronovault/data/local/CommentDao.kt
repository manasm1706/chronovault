package com.example.chronovault.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chronovault.data.local.entity.CommentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Comment entities
 */
@Dao
interface CommentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("SELECT * FROM comments WHERE capsuleId = :capsuleId ORDER BY createdAt DESC")
    fun getCommentsForCapsule(capsuleId: String): Flow<List<CommentEntity>>

    @Query("DELETE FROM comments WHERE id = :commentId AND authorId = :userId")
    suspend fun deleteComment(commentId: String, userId: String)

    @Query("DELETE FROM comments WHERE capsuleId = :capsuleId")
    suspend fun deleteAllCommentsForCapsule(capsuleId: String)

    @Query("SELECT COUNT(*) FROM comments WHERE capsuleId = :capsuleId")
    suspend fun getCommentCount(capsuleId: String): Int

    @Query(
        """
        SELECT comments.id AS id,
               comments.capsuleId AS capsuleId,
               comments.authorId AS authorId,
               comments.authorName AS authorName,
               comments.text AS text,
               comments.createdAt AS createdAt,
               capsules.title AS capsuleTitle
        FROM comments
        INNER JOIN capsules ON comments.capsuleId = capsules.id
        WHERE capsules.ownerId = :ownerId
        ORDER BY comments.createdAt DESC
        """
    )
    fun getCommentsForCapsuleOwner(ownerId: String): Flow<List<OwnerCapsuleCommentItem>>
}

data class OwnerCapsuleCommentItem(
    val id: String,
    val capsuleId: String,
    val authorId: String,
    val authorName: String,
    val text: String,
    val createdAt: Long,
    val capsuleTitle: String
)

