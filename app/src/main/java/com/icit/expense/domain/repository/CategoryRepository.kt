package com.icit.expense.domain.repository

import com.icit.expense.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun addCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun seedDefaults()
    suspend fun syncFromFirestore()
    suspend fun clearAllCategories()
}
