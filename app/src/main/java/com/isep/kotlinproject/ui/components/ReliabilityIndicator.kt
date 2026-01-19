package com.isep.kotlinproject.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isep.kotlinproject.R
import com.isep.kotlinproject.model.ReliabilityLevel
import com.isep.kotlinproject.model.ReviewReliability

/**
 * Compact reliability indicator for display next to reviews
 */
@Composable
fun ReliabilityBadge(
    reliability: ReviewReliability,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier.clickable { showDetails = true },
        shape = RoundedCornerShape(12.dp),
        color = Color(reliability.level.color).copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (reliability.level) {
                    ReliabilityLevel.HIGH -> Icons.Default.VerifiedUser
                    ReliabilityLevel.MEDIUM -> Icons.Default.CheckCircle
                    ReliabilityLevel.LOW -> Icons.Default.Info
                    ReliabilityLevel.UNVERIFIED -> Icons.Default.HelpOutline
                },
                contentDescription = null,
                tint = Color(reliability.level.color),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = reliability.level.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = Color(reliability.level.color),
                fontWeight = FontWeight.Medium
            )
        }
    }
    
    if (showDetails) {
        ReliabilityDetailsDialog(
            reliability = reliability,
            onDismiss = { showDetails = false }
        )
    }
}

/**
 * Dialog showing detailed reliability information
 */
@Composable
fun ReliabilityDetailsDialog(
    reliability: ReviewReliability,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Color(reliability.level.color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.reviewer_reliability))
            }
        },
        text = {
            Column {
                // Overall score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = reliability.level.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(reliability.level.color),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(reliability.score * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Score bar
                LinearProgressIndicator(
                    progress = { reliability.score },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(reliability.level.color),
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.reliability_factors),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Factors
                reliability.factors.forEach { factor ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = factor.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = factor.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "+${(factor.score * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Disclaimer
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.reliability_disclaimer),
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
