package com.icit.expense.ui

import com.icit.expense.domain.model.Budget
import com.icit.expense.domain.model.Category
import com.icit.expense.domain.model.CategoryBudget
import com.icit.expense.domain.model.Transaction
import com.icit.expense.domain.repository.SettingsRepository
import com.icit.expense.domain.usecase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {

    private val getTransactionsUseCase: GetTransactionsUseCase = mock()
    private val addTransactionUseCase: AddTransactionUseCase = mock()
    private val updateTransactionUseCase: UpdateTransactionUseCase = mock()
    private val deleteTransactionUseCase: DeleteTransactionUseCase = mock()
    private val getTotalSpentUseCase: GetTotalSpentUseCase = mock()
    private val getThemeModeUseCase: GetThemeModeUseCase = mock()
    private val setThemeModeUseCase: SetThemeModeUseCase = mock()
    private val getFontIndexUseCase: GetFontIndexUseCase = mock()
    private val setFontIndexUseCase: SetFontIndexUseCase = mock()
    private val getLoginStatusUseCase: GetLoginStatusUseCase = mock()
    private val setLoginStatusUseCase: SetLoginStatusUseCase = mock()
    private val clearLocalDataUseCase: ClearLocalDataUseCase = mock()
    private val syncUserDataUseCase: SyncUserDataUseCase = mock()
    private val settingsRepository: SettingsRepository = mock()
    private val getCategoriesUseCase: GetCategoriesUseCase = mock()
    private val addCategoryUseCase: AddCategoryUseCase = mock()
    private val deleteCategoryUseCase: DeleteCategoryUseCase = mock()
    private val seedCategoriesUseCase: SeedCategoriesUseCase = mock()
    private val getBudgetUseCase: GetBudgetUseCase = mock()
    private val setBudgetUseCase: SetBudgetUseCase = mock()
    private val deleteBudgetUseCase: DeleteBudgetUseCase = mock()
    private val getCategoryBudgetsUseCase: GetCategoryBudgetsUseCase = mock()
    private val setCategoryBudgetUseCase: SetCategoryBudgetUseCase = mock()
    private val deleteCategoryBudgetUseCase: DeleteCategoryBudgetUseCase = mock()

    private lateinit var viewModel: ExpenseViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // These are invoked while the ViewModel's properties initialise, so they must return a
        // real Flow before construction — stateIn() on a null upstream would throw.
        whenever(getCategoriesUseCase()).thenReturn(flowOf(emptyList<Category>()))
        whenever(getCategoryBudgetsUseCase()).thenReturn(flowOf(emptyList<CategoryBudget>()))
        whenever(getBudgetUseCase(any())).thenReturn(flowOf<Budget?>(null))
        whenever(getLoginStatusUseCase()).thenReturn(flowOf(false))
        whenever(getThemeModeUseCase()).thenReturn(flowOf(0))
        whenever(getFontIndexUseCase()).thenReturn(flowOf(0))

        whenever(settingsRepository.dailyReminderEnabled).thenReturn(flowOf(true))
        whenever(settingsRepository.dailyReminderTime).thenReturn(flowOf(20 to 0))
        whenever(settingsRepository.dailySummaryEnabled).thenReturn(flowOf(true))
        whenever(settingsRepository.dailySummaryTime).thenReturn(flowOf(21 to 0))
        whenever(settingsRepository.budgetAlertsEnabled).thenReturn(flowOf(true))
        whenever(settingsRepository.biometricEnabled).thenReturn(flowOf<Boolean?>(false))

        viewModel = ExpenseViewModel(
            getTransactionsUseCase = getTransactionsUseCase,
            addTransactionUseCase = addTransactionUseCase,
            updateTransactionUseCase = updateTransactionUseCase,
            deleteTransactionUseCase = deleteTransactionUseCase,
            getTotalSpentUseCase = getTotalSpentUseCase,
            getThemeModeUseCase = getThemeModeUseCase,
            setThemeModeUseCase = setThemeModeUseCase,
            getFontIndexUseCase = getFontIndexUseCase,
            setFontIndexUseCase = setFontIndexUseCase,
            getLoginStatusUseCase = getLoginStatusUseCase,
            setLoginStatusUseCase = setLoginStatusUseCase,
            clearLocalDataUseCase = clearLocalDataUseCase,
            syncUserDataUseCase = syncUserDataUseCase,
            settingsRepository = settingsRepository,
            getCategoriesUseCase = getCategoriesUseCase,
            addCategoryUseCase = addCategoryUseCase,
            deleteCategoryUseCase = deleteCategoryUseCase,
            seedCategoriesUseCase = seedCategoriesUseCase,
            getBudgetUseCase = getBudgetUseCase,
            setBudgetUseCase = setBudgetUseCase,
            deleteBudgetUseCase = deleteBudgetUseCase,
            getCategoryBudgetsUseCase = getCategoryBudgetsUseCase,
            setCategoryBudgetUseCase = setCategoryBudgetUseCase,
            deleteCategoryBudgetUseCase = deleteCategoryBudgetUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `seedCategoriesUseCase runs on init`() = runTest {
        advanceUntilIdle()
        verify(seedCategoriesUseCase).invoke()
    }

    @Test
    fun `setSkippedLogin should call setLoginStatusUseCase`() = runTest {
        viewModel.setSkippedLogin(true)
        advanceUntilIdle()
        verify(setLoginStatusUseCase).invoke(true)
    }

    @Test
    fun `addTransaction should delegate to AddTransactionUseCase`() = runTest {
        viewModel.addTransaction(
            title = "Coffee",
            amount = 350.0,
            category = "Food",
            date = 1_757_000_000_000L
        )
        advanceUntilIdle()

        verify(addTransactionUseCase).invoke(
            Transaction(
                title = "Coffee",
                amount = 350.0,
                category = "Food",
                date = 1_757_000_000_000L
            )
        )
    }

    @Test
    fun `updateTransaction should delegate to UpdateTransactionUseCase`() = runTest {
        val transaction = Transaction(7, "Pizza", 860.0, "Food", 1_757_000_000_000L)

        viewModel.updateTransaction(transaction)
        advanceUntilIdle()

        verify(updateTransactionUseCase).invoke(transaction)
    }

    @Test
    fun `deleteTransaction should delegate to DeleteTransactionUseCase`() = runTest {
        val transaction = Transaction(3, "Bus Fare", 25.0, "Transport", 1_757_000_000_000L)

        viewModel.deleteTransaction(transaction)
        advanceUntilIdle()

        verify(deleteTransactionUseCase).invoke(transaction)
    }

    @Test
    fun `setThemeMode should delegate to SetThemeModeUseCase`() = runTest {
        viewModel.setThemeMode(2)
        advanceUntilIdle()
        verify(setThemeModeUseCase).invoke(2)
    }

    @Test
    fun `setFontIndex should delegate to SetFontIndexUseCase`() = runTest {
        viewModel.setFontIndex(3)
        advanceUntilIdle()
        verify(setFontIndexUseCase).invoke(3)
    }

    @Test
    fun `addCategory should delegate to AddCategoryUseCase`() = runTest {
        viewModel.addCategory("Groceries")
        advanceUntilIdle()
        verify(addCategoryUseCase).invoke("Groceries")
    }

    @Test
    fun `deleteCategory should delegate to DeleteCategoryUseCase`() = runTest {
        val category = Category(id = 2, name = "Sports", isRemovable = true)

        viewModel.deleteCategory(category)
        advanceUntilIdle()

        verify(deleteCategoryUseCase).invoke(category)
    }

    @Test
    fun `setMonthlyBudget should delegate to SetBudgetUseCase for the current month`() = runTest {
        viewModel.setMonthlyBudget(25_000.0)
        advanceUntilIdle()
        // The month key is derived from the system clock, so match it loosely.
        verify(setBudgetUseCase).invoke(any(), eq(25_000.0))
    }

    @Test
    fun `setCategoryBudget should delegate to SetCategoryBudgetUseCase`() = runTest {
        viewModel.setCategoryBudget("Food", 8_000.0)
        advanceUntilIdle()
        verify(setCategoryBudgetUseCase).invoke("Food", 8_000.0)
    }

    @Test
    fun `deleteCategoryBudget should delegate to DeleteCategoryBudgetUseCase`() = runTest {
        viewModel.deleteCategoryBudget("Transport")
        advanceUntilIdle()
        verify(deleteCategoryBudgetUseCase).invoke("Transport")
    }

    @Test
    fun `logout should clear local data before invoking the callback`() = runTest {
        var completed = false

        viewModel.logout { completed = true }
        advanceUntilIdle()

        verify(clearLocalDataUseCase).invoke()
        assert(completed) { "logout callback was never invoked" }
    }

    @Test
    fun `syncData should delegate to SyncUserDataUseCase`() = runTest {
        viewModel.syncData()
        advanceUntilIdle()
        verify(syncUserDataUseCase).invoke()
    }
}
