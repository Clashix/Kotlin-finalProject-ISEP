package com.isep.kotlinproject.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isep.kotlinproject.R
import com.isep.kotlinproject.model.Badge
import com.isep.kotlinproject.model.BadgeType
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Display a horizontal row of badges
 */
@Composable
fun BadgesRow(
    badges: List<Badge>,
    modifier: Modifier = Modifier
) {
    if (badges.isEmpty()) {
        Text(
            text = stringResource(R.string.no_badges),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(16.dp)
        )
        return
    }
    
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(badges) { badge ->
            BadgeItem(badge = badge)
        }
    }
}

/**
 * Single badge display item
 */
@Composable
fun BadgeItem(
    badge: Badge,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(80.dp)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Badge icon
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = getBadgeColor(badge.type)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getBadgeIcon(badge.type),
                    contentDescription = badge.name,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Badge name
        Text(
            text = badge.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        // Earned date
        badge.earnedAt?.let { timestamp ->
            val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            Text(
                text = dateFormat.format(timestamp.toDate()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Compact badge chip for inline display
 */
@Composable
fun BadgeChip(
    badge: Badge,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = getBadgeColor(badge.type).copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getBadgeIcon(badge.type),
                contentDescription = null,
                tint = getBadgeColor(badge.type),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = badge.name,
                style = MaterialTheme.typography.labelSmall,
                color = getBadgeColor(badge.type)
            )
        }
    }
}

/**
 * New badge notification card
 */
@Composable
fun NewBadgeCard(
    badge: Badge,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = getBadgeColor(badge.type)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getBadgeIcon(badge.type),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.badge_earned),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = badge.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.close)
                )
            }
        }
    }
}

private fun getBadgeColor(type: BadgeType): Color {
    return when (type) {
        BadgeType.FIRST_REVIEW -> Color(0xFF4CAF50) // Green
        BadgeType.FIVE_REVIEWS -> Color(0xFF2196F3) // Blue
        BadgeType.TEN_REVIEWS -> Color(0xFF9C27B0) // Purple
        BadgeType.TWENTY_FIVE_REVIEWS -> Color(0xFFFF9800) // Orange
        BadgeType.FIFTY_REVIEWS -> Color(0xFFFFD700) // Gold
    }
}

private fun getBadgeIcon(type: BadgeType): ImageVector {
    return when (type) {
        BadgeType.FIRST_REVIEW -> Icons.Default.Star
        BadgeType.FIVE_REVIEWS -> Icons.Default.Stars
        BadgeType.TEN_REVIEWS -> Icons.Default.Verified
        BadgeType.TWENTY_FIVE_REVIEWS -> Icons.Default.WorkspacePremium
        BadgeType.FIFTY_REVIEWS -> Icons.Default.MilitaryTech
    }
}
