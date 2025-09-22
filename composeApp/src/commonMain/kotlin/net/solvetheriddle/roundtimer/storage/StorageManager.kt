package net.solvetheriddle.roundtimer.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import net.solvetheriddle.roundtimer.model.Game
import net.solvetheriddle.roundtimer.model.Round

/**
 * Cross-platform storage manager for Round data with persistent storage.
 * Uses platform-specific storage mechanisms:
 * - Android: DataStore preferences
 * - iOS: UserDefaults
 * - JVM: File-based storage
 * - JS/WASM: localStorage
 */
class RoundTimerStorage(
    private val platformStorage: PlatformStorage
) {
    private var cachedRounds: List<Round> = emptyList()
    private val json = Json { ignoreUnknownKeys = true }
    private val storageScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    companion object {
        private const val ROUNDS_KEY = "rounds_data"
        private const val GAMES_KEY = "games_data"
        private const val CONFIGURED_TIME_KEY = "configured_time"
        private const val VERSION_KEY = "storage_version"
        private const val CURRENT_VERSION = "1.0"
    }

    /**
     * Save configured time to persistent storage
     */
    suspend fun saveConfiguredTime(time: Long) {
        try {
            platformStorage.saveLong(CONFIGURED_TIME_KEY, time)
        } catch (e: Exception) {
            throw StorageException("Failed to save configured time", e)
        }
    }

    /**
     * Load configured time from persistent storage
     */
    suspend fun loadConfiguredTime(): Long? {
        return try {
            platformStorage.loadLong(CONFIGURED_TIME_KEY)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Save rounds to persistent storage
     */
    suspend fun saveRounds(rounds: List<Round>) {
        try {
            cachedRounds = rounds
            val jsonString = json.encodeToString(rounds)
            platformStorage.saveString(ROUNDS_KEY, jsonString)
        } catch (e: Exception) {
            // Keep cached data even if persistence fails
            cachedRounds = rounds
            throw StorageException("Failed to save rounds", e)
        }
    }
    
    /**
     * Load rounds from persistent storage
     */
    suspend fun loadRounds(): List<Round> {
        try {
            val jsonString = platformStorage.loadString(ROUNDS_KEY)
            if (jsonString != null) {
                val rounds = json.decodeFromString<List<Round>>(jsonString)
                cachedRounds = rounds
                return rounds
            }
        } catch (e: Exception) {
            // Fall back to cached data if available
            if (cachedRounds.isNotEmpty()) {
                return cachedRounds
            }
        }
        
        // Return empty list if no data available
        return emptyList()
    }

    /**
     * Save games to persistent storage
     */
    suspend fun saveGames(games: List<Game>) {
        try {
            val jsonString = json.encodeToString(games)
            platformStorage.saveString(GAMES_KEY, jsonString)
        } catch (e: Exception) {
            throw StorageException("Failed to save games", e)
        }
    }

    /**
     * Load games from persistent storage
     */
    suspend fun loadGames(): List<Game> {
        try {
            val jsonString = platformStorage.loadString(GAMES_KEY)
            if (jsonString != null) {
                return json.decodeFromString<List<Game>>(jsonString)
            }
        } catch (e: Exception) {
            // Return empty list if loading fails
        }
        return emptyList()
    }
    
    /**
     * Get rounds synchronously from cache (for quick UI updates)
     */
    fun getCachedRounds(): List<Round> {
        return cachedRounds
    }
    
    /**
     * Clear all stored data
     */
    suspend fun clearAll() {
        try {
            cachedRounds = emptyList()
            platformStorage.remove(ROUNDS_KEY)
        } catch (e: Exception) {
            throw StorageException("Failed to clear storage", e)
        }
    }
    
    /**
     * Initialize storage and load existing data
     */
    fun initialize() {
        storageScope.launch {
            try {
                // Check storage version and migrate if needed
                val currentStorageVersion = platformStorage.loadString(VERSION_KEY)
                if (currentStorageVersion == null) {
                    // First time setup - mark current version
                    platformStorage.saveString(VERSION_KEY, CURRENT_VERSION)
                } else if (currentStorageVersion != CURRENT_VERSION) {
                    // Migration would go here if needed in the future
                    // For now, just update the version
                    platformStorage.saveString(VERSION_KEY, CURRENT_VERSION)
                }
                
                loadRounds()
            } catch (e: Exception) {
                // Continue with empty cache
            }
        }
    }
}

/**
 * Custom exception for storage operations
 */
class StorageException(message: String, cause: Throwable? = null) : Exception(message, cause)
