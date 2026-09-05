package com.icit.expense.domain.repository

import com.icit.expense.domain.model.Budget
import com.icit.expense.domain.model.CategoryBudget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getMonthlyBudget(monthYear: String): Flow<Budget?>
    suspend fun setMonthlyBudget(budget: Budget)
    suspend fun deleteMonthlyBudget(monthYear: String)
    
    fun getAllCategoryBudgets(): Flow<List<CategoryBudget>>
    suspend fun setCategoryBudget(budget: CategoryBudget)
    suspend fun deleteCategoryBudget(categoryName: String)
    
    suspend fun syncFromFirestore()
    suspend fun clearLocalData()
}
