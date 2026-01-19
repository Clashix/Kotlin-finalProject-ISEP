package com.isep.kotlinproject.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.isep.kotlinproject.model.BiasDetector
import com.isep.kotlinproject.model.BiasIndicator
import com.isep.kotlinproject.model.ReliabilityLevel
import com.isep.kotlinproject.model.ReportReason
import com.isep.kotlinproject.model.Review
import com.isep.kotlinproject.model.ReviewReliability
import com.isep.kotlinproject.model.UserRole
import com.isep.kotlinproject.repository.ReportRepository
import com.isep.kotlinproject.ui.components.BiasWarningChip
import com.isep.kotlinproject.ui.components.BiasWarningCard
import com.isep.kotlinproject.ui.components.ReliabilityBadge
import com.isep.kotlinproject.ui.components.ReportReviewDialog
import com.isep.kotlinproject.viewmodel.GameViewModel
import com.isep.kotlinproject.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    gameId: String,
    viewModel: GameViewModel,
    userViewModel: UserViewModel,
    userRole: UserRole,
    onEditClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val game by viewModel.selectedGame.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val userReview by viewModel.userReview.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser by userViewModel.currentUser.collectAsState()
    val successMessage by userViewModel.successMessage.collectAsState()
    val error by userViewModel.error.collectAsState()
    var showReviewDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Report states
    var showReportDialog by remember { mutableStateOf(false) }
    var reviewToReport by remember { mutableStateOf<Review?>(null) }
    val reportRepository = remember { ReportRepository() }
    
    // Badge notification
    val newBadge by viewModel.newBadgeAwarded.collectAsState()
    
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isEditor = userRole == UserRole.EDITOR
    val isOwner = isEditor && game?.editorId == currentUserId
    
    // Game list states
    val isLiked = currentUser?.likedGames?.contains(gameId) == true
    val isPlayed = currentUser?.playedGames?.contains(gameId) == true
    val isInWishlist = currentUser?.wishlist?.contains(gameId) == true
    
    // Show success message in Snackbar
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            userViewModel.clearSuccessMessage()
        }
    }
    
    // Show error message in Snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            userViewModel.clearError()
        }
    }

    LaunchedEffect(gameId) {
        viewModel.selectGame(gameId)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSelectedGame()
        }
    }

    if (isLoading || game == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!isEditor) {
                ExtendedFloatingActionButton(
                    onClick = { showReviewDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(
                        if (userReview != null) Icons.Default.Edit else Icons.Default.Star,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (userReview != null) "Edit Review" else "Rate Game")
                }
            } else if (isOwner) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    FloatingActionButton(
                        onClick = { onEditClick(game!!.id) },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    FloatingActionButton(
                        onClick = { showDeleteConfirmation = true },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp) // Space for FAB
            ) {
                // HERO SECTION
                item {
                    Box(modifier = Modifier.height(350.dp)) {
                        AsyncImage(
                            model = game!!.posterUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .height(350.dp),
                            contentScale = ContentScale.Crop
                        )
                        // Gradient Fade
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                            MaterialTheme.colorScheme.background
                                        ),
                                        startY = 200f
                                    )
                                )
                        )
                        
                        // Back Button (Overlaid)
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(top = 40.dp, start = 16.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        
                        // Title Overlay
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = game!!.title,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFFFB800), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%.1f", game!!.averageRating),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = " (${game!!.ratingCount} reviews)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }

                // INFO CHIPS
                item {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoChip(icon = Icons.Default.Gamepad, text = game!!.genre)
                        InfoChip(icon = Icons.Default.Code, text = game!!.developer)
                        InfoChip(icon = Icons.Default.CalendarToday, text = game!!.releaseDate.take(4))
                    }
                }
                
                // ACTION BUTTONS (Like, Played, Wishlist) - Players only
                if (!isEditor) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Like button
                            ActionButton(
                                icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                label = if (isLiked) "Liked" else "Like",
                                isActive = isLiked,
                                activeColor = Color(0xFFE91E63),
                                onClick = { userViewModel.toggleLike(gameId) }
                            )
                            
                            // Played button
                            ActionButton(
                                icon = if (isPlayed) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                label = if (isPlayed) "Played" else "Mark Played",
                                isActive = isPlayed,
                                activeColor = Color(0xFF4CAF50),
                                onClick = { userViewModel.togglePlayed(gameId) }
                            )
                            
                            // Wishlist button
                            ActionButton(
                                icon = if (isInWishlist) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                                label = if (isInWishlist) "In Wishlist" else "Wishlist",
                                isActive = isInWishlist,
                                activeColor = Color(0xFF2196F3),
                                onClick = { userViewModel.toggleWishlist(gameId) }
                            )
                        }
                    }
                }

                // DESCRIPTION
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = game!!.description.ifEmpty { "No description available." },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }

                // REVIEWS SECTION
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reviews",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (reviews.isNotEmpty()) {
                                TextButton(onClick = { /* See All Logic if needed */ }) {
                                    Text("See All")
                                }
                            }
                        }
                        
                        if (reviews.isEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("No reviews yet")
                                    Text("Be the first to share your opinion!", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                
                // My Review (if exists)
                 if (!isEditor && userReview != null) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                             Text(
                                text = "Your Review",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                             UserReviewCard(
                                review = userReview!!,
                                onEdit = { showReviewDialog = true },
                                onDelete = { viewModel.deleteMyReview(gameId) }
                            )
                        }
                    }
                }

                // Game-level bias warning (if few reviews)
                if (reviews.isNotEmpty()) {
                    item {
                        val gameBiasIndicators = remember(reviews) {
                            BiasDetector.detectGameBias(reviews, game?.averageRating ?: 0.0)
                        }
                        if (gameBiasIndicators.isNotEmpty()) {
                            BiasWarningCard(
                                indicators = gameBiasIndicators,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                items(reviews) { review ->
                    // Calculate reliability based on review data (simplified client-side approach)
                    // In production, this would come from user data loaded separately
                    val reliability = remember(review) {
                        // Simplified: estimate based on review timestamp (older = more established)
                        val reviewAge = System.currentTimeMillis() - review.timestamp
                        val reviewAgeDays = reviewAge / (1000 * 60 * 60 * 24)
                        
                        ReviewReliability.calculate(
                            accountCreatedAt = Timestamp(Date(review.timestamp - (reviewAgeDays * 30 * 24 * 60 * 60 * 1000))), // Estimate
                            reviewCount = (reviewAgeDays / 10).toInt().coerceIn(1, 20), // Heuristic
                            hasProfilePhoto = review.userPhotoURL.isNotBlank(),
                            hasBio = false // Unknown from review data
                        )
                    }
                    
                    // Detect bias for this review (simplified - would need user's other reviews)
                    val biasIndicators = remember(review, reviews) {
                        // Check if this user has other reviews in the same game (suspicious)
                        val userReviewsInGame = reviews.filter { it.userId == review.userId }
                        BiasDetector.detectUserBias(userReviewsInGame, review)
                    }
                    
                    ReviewItem(
                        review = review,
                        isCurrentUser = review.userId == currentUserId,
                        onReport = if (review.userId != currentUserId) {
                            {
                                reviewToReport = review
                                showReportDialog = true
                            }
                        } else null,
                        reliability = reliability,
                        biasIndicators = biasIndicators
                    )
                }
            }
        }
    }

    // DIALOGS
    if (showReviewDialog) {
        AddReviewDialog(
            existingReview = userReview,
            onDismiss = { showReviewDialog = false },
            onSubmit = { rating, comment ->
                viewModel.submitReview(gameId, rating, comment)
                showReviewDialog = false
            }
        )
    }
    
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Game?") },
            text = { Text("This action cannot be undone. Are you sure you want to remove this game from the library?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGame(game!!.id, onBack)
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Report Review Dialog
    if (showReportDialog && reviewToReport != null) {
        // Capture values before the coroutine to avoid null issues
        val reportReview = reviewToReport!!
        
        ReportReviewDialog(
            onDismiss = { 
                showReportDialog = false 
                reviewToReport = null
            },
            onReport = { reason, additionalInfo ->
                // Close dialog immediately for better UX
                showReportDialog = false
                reviewToReport = null
                
                // Submit report in background
                scope.launch {
                    val result = reportRepository.reportReview(
                        reviewId = reportReview.id,
                        gameId = gameId,
                        reportedUserId = reportReview.userId,
                        reason = reason,
                        additionalInfo = additionalInfo
                    )
                    result.fold(
                        onSuccess = {
                            snackbarHostState.showSnackbar("Report submitted successfully")
                        },
                        onFailure = { e ->
                            snackbarHostState.showSnackbar("Failed to submit report: ${e.message ?: "Unknown error"}")
                        }
                    )
                }
            }
        )
    }
    
    // New Badge Notification
    newBadge?.let { badge ->
        AlertDialog(
            onDismissRequest = { viewModel.clearNewBadge() },
            title = { Text("New Badge Earned!") },
            text = { 
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Badge icon
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = badge.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = badge.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.clearNewBadge() }) {
                    Text("Awesome!")
                }
            }
        )
    }
}

