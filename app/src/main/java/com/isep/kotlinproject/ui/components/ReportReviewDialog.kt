package com.isep.kotlinproject.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.isep.kotlinproject.R
import com.isep.kotlinproject.model.ReportReason

/**
 * Dialog for reporting a review.
 */
@Composable
fun ReportReviewDialog(
    onDismiss: () -> Unit,
    onReport: (ReportReason, String) -> Unit,
    isLoading: Boolean = false
) {
    var selectedReason by remember { mutableStateOf<ReportReason?>(null) }
    var additionalInfo by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(stringResource(R.string.report_review)) },
        text = {
            Column(
                modifier = Modifier.selectableGroup()
            ) {
                Text(
                    text = stringResource(R.string.report_reason_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ReportReason.values().forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reason.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = additionalInfo,
                    onValueChange = { additionalInfo = it },
                    label = { Text(stringResource(R.string.additional_info)) },
                    placeholder = { Text(stringResource(R.string.additional_info_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    selectedReason?.let { reason ->
                        onReport(reason, additionalInfo)
                    }
                },
                enabled = selectedReason != null && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.submit))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
