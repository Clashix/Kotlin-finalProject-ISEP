package com.isep.kotlinproject.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.isep.kotlinproject.R
import com.isep.kotlinproject.model.FriendRequest
import com.isep.kotlinproject.model.Notification
import com.isep.kotlinproject.model.NotificationType
import com.isep.kotlinproject.viewmodel.UserViewModel
import kotlinx.coroutines.launch

/**
 * Notifications screen showing friend requests and other notifications.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: UserViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToUser: (String) -> Unit
) {
    val pendingRequests by viewModel.pendingFriendRequests.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (pendingRequests.isEmpty() && notifications.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_notifications),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Friend Requests Section
                if (pendingRequests.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.friend_requests),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(
                        items = pendingRequests,
                        key = { it.id }
                    ) { request ->
                        FriendRequestCard(
                            request = request,
                            onAccept = {
                                viewModel.acceptFriendRequest(request.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Friend request accepted!")
                                }
                            },
                            onReject = {
                                viewModel.rejectFriendRequest(request.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Friend request rejected")
                                }
                            },
                            onUserClick = { onNavigateToUser(request.fromUserId) }
                        )
                    }
                }
                
                // Other Notifications Section
                if (notifications.isNotEmpty()) {
                    if (pendingRequests.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    item {
                        Text(
                            text = stringResource(R.string.notifications),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(
                        items = notifications,
                        key = { it.id }
                    ) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = {
                                viewModel.markNotificationAsRead(notification.id)
                                if (notification.fromUserId.isNotBlank()) {
                                    onNavigateToUser(notification.fromUserId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendRequestCard(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onUserClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(
                        request.fromUserPhotoURL.ifBlank {
                            "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y"
                        }
                    )
                    .crossfade(true)
                    .build(),
                contentDescription = request.fromUserName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onUserClick)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.fromUserName.ifBlank { "Unknown User" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.friend_request_received, "").replace("%s", "").trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = request.getRelativeTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Reject
                FilledTonalIconButton(
                    onClick = onReject,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.reject),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                
                // Accept
                FilledIconButton(
                    onClick = onAccept,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.accept),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on notification type
            val icon = when (notification.type) {
                NotificationType.FRIEND_REQUEST -> Icons.Default.PersonAdd
                NotificationType.FRIEND_ACCEPTED -> Icons.Default.People
                NotificationType.NEW_MESSAGE -> Icons.Default.Message
                NotificationType.GAME_UPDATE -> Icons.Default.SportsEsports
            }
            
            val iconColor = when (notification.type) {
                NotificationType.FRIEND_REQUEST -> MaterialTheme.colorScheme.primary
                NotificationType.FRIEND_ACCEPTED -> MaterialTheme.colorScheme.tertiary
                NotificationType.NEW_MESSAGE -> MaterialTheme.colorScheme.secondary
                NotificationType.GAME_UPDATE -> MaterialTheme.colorScheme.primary
            }
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.message.ifBlank { 
                        when (notification.type) {
                            NotificationType.FRIEND_REQUEST -> 
                                stringResource(R.string.friend_request_received, notification.fromUserName)
                            NotificationType.FRIEND_ACCEPTED -> 
                                stringResource(R.string.friend_request_accepted, notification.fromUserName)
                            NotificationType.NEW_MESSAGE -> 
                                stringResource(R.string.new_message_notification, notification.fromUserName)
                            NotificationType.GAME_UPDATE -> "Game update"
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (notification.read) FontWeight.Normal else FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.getRelativeTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Unread indicator
            if (!notification.read) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
