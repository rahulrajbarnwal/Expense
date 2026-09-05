package com.icit.expense.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeModeFlow: Flow<Int>
    suspend fun setThemeMode(themeMode: Int)
    val fontIndexFlow: Flow<Int>
    suspend fun setFontIndex(index: Int)
    val hasSkippedLoginFlow: Flow<Boolean>
    suspend fun setHasSkippedLogin(skipped: Boolean)
    suspend fun clearSettings()

    // Notification Preferences (Local Only)
    val dailyReminderEnabled: Flow<Boolean>
    val dailyReminderTime: Flow<Pair<Int, Int>>
    val dailySummaryEnabled: Flow<Boolean>
    val dailySummaryTime: Flow<Pair<Int, Int>>
    val budgetAlertsEnabled: Flow<Boolean>

    suspend fun setDailyReminderEnabled(enabled: Boolean)
    suspend fun setDailyReminderTime(hour: Int, minute: Int)
    suspend fun setDailySummaryEnabled(enabled: Boolean)
    suspend fun setDailySummaryTime(hour: Int, minute: Int)
    suspend fun setBudgetAlertsEnabled(enabled: Boolean)

    // Security Preferences (Local Only)
    val biometricEnabled: Flow<Boolean?>
    suspend fun setBiometricEnabled(enabled: Boolean)
}
