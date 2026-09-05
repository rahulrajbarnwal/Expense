package com.icit.expense.domain.usecase

import com.icit.expense.domain.model.CategoryBudget
import com.icit.expense.domain.repository.BudgetRepository
import javax.inject.Inject

class SetCategoryBudgetUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(categoryName: String, amount: Double) {
        repository.setCategoryBudget(CategoryBudget(categoryName, amount))
    }
}
