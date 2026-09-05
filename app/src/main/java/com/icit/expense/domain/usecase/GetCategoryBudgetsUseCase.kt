package com.icit.expense.domain.usecase

import com.icit.expense.domain.model.CategoryBudget
import com.icit.expense.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoryBudgetsUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    operator fun invoke(): Flow<List<CategoryBudget>> = repository.getAllCategoryBudgets()
}
