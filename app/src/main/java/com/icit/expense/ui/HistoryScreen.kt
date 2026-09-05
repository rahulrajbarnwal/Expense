package com.icit.expense.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.icit.expense.domain.model.Transaction
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HistoryScreen(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit,
    onEditTransaction: (Transaction) -> Unit
) {
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var filterByDate by remember { mutableStateOf(false) }

    val navigator = rememberListDetailPaneScaffoldNavigator<Transaction>()
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        if (navigator.canNavigateBack()) {
            coroutineScope.launch {
                navigator.navigateBack()
            }
        } else {
            onBack()
        }
    }

    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)

    val transactionsFlow = if (filterByDate) {
        val start = getStartOfDay(calendar)
        val end = getEndOfDay(calendar)
        viewModel.getTransactionsForRange(start, end)
    } else {
        viewModel.getTransactionsForMonth(year, month)
    }

    val totalSpentFlow = if (filterByDate) {
        val start = getStartOfDay(calendar)
        val end = getEndOfDay(calendar)
        viewModel.getTotalSpentForRange(start, end)
    } else {
        viewModel.getTotalSpentForMonth(year, month)
    }

    val transactions by transactionsFlow.collectAsState(initial = emptyList())
    val totalSpent by totalSpentFlow.collectAsState(initial = 0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historical Explorer") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (navigator.canNavigateBack()) {
                            coroutineScope.launch {
                                navigator.navigateBack()
                            }
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = "Pick Date")
                    }
                }
            )
        }
    ) { innerPadding ->
        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            listPane = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (!filterByDate) {
                        MonthSelector(
                            calendar = calendar,
                            onPreviousMonth = {
                                val newCal = calendar.clone() as Calendar
                                newCal.add(Calendar.MONTH, -1)
                                calendar = newCal
                            },
                            onNextMonth = {
                                val newCal = calendar.clone() as Calendar
                                newCal.add(Calendar.MONTH, 1)
                                calendar = newCal
                            }
                        )
                    } else {
                        DateSelector(
                            calendar = calendar,
                            onClearFilter = { filterByDate = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    //TotalSpentCard(totalSpent ?: 0.0)
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (filterByDate) "Transactions on ${formatDate(calendar.timeInMillis)}" 
                               else "Transactions in ${getMonthName(month)} $year",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (transactions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No transactions found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(transactions) { transaction ->
                                TransactionItemWithActions(
                                    transaction = transaction,
                                    onClick = {
                                        selectedTransaction = transaction
                                        coroutineScope.launch {
                                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, transaction)
                                        }
                                    },
                                    onDelete = { viewModel.deleteTransaction(transaction) },
                                    onEdit = { onEditTransaction(transaction) }
                                )
                            }
                        }
                    }
                }
            },
            detailPane = {
                selectedTransaction?.let { transaction ->
                    TransactionDetailPane(
                        transaction = transaction,
                        onDelete = {
                            viewModel.deleteTransaction(transaction)
                            coroutineScope.launch {
                                navigator.navigateBack()
                            }
                        },
                        onEdit = {
                            coroutineScope.launch {
                                navigator.navigateBack()
                            }
                            onEditTransaction(transaction)
                        }
                    )
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a transaction to see details")
                }
            },
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = calendar.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val newCal = Calendar.getInstance()
                        newCal.timeInMillis = it
                        calendar = newCal
                        filterByDate = true
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

@Composable
fun MonthSelector(
    calendar: Calendar,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous Month")
        }
        
        Text(
            text = "${getMonthName(calendar.get(Calendar.MONTH))} ${calendar.get(Calendar.YEAR)}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        IconButton(onClick = onNextMonth) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Next Month")
        }
    }
}

fun getMonthName(month: Int): String {
    val cal = Calendar.getInstance()
    cal.set(Calendar.MONTH, month)
    return java.text.SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
}

@Composable
fun DateSelector(calendar: Calendar, onClearFilter: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Date: ${formatDate(calendar.timeInMillis)}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onClearFilter) {
            Text("Clear Filter")
        }
    }
}

private fun getStartOfDay(calendar: Calendar): Long {
    val cal = calendar.clone() as Calendar
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun getEndOfDay(calendar: Calendar): Long {
    val cal = calendar.clone() as Calendar
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}
