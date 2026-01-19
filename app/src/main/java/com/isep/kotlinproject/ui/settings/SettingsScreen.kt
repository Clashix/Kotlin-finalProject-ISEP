package com.isep.kotlinproject.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isep.kotlinproject.R
import com.isep.kotlinproject.model.ThemePreference
import com.isep.kotlinproject.viewmodel.SettingsViewModel

/**
 * Settings screen with theme selection and other preferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val themePreference by viewModel.themePreference.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Appearance Section
            SettingsSectionHeader(title = stringResource(R.string.appearance))
            
            // Theme selection
            SettingsItem(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.theme),
                subtitle = when (themePreference) {
                    ThemePreference.LIGHT -> stringResource(R.string.theme_light)
                    ThemePreference.DARK -> stringResource(R.string.theme_dark)
                    ThemePreference.SYSTEM -> stringResource(R.string.theme_system)
                },
                onClick = { showThemeDialog = true }
            )
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            
            // Language Section (placeholder for future implementation)
            SettingsSectionHeader(title = stringResource(R.string.language))
            
            SettingsItem(
                icon = Icons.Default.Language,
                title = stringResource(R.string.language),
                subtitle = stringResource(R.string.language_en),
                onClick = { /* TODO: Language selection */ }
            )
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            
            // About Section
            SettingsSectionHeader(title = stringResource(R.string.about_app))
            
            SettingsItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.about_app),
                subtitle = stringResource(R.string.version, "1.0.0"),
                onClick = { /* TODO: About screen */ }
            )
            
            SettingsItem(
                icon = Icons.Default.Policy,
                title = stringResource(R.string.privacy_policy),
                subtitle = null,
                onClick = { /* TODO: Privacy policy */ }
            )
            
            SettingsItem(
                icon = Icons.Default.Description,
                title = stringResource(R.string.terms_of_service),
                subtitle = null,
                onClick = { /* TODO: Terms of service */ }
            )
        }
    }
    
    // Theme selection dialog
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = themePreference,
            onThemeSelected = { theme ->
                viewModel.setThemePreference(theme)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: ThemePreference,
    onThemeSelected: (ThemePreference) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                ThemeOption(
                    title = stringResource(R.string.theme_system),
                    icon = Icons.Default.SettingsBrightness,
                    selected = currentTheme == ThemePreference.SYSTEM,
                    onClick = { onThemeSelected(ThemePreference.SYSTEM) }
                )
                ThemeOption(
                    title = stringResource(R.string.theme_light),
                    icon = Icons.Default.LightMode,
                    selected = currentTheme == ThemePreference.LIGHT,
                    onClick = { onThemeSelected(ThemePreference.LIGHT) }
                )
                ThemeOption(
                    title = stringResource(R.string.theme_dark),
                    icon = Icons.Default.DarkMode,
                    selected = currentTheme == ThemePreference.DARK,
                    onClick = { onThemeSelected(ThemePreference.DARK) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun ThemeOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = selected,
            onClick = null // handled by Row's selectable
        )
    }
}
