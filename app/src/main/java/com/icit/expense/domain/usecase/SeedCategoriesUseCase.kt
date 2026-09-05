package com.icit.expense.domain.usecase

import com.icit.expense.domain.repository.CategoryRepository
import javax.inject.Inject

class SeedCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke() = repository.seedDefaults()
}
