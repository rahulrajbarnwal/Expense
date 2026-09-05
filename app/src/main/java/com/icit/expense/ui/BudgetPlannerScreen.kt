package com.icit.expense.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.icit.expense.domain.model.CategoryBudget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetPlannerScreen(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
    val categoryBudgets by viewModel.categoryBudgets.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val monthlySpent by viewModel.currentMonthTotalSpent.collectAsStateWithLifecycle()
    val spendingByCategory by viewModel.currentMonthSpendingByCategory.collectAsStateWithLifecycle()

    var showMonthlyDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var selectedCategoryForBudget by remember { mutableStateOf<String?>(null) }
    var budgetAmount by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Planner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Monthly Budget Card
            item {
                BudgetCard(
                    title = "Total Monthly Budget",
                    spent = monthlySpent ?: 0.0,
                    budget = monthlyBudget?.amount ?: 0.0,
                    onEdit = {
                        budgetAmount = (monthlyBudget?.amount ?: 0.0).toString()
                        showMonthlyDialog = true
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category Budgets",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = { 
                        selectedCategoryForBudget = null
                        budgetAmount = ""
                        showCategoryDialog = true 
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add")
                    }
                }
            }

            items(categoryBudgets) { budget ->
                val spent = spendingByCategory[budget.categoryName] ?: 0.0
                CategoryBudgetListItem(
                    budget = budget,
                    spent = spent,
                    onDelete = { viewModel.deleteCategoryBudget(budget.categoryName) }
                )
            }
        }

        if (showMonthlyDialog) {
            BudgetDialog(
                title = "Set Monthly Budget",
                amount = budgetAmount,
                onAmountChange = { budgetAmount = it },
                onConfirm = {
                    budgetAmount.toDoubleOrNull()?.let {
                        viewModel.setMonthlyBudget(it)
                    }
                    showMonthlyDialog = false
                },
                onDismiss = { showMonthlyDialog = false }
            )
        }

        if (showCategoryDialog) {
            CategoryBudgetDialog(
                categories = categories.map { it.name }.filter { name -> 
                    categoryBudgets.none { it.categoryName == name }
                },
                amount = budgetAmount,
                onAmountChange = { budgetAmount = it },
                onConfirm = { category ->
                    budgetAmount.toDoubleOrNull()?.let {
                        viewModel.setCategoryBudget(category, it)
                    }
                    showCategoryDialog = false
                },
                onDismiss = { showCategoryDialog = false }
            )
        }
    }
}

@Composable
fun BudgetCard(
    title: String,
    spent: Double,
    budget: Double,
    onEdit: () -> Unit
) {
    val progress = if (budget > 0) (spent / budget).toFloat() else 0f
    val percentage = (progress * 100).toInt()
    
    val color = when {
        percentage >= 100 -> Color.Red
        percentage >= 80 -> Color(0xFFFFA500) // Orange
        else -> Color(0xFF4CAF50) // Green
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(text = "Spent", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = formatCurrency(spent),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Budget", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = formatCurrency(budget),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "$percentage% used",
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
            
            if (spent > budget && budget > 0) {
                Text(
                    text = "Budget exceeded by ${formatCurrency(spent - budget)}!",
                    color = Color.Red,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryBudgetListItem(
    budget: CategoryBudget,
    spent: Double,
    onDelete: () -> Unit
) {
    val progress = if (budget.amount > 0) (spent / budget.amount).toFloat() else 0f
    val percentage = (progress * 100).toInt()
    
    val color = when {
        percentage >= 100 -> Color.Red
        percentage >= 80 -> Color(0xFFFFA500)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = budget.categoryName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${formatCurrency(spent)} / ${formatCurrency(budget.amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$percentage% used",
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
                if (spent > budget.amount && budget.amount > 0) {
                    Text(
                        text = "Exceeded!",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDialog(
    title: String,
    amount: String,
    onAmountChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBudgetDialog(
    categories: List<String>,
    amount: String,
    onAmountChange: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(if (categories.isNotEmpty()) categories[0] else "") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Category Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = { Text("Monthly Limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (selectedCategory.isNotEmpty()) onConfirm(selectedCategory) },
                enabled = selectedCategory.isNotEmpty()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
