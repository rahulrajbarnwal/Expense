package com.icit.expense.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.icit.expense.domain.model.Transaction
import com.icit.expense.ui.theme.ExpenseTheme
import com.icit.expense.core.font.AppFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ExpenseHomeScreen(
    viewModel: ExpenseViewModel,
    onAddExpenseClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onProfileClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onBudgetClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSecurityClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    val uiState by viewModel.dashboardUiState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val fontIndex by viewModel.fontIndex.collectAsStateWithLifecycle()

    var showSettings by remember { mutableStateOf(false) }
    val navigator = rememberListDetailPaneScaffoldNavigator<Transaction>()
    val coroutineScope = rememberCoroutineScope()
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    // Double Back Press to Exit Logic
    var backPressCount by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(backPressCount) {
        if (backPressCount > 0) {
            delay(2000)
            backPressCount = 0
        }
    }

    BackHandler {
        if (navigator.canNavigateBack()) {
            coroutineScope.launch {
                navigator.navigateBack()
            }
        } else {
            if (backPressCount >= 1) {
                (context as? android.app.Activity)?.finish()
            } else {
                backPressCount++
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ExpenseHomeContent(
        uiState = uiState,
        themeMode = themeMode,
        onAddExpenseClick = onAddExpenseClick,
        onHistoryClick = onHistoryClick,
        onSettingsClick = { showSettings = true },
        navigator = navigator,
        selectedTransaction = selectedTransaction,
        onTransactionClick = { transaction ->
            selectedTransaction = transaction
            coroutineScope.launch {
                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, transaction)
            }
        },
        onDeleteTransaction = { viewModel.deleteTransaction(it) },
        onEditTransaction = onEditTransaction,
        onProfileClick = onProfileClick,
        onBudgetClick = onBudgetClick,
        onAnalyticsClick = onAnalyticsClick,
        onNotificationsClick = onNotificationsClick,
        onSecurityClick = onSecurityClick,
        onBackDetail = {
            coroutineScope.launch {
                navigator.navigateBack()
            }
        }
    )

    if (showSettings) {
        val isUserLoggedIn = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
        SettingsBottomSheet(
            themeMode = themeMode,
            onThemeChange = { 
                viewModel.setThemeMode(it)
            },
            selectedFontIndex = fontIndex,
            onFontChange = {
                viewModel.setFontIndex(it)
            },
            isUserLoggedIn = isUserLoggedIn,
            onProfileClick = onProfileClick,
            onCategoryClick = onCategoryClick,
            onBudgetClick = onBudgetClick,
            onAnalyticsClick = onAnalyticsClick,
            onNotificationsClick = onNotificationsClick,
            onSecurityClick = onSecurityClick,
            onLoginClick = onLoginClick,
            onDismiss = { showSettings = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ExpenseHomeContent(
    uiState: DashboardUiState,
    themeMode: Int,
    onAddExpenseClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    navigator: androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator<Transaction>,
    selectedTransaction: Transaction?,
    onTransactionClick: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onProfileClick: () -> Unit,
    onBudgetClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSecurityClick: () -> Unit,
    onBackDetail: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Expense", 
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    if (navigator.canNavigateBack()) {
                        IconButton(onClick = onBackDetail) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Rounded.History, contentDescription = "View History")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!navigator.canNavigateBack()) {
                FloatingActionButton(
                    onClick = onAddExpenseClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Expense")
                }
            }
        }
    ) { innerPadding ->
        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            listPane = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        EnhancedSummaryCard(
                            todayExpense = uiState.todayExpense,
                            monthlyExpense = uiState.monthlyExpense,
                            remainingBudget = uiState.remainingBudget,
                            progress = uiState.budgetProgress
                        )
                    }

                    item {
                        FinancialInsightsSection(
                            highestCategory = uiState.highestCategory,
                            highestAmount = uiState.highestCategoryAmount,
                            todayCount = uiState.todayTransactionCount,
                            avgDaily = uiState.averageDailyExpense,
                            remainingDaily = uiState.remainingDailyBudget,
                            onAnalyticsClick = onAnalyticsClick
                        )
                    }
                    
                    if (uiState.topCategories.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Top Spending Categories",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = onBudgetClick) {
                                    Text("View All")
                                }
                            }
                        }
                        
                        items(uiState.topCategories) { categoryData ->
                            CategoryBudgetDashboardItem(
                                category = categoryData.categoryName,
                                spent = categoryData.spent,
                                budget = categoryData.budget
                            )
                        }
                    }
                    
                    item {
                        Text(
                            text = "Today's Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        
                        if (uiState.todayTransactions.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                Text("No transactions yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    
                    items(uiState.todayTransactions) { transaction ->
                        TransactionItemWithActions(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction) },
                            onDelete = { onDeleteTransaction(transaction) },
                            onEdit = { onEditTransaction(transaction) }
                        )
                    }
                }
            },
            detailPane = {
                selectedTransaction?.let { transaction ->
                    TransactionDetailPane(
                        transaction = transaction,
                        onDelete = {
                            onDeleteTransaction(transaction)
                            onBackDetail()
                        },
                        onEdit = {
                            onBackDetail()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    themeMode: Int,
    onThemeChange: (Int) -> Unit,
    selectedFontIndex: Int,
    onFontChange: (Int) -> Unit,
    isUserLoggedIn: Boolean,
    onProfileClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onBudgetClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSecurityClick: () -> Unit,
    onLoginClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var fontDropdownExpanded by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold
            )

            // Theme Selection
            ListItem(
                headlineContent = { Text("Dark Mode") },
                leadingContent = { Icon(Icons.Rounded.DarkMode, contentDescription = null) },
                trailingContent = {
                    val options = listOf("AUTO", "OFF", "ON")
                    CustomSelector(
                        selectedOption = themeMode,
                        options = options,
                        onOptionSelected = onThemeChange
                    )
                }
            )

            // Font Selection
            ListItem(
                headlineContent = { Text("Font Change") },
                leadingContent = { Icon(Icons.Rounded.TextFields, contentDescription = null) },
                trailingContent = {
                    ExposedDropdownMenuBox(
                        expanded = fontDropdownExpanded,
                        onExpandedChange = { fontDropdownExpanded = it },
                        modifier = Modifier.width(160.dp)
                    ) {
                        OutlinedTextField(
                            value = AppFont.entries[selectedFontIndex].label,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontDropdownExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
                            textStyle = TextStyle(fontSize = 12.sp),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = fontDropdownExpanded,
                            onDismissRequest = { fontDropdownExpanded = false }
                        ) {
                            AppFont.entries.forEachIndexed { index, font ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = font.label,
                                            style = TextStyle(
                                                fontFamily = font.fontFamily, 
                                                fontStyle = font.fontStyle,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 14.sp
                                            )
                                        ) 
                                    },
                                    onClick = {
                                        onFontChange(index)
                                        fontDropdownExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }
            )

            // Category Management
            ListItem(
                headlineContent = { Text("Categories") },
                leadingContent = { Icon(Icons.Rounded.Category, contentDescription = null) },
                modifier = Modifier.clickable {
                    onDismiss()
                    onCategoryClick()
                }
            )

            // Budget Planner
            ListItem(
                headlineContent = { Text("Budget Planner") },
                leadingContent = { Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null) },
                modifier = Modifier.clickable {
                    onDismiss()
                    onBudgetClick()
                }
            )

            // Analytics
            ListItem(
                headlineContent = { Text("Analytics") },
                leadingContent = { Icon(Icons.Rounded.Analytics, contentDescription = null) },
                modifier = Modifier.clickable {
                    onDismiss()
                    onAnalyticsClick()
                }
            )

            // Notifications
            ListItem(
                headlineContent = { Text("Notifications") },
                leadingContent = { Icon(Icons.Rounded.Notifications, contentDescription = null) },
                modifier = Modifier.clickable {
                    onDismiss()
                    onNotificationsClick()
                }
            )

            // Security
            ListItem(
                headlineContent = { Text("Security") },
                leadingContent = { Icon(Icons.Rounded.PrivacyTip, contentDescription = null) },
                modifier = Modifier.clickable {
                    onDismiss()
                    onSecurityClick()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (isUserLoggedIn) {
                // Profile
                ListItem(
                    headlineContent = { Text("Profile") },
                    leadingContent = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    modifier = Modifier.clickable {
                        onDismiss()
                        onProfileClick()
                    }
                )
            } else {
                // Login CTA
                ListItem(
                    headlineContent = { Text("Login") },
                    leadingContent = { Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null) },
                    modifier = Modifier.clickable {
                        onDismiss()
                        onLoginClick()
                    }
                )
            }

            ListItem(
                headlineContent = { Text("Privacy Policy") },
                leadingContent = { Icon(Icons.Rounded.PrivacyTip, contentDescription = null) },
                modifier = Modifier.clickable { /* Placeholder */ }
            )
        }
    }
}

@Composable
fun CustomSelector(
    selectedOption: Int,
    options: List<String>,
    onOptionSelected: (Int) -> Unit,
    width: androidx.compose.ui.unit.Dp = 140.dp
) {
    Surface(
        modifier = Modifier
            .width(width)
            .height(32.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, label ->
                val isSelected = selectedOption == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.secondaryContainer 
                            else Color.Transparent
                        )
                        .clickable { onOptionSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer 
                                else MaterialTheme.colorScheme.onSurface,
                        style = TextStyle(
                            fontSize = 9.sp, // Slightly smaller for more items
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                // Add vertical separator between items
                if (index < options.size - 1) {
                    VerticalDivider(
                        modifier = Modifier
                            .fillMaxHeight(0.6f)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}


@Composable
fun CategoryBudgetDashboardItem(
    category: String,
    spent: Double,
    budget: Double
) {
    val progress = if (budget > 0) (spent / budget).toFloat() else 0f
    val percentage = (progress * 100).toInt()
    
    val color = when {
        percentage >= 100 -> Color.Red
        percentage >= 80 -> Color(0xFFFFA500)
        else -> Color(0xFF4CAF50)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Category, 
                    contentDescription = null, 
                    tint = getCategoryColor(category),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = category, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "${formatCurrency(spent)} / ${if (budget > 0) formatCurrency(budget) else "No Limit"}",
                style = MaterialTheme.typography.labelMedium
            )
        }
        
        if (budget > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ExpenseHomePreview() {
    ExpenseTheme {
        ExpenseHomeContent(
            uiState = DashboardUiState(
                todayExpense = 18.0,
                monthlyExpense = 500.0,
                remainingBudget = 500.0,
                budgetProgress = 0.5f,
                todayTransactions = listOf(
                    Transaction(1, "Lunch", 15.50, "Food", System.currentTimeMillis()),
                    Transaction(2, "Bus Fare", 2.50, "Transport", System.currentTimeMillis())
                )
            ),
            themeMode = 0,
            onAddExpenseClick = {},
            onHistoryClick = {},
            onSettingsClick = {},
            navigator = rememberListDetailPaneScaffoldNavigator<Transaction>(),
            selectedTransaction = null,
            onTransactionClick = {},
            onDeleteTransaction = {},
            onEditTransaction = {},
            onProfileClick = {},
            onBudgetClick = {},
            onAnalyticsClick = {},
            onNotificationsClick = {},
            onSecurityClick = {},
            onBackDetail = {}
        )
    }
}
