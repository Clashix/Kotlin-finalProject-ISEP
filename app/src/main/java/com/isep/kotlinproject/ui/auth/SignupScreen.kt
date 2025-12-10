package com.isep.kotlinproject.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.isep.kotlinproject.model.UserRole
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

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Create Account", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text(
                    text = "Min 6 chars, at least 1 number",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (password.isNotEmpty() && (!password.any { it.isDigit() } || password.length < 6)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            isError = password.isNotEmpty() && (!password.any { it.isDigit() } || password.length < 6)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ... Role Selection ...

        // ...

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { 
                    if (name.isNotBlank() && email.isNotBlank() && password.length >= 6 && password.any { it.isDigit() }) {
                        viewModel.signup(name.trim(), email.trim(), password.trim(), selectedRole)
                    }
                },
                enabled = name.isNotBlank() && email.isNotBlank() && password.length >= 6 && password.any { it.isDigit() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Up")
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = error!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("Already have an account? Log In")
        }
    }
}
