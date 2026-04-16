package com.example.chronovault.data.repository

import com.example.chronovault.data.local.FriendDao
import com.example.chronovault.data.local.entity.FriendEntity
import com.example.chronovault.data.local.entity.FriendStatus
import com.example.chronovault.data.remote.firebase.FirebaseFriendService
import com.example.chronovault.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
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
        val targetId = friendUserId.trim()
        if (targetId.isBlank()) return Result.failure(Exception("Friend ID is required"))
        if (targetId == userId) return Result.failure(Exception("You cannot send a request to yourself"))

        val targetExists = firebaseFriendService.userExists(targetId)
        if (targetExists.isFailure) {
            return Result.failure(targetExists.exceptionOrNull() ?: Exception("Unable to verify user"))
        }
        if (!targetExists.getOrDefault(false)) {
            return Result.failure(Exception("User does not exist"))
        }

        val alreadyFriends = firebaseFriendService.areFriends(userId, targetId)
        if (alreadyFriends.isFailure) {
            return Result.failure(alreadyFriends.exceptionOrNull() ?: Exception("Unable to verify friendship"))
        }
        if (alreadyFriends.getOrDefault(false)) {
            return Result.failure(Exception("You are already friends"))
        }

        val pendingRequestExists = firebaseFriendService.hasPendingRequestBetween(userId, targetId)
        if (pendingRequestExists.isFailure) {
            return Result.failure(pendingRequestExists.exceptionOrNull() ?: Exception("Unable to check pending requests"))
        }
        if (pendingRequestExists.getOrDefault(false)) {
            return Result.failure(Exception("A friend request is already pending"))
        }

        val remoteResult = firebaseFriendService.sendFriendRequest(userId, targetId)
        if (remoteResult.isFailure) {
            return Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to send request"))
        }

        val localId = "${userId}_$targetId"
        friendDao.upsert(
            FriendEntity(
                id = localId,
                userId = userId,
                friendUserId = targetId,
                status = FriendStatus.PENDING
            )
        )

        return Result.success(Unit)
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

    suspend fun syncAcceptedFriends() {
        val currentUserId = preferencesManager.getUserId().orEmpty()
        if (currentUserId.isBlank()) return

        firebaseFriendService.observeAcceptedFriends(currentUserId).collectLatest { friendIds ->
            friendIds.forEach { friendId ->
                val localId = "${currentUserId}_$friendId"
                friendDao.upsert(
                    FriendEntity(
                        id = localId,
                        userId = currentUserId,
                        friendUserId = friendId,
                        status = FriendStatus.ACCEPTED
                    )
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

