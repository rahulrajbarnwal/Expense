package com.icit.expense.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.icit.expense.domain.model.Transaction
import com.icit.expense.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItemWithActions(
    transaction: Transaction,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    false
                }
                else -> false
            }
        }
    )

    // Reset dismiss state if transaction changes or after a non-dismissing action
    LaunchedEffect(transaction) {
        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF64B5F6) // Blue for Edit
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFE57373) // Red for Delete
                    else -> Color.Transparent
                }, label = "background"
            )
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Rounded.Edit
                SwipeToDismissBoxValue.EndToStart -> Icons.Rounded.Delete
                else -> null
            }
            val scale by animateFloatAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f, label = "scale"
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color, MaterialTheme.shapes.medium)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                icon?.let {
                    Icon(
                        it,
                        contentDescription = null,
                        modifier = Modifier.scale(scale),
                        tint = Color.White
                    )
                }
            }
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(getCategoryColor(transaction.category), MaterialTheme.shapes.small)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = transaction.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = transaction.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatCurrency(transaction.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formatDateTime(transaction.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun TransactionDetailPane(
    transaction: Transaction,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transaction Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("Title", transaction.title)
                DetailRow("Amount", formatCurrency(transaction.amount))
                DetailRow("Category", transaction.category)
                DetailRow("Date", formatDateTime(transaction.date))
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun EnhancedSummaryCard(
    todayExpense: Double,
    monthlyExpense: Double,
    remainingBudget: Double,
    progress: Float
) {
    val percentage = (progress * 100).toInt()
    val hasBudget = progress > 0 || remainingBudget > 0
    
    val progressColor = when {
        percentage >= 100 -> Color.Red
        percentage >= 80 -> Color(0xFFFFA500)
        else -> Color.White
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Today's Expense",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = formatCurrency(todayExpense),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black
                    )
                }
                
                if (hasBudget) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Remaining Budget",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = formatCurrency(remainingBudget),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "This Month: ${formatCurrency(monthlyExpense)}",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (hasBudget) {
                        Text(
                            text = "$percentage%",
                            color = progressColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "No Budget Configured",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                
                if (hasBudget) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = progressColor,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun FinancialInsightsSection(
    highestCategory: String,
    highestAmount: Double,
    todayCount: Int,
    avgDaily: Double,
    remainingDaily: Double,
    onAnalyticsClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Financial Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            IconButton(onClick = onAnalyticsClick) {
                Icon(
                    imageVector = Icons.Rounded.Analytics,
                    contentDescription = "Analytics",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InsightCard(
                label = "Highest Category",
                value = highestCategory,
                subValue = formatCurrency(highestAmount),
                modifier = Modifier.weight(1f)
            )
            InsightCard(
                label = "Today's Count",
                value = "$todayCount",
                subValue = "Transactions",
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InsightCard(
                label = "Avg. Daily",
                value = formatCurrency(avgDaily),
                subValue = "This Month",
                modifier = Modifier.weight(1f)
            )
            InsightCard(
                label = "Daily Budget",
                value = if (remainingDaily > 0) formatCurrency(remainingDaily) else "--",
                subValue = "Remaining",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun InsightCard(
    label: String,
    value: String,
    subValue: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text = subValue, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(amount)
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "food" -> ColorFood
        "transport" -> ColorTransport
        "health" -> ColorHealth
        "sports" -> ColorSport
        "entertainment" -> ColorEntertainment
        "shopping" -> ColorShopping
        else -> ColorOther
    }
}
