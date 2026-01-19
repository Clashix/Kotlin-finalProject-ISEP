package com.isep.kotlinproject.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.isep.kotlinproject.R
import java.time.format.DateTimeFormatter

/**
 * A standardized Text Input with built-in support for Error handling and Help Tooltips.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    helpText: String? = null,
    errorMessage: String? = null,
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    var showHelpDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage != null,
            singleLine = singleLine,
            minLines = minLines,
            leadingIcon = if (leadingIcon != null) {
                { Icon(leadingIcon, contentDescription = null) }
            } else null,
            trailingIcon = {
                if (errorMessage != null) {
                    Icon(Icons.Default.Error, contentDescription = stringResource(R.string.error), tint = MaterialTheme.colorScheme.error)
                } else if (helpText != null) {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            Icons.Default.HelpOutline, 
                            contentDescription = stringResource(R.string.help),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            supportingText = {
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    if (showHelpDialog && helpText != null) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(stringResource(R.string.information)) },
            text = { Text(helpText) },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(stringResource(R.string.got_it))
                }
            }
        )
    }
}

/**
 * A Read-Only Text Field that opens a Date Picker Dialog on click.
 * Ensures strict date formatting (YYYY-MM-DD).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerField(
    value: String,
    onDateSelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    helpText: String? = null,
    errorMessage: String? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    
    // Date Picker State
    val datePickerState = rememberDatePickerState()

    // Handle Date Selection
    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { millis ->
            val localDate = java.time.Instant.ofEpochMilli(millis)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            val formatted = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE) // YYYY-MM-DD
            onDateSelected(formatted)
            showDatePicker = false
        }
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {}, // Read Only
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            isError = errorMessage != null,
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.select_date)) },
            trailingIcon = {
                 if (helpText != null) {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            Icons.Default.HelpOutline, 
                            contentDescription = stringResource(R.string.help),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
             supportingText = {
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        
        // Transparent overlay to capture clicks
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDatePicker = true }
        )
    }
    
     if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                 TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

     if (showHelpDialog && helpText != null) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(stringResource(R.string.date_info)) },
            text = { Text(helpText) },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

/**
 * A specialized Text Field for Passwords with visibility toggle.
 */
@Composable
fun AppPasswordInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = stringResource(R.string.password),
    modifier: Modifier = Modifier,
    helpText: String? = null,
    errorMessage: String? = null
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = errorMessage != null,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    val icon = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    Icon(icon, contentDescription = stringResource(R.string.toggle_password_visibility))
                }
            },
            supportingText = {
                Column {
                    if (errorMessage != null) {
                        Text(errorMessage, color = MaterialTheme.colorScheme.error)
                    }
                    if (helpText != null && errorMessage == null) {
                         Text(helpText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )
    }
}