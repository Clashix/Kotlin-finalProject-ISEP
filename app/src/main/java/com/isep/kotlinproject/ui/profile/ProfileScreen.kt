package com.isep.kotlinproject.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.isep.kotlinproject.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit
) {
    val user by viewModel.user.collectAsState()
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.uploadProfileImage(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Profile Image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(if (user?.profileImageUrl.isNullOrEmpty()) "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y" else user?.profileImageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Profile Picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = { 
            imagePickerLauncher.launch(arrayOf("image/*"))
        }) {
            Text("Change Profile Picture")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = user?.name ?: "Guest User", style = MaterialTheme.typography.headlineSmall)
        Text(text = user?.email ?: "", style = MaterialTheme.typography.bodyMedium)
        Text(text = "Role: ${user?.role?.name ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                viewModel.logout()
                onNavigateToLogin()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Log Out")
        }
    }
}