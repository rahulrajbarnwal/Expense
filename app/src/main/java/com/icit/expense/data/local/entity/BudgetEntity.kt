package com.icit.expense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_budgets")
data class BudgetEntity(
    @PrimaryKey val monthYear: String, // Format: "MM-YYYY"
    val amount: Double
)

@Entity(tableName = "category_budgets")
data class CategoryBudgetEntity(
    @PrimaryKey val categoryName: String,
    val amount: Double
)
