package com.icit.expense.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.icit.expense.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {
    private object PreferencesKeys {
        val THEME_MODE = intPreferencesKey("theme_mode")
        val FONT_INDEX = intPreferencesKey("font_index")
        val HAS_SKIPPED_LOGIN = booleanPreferencesKey("has_skipped_login")
        
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        val DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
        
        val DAILY_SUMMARY_ENABLED = booleanPreferencesKey("daily_summary_enabled")
        val DAILY_SUMMARY_HOUR = intPreferencesKey("daily_summary_hour")
        val DAILY_SUMMARY_MINUTE = intPreferencesKey("daily_summary_minute")
        val BUDGET_ALERTS_ENABLED = booleanPreferencesKey("budget_alerts_enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }

    override val themeModeFlow: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            // 0: Auto, 1: Off (Light), 2: On (Dark)
            preferences[PreferencesKeys.THEME_MODE] ?: 0
        }

    override suspend fun setThemeMode(themeMode: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode
        }
    }

    override val fontIndexFlow: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.FONT_INDEX] ?: 0
        }

    override suspend fun setFontIndex(index: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_INDEX] = index
        }
    }

    override val hasSkippedLoginFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.HAS_SKIPPED_LOGIN] ?: false
        }

    override suspend fun setHasSkippedLogin(skipped: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SKIPPED_LOGIN] = skipped
        }
    }

    override suspend fun clearSettings() {
        context.dataStore.edit { it.clear() }
    }

    override val dailyReminderEnabled: Flow<Boolean> = context.dataStore.data.map { 
        it[PreferencesKeys.DAILY_REMINDER_ENABLED] ?: true 
    }

    override val dailyReminderTime: Flow<Pair<Int, Int>> = context.dataStore.data.map { 
        val h = it[PreferencesKeys.DAILY_REMINDER_HOUR] ?: 20
        val m = it[PreferencesKeys.DAILY_REMINDER_MINUTE] ?: 0
        h to m
    }

    override val dailySummaryEnabled: Flow<Boolean> = context.dataStore.data.map { 
        it[PreferencesKeys.DAILY_SUMMARY_ENABLED] ?: true 
    }

    override val dailySummaryTime: Flow<Pair<Int, Int>> = context.dataStore.data.map { 
        val h = it[PreferencesKeys.DAILY_SUMMARY_HOUR] ?: 21
        val m = it[PreferencesKeys.DAILY_SUMMARY_MINUTE] ?: 0
        h to m
    }

    override val budgetAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { 
        it[PreferencesKeys.BUDGET_ALERTS_ENABLED] ?: true
    }

    override suspend fun setDailyReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DAILY_REMINDER_ENABLED] = enabled }
    }

    override suspend fun setDailyReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[PreferencesKeys.DAILY_REMINDER_HOUR] = hour
            it[PreferencesKeys.DAILY_REMINDER_MINUTE] = minute
        }
    }

    override suspend fun setDailySummaryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DAILY_SUMMARY_ENABLED] = enabled }
    }

    override suspend fun setDailySummaryTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[PreferencesKeys.DAILY_SUMMARY_HOUR] = hour
            it[PreferencesKeys.DAILY_SUMMARY_MINUTE] = minute
        }
    }

    override suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BUDGET_ALERTS_ENABLED] = enabled }
    }

    override val biometricEnabled: Flow<Boolean?> = context.dataStore.data.map {
        it[PreferencesKeys.BIOMETRIC_ENABLED] ?: false
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BIOMETRIC_ENABLED] = enabled }
    }
}
