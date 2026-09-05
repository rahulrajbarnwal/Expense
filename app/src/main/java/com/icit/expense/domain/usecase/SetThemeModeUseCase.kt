package com.icit.expense.domain.usecase

import com.icit.expense.domain.repository.SettingsRepository
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke(themeMode: Int) {
        repository.setThemeMode(themeMode)
    }
}
