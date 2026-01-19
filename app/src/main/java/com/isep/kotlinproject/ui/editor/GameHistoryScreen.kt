package com.isep.kotlinproject.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isep.kotlinproject.R
import com.isep.kotlinproject.model.GameAction
import com.isep.kotlinproject.model.GameHistory
import com.isep.kotlinproject.repository.GameHistoryRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen displaying game edit history for editors.
 * Shows all create/update/delete actions performed by the editor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHistoryScreen(
    editorId: String,
    onNavigateBack: () -> Unit,
    onNavigateToGame: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = remember { GameHistoryRepository() }
    
    var history by remember { mutableStateOf<List<GameHistory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(editorId) {
        scope.launch {
            repository.getEditorHistoryFlow(editorId).collect { items ->
                history = items
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_history)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            history.isEmpty() -> {
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
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_history),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(history) { item ->
                        HistoryItem(
                            history = item,
                            onClick = { 
                                if (item.action != GameAction.DELETE) {
                                    onNavigateToGame(item.gameId)
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
private fun HistoryItem(
    history: GameHistory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Action icon
            Surface(
                shape = MaterialTheme.shapes.small,
                color = when (history.action) {
                    GameAction.CREATE -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                    GameAction.UPDATE -> Color(0xFF2196F3).copy(alpha = 0.15f)
                    GameAction.DELETE -> Color(0xFFF44336).copy(alpha = 0.15f)
                }
            ) {
                Icon(
                    imageVector = when (history.action) {
                        GameAction.CREATE -> Icons.Default.Add
                        GameAction.UPDATE -> Icons.Default.Edit
                        GameAction.DELETE -> Icons.Default.Delete
                    },
                    contentDescription = null,
                    tint = when (history.action) {
                        GameAction.CREATE -> Color(0xFF4CAF50)
                        GameAction.UPDATE -> Color(0xFF2196F3)
                        GameAction.DELETE -> Color(0xFFF44336)
                    },
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = history.gameTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = history.getSummary(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Show changed fields for updates
                if (history.action == GameAction.UPDATE && history.changedFields.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    history.changedFields.forEach { (field, change) ->
                        Text(
                            text = "$field: ${change.oldValue.take(30)} → ${change.newValue.take(30)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Timestamp
                history.timestamp?.let { timestamp ->
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                    Text(
                        text = dateFormat.format(timestamp.toDate()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
