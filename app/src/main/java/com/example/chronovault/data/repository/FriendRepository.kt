package com.example.chronovault.data.repository

import com.example.chronovault.data.local.FriendDao
import com.example.chronovault.data.local.entity.FriendEntity
import com.example.chronovault.data.local.entity.FriendStatus
import com.example.chronovault.data.remote.firebase.FirebaseFriendService
import com.example.chronovault.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FriendRepository(
    private val friendDao: FriendDao,
    private val firebaseFriendService: FirebaseFriendService,
    private val preferencesManager: PreferencesManager
) {

    data class FriendRequest(
        val requestId: String,
        val fromUserId: String,
        val status: String
    )

    fun observeFriends(): Flow<List<FriendEntity>> {
        val userId = preferencesManager.getUserId().orEmpty()
        return friendDao.observeFriends(userId)
    }

    suspend fun sendFriendRequest(friendUserId: String): Result<Unit> {
        val userId = preferencesManager.getUserId() ?: return Result.failure(Exception("User not authenticated"))
        if (friendUserId.isBlank()) return Result.failure(Exception("Friend ID is required"))
        if (friendUserId == userId) return Result.failure(Exception("You cannot add yourself"))

        val localId = "${userId}_$friendUserId"
        friendDao.upsert(
            FriendEntity(
                id = localId,
                userId = userId,
                friendUserId = friendUserId,
                status = FriendStatus.PENDING
            )
        )

        return firebaseFriendService.sendFriendRequest(userId, friendUserId).map { Unit }
    }

    suspend fun acceptFriend(friendUserId: String): Result<Unit> {
        val userId = preferencesManager.getUserId() ?: return Result.failure(Exception("User not authenticated"))
        val localId = "${userId}_$friendUserId"
        friendDao.upsert(
            FriendEntity(
                id = localId,
                userId = userId,
                friendUserId = friendUserId,
                status = FriendStatus.ACCEPTED
            )
        )
        return Result.success(Unit)
    }

    fun observeIncomingRequests(): Flow<List<FriendRequest>> {
        val userId = preferencesManager.getUserId().orEmpty()
        return firebaseFriendService.observeIncomingRequests(userId).map { remoteRequests ->
            remoteRequests.map { request ->
                FriendRequest(
                    requestId = request.id,
                    fromUserId = request.senderId,
                    status = request.status
                )
            }
        }
    }

    suspend fun acceptFriendRequest(requestId: String, fromUserId: String): Result<Unit> {
        val remote = firebaseFriendService.acceptFriendRequest(requestId)
        if (remote.isFailure) return Result.failure(remote.exceptionOrNull() ?: Exception("Failed to accept request"))

        val currentUserId = preferencesManager.getUserId()
            ?: return Result.failure(Exception("User not authenticated"))

        val friendshipResult = firebaseFriendService.createFriendshipIfMissing(currentUserId, fromUserId)
        if (friendshipResult.isFailure) {
            return Result.failure(friendshipResult.exceptionOrNull() ?: Exception("Failed to create friendship"))
        }

        return acceptFriend(fromUserId)
    }

    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return firebaseFriendService.rejectFriendRequest(requestId)
    }
}

