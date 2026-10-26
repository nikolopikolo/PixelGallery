package com.pixelgallery.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pixelgallery_wear_prefs")

class WearPreferencesRepository(private val context: Context) {

    companion object {
        val KEY_KEEP_SCREEN_AWAKE = booleanPreferencesKey("pref_keep_screen_awake")
        const val DEFAULT_KEEP_SCREEN_AWAKE = true
    }

    val keepScreenAwakeFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_KEEP_SCREEN_AWAKE] ?: DEFAULT_KEEP_SCREEN_AWAKE
        }

    suspend fun setKeepScreenAwake(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_KEEP_SCREEN_AWAKE] = enabled
        }
    }
}
