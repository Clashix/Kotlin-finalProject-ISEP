package com.isep.kotlinproject.ui.users

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.isep.kotlinproject.R
import com.isep.kotlinproject.model.User
import com.isep.kotlinproject.model.UserRole
import com.isep.kotlinproject.viewmodel.UsersViewModel

/**
 * Users directory list screen with search and pagination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersListScreen(
    viewModel: UsersViewModel,
    onUserClick: (String) -> Unit,
    onProfileClick: () -> Unit
) {
    val usersListState by viewModel.usersListState.collectAsState()
    val users by viewModel.users.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    val listState = rememberLazyListState()
    
    // Load users on first composition
    LaunchedEffect(Unit) {
        if (users.isEmpty()) {
            viewModel.loadUsers()
        }
    }
    
    // Pagination trigger
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 3 && totalItems > 0
        }
    }
    
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && searchQuery.isBlank()) {
            viewModel.loadMoreUsers()
        } else if (shouldLoadMore && searchQuery.isNotBlank()) {
            viewModel.loadMoreSearchResults()
        }
    }
    
    val displayUsers = if (searchQuery.isNotBlank()) searchResults else users
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.nav_users),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = stringResource(R.string.profile),
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onSearch = { },
                active = false,
                onActiveChange = { },
                placeholder = { Text(stringResource(R.string.search_users)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.close))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                content = {}
            )
            
            // Content
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshUsers() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    // Initial loading
                    usersListState is UsersViewModel.UsersListState.Loading && displayUsers.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    // Searching
                    isSearching && searchQuery.isNotBlank() && searchResults.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    // Error state
                    usersListState is UsersViewModel.UsersListState.Error -> {
                        ErrorState(
                            message = (usersListState as UsersViewModel.UsersListState.Error).message,
                            onRetry = { viewModel.loadUsers() }
                        )
                    }
                    
                    // Empty state
                    displayUsers.isEmpty() -> {
                        EmptyState(
                            isSearchResult = searchQuery.isNotBlank()
                        )
                    }
                    
                    // Users list
                    else -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = displayUsers,
                                key = { it.uid }
                            ) { user ->
                                UserListItem(
                                    user = user,
                                    onClick = { onUserClick(user.uid) }
                                )
                            }
                            
                            // Loading more indicator
                            if (usersListState is UsersViewModel.UsersListState.LoadingMore ||
                                (isSearching && searchQuery.isNotBlank() && searchResults.isNotEmpty())
                            ) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserListItem(
    user: User,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(
                        user.photoURL.ifBlank { 
                            "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y" 
                        }
                    )
                    .crossfade(true)
                    .build(),
                contentDescription = user.getDisplayNameOrLegacy(),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // User info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.getDisplayNameOrLegacy().ifBlank { "Anonymous" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (user.bio.isNotBlank()) {
                    Text(
                        text = user.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Role badge
            RoleBadge(role = user.userRole)
        }
    }
}

@Composable
private fun RoleBadge(role: UserRole) {
    val (backgroundColor, textColor, icon, label) = when (role) {
        UserRole.EDITOR -> Quadruple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.Edit,
            stringResource(R.string.role_editor)
        )
        UserRole.PLAYER -> Quadruple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Default.SportsEsports,
            stringResource(R.string.role_player)
        )
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmptyState(isSearchResult: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isSearchResult) Icons.Default.SearchOff else Icons.Default.People,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSearchResult) 
                    stringResource(R.string.no_results) 
                else 
                    stringResource(R.string.no_users),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

/**
 * Helper data class for role badge
 */
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
