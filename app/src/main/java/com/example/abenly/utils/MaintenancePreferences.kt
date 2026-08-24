package com.example.abenly.utils

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "maintenance_prefs")

object MaintenancePreferences {
    fun getLastDate(context: Context, key: String): Flow<Long?> {
        val prefKey = longPreferencesKey(key)
        return context.dataStore.data.map { preferences ->
            preferences[prefKey]
        }
    }

    suspend fun saveLastDate(context: Context, key: String, timestamp: Long) {
        val prefKey = longPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[prefKey] = timestamp
        }
    }

    private val CUSTOM_ITEMS_KEY = stringSetPreferencesKey("custom_items_set")

    fun getCustomItems(context: Context): Flow<Set<String>> {
        return context.dataStore.data.map { prefs -> prefs[CUSTOM_ITEMS_KEY] ?: emptySet() }
    }

    suspend fun addCustomItem(context: Context, name: String, maxMonths: Long) {
        context.dataStore.edit { prefs ->
            val currentSet = prefs[CUSTOM_ITEMS_KEY] ?: emptySet()
            // Stocke sous le format "nom|intervalle"
            prefs[CUSTOM_ITEMS_KEY] = currentSet + "$name|$maxMonths"
        }
    }

    suspend fun removeCustomItem(context: Context, name: String, maxMonths: Long) {
        context.dataStore.edit { prefs ->
            val currentSet = prefs[CUSTOM_ITEMS_KEY] ?: emptySet()
            // Retire l'élément du Set
            prefs[CUSTOM_ITEMS_KEY] = currentSet - "$name|$maxMonths"
            // Supprime aussi la date enregistrée pour cet élément
            prefs.remove(longPreferencesKey("date_$name"))
        }
    }
}