package com.icit.expense.domain.usecase

import com.icit.expense.domain.model.Category
import com.icit.expense.domain.repository.CategoryRepository
import javax.inject.Inject

class AddCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(name: String) {
        repository.addCategory(Category(name = name))
    }
}
