package net.solvetheriddle.roundtimer.storage

import kotlinx.browser.localStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class PlatformStorage() {
    private val keyPrefix = "roundtimer_"
    
    actual suspend fun saveString(key: String, value: String) {
        withContext(Dispatchers.Default) {
            try {
                localStorage.setItem("${keyPrefix}$key", value)
            } catch (e: Exception) {
                throw e
            }
        }
    }
    
    actual suspend fun loadString(key: String): String? {
        return withContext(Dispatchers.Default) {
            try {
                localStorage.getItem("${keyPrefix}$key")
            } catch (e: Exception) {
                null
            }
        }
    }
    
    actual suspend fun remove(key: String) {
        withContext(Dispatchers.Default) {
            try {
                localStorage.removeItem("${keyPrefix}$key")
            } catch (e: Exception) {
                // Ignore removal errors
            }
        }
    }
    
    actual suspend fun clear() {
        withContext(Dispatchers.Default) {
            try {
                // Get all keys and remove the ones with our prefix
                val keysToRemove = mutableListOf<String>()
                for (i in 0 until localStorage.length) {
                    val key = localStorage.key(i)
                    if (key != null && key.startsWith(keyPrefix)) {
                        keysToRemove.add(key)
                    }
                }
                keysToRemove.forEach { key ->
                    localStorage.removeItem(key)
                }
            } catch (e: Exception) {
                // Ignore clear errors
            }
        }
    }
}