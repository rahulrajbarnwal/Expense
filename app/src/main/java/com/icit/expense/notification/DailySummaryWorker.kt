package com.icit.expense.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.icit.expense.domain.usecase.GetTotalSpentUseCase
import com.icit.expense.domain.usecase.GetBudgetUseCase
import com.icit.expense.ui.formatCurrency
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getTotalSpentUseCase: GetTotalSpentUseCase,
    private val getBudgetUseCase: GetBudgetUseCase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val cal = Calendar.getInstance()
        val todayStart = cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        
        val todayEnd = cal.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        val todaySpent = getTotalSpentUseCase.getByDateRange(todayStart, todayEnd).first() ?: 0.0

        if (todaySpent > 0) {
            NotificationHelper.showNotification(
                applicationContext,
                2002,
                "Today's Spending",
                "You spent ${formatCurrency(todaySpent)} today."
            )
        }

        // Budget Alerts
        val monthYear = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
        val monthlyBudget = getBudgetUseCase(monthYear).first()?.amount ?: 0.0

        if (monthlyBudget > 0) {
            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
            }.timeInMillis
            
            val totalMonthSpent = getTotalSpentUseCase.getByDateRange(monthStart, todayEnd).first() ?: 0.0
            
            if (totalMonthSpent >= monthlyBudget) {
                NotificationHelper.showNotification(
                    applicationContext,
                    2003,
                    "Budget Exceeded",
                    "You've exceeded your monthly budget by ${formatCurrency(totalMonthSpent - monthlyBudget)}."
                )
            } else if (totalMonthSpent >= monthlyBudget * 0.8) {
                NotificationHelper.showNotification(
                    applicationContext,
                    2004,
                    "Budget Warning",
                    "You've used 80% of your monthly budget."
                )
            }
        }

        return Result.success()
    }
}
