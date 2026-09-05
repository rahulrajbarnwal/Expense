package com.icit.expense.data.repository

import com.icit.expense.data.local.dao.CategoryDao
import com.icit.expense.data.mapper.toDomain
import com.icit.expense.data.mapper.toEntity
import com.icit.expense.domain.model.Category
import com.icit.expense.domain.repository.CategoryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CategoryRepository {

    private val userCollection get() = auth.currentUser?.uid?.let { uid ->
        firestore.collection("users").document(uid).collection("categories")
    }

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addCategory(category: Category) {
        categoryDao.insertCategory(category.toEntity())
        
        userCollection?.let { collection ->
            try {
                collection.document(category.name).set(category).await()
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category.toEntity())
        
        userCollection?.let { collection ->
            try {
                collection.document(category.name).delete().await()
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    override suspend fun seedDefaults() {
        categoryDao.seedDefaultCategories()
    }

    override suspend fun syncFromFirestore() {
        userCollection?.let { collection ->
            try {
                val snapshot = collection.get().await()
                val categories = snapshot.toObjects(Category::class.java)
                categories.forEach { category ->
                    categoryDao.insertCategory(category.toEntity())
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    override suspend fun clearAllCategories() {
        // We don't have a specific deleteAllCategories in CategoryDao yet, 
        // but for now, we just clear and re-seed defaults on next launch
    }
}
