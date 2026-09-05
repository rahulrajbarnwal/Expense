package com.icit.expense.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.*
import com.patrykandpatrick.vico.compose.cartesian.axis.*
import com.patrykandpatrick.vico.compose.cartesian.layer.*
import com.patrykandpatrick.vico.core.cartesian.data.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics", fontWeight = FontWeight.Bold) },
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
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Group Filters and Summary for tighter control
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = uiState.selectedFilter == filter,
                                onClick = { viewModel.setFilter(filter) },
                                label = { 
                                    Text(
                                        filter.name.lowercase().replace("_", " ").capitalizeWords(),
                                        style = MaterialTheme.typography.labelMedium
                                    ) 
                                }
                            )
                        }
                    }

                    // Summary Cards
                    AnalyticsSummaryGrid(uiState)
                }
            }

            // Monthly Distribution (Pie Chart)
            item {
                SectionTitle("Expense Distribution")
                if (uiState.categoryDistribution.isEmpty()) {
                    EmptyChartState()
                } else {
                    PieChartWithLegend(uiState.categoryDistribution)
                }
            }

            // Weekly Spending Trend (Bar Chart)
            item {
                SectionTitle("Spending Trend")
                if (uiState.weeklyTrend.isEmpty() && uiState.monthlyTrend.isEmpty()) {
                    EmptyChartState()
                } else {
                    val trendData = if (uiState.selectedFilter == TimeFilter.THIS_WEEK) uiState.weeklyTrend else uiState.monthlyTrend
                    TrendBarChart(trendData)
                }
            }
        }
    }
}

@Composable
fun AnalyticsSummaryGrid(state: AnalyticsUiState) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InsightCard(
                label = "Total Expense",
                value = formatCurrency(state.totalExpense),
                subValue = "Selected Period",
                modifier = Modifier.weight(1f)
            )
            InsightCard(
                label = "Highest",
                value = state.highestCategory,
                subValue = formatCurrency(state.highestCategoryAmount),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InsightCard(
                label = "Transactions",
                value = "${state.totalTransactions}",
                subValue = "Count",
                modifier = Modifier.weight(1f)
            )
            InsightCard(
                label = "Avg. Daily",
                value = formatCurrency(state.averageDailyExpense),
                subValue = "Calculated",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PieChartWithLegend(data: List<CategoryPieData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PieChart(
                modifier = Modifier.size(150.dp),
                data = data
            )
            Spacer(Modifier.width(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                data.forEach { item ->
                    LegendItem(item)
                }
            }
        }
    }
}

@Composable
fun PieChart(
    modifier: Modifier = Modifier,
    data: List<CategoryPieData>
) {
    val animateFloat = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animateFloat.animateTo(1f, tween(1000))
    }

    Canvas(modifier = modifier) {
        var startAngle = 270f
        data.forEach { item ->
            val sweepAngle = item.percentage * 360f * animateFloat.value
            drawArc(
                color = item.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 30.dp.toPx())
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun LegendItem(item: CategoryPieData) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(12.dp), shape = RoundedCornerShape(2.dp), color = item.color) {}
        Spacer(Modifier.width(8.dp))
        Column {
            Text(item.category, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("${(item.percentage * 100).toInt()}% (${formatCurrency(item.amount)})", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun TrendBarChart(data: List<DayValue>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    
    LaunchedEffect(data) {
        modelProducer.runTransaction {
            columnSeries {
                series(data.map { it.value })
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(250.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Box(Modifier.padding(16.dp)) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { value, _, _ -> 
                            data.getOrNull(value.toInt())?.day ?: ""
                        }
                    )
                ),
                modelProducer = modelProducer
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun EmptyChartState() {
    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
        Text("No data available for this period", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun String.capitalizeWords(): String = this.split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (it.isNotEmpty()) char.uppercase() else "" } }