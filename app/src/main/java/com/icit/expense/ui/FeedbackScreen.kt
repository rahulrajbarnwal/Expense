package com.icit.expense.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.icit.expense.domain.model.Feedback
import com.icit.expense.domain.model.FeedbackCategory
import com.icit.expense.domain.model.FeedbackLimits
import com.icit.expense.domain.model.FeedbackStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    viewModel: FeedbackViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val text = when (event) {
                is FeedbackEvent.Sent -> "Message delivered · Ref ${event.messageId.takeLast(8)}"
                is FeedbackEvent.Queued -> event.reason
                is FeedbackEvent.Failed -> "Couldn't send: ${event.reason}"
                FeedbackEvent.DraftSaved -> "Draft saved"
            }
            snackbarHostState.showSnackbar(text)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Write to CEO", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::saveDraftNow,
                        enabled = state.subject.isNotBlank() || state.message.isNotBlank()
                    ) {
                        Text("Save draft")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Your message goes straight to the leadership team. Every one of them is read.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                CategoryPicker(
                    selected = state.category,
                    onSelect = viewModel::onCategoryChange
                )
            }

            item {
                OutlinedTextField(
                    value = state.subject,
                    onValueChange = viewModel::onSubjectChange,
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = state.subjectError != null,
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(state.subjectError ?: "")
                            Text("${state.subject.length}/${FeedbackLimits.SUBJECT_MAX}")
                        }
                    }
                )
            }

            item {
                OutlinedTextField(
                    value = state.message,
                    onValueChange = viewModel::onMessageChange,
                    label = { Text("Message") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                    isError = state.messageError != null,
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(state.messageError ?: "")
                            Text("${state.message.length}/${FeedbackLimits.MESSAGE_MAX}")
                        }
                    }
                )
            }

            item {
                Button(
                    onClick = viewModel::submit,
                    enabled = state.canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send message", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (history.isNotEmpty()) {
                item {
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
                        Text(
                            "Previous messages",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(history, key = { it.id }) { feedback ->
                    FeedbackHistoryItem(
                        feedback = feedback,
                        onRetry = viewModel::retryUnsent,
                        onDelete = { viewModel.deleteFeedback(feedback.id) }
                    )
                }
            } else if (!state.isLoadingDraft) {
                item { FeedbackEmptyState() }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(
    selected: FeedbackCategory,
    onSelect: (FeedbackCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = { },
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            FeedbackCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.label) },
                    onClick = {
                        onSelect(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FeedbackHistoryItem(
    feedback: Feedback,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    feedback.subject,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(feedback.status)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "${feedback.category.label} · ${formatFeedbackDate(feedback.updatedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                feedback.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )

            if (feedback.status != FeedbackStatus.SENT && feedback.lastError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    feedback.lastError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (feedback.status != FeedbackStatus.SENT) {
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete message",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: FeedbackStatus) {
    val (label, container, content) = when (status) {
        FeedbackStatus.SENT -> Triple(
            "Delivered",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        FeedbackStatus.PENDING -> Triple(
            "Queued",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        FeedbackStatus.FAILED -> Triple(
            "Failed",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        FeedbackStatus.DRAFT -> Triple(
            "Draft",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = container,
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun FeedbackEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.MarkEmailRead,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "No messages yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatFeedbackDate(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