@Composable
fun InfoChip(icon: ImageVector, text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isActive) activeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun UserReviewCard(
    review: Review,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (index < review.rating) Color(0xFFFFB800) else Color.Gray.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${review.rating}/5",
                        fontWeight = FontWeight.Bold
                    )
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (review.comment.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(review.comment)
            }
        }
    }
}

@Composable
fun ReviewItem(
    review: Review, 
    isCurrentUser: Boolean,
    onReport: (() -> Unit)? = null,
    reliability: ReviewReliability? = null,
    biasIndicators: List<BiasIndicator> = emptyList()
) {
    var showMenu by remember { mutableStateOf(false) }
    
    // Basic review item (reuse similar logic or simplify)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = review.userName.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(review.userName, fontWeight = FontWeight.Bold)
                    
                    // Reliability badge
                    if (reliability != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        ReliabilityBadge(reliability = reliability)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (index < review.rating) Color(0xFFFFB800) else Color.Gray.copy(alpha = 0.3f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(review.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Report menu for other users' reviews
            if (onReport != null) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert, 
                            contentDescription = "More options",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Report") },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Flag, 
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                ) 
                            },
                            onClick = {
                                showMenu = false
                                onReport()
                            }
                        )
                    }
                }
            }
        }
        if (review.comment.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 44.dp)
            )
        }
        
        // Bias indicators (if any)
        if (biasIndicators.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.padding(start = 44.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                biasIndicators.take(2).forEach { indicator ->
                    BiasWarningChip(indicator = indicator)
                }
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp).padding(start = 44.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun AddReviewDialog(
    existingReview: Review?,
    onDismiss: () -> Unit, 
    onSubmit: (Int, String) -> Unit
) {
    var rating by remember { mutableStateOf(existingReview?.rating ?: 5) }
    var comment by remember { mutableStateOf(existingReview?.comment ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingReview != null) "Edit Review" else "Rate Game") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    repeat(5) { index ->
                        val starRating = index + 1
                        Icon(
                            imageVector = if (starRating <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = null,
                            tint = if (starRating <= rating) Color(0xFFFFB800) else Color.Gray,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { rating = starRating }
                                .padding(4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Write a review (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(rating, comment) }) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
