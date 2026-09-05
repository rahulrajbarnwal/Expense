package com.icit.expense.data.local.dao

import androidx.room.*
import com.icit.expense.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Transaction
    suspend fun seedDefaultCategories() {
        if (getCategoryCount() == 0) {
            val defaults = listOf("Food", "Transport", "Shopping", "Health", "Sports", "Entertainment")
            defaults.forEach { insertCategory(CategoryEntity(name = it)) }
            insertCategory(CategoryEntity(name = "Other", isRemovable = false))
        }
    }
}
