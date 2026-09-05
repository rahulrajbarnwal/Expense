package com.icit.expense.notification

import android.content.Context
import androidx.work.*
import com.icit.expense.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.*
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    const val DAILY_REMINDER_TAG = "daily_reminder"
    const val DAILY_SUMMARY_TAG = "daily_summary"

    fun cancelAllNotifications(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(DAILY_REMINDER_TAG)
        WorkManager.getInstance(context).cancelAllWorkByTag(DAILY_SUMMARY_TAG)
    }

    suspend fun scheduleDailyNotifications(context: Context, settingsRepository: SettingsRepository) {
        // Daily Reminder
        if (settingsRepository.dailyReminderEnabled.first()) {
            val (hour, minute) = settingsRepository.dailyReminderTime.first()
            scheduleWorker(context, hour, minute, DAILY_REMINDER_TAG, DailyReminderWorker::class.java)
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(DAILY_REMINDER_TAG)
        }

        // Daily Summary
        if (settingsRepository.dailySummaryEnabled.first()) {
            val (hour, minute) = settingsRepository.dailySummaryTime.first()
            scheduleWorker(context, hour, minute, DAILY_SUMMARY_TAG, DailySummaryWorker::class.java)
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(DAILY_SUMMARY_TAG)
        }
    }

    private fun <T : ListenableWorker> scheduleWorker(
        context: Context,
        hour: Int,
        minute: Int,
        tag: String,
        workerClass: Class<T>
    ) {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val initialDelay = calendar.timeInMillis - now

        val workRequest = PeriodicWorkRequest.Builder(workerClass, 24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(tag)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            tag,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
