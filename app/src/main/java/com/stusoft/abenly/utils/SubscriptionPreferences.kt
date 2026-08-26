package com.stusoft.abenly.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.subscriptionDataStore by preferencesDataStore(name = "subscription_preferences")

object SubscriptionPreferences {

    private val CUSTOM_SUBSCRIPTIONS_KEY = stringSetPreferencesKey("custom_subscriptions_set")

    // 1. Obtenir la liste brute des abonnements
    fun getCustomSubscriptions(context: Context): Flow<Set<String>> {
        return context.subscriptionDataStore.data.map { preferences ->
            preferences[CUSTOM_SUBSCRIPTIONS_KEY] ?: emptySet()
        }
    }

    // 2. Ajouter un abonnement ("Nom|Mois|Prix")
    suspend fun addCustomSubscription(context: Context, itemRaw: String) {
        context.subscriptionDataStore.edit { preferences ->
            val currentSet = preferences[CUSTOM_SUBSCRIPTIONS_KEY] ?: emptySet()
            preferences[CUSTOM_SUBSCRIPTIONS_KEY] = currentSet + itemRaw
        }
    }

    // 3. Supprimer un abonnement
    suspend fun removeCustomSubscription(context: Context, itemRaw: String) {
        context.subscriptionDataStore.edit { preferences ->
            val currentSet = preferences[CUSTOM_SUBSCRIPTIONS_KEY] ?: emptySet()
            preferences[CUSTOM_SUBSCRIPTIONS_KEY] = currentSet - itemRaw
        }
    }

    // 4. Enregistrer la dernière date (timestamp en millis)
    suspend fun saveLastDate(context: Context, key: String, dateMillis: Long) {
        val prefKey = longPreferencesKey(key)
        context.subscriptionDataStore.edit { preferences ->
            preferences[prefKey] = dateMillis
        }
    }

    // 5. Lire la dernière date
    fun getLastDate(context: Context, key: String): Flow<Long?> {
        val prefKey = longPreferencesKey(key)
        return context.subscriptionDataStore.data.map { preferences ->
            preferences[prefKey]
        }
    }
}