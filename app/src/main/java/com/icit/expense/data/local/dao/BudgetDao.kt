package com.icit.expense.data.local.dao

import androidx.room.*
import com.icit.expense.data.local.entity.BudgetEntity
import com.icit.expense.data.local.entity.CategoryBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE monthYear = :monthYear")
    fun getMonthlyBudget(monthYear: String): Flow<BudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyBudget(budget: BudgetEntity)

    @Query("DELETE FROM monthly_budgets WHERE monthYear = :monthYear")
    suspend fun deleteMonthlyBudget(monthYear: String)

    @Query("SELECT * FROM category_budgets")
    fun getAllCategoryBudgets(): Flow<List<CategoryBudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryBudget(budget: CategoryBudgetEntity)

    @Query("DELETE FROM category_budgets WHERE categoryName = :categoryName")
    suspend fun deleteCategoryBudget(categoryName: String)
    
    @Query("DELETE FROM monthly_budgets")
    suspend fun deleteAllMonthlyBudgets()

    @Query("DELETE FROM category_budgets")
    suspend fun deleteAllCategoryBudgets()
}
