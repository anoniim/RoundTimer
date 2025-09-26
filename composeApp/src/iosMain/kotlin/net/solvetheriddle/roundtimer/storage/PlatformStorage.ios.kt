package net.solvetheriddle.roundtimer.storage

import platform.Foundation.NSUserDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

actual class PlatformStorage() {
    private val userDefaults = NSUserDefaults.standardUserDefaults()
    private val keyPrefix = "roundtimer_"
    
    actual suspend fun saveString(key: String, value: String) {
        withContext(Dispatchers.IO) {
            userDefaults.setObject(value, "${keyPrefix}$key")
        }
    }
    
    actual suspend fun loadString(key: String): String? {
        return withContext(Dispatchers.IO) {
            userDefaults.stringForKey("${keyPrefix}$key")
        }
    }
    
    actual suspend fun remove(key: String) {
        withContext(Dispatchers.IO) {
            userDefaults.removeObjectForKey("${keyPrefix}$key")
        }
    }
    
    actual suspend fun clear() {
        withContext(Dispatchers.IO) {
            // Get all keys and remove the ones with our prefix
            val allKeys = userDefaults.dictionaryRepresentation().keys
            allKeys.forEach { key ->
                if (key is String && key.startsWith(keyPrefix)) {
                    userDefaults.removeObjectForKey(key)
                }
            }
        }
    }
}