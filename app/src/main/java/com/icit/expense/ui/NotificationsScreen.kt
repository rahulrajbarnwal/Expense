package com.icit.expense.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.icit.expense.notification.NotificationScheduler
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    val dailyReminderEnabled by viewModel.dailyReminderEnabled.collectAsStateWithLifecycle()
    val dailyReminderTime by viewModel.dailyReminderTime.collectAsStateWithLifecycle()
    val dailySummaryEnabled by viewModel.dailySummaryEnabled.collectAsStateWithLifecycle()
    val dailySummaryTime by viewModel.dailySummaryTime.collectAsStateWithLifecycle()
    val budgetAlertsEnabled by viewModel.budgetAlertsEnabled.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showReminderTimePicker by remember { mutableStateOf(false) }
    var showSummaryTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Daily Expense Reminder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                NotificationSettingItem(
                    title = "Daily Reminder",
                    subtitle = "Remind me to add today's expenses\nEvery day at ${formatTime(dailyReminderTime.first, dailyReminderTime.second)}",
                    enabled = dailyReminderEnabled,
                    onEnabledChange = {
                        viewModel.setDailyReminderEnabled(it)
                        scope.launch {
                            NotificationScheduler.scheduleDailyNotifications(context, viewModel.settingsRepository)
                        }
                    },
                    onTimeClick = { if (dailyReminderEnabled) showReminderTimePicker = true }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                Text(
                    "Daily Spending Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                NotificationSettingItem(
                    title = "Daily Summary",
                    subtitle = "Receive today's spending summary\nEvery day at ${formatTime(dailySummaryTime.first, dailySummaryTime.second)}",
                    enabled = dailySummaryEnabled,
                    onEnabledChange = {
                        viewModel.setDailySummaryEnabled(it)
                        scope.launch {
                            NotificationScheduler.scheduleDailyNotifications(context, viewModel.settingsRepository)
                        }
                    },
                    onTimeClick = { if (dailySummaryEnabled) showSummaryTimePicker = true }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                Text(
                    "Budget Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                NotificationSettingItem(
                    title = "Budget Alerts",
                    subtitle = "Notify me when monthly budget reaches 80%",
                    enabled = budgetAlertsEnabled,
                    onEnabledChange = { viewModel.setBudgetAlertsEnabled(it) },
                    onTimeClick = null // No time for budget alerts
                )
            }
        }

        if (showReminderTimePicker) {
            TimePickerDialog(
                initialHour = dailyReminderTime.first,
                initialMinute = dailyReminderTime.second,
                onConfirm = { h, m ->
                    viewModel.setDailyReminderTime(h, m)
                    scope.launch {
                        NotificationScheduler.scheduleDailyNotifications(context, viewModel.settingsRepository)
                    }
                    showReminderTimePicker = false
                },
                onDismiss = { showReminderTimePicker = false }
            )
        }

        if (showSummaryTimePicker) {
            TimePickerDialog(
                initialHour = dailySummaryTime.first,
                initialMinute = dailySummaryTime.second,
                onConfirm = { h, m ->
                    viewModel.setDailySummaryTime(h, m)
                    scope.launch {
                        NotificationScheduler.scheduleDailyNotifications(context, viewModel.settingsRepository)
                    }
                    showSummaryTimePicker = false
                },
                onDismiss = { showSummaryTimePicker = false }
            )
        }
    }
}

@Composable
fun NotificationSettingItem(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onTimeClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && onTimeClick != null) { onTimeClick?.invoke() },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle, 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    lineHeight = 20.sp
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        }
    )
}

fun formatTime(hour: Int, minute: Int): String {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, minute)
    return java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
}
