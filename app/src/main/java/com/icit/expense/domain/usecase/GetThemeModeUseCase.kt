package com.icit.expense.domain.usecase

import com.icit.expense.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThemeModeUseCase @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<Int> {
        return repository.themeModeFlow
    }
}
