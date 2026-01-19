package com.isep.kotlinproject.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isep.kotlinproject.R
import com.isep.kotlinproject.model.UserRole
import com.isep.kotlinproject.ui.components.AppPasswordInput
import com.isep.kotlinproject.ui.components.AppTextField
import com.isep.kotlinproject.viewmodel.AuthViewModel

@Composable
fun SignupScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.PLAYER) }

    // Validation Logic
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$".toRegex()
    val isEmailValid = email.isEmpty() || email.matches(emailRegex)
    val isPasswordValid = password.isEmpty() || (password.length >= 6 && password.any { it.isDigit() })
    val isNameValid = name.isNotBlank()

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                // If the screen is small, we might want to make it scrollable, 
                // but for now let's assume it fits or rely on system scrolling behavior if keyboard opens.
                // Ideally, wrap content in a scrollable column if too long.
                ,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.create_account), 
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.full_name),
                    leadingIcon = Icons.Default.Person,
                    errorMessage = if (!isNameValid && name.isNotEmpty()) stringResource(R.string.error) else null
                )

                Spacer(modifier = Modifier.height(16.dp))

                AppTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = stringResource(R.string.email_address),
                    leadingIcon = Icons.Default.Email,
                    errorMessage = if (!isEmailValid) stringResource(R.string.error) else null
                )

                Spacer(modifier = Modifier.height(16.dp))

                AppPasswordInput(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.password),
                    helpText = stringResource(R.string.password_help),
                    errorMessage = if (!isPasswordValid && password.isNotEmpty()) stringResource(R.string.error) else null
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.role_prompt), 
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = selectedRole == UserRole.PLAYER,
                        onClick = { selectedRole = UserRole.PLAYER },
                        label = { Text(stringResource(R.string.role_player)) },
                        leadingIcon = { if (selectedRole == UserRole.PLAYER) Icon(Icons.Default.Person, null) else null }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    FilterChip(
                        selected = selectedRole == UserRole.EDITOR,
                        onClick = { selectedRole = UserRole.EDITOR },
                        label = { Text(stringResource(R.string.role_editor)) },
                        leadingIcon = { if (selectedRole == UserRole.EDITOR) Icon(Icons.Default.Person, null) else null } // Use proper icon if available
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = { 
                            if (name.isNotBlank() && isEmailValid && isPasswordValid && password.isNotEmpty()) {
                                viewModel.signup(name.trim(), email.trim(), password.trim(), selectedRole)
                            }
                        },
                        enabled = name.isNotBlank() && isEmailValid && isPasswordValid && password.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(stringResource(R.string.signup))
                    }
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error!!, 
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onNavigateToLogin) {
                    Text(stringResource(R.string.have_account))
                }
            }
        }
    }
}