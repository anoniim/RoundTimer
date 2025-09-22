package net.solvetheriddle.roundtimer.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties

actual class PlatformStorage() {
    private val userHome = System.getProperty("user.home")
    private val appDataDir = File(userHome, ".roundtimer")
    private val storageFile = File(appDataDir, "storage.properties")
    
    init {
        // Ensure the app data directory exists
        if (!appDataDir.exists()) {
            appDataDir.mkdirs()
        }
    }
    
    actual suspend fun saveString(key: String, value: String) {
        withContext(Dispatchers.IO) {
            val properties = loadProperties()
            properties[key] = value
            saveProperties(properties)
        }
    }
    
    actual suspend fun loadString(key: String): String? {
        return withContext(Dispatchers.IO) {
            val properties = loadProperties()
            properties.getProperty(key)
        }
    }

    actual suspend fun saveLong(key: String, value: Long) {
        withContext(Dispatchers.IO) {
            val properties = loadProperties()
            properties[key] = value.toString()
            saveProperties(properties)
        }
    }

    actual suspend fun loadLong(key: String): Long? {
        return withContext(Dispatchers.IO) {
            val properties = loadProperties()
            properties.getProperty(key)?.toLongOrNull()
        }
    }
    
    actual suspend fun remove(key: String) {
        withContext(Dispatchers.IO) {
            val properties = loadProperties()
            properties.remove(key)
            saveProperties(properties)
        }
    }
    
    actual suspend fun clear() {
        withContext(Dispatchers.IO) {
            if (storageFile.exists()) {
                storageFile.delete()
            }
        }
    }
    
    private fun loadProperties(): Properties {
        val properties = Properties()
        if (storageFile.exists()) {
            try {
                storageFile.inputStream().use { inputStream ->
                    properties.load(inputStream)
                }
            } catch (e: Exception) {
                // If we can't read the file, start with empty properties
            }
        }
        return properties
    }
    
    private fun saveProperties(properties: Properties) {
        try {
            storageFile.outputStream().use { outputStream ->
                properties.store(outputStream, "RoundTimer Storage")
            }
        } catch (e: Exception) {
            throw e
        }
    }
}