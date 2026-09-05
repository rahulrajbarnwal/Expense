package com.icit.expense.data.mapper

import com.icit.expense.data.local.entity.CategoryEntity
import com.icit.expense.domain.model.Category

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    isRemovable = isRemovable
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    isRemovable = isRemovable
)
