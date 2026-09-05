package com.icit.expense.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.icit.expense.domain.usecase.GetTransactionsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.*

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getTransactionsUseCase: GetTransactionsUseCase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val start = cal.timeInMillis
        
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val end = cal.timeInMillis

        val transactions = getTransactionsUseCase.getByDateRange(start, end).first()

        if (transactions.isEmpty()) {
            NotificationHelper.showNotification(
                applicationContext,
                2001,
                "Expense Tracker",
                "Don't forget to add today's expenses."
            )
        }

        return Result.success()
    }
}
