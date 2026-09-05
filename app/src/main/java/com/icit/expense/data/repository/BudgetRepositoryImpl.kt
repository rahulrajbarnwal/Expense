package com.icit.expense.data.repository

import com.icit.expense.data.local.dao.BudgetDao
import com.icit.expense.data.mapper.toDomain
import com.icit.expense.data.mapper.toEntity
import com.icit.expense.domain.model.Budget
import com.icit.expense.domain.model.CategoryBudget
import com.icit.expense.domain.repository.BudgetRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : BudgetRepository {

    private val userDoc get() = auth.currentUser?.uid?.let { uid ->
        firestore.collection("users").document(uid)
    }

    override fun getMonthlyBudget(monthYear: String): Flow<Budget?> {
        return budgetDao.getMonthlyBudget(monthYear).map { it?.toDomain() }
    }

    override suspend fun setMonthlyBudget(budget: Budget) {
        budgetDao.insertMonthlyBudget(budget.toEntity())
        userDoc?.collection("monthly_budgets")?.document(budget.monthYear)?.set(budget)?.await()
    }

    override suspend fun deleteMonthlyBudget(monthYear: String) {
        budgetDao.deleteMonthlyBudget(monthYear)
        userDoc?.collection("monthly_budgets")?.document(monthYear)?.delete()?.await()
    }

    override fun getAllCategoryBudgets(): Flow<List<CategoryBudget>> {
        return budgetDao.getAllCategoryBudgets().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun setCategoryBudget(budget: CategoryBudget) {
        budgetDao.insertCategoryBudget(budget.toEntity())
        userDoc?.collection("category_budgets")?.document(budget.categoryName)?.set(budget)?.await()
    }

    override suspend fun deleteCategoryBudget(categoryName: String) {
        budgetDao.deleteCategoryBudget(categoryName)
        userDoc?.collection("category_budgets")?.document(categoryName)?.delete()?.await()
    }

    override suspend fun syncFromFirestore() {
        userDoc?.let { doc ->
            try {
                // Sync Monthly Budgets
                val monthlySnapshot = doc.collection("monthly_budgets").get().await()
                monthlySnapshot.toObjects(Budget::class.java).forEach {
                    budgetDao.insertMonthlyBudget(it.toEntity())
                }
                
                // Sync Category Budgets
                val categorySnapshot = doc.collection("category_budgets").get().await()
                categorySnapshot.toObjects(CategoryBudget::class.java).forEach {
                    budgetDao.insertCategoryBudget(it.toEntity())
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override suspend fun clearLocalData() {
        budgetDao.deleteAllMonthlyBudgets()
        budgetDao.deleteAllCategoryBudgets()
    }
}
