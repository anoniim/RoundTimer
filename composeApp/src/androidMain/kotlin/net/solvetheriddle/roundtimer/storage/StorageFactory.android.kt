package net.solvetheriddle.roundtimer.storage

import android.content.Context

// This will need to be initialized with context when the app starts
private lateinit var applicationContext: Context

fun initializeStorageFactory(context: Context) {
    applicationContext = context.applicationContext
}

actual fun createPlatformStorage(): PlatformStorage {
    if (!::applicationContext.isInitialized) {
        throw IllegalStateException("StorageFactory not initialized. Call initializeStorageFactory() first.")
    }
    return PlatformStorage(applicationContext)
}
