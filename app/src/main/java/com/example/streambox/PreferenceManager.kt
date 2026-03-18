package com.example.streambox

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferenceManager(private val context: Context) {
    companion object {
        private val RD_TOKEN = stringPreferencesKey("rd_token")
        private val WATCH_LIST = stringSetPreferencesKey("watch_list")
        private val WATCHED_HISTORY = stringSetPreferencesKey("watched_history")
    }

    val rdToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[RD_TOKEN]
    }

    val watchList: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[WATCH_LIST] ?: emptySet()
    }

    val watchedHistory: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[WATCHED_HISTORY] ?: emptySet()
    }

    suspend fun saveRdToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[RD_TOKEN] = token
        }
    }

    suspend fun addToWatchList(movieId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[WATCH_LIST] ?: emptySet()
            preferences[WATCH_LIST] = current + movieId
        }
    }

    suspend fun removeFromWatchList(movieId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[WATCH_LIST] ?: emptySet()
            preferences[WATCH_LIST] = current - movieId
        }
    }

    suspend fun addToHistory(movieId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[WATCHED_HISTORY] ?: emptySet()
            preferences[WATCHED_HISTORY] = current + movieId
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
