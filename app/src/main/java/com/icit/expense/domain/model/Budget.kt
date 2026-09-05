package com.icit.expense.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Budget(
    val monthYear: String,
    val amount: Double = 0.0
)

@Serializable
data class CategoryBudget(
    val categoryName: String,
    val amount: Double = 0.0
)
