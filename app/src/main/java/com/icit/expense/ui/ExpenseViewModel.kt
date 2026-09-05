package com.icit.expense.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.icit.expense.domain.model.Transaction
import com.icit.expense.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class CategorySpending(val category: String, val amount: Double)

data class DashboardUiState(
    val todayExpense: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val budgetProgress: Float = 0f,
    val highestCategory: String = "None",
    val highestCategoryAmount: Double = 0.0,
    val todayTransactionCount: Int = 0,
    val averageDailyExpense: Double = 0.0,
    val remainingDailyBudget: Double = 0.0,
    val topCategories: List<CategoryBudgetData> = emptyList(),
    val todayTransactions: List<Transaction> = emptyList()
)

data class CategoryBudgetData(
    val categoryName: String,
    val spent: Double,
    val budget: Double,
    val progress: Float
)

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getTotalSpentUseCase: GetTotalSpentUseCase,
    private val getThemeModeUseCase: GetThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val getFontIndexUseCase: GetFontIndexUseCase,
    private val setFontIndexUseCase: SetFontIndexUseCase,
    private val getLoginStatusUseCase: GetLoginStatusUseCase,
    private val setLoginStatusUseCase: SetLoginStatusUseCase,
    private val clearLocalDataUseCase: ClearLocalDataUseCase,
    private val syncUserDataUseCase: SyncUserDataUseCase,
    val settingsRepository: com.icit.expense.domain.repository.SettingsRepository,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val seedCategoriesUseCase: SeedCategoriesUseCase,
    private val getBudgetUseCase: GetBudgetUseCase,
    private val setBudgetUseCase: SetBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val getCategoryBudgetsUseCase: GetCategoryBudgetsUseCase,
    private val setCategoryBudgetUseCase: SetCategoryBudgetUseCase,
    private val deleteCategoryBudgetUseCase: DeleteCategoryBudgetUseCase
) : ViewModel() {

    init {
        viewModelScope.launch {
            seedCategoriesUseCase()
        }
    }

    private val _refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    val categories: StateFlow<List<com.icit.expense.domain.model.Category>> = getCategoriesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addCategory(name: String) {
        viewModelScope.launch {
            addCategoryUseCase(name)
        }
    }

    fun deleteCategory(category: com.icit.expense.domain.model.Category) {
        viewModelScope.launch {
            deleteCategoryUseCase(category)
        }
    }

    // Budgeting Logic
    private fun getCurrentMonthYear(): String {
        val cal = Calendar.getInstance()
        return SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(cal.time)
    }

    val monthlyBudget: StateFlow<com.icit.expense.domain.model.Budget?> = getBudgetUseCase(getCurrentMonthYear())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val categoryBudgets: StateFlow<List<com.icit.expense.domain.model.CategoryBudget>> = getCategoryBudgetsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setMonthlyBudget(amount: Double) {
        viewModelScope.launch {
            setBudgetUseCase(getCurrentMonthYear(), amount)
        }
    }

    fun deleteMonthlyBudget() {
        viewModelScope.launch {
            deleteBudgetUseCase(getCurrentMonthYear())
        }
    }

    fun setCategoryBudget(categoryName: String, amount: Double) {
        viewModelScope.launch {
            setCategoryBudgetUseCase(categoryName, amount)
        }
    }

    fun deleteCategoryBudget(categoryName: String) {
        viewModelScope.launch {
            deleteCategoryBudgetUseCase(categoryName)
        }
    }

    val hasSkippedLogin: StateFlow<Boolean> = getLoginStatusUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setSkippedLogin(skipped: Boolean) {
        viewModelScope.launch {
            setLoginStatusUseCase(skipped)
        }
    }

    private fun getStartOfDay(time: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = time
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfDay(time: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = time
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayTransactions: StateFlow<List<Transaction>> = _refreshTrigger
        .flatMapLatest { time ->
            getTransactionsUseCase.getByDateRange(getStartOfDay(time), getEndOfDay(time))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val topThreeCategoriesBySpending: StateFlow<List<CategorySpending>> = todayTransactions
        .map { transactions ->
            transactions.groupBy { it.category }
                .map { (category, list) -> CategorySpending(category, list.sumOf { it.amount }) }
                .sortedByDescending { it.amount }
                .take(3)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentMonthSpendingByCategory: StateFlow<Map<String, Double>> = _refreshTrigger
        .flatMapLatest { time ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = time
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis
            
            getTransactionsUseCase.getByDateRange(start, end).map { transactions ->
                transactions.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayTotalSpent: StateFlow<Double?> = _refreshTrigger
        .flatMapLatest { time ->
            getTotalSpentUseCase.getByDateRange(getStartOfDay(time), getEndOfDay(time))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMonthTotalSpent: StateFlow<Double?> = _refreshTrigger
        .flatMapLatest { time ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = time
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            getTotalSpentForMonth(year, month)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val themeMode: StateFlow<Int> = getThemeModeUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun setThemeMode(mode: Int) {
        viewModelScope.launch {
            setThemeModeUseCase(mode)
        }
    }

    val fontIndex: StateFlow<Int> = getFontIndexUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun setFontIndex(index: Int) {
        viewModelScope.launch {
            setFontIndexUseCase(index)
        }
    }

    // Notification Preferences
    val dailyReminderEnabled: StateFlow<Boolean> = settingsRepository.dailyReminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dailyReminderTime: StateFlow<Pair<Int, Int>> = settingsRepository.dailyReminderTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20 to 0)

    val dailySummaryEnabled: StateFlow<Boolean> = settingsRepository.dailySummaryEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dailySummaryTime: StateFlow<Pair<Int, Int>> = settingsRepository.dailySummaryTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 21 to 0)

    val budgetAlertsEnabled: StateFlow<Boolean> = settingsRepository.budgetAlertsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setDailyReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDailyReminderEnabled(enabled) }
    }

    fun setDailyReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch { settingsRepository.setDailyReminderTime(hour, minute) }
    }

    fun setDailySummaryEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDailySummaryEnabled(enabled) }
    }

    fun setDailySummaryTime(hour: Int, minute: Int) {
        viewModelScope.launch { settingsRepository.setDailySummaryTime(hour, minute) }
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBudgetAlertsEnabled(enabled) }
    }

    val biometricEnabled: StateFlow<Boolean?> = settingsRepository.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBiometricEnabled(enabled) }
    }

    fun refreshData() {
        _refreshTrigger.value = System.currentTimeMillis()
    }

    fun getTransactionsForMonth(year: Int, month: Int): Flow<List<Transaction>> {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        
        return getTransactionsUseCase.getByDateRange(start, end)
    }

    fun getTotalSpentForMonth(year: Int, month: Int): Flow<Double?> {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        
        return getTotalSpentUseCase.getByDateRange(start, end)
    }

    fun getTransactionsForRange(start: Long, end: Long): Flow<List<Transaction>> {
        return getTransactionsUseCase.getByDateRange(start, end)
    }

    fun getTotalSpentForRange(start: Long, end: Long): Flow<Double?> {
        return getTotalSpentUseCase.getByDateRange(start, end)
    }

    fun addTransaction(title: String, amount: Double, category: String, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val transaction = Transaction(
                title = title,
                amount = amount,
                category = category,
                date = date
            )
            addTransactionUseCase(transaction)
            refreshData()
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            updateTransactionUseCase(transaction)
            refreshData()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            deleteTransactionUseCase(transaction)
            refreshData()
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            clearLocalDataUseCase()
            onComplete()
        }
    }

    fun syncData() {
        viewModelScope.launch {
            syncUserDataUseCase()
            refreshData()
        }
    }

    val dashboardUiState: StateFlow<DashboardUiState> = combine(
        todayTransactions,
        todayTotalSpent,
        currentMonthTotalSpent,
        monthlyBudget,
        currentMonthSpendingByCategory,
        categoryBudgets
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val todayTx = args[0] as List<Transaction>
        val todayTotal = args[1] as Double?
        val monthlyTotal = args[2] as Double?
        val mBudget = args[3] as com.icit.expense.domain.model.Budget?
        @Suppress("UNCHECKED_CAST")
        val monthCategorySpending = args[4] as Map<String, Double>
        @Suppress("UNCHECKED_CAST")
        val catBudgets = args[5] as List<com.icit.expense.domain.model.CategoryBudget>

        val monthlySpent = monthlyTotal ?: 0.0
        val explicitBudget = mBudget?.amount ?: 0.0
        val categoryTotalBudget = catBudgets.sumOf { it.amount }
        
        // If total budget is not provided (0.0), use sum of category budgets
        val budgetAmount = if (explicitBudget > 0) explicitBudget else categoryTotalBudget
        
        val remaining = maxOf(0.0, budgetAmount - monthlySpent)
        val progress = if (budgetAmount > 0) (monthlySpent / budgetAmount).toFloat() else 0f
        
        val highestEntry = monthCategorySpending.maxByOrNull { it.value }
        
        val cal = Calendar.getInstance()
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val avgDaily = if (dayOfMonth > 0) monthlySpent / dayOfMonth else 0.0
        // Daily budget: budgetAmount / total number of days in month
        val dailyBudget = if (totalDays > 0 && budgetAmount > 0) budgetAmount / totalDays else 0.0
        
        val topCats = monthCategorySpending.toList()
            .sortedByDescending { it.second }
            .take(3)
            .map { (name, spent) ->
                val catBudget = catBudgets.find { it.categoryName == name }?.amount ?: 0.0
                CategoryBudgetData(
                    categoryName = name,
                    spent = spent,
                    budget = catBudget,
                    progress = if (catBudget > 0) (spent / catBudget).toFloat() else 0f
                )
            }

        DashboardUiState(
            todayExpense = todayTotal ?: 0.0,
            monthlyExpense = monthlySpent,
            remainingBudget = remaining,
            budgetProgress = progress,
            highestCategory = highestEntry?.key ?: "None",
            highestCategoryAmount = highestEntry?.value ?: 0.0,
            todayTransactionCount = todayTx.size,
            averageDailyExpense = avgDaily,
            remainingDailyBudget = dailyBudget,
            topCategories = topCats,
            todayTransactions = todayTx
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}
