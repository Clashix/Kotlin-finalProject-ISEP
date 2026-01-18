package com.isep.kotlinproject.ui.editor

import android.graphics.Color as AndroidColor
import android.view.ViewGroup
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.isep.kotlinproject.R
import com.isep.kotlinproject.model.EditorStats
import com.isep.kotlinproject.model.Game
import com.isep.kotlinproject.model.GameStats
import com.isep.kotlinproject.repository.StatsRepository
import com.isep.kotlinproject.viewmodel.StatsViewModel

/**
 * Editor statistics dashboard with interactive charts.
 * Uses MPAndroidChart for visualizations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorDashboardScreen(
    viewModel: StatsViewModel,
    editorGames: List<Game>,
    onNavigateToGame: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val editorStats by viewModel.editorStats.collectAsState()
    val ratingDistribution by viewModel.ratingDistribution.collectAsState()
    val ratingEvolution by viewModel.ratingEvolution.collectAsState()
    val reviewsPerDay by viewModel.reviewsPerDay.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var selectedGameId by remember { mutableStateOf<String?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        viewModel.loadEditorStats()
    }
    
    LaunchedEffect(selectedGameId) {
        selectedGameId?.let { gameId ->
            viewModel.loadRatingDistribution(gameId)
            viewModel.loadRatingEvolution(gameId)
            viewModel.loadReviewsPerDay(gameId)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshEditorStats() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && editorStats == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overview Cards
                item {
                    Text(
                        text = stringResource(R.string.overview),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item {
                    OverviewCards(editorStats)
                }
                
                // Game Selector
                if (editorGames.isNotEmpty()) {
                    item {
                        Text(
                            text = "Select a game for detailed statistics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    item {
                        GameSelector(
                            games = editorGames,
                            selectedGameId = selectedGameId,
                            onGameSelected = { selectedGameId = it }
                        )
                    }
                    
                    // Detailed Stats for Selected Game
                    if (selectedGameId != null) {
                        item {
                            TabRow(
                                selectedTabIndex = selectedTabIndex,
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Tab(
                                    selected = selectedTabIndex == 0,
                                    onClick = { selectedTabIndex = 0 },
                                    text = { Text(stringResource(R.string.rating_distribution)) }
                                )
                                Tab(
                                    selected = selectedTabIndex == 1,
                                    onClick = { selectedTabIndex = 1 },
                                    text = { Text(stringResource(R.string.rating_evolution)) }
                                )
                                Tab(
                                    selected = selectedTabIndex == 2,
                                    onClick = { selectedTabIndex = 2 },
                                    text = { Text(stringResource(R.string.reviews_per_day)) }
                                )
                            }
                        }
                        
                        item {
                            when (selectedTabIndex) {
                                0 -> RatingDistributionChart(ratingDistribution)
                                1 -> RatingEvolutionChart(ratingEvolution)
                                2 -> ReviewsPerDayChart(reviewsPerDay)
                            }
                        }
                    }
                }
                
                // Games List with Stats
                item {
                    Text(
                        text = stringResource(R.string.my_games),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                
                items(editorGames) { game ->
                    GameStatsCard(
                        game = game,
                        stats = editorStats?.gameStats?.find { it.gameId == game.id },
                        onClick = { onNavigateToGame(game.id) }
                    )
                }
                
                // Bottom spacing
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun OverviewCards(editorStats: EditorStats?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Gamepad,
            title = stringResource(R.string.total_games),
            value = (editorStats?.totalGames ?: 0).toString(),
            color = MaterialTheme.colorScheme.primary
        )
        
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Reviews,
            title = stringResource(R.string.total_reviews),
            value = (editorStats?.totalReviews ?: 0).toString(),
            color = MaterialTheme.colorScheme.secondary
        )
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.average_rating),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format("%.2f", editorStats?.overallAverageRating ?: 0.0),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = " / 5",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFFFFB800)
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameSelector(
    games: List<Game>,
    selectedGameId: String?,
    onGameSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedGame = games.find { it.id == selectedGameId }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedGame?.title ?: "Select a game",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            games.forEach { game ->
                DropdownMenuItem(
                    text = { Text(game.title) },
                    onClick = {
                        onGameSelected(game.id)
                        expanded = false
                    },
                    leadingIcon = {
                        AsyncImage(
                            model = game.posterUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun RatingDistributionChart(
    data: List<StatsRepository.RatingDistributionData>
) {
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (data.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_stats))
            }
        } else {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                factory = { context ->
                    PieChart(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        description.isEnabled = false
                        setUsePercentValues(true)
                        setEntryLabelColor(textColor)
                        setEntryLabelTextSize(12f)
                        legend.textColor = textColor
                        setHoleColor(surfaceColor)
                        setCenterTextColor(textColor)
                        isDrawHoleEnabled = true
                        holeRadius = 50f
                    }
                },
                update = { chart ->
                    val entries = data.map { 
                        PieEntry(it.percentage, "${it.stars} stars") 
                    }
                    
                    val colors = listOf(
                        AndroidColor.parseColor("#F44336"), // 1 star - red
                        AndroidColor.parseColor("#FF9800"), // 2 stars - orange
                        AndroidColor.parseColor("#FFC107"), // 3 stars - yellow
                        AndroidColor.parseColor("#8BC34A"), // 4 stars - light green
                        AndroidColor.parseColor("#4CAF50")  // 5 stars - green
                    )
                    
                    val dataSet = PieDataSet(entries, "").apply {
                        this.colors = colors
                        valueTextSize = 14f
                        valueTextColor = AndroidColor.WHITE
                        valueFormatter = PercentFormatter(chart)
                    }
                    
                    chart.data = PieData(dataSet)
                    chart.invalidate()
                }
            )
        }
    }
}

@Composable
private fun RatingEvolutionChart(
    data: List<StatsRepository.RatingDataPoint>
) {
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val chartGridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (data.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_stats))
            }
        } else {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                factory = { context ->
                    LineChart(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        description.isEnabled = false
                        legend.textColor = textColor
                        
                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            setTextColor(textColor)
                            this.gridColor = chartGridColor
                            setDrawGridLines(true)
                        }
                        
                        axisLeft.apply {
                            setTextColor(textColor)
                            axisMinimum = 0f
                            axisMaximum = 5f
                            granularity = 1f
                        }
                        
                        axisRight.isEnabled = false
                        setTouchEnabled(true)
                        isDragEnabled = true
                        setScaleEnabled(true)
                    }
                },
                update = { chart ->
                    val entries = data.mapIndexed { index, point ->
                        Entry(index.toFloat(), point.averageRating.toFloat())
                    }
                    
                    val dates = data.map { it.date.takeLast(5) } // MM-DD format
                    
                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(dates)
                    
                    val dataSet = LineDataSet(entries, "Average Rating").apply {
                        color = primaryColor
                        lineWidth = 2f
                        setCircleColor(primaryColor)
                        circleRadius = 4f
                        setDrawValues(false)
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                        setDrawFilled(true)
                        fillColor = primaryColor
                        fillAlpha = 50
                    }
                    
                    chart.data = LineData(dataSet)
                    chart.invalidate()
                }
            )
        }
    }
}

@Composable
private fun ReviewsPerDayChart(
    data: List<Pair<String, Int>>
) {
    val primaryColor = MaterialTheme.colorScheme.secondary.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val chartGridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (data.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_stats))
            }
        } else {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                factory = { context ->
                    BarChart(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        description.isEnabled = false
                        legend.textColor = textColor
                        
                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            setTextColor(textColor)
                            this.gridColor = chartGridColor
                            setDrawGridLines(false)
                            granularity = 1f
                        }
                        
                        axisLeft.apply {
                            setTextColor(textColor)
                            axisMinimum = 0f
                            granularity = 1f
                        }
                        
                        axisRight.isEnabled = false
                        setTouchEnabled(true)
                        setFitBars(true)
                    }
                },
                update = { chart ->
                    val entries = data.mapIndexed { index, (_, count) ->
                        BarEntry(index.toFloat(), count.toFloat())
                    }
                    
                    val dates = data.map { it.first.takeLast(5) }
                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(dates)
                    
                    val dataSet = BarDataSet(entries, "Reviews").apply {
                        color = primaryColor
                        setDrawValues(true)
                        valueTextColor = textColor
                        valueTextSize = 10f
                    }
                    
                    chart.data = BarData(dataSet).apply {
                        barWidth = 0.8f
                    }
                    chart.invalidate()
                }
            )
        }
    }
}

@Composable
private fun GameStatsCard(
    game: Game,
    stats: GameStats?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = game.posterUrl,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", stats?.averageRating ?: game.averageRating),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Icon(
                        Icons.Default.Reviews,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${stats?.totalReviews ?: game.ratingCount} reviews",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
