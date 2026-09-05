package com.icit.expense.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.icit.expense.domain.model.Transaction
import com.icit.expense.ui.theme.ExpenseTheme

@Composable
fun AddExpenseScreen(
    viewModel: ExpenseViewModel,
    transaction: Transaction? = null,
    onBack: () -> Unit
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val categoryNames = categories.map { it.name }

    AddExpenseContent(
        transaction = transaction,
        categories = categoryNames,
        onSave = { title, amount, category, date ->
            if (transaction != null) {
                viewModel.updateTransaction(transaction.copy(
                    title = title,
                    amount = amount,
                    category = category,
                    date = date
                ))
            } else {
                viewModel.addTransaction(title, amount, category, date)
            }
            onBack()
        },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseContent(
    transaction: Transaction? = null,
    categories: List<String>,
    onSave: (String, Double, String, Long) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(transaction?.title ?: "") }
    var amount by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(transaction?.category ?: "") }
    var date by remember { mutableLongStateOf(transaction?.date ?: System.currentTimeMillis()) }
    var expanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Categories load asynchronously, so the list is empty on first composition.
    // Pre-select the first one as soon as it arrives, without overriding a user pick.
    LaunchedEffect(categories) {
        if (category.isBlank() && categories.isNotEmpty()) {
            category = categories.first()
        }
    }

    // Improved validation: Check if amount is a valid positive number
    val amountDouble = amount.toDoubleOrNull()
    val isAmountValid = amountDouble != null && amountDouble > 0
    val isFormValid = title.isNotBlank() && isAmountValid && category.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transaction != null) "Edit Expense" else "Add Expense") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            // Form scrolls so nothing is unreachable on short screens or with the keyboard up.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = title.isBlank() && title.isNotEmpty(),
                supportingText = {
                    if (title.isBlank() && title.isNotEmpty()) {
                        Text("Title cannot be empty")
                    }
                }
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { newValue ->
                    // Basic filtering: allow only digits and at most one decimal point
                    if (newValue.isEmpty() || newValue.matches(Regex("""^\d*\.?\d*$"""))) {
                        amount = newValue
                    }
                },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = amount.isNotEmpty() && !isAmountValid,
                supportingText = {
                    if (amount.isNotEmpty() && !isAmountValid) {
                        Text("Please enter a valid positive amount")
                    }
                }
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                category = selectionOption
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Date Picker Field
            OutlinedTextField(
                value = formatDate(date),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Rounded.DateRange, contentDescription = "Select Date")
                    }
                }
            )

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                date = it
                            }
                            showDatePicker = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            }

            // Pinned below the scrolling form so it is always reachable.
            Button(
                onClick = {
                    amountDouble?.let { onSave(title.trim(), it, category, date) }
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isFormValid,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    if (transaction != null) "Update Expense" else "Save Expense",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun AddExpensePreview() {
    ExpenseTheme {
        AddExpenseContent(
            categories = listOf("Food", "Transport", "Other"),
            onSave = { _, _, _, _ -> },
            onBack = {}
        )
    }
}
