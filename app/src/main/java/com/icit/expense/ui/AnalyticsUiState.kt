package com.icit.expense.ui

import com.icit.expense.domain.model.Transaction

data class AnalyticsUiState(
    val totalExpense: Double = 0.0,
    val highestCategory: String = "--",
    val highestCategoryAmount: Double = 0.0,
    val totalTransactions: Int = 0,
    val averageDailyExpense: Double = 0.0,
    val categoryDistribution: List<CategoryPieData> = emptyList(),
    val weeklyTrend: List<DayValue> = emptyList(),
    val monthlyTrend: List<DayValue> = emptyList(),
    val selectedFilter: TimeFilter = TimeFilter.THIS_WEEK,
    val isLoading: Boolean = false
)

data class CategoryPieData(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val color: androidx.compose.ui.graphics.Color
)

data class DayValue(
    val day: String,
    val value: Double,
    val isToday: Boolean = false
)

enum class TimeFilter {
    THIS_WEEK, THIS_MONTH, LAST_MONTH, CUSTOM_RANGE
}
