package com.icit.expense.domain.usecase

import com.icit.expense.domain.model.Budget
import com.icit.expense.domain.repository.BudgetRepository
import javax.inject.Inject

class SetBudgetUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(monthYear: String, amount: Double) {
        repository.setMonthlyBudget(Budget(monthYear, amount))
    }
}
