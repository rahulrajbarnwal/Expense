package com.icit.expense.domain.usecase

import com.icit.expense.domain.repository.SettingsRepository
import javax.inject.Inject

class SetFontIndexUseCase @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke(index: Int) = repository.setFontIndex(index)
}
