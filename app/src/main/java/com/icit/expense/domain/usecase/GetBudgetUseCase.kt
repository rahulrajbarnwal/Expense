package com.icit.expense.domain.usecase

import com.icit.expense.domain.model.Budget
import com.icit.expense.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBudgetUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    operator fun invoke(monthYear: String): Flow<Budget?> = repository.getMonthlyBudget(monthYear)
}
