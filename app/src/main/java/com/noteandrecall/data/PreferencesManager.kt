package com.noteandrecall.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    private object Keys {
        val AI_ENDPOINT = stringPreferencesKey("ai_endpoint")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val FAMILIAR_BONUS = intPreferencesKey("familiar_bonus")
        val UNFAMILIAR_PENALTY = intPreferencesKey("unfamiliar_penalty")
    }

    val aiEndpoint: Flow<String> = context.dataStore.data.map { it[Keys.AI_ENDPOINT] ?: "https://api.xiaomimimo.com/v1" }
    val aiApiKey: Flow<String> = context.dataStore.data.map { it[Keys.AI_API_KEY] ?: "" }
    val aiModel: Flow<String> = context.dataStore.data.map { it[Keys.AI_MODEL] ?: "mimo-v2.5" }
    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_DARK_THEME] ?: false }
    val familiarBonus: Flow<Int> = context.dataStore.data.map { it[Keys.FAMILIAR_BONUS] ?: 1 }
    val unfamiliarPenalty: Flow<Int> = context.dataStore.data.map { it[Keys.UNFAMILIAR_PENALTY] ?: 5 }

    suspend fun setAiEndpoint(value: String) { context.dataStore.edit { it[Keys.AI_ENDPOINT] = value } }
    suspend fun setAiApiKey(value: String) { context.dataStore.edit { it[Keys.AI_API_KEY] = value } }
    suspend fun setAiModel(value: String) { context.dataStore.edit { it[Keys.AI_MODEL] = value } }
    suspend fun setDarkTheme(value: Boolean) { context.dataStore.edit { it[Keys.IS_DARK_THEME] = value } }
    suspend fun setFamiliarBonus(value: Int) { context.dataStore.edit { it[Keys.FAMILIAR_BONUS] = value } }
    suspend fun setUnfamiliarPenalty(value: Int) { context.dataStore.edit { it[Keys.UNFAMILIAR_PENALTY] = value } }
}
