package com.isep.kotlinproject.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.isep.kotlinproject.R
import com.isep.kotlinproject.model.BiasIndicator
import com.isep.kotlinproject.model.BiasSeverity
import com.isep.kotlinproject.model.BiasType

/**
 * Compact bias warning chip for display on reviews or games
 */
@Composable
fun BiasWarningChip(
    indicator: BiasIndicator,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(indicator.severity.color).copy(alpha = 0.15f),
        onClick = { showDetails = true }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (indicator.severity) {
                    BiasSeverity.INFO -> Icons.Default.Info
                    BiasSeverity.WARNING -> Icons.Default.Warning
                    BiasSeverity.HIGH -> Icons.Default.ReportProblem
                },
                contentDescription = null,
                tint = Color(indicator.severity.color),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = indicator.message,
                style = MaterialTheme.typography.labelSmall,
                color = Color(indicator.severity.color)
            )
        }
    }
    
    if (showDetails) {
        BiasDetailsDialog(
            indicator = indicator,
            onDismiss = { showDetails = false }
        )
    }
}

/**
 * Multiple bias warnings in a row
 */
@Composable
fun BiasWarningsRow(
    indicators: List<BiasIndicator>,
    modifier: Modifier = Modifier
) {
    if (indicators.isEmpty()) return
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        indicators.take(3).forEach { indicator ->
            BiasWarningChip(indicator = indicator)
        }
        
        if (indicators.size > 3) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "+${indicators.size - 3}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Full bias card for prominent display (e.g., on game detail page)
 */
@Composable
fun BiasWarningCard(
    indicators: List<BiasIndicator>,
    modifier: Modifier = Modifier
) {
    if (indicators.isEmpty()) return
    
    val mostSevere = indicators.maxByOrNull { 
        when (it.severity) {
            BiasSeverity.HIGH -> 2
            BiasSeverity.WARNING -> 1
            BiasSeverity.INFO -> 0
        }
    } ?: return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(mostSevere.severity.color).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = when (mostSevere.severity) {
                    BiasSeverity.INFO -> Icons.Default.Info
                    BiasSeverity.WARNING -> Icons.Default.Warning
                    BiasSeverity.HIGH -> Icons.Default.ReportProblem
                },
                contentDescription = null,
                tint = Color(mostSevere.severity.color),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.rating_notice),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(mostSevere.severity.color)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                indicators.forEach { indicator ->
                    Text(
                        text = "• ${indicator.details}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Dialog showing detailed bias information
 */
@Composable
private fun BiasDetailsDialog(
    indicator: BiasIndicator,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = when (indicator.severity) {
                    BiasSeverity.INFO -> Icons.Default.Info
                    BiasSeverity.WARNING -> Icons.Default.Warning
                    BiasSeverity.HIGH -> Icons.Default.ReportProblem
                },
                contentDescription = null,
                tint = Color(indicator.severity.color)
            )
        },
        title = { Text(indicator.type.displayName) },
        text = {
            Column {
                Text(
                    text = indicator.details,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.bias_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}
