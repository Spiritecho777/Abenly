package com.example.abenly.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
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
}