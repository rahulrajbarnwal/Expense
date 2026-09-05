package com.icit.expense.domain.usecase

import com.icit.expense.domain.repository.SettingsRepository
import javax.inject.Inject

class SetLoginStatusUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(skipped: Boolean) {
        repository.setHasSkippedLogin(skipped)
    }
}
