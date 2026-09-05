package com.icit.expense.domain.usecase

import com.icit.expense.domain.repository.BudgetRepository
import javax.inject.Inject

class DeleteBudgetUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(monthYear: String) = repository.deleteMonthlyBudget(monthYear)
}
