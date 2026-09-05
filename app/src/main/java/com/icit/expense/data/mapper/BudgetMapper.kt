package com.icit.expense.data.mapper

import com.icit.expense.data.local.entity.BudgetEntity
import com.icit.expense.data.local.entity.CategoryBudgetEntity
import com.icit.expense.domain.model.Budget
import com.icit.expense.domain.model.CategoryBudget

fun BudgetEntity.toDomain() = Budget(
    monthYear = monthYear,
    amount = amount
)

fun Budget.toEntity() = BudgetEntity(
    monthYear = monthYear,
    amount = amount
)

fun CategoryBudgetEntity.toDomain() = CategoryBudget(
    categoryName = categoryName,
    amount = amount
)

fun CategoryBudget.toEntity() = CategoryBudgetEntity(
    categoryName = categoryName,
    amount = amount
)
