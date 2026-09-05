package com.icit.expense.domain.usecase

import com.icit.expense.domain.model.Category
import com.icit.expense.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    operator fun invoke(): Flow<List<Category>> = repository.getAllCategories()
}
