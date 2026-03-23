package com.example.chronovault.data

import android.content.Context
import androidx.room.Room
import com.example.chronovault.data.local.ChronoVaultDatabase
import com.example.chronovault.data.remote.firebase.FirebaseAuthService
import com.example.chronovault.data.remote.firebase.FirestoreCapsuleService
import com.example.chronovault.data.remote.firebase.FirebaseUserService
import com.example.chronovault.data.remote.firebase.FirebaseSharingService
import com.example.chronovault.data.local.CommentDao
import com.example.chronovault.data.repository.AuthRepository
import com.example.chronovault.data.repository.CapsuleRepository
import com.example.chronovault.data.repository.UserRepository
import com.example.chronovault.data.repository.SharingRepository
import com.example.chronovault.utils.PreferencesManager

/**
 * Service Locator for dependency injection
 * Provides singleton instances of repositories and databases (Firebase + Room)
 */
object ServiceLocator {

    private var database: ChronoVaultDatabase? = null
    private var capsuleRepository: CapsuleRepository? = null
    private var authRepository: AuthRepository? = null
    private var userRepository: UserRepository? = null
    private var sharingRepository: SharingRepository? = null
    private var preferencesManager: PreferencesManager? = null
    private var firebaseAuthService: FirebaseAuthService? = null
    private var firestoreCapsuleService: FirestoreCapsuleService? = null
    private var firebaseUserService: FirebaseUserService? = null
    private var firebaseSharingService: FirebaseSharingService? = null

    @Volatile
    private var dbLock = Any()

    fun provideDatabase(context: Context): ChronoVaultDatabase {
        return database ?: synchronized(dbLock) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                ChronoVaultDatabase::class.java,
                ChronoVaultDatabase.DATABASE_NAME
            ).fallbackToDestructiveMigration().build()
            database = instance
            instance
        }
    }

    fun providePreferencesManager(context: Context): PreferencesManager {
        return preferencesManager ?: synchronized(dbLock) {
            val instance = PreferencesManager(context)
            preferencesManager = instance
            instance
        }
    }

    fun provideFirebaseAuthService(): FirebaseAuthService {
        return firebaseAuthService ?: synchronized(dbLock) {
            val instance = FirebaseAuthService()
            firebaseAuthService = instance
            instance
        }
    }

    fun provideFirestoreCapsuleService(): FirestoreCapsuleService {
        return firestoreCapsuleService ?: synchronized(dbLock) {
            val instance = FirestoreCapsuleService()
            firestoreCapsuleService = instance
            instance
        }
    }

    fun provideFirebaseUserService(): FirebaseUserService {
        return firebaseUserService ?: synchronized(dbLock) {
            val instance = FirebaseUserService()
            firebaseUserService = instance
            instance
        }
    }

    fun provideFirebaseSharingService(): FirebaseSharingService {
        return firebaseSharingService ?: synchronized(dbLock) {
            val instance = FirebaseSharingService()
            firebaseSharingService = instance
            instance
        }
    }

    fun provideCapsuleRepository(context: Context): CapsuleRepository {
        return capsuleRepository ?: synchronized(dbLock) {
            val db = provideDatabase(context)
            val firestoreService = provideFirestoreCapsuleService()
            val prefs = providePreferencesManager(context)
            val repo = CapsuleRepository(db.capsuleDao(), firestoreService, prefs)
            capsuleRepository = repo
            repo
        }
    }

    fun provideAuthRepository(context: Context): AuthRepository {
        return authRepository ?: synchronized(dbLock) {
            val firebaseAuth = provideFirebaseAuthService()
            val prefs = providePreferencesManager(context)
            val repo = AuthRepository(firebaseAuth, prefs)
            authRepository = repo
            repo
        }
    }

    fun provideUserRepository(context: Context): UserRepository {
        return userRepository ?: synchronized(dbLock) {
            val firebaseUser = provideFirebaseUserService()
            val prefs = providePreferencesManager(context)
            val repo = UserRepository(firebaseUser, prefs)
            userRepository = repo
            repo
        }
    }

    fun provideSharingRepository(context: Context): SharingRepository {
        return sharingRepository ?: synchronized(dbLock) {
            val firebaseSharing = provideFirebaseSharingService()
            val prefs = providePreferencesManager(context)
            val repo = SharingRepository(firebaseSharing, prefs)
            sharingRepository = repo
            repo
        }
    }

    fun provideCommentDao(context: Context): CommentDao {
        return provideDatabase(context).commentDao()
    }

    fun resetRepositories() {
        synchronized(dbLock) {
            capsuleRepository = null
            authRepository = null
            userRepository = null
            sharingRepository = null
            database = null
            preferencesManager = null
            firebaseAuthService = null
            firestoreCapsuleService = null
            firebaseUserService = null
            firebaseSharingService = null
        }
    }
}


