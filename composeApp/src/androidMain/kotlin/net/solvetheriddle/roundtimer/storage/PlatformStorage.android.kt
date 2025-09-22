package net.solvetheriddle.roundtimer.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout

// Extension property to create DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "roundtimer_prefs")

actual class PlatformStorage(private val context: Context) {
    
    actual suspend fun saveString(key: String, value: String) {
        try {
            val dataStoreKey = stringPreferencesKey(key)
            withTimeout(5000) { // 5 second timeout
                context.dataStore.edit { preferences ->
                    preferences[dataStoreKey] = value
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }
    
    actual suspend fun loadString(key: String): String? {
        return try {
            val dataStoreKey = stringPreferencesKey(key)
            withTimeout(5000) { // 5 second timeout
                context.dataStore.data.map { preferences ->
                    preferences[dataStoreKey]
                }.first()
            }
        } catch (e: Exception) {
            null
        }
    }

    actual suspend fun saveLong(key: String, value: Long) {
        try {
            val dataStoreKey = longPreferencesKey(key)
            withTimeout(5000) { // 5 second timeout
                context.dataStore.edit { preferences ->
                    preferences[dataStoreKey] = value
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    actual suspend fun loadLong(key: String): Long? {
        return try {
            val dataStoreKey = longPreferencesKey(key)
            withTimeout(5000) { // 5 second timeout
                context.dataStore.data.map { preferences ->
                    preferences[dataStoreKey]
                }.first()
            }
        } catch (e: Exception) {
            null
        }
    }
    
    actual suspend fun remove(key: String) {
        val dataStoreKey = stringPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences.remove(dataStoreKey)
        }
    }
    
    actual suspend fun clear() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}