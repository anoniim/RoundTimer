package net.solvetheriddle.roundtimer.storage

/**
 * Platform-specific storage interface for persisting data across app sessions.
 * Each platform implements this interface with appropriate storage mechanisms:
 * - Android: DataStore preferences
 * - iOS: UserDefaults
 * - JVM: File-based storage
 * - JS/WASM: localStorage
 */
expect class PlatformStorage {
    /**
     * Save a string value with the given key
     */
    suspend fun saveString(key: String, value: String)
    
    /**
     * Load a string value by key, returns null if not found
     */
    suspend fun loadString(key: String): String?
    
    /**
     * Remove a value by key
     */
    suspend fun remove(key: String)
    
    /**
     * Clear all stored data
     */
    suspend fun clear()
}