package com.ifsvivek.nammahomestay.data.role

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.modeDataStore by preferencesDataStore(name = "user_mode")
private val MODE_KEY = stringPreferencesKey("current_mode")

/**
 * Persists [UserMode] to disk via Preferences DataStore. `null` means "the user
 * hasn't picked a mode yet" — which triggers the one-time ModePickerScreen.
 */
class UserModeStore(private val appContext: Context) {

    val mode: Flow<UserMode?> = appContext.modeDataStore.data.map { prefs ->
        prefs[MODE_KEY]?.let { name -> runCatching { UserMode.valueOf(name) }.getOrNull() }
    }

    suspend fun set(mode: UserMode) {
        appContext.modeDataStore.edit { it[MODE_KEY] = mode.name }
    }

    suspend fun clear() {
        appContext.modeDataStore.edit { it.remove(MODE_KEY) }
    }
}
