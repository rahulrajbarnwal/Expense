package com.icit.expense.ui

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.icit.expense.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(TimeFilter.THIS_WEEK)
    private val _customRange = MutableStateFlow<Pair<Long, Long>?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AnalyticsUiState> = _selectedFilter.flatMapLatest { filter ->
        val range = getRangeForFilter(filter)
        getTransactionsUseCase.getByDateRange(range.first, range.second).map { transactions ->
            calculateAnalytics(transactions, filter)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    fun setFilter(filter: TimeFilter) {
        _selectedFilter.value = filter
    }

    private fun getRangeForFilter(filter: TimeFilter): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (filter) {
            TimeFilter.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                start to cal.timeInMillis
            }
            TimeFilter.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                start to cal.timeInMillis
            }
            TimeFilter.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                start to cal.timeInMillis
            }
            TimeFilter.CUSTOM_RANGE -> _customRange.value ?: (0L to System.currentTimeMillis())
        }
    }

    private fun calculateAnalytics(transactions: List<com.icit.expense.domain.model.Transaction>, filter: TimeFilter): AnalyticsUiState {
        if (transactions.isEmpty()) return AnalyticsUiState(selectedFilter = filter)

        val total = transactions.sumOf { it.amount }
        val highestEntry = transactions.groupBy { it.category }
            .mapValues { it.value.sumOf { t -> t.amount } }
            .maxByOrNull { it.value }

        val categoryDistribution = transactions.groupBy { it.category }
            .map { (cat, list) ->
                val amount = list.sumOf { it.amount }
                CategoryPieData(
                    category = cat,
                    amount = amount,
                    percentage = (amount / total).toFloat(),
                    color = getCategoryColor(cat)
                )
            }.sortedByDescending { it.amount }

        // Trend logic
        val trend = when (filter) {
            TimeFilter.THIS_WEEK -> calculateWeeklyTrend(transactions)
            else -> calculateMonthlyTrend(transactions)
        }

        val daysCount = transactions.map { 
            val c = Calendar.getInstance()
            c.timeInMillis = it.date
            c.get(Calendar.DAY_OF_YEAR)
        }.distinct().size.coerceAtLeast(1)

        return AnalyticsUiState(
            totalExpense = total,
            highestCategory = highestEntry?.key ?: "--",
            highestCategoryAmount = highestEntry?.value ?: 0.0,
            totalTransactions = transactions.size,
            averageDailyExpense = total / daysCount,
            categoryDistribution = categoryDistribution,
            weeklyTrend = if (filter == TimeFilter.THIS_WEEK) trend else emptyList(),
            monthlyTrend = if (filter != TimeFilter.THIS_WEEK) trend else emptyList(),
            selectedFilter = filter
        )
    }

    private fun calculateWeeklyTrend(transactions: List<com.icit.expense.domain.model.Transaction>): List<DayValue> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val days = mutableListOf<DayValue>()
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        for (i in 0..6) {
            val currentDay = cal.get(Calendar.DAY_OF_WEEK)
            val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
            val amount = transactions.filter {
                val tCal = Calendar.getInstance()
                tCal.timeInMillis = it.date
                tCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
            }.sumOf { it.amount }
            
            days.add(DayValue(dayName, amount, currentDay == today))
            cal.add(Calendar.DAY_OF_WEEK, 1)
        }
        return days
    }

    private fun calculateMonthlyTrend(transactions: List<com.icit.expense.domain.model.Transaction>): List<DayValue> {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        
        cal.set(currentYear, currentMonth, 1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val trend = mutableListOf<DayValue>()
        
        for (i in 1..daysInMonth) {
            val amount = transactions.filter {
                val tCal = Calendar.getInstance()
                tCal.timeInMillis = it.date
                tCal.get(Calendar.DAY_OF_MONTH) == i && tCal.get(Calendar.MONTH) == currentMonth
            }.sumOf { it.amount }
            
            trend.add(DayValue(i.toString(), amount))
        }
        return trend
    }
}
