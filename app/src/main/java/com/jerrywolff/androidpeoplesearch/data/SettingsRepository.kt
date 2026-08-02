package com.jerrywolff.androidpeoplesearch.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jerrywolff.androidpeoplesearch.model.AppSettings
import com.jerrywolff.androidpeoplesearch.model.SearchSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("privacy_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val historyEnabled = booleanPreferencesKey("history_enabled")
        val privacyMode = booleanPreferencesKey("privacy_mode")
        val pageDepth = intPreferencesKey("page_depth")
        val history = androidx.datastore.preferences.core.stringSetPreferencesKey("search_history")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { values ->
        AppSettings(
            historyEnabled = values[Keys.historyEnabled] ?: false,
            privacyMode = values[Keys.privacyMode] ?: true,
            pageDepth = (values[Keys.pageDepth] ?: 5).coerceIn(1, 5),
        )
    }

    suspend fun setHistoryEnabled(enabled: Boolean) = context.dataStore.edit { values ->
        values[Keys.historyEnabled] = enabled
        if (!enabled) values.remove(Keys.history)
    }

    suspend fun setPrivacyMode(enabled: Boolean) = context.dataStore.edit { it[Keys.privacyMode] = enabled }

    suspend fun setPageDepth(pageDepth: Int) = context.dataStore.edit { it[Keys.pageDepth] = pageDepth.coerceIn(1, 5) }

    suspend fun record(session: SearchSession) = context.dataStore.edit { values ->
        if (values[Keys.historyEnabled] == true) {
            val entry = "${session.searchedAtEpochMillis}|${session.query.searchText()}|${session.resultCount}"
            values[Keys.history] = ((values[Keys.history] ?: emptySet()) + entry).sortedDescending().take(20).toSet()
        }
    }

    suspend fun clearLocalData() = context.dataStore.edit { it.clear() }
}
