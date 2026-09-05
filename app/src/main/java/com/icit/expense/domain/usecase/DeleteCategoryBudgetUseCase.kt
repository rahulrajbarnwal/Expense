package com.icit.expense.domain.usecase

import com.icit.expense.domain.repository.BudgetRepository
import javax.inject.Inject

class DeleteCategoryBudgetUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(categoryName: String) = repository.deleteCategoryBudget(categoryName)
}
