package com.isep.kotlinproject.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.isep.kotlinproject.model.User
import com.isep.kotlinproject.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Navigation event state
    private val _navigateDestination = MutableStateFlow<String?>(null)
    val navigateDestination: StateFlow<String?> = _navigateDestination

    init {
        // Check for currently signed-in user
        auth.currentUser?.let { firebaseUser ->
            fetchUserData(firebaseUser.uid)
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                authResult.user?.let { firebaseUser ->
                    fetchUserData(firebaseUser.uid)
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signup(name: String, email: String, password: String, role: UserRole) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                authResult.user?.let { firebaseUser ->
                    val newUser = User(
                        id = firebaseUser.uid,
                        name = name,
                        email = email,
                        role = role
                    )
                    saveUserToFirestore(newUser)
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ... (rest of code)

    private suspend fun saveUserToFirestore(user: User) {
        try {
            db.collection("users").document(user.id).set(user).await()
            _user.value = user
            routeUser(user)
        } catch (e: Exception) {
            _error.value = "Failed to save user data: ${e.message}"
            // If saving to Firestore fails, we might want to consider deleting the auth user
            // to keep states in sync, but for now we just report the error.
        }
    }

    private fun fetchUserData(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val document = db.collection("users").document(userId).get().await()
                val fetchedUser = document.toObject(User::class.java)
                if (fetchedUser != null) {
                    _user.value = fetchedUser
                    routeUser(fetchedUser)
                } else {
                     // Handle case where user exists in Auth but not Firestore (edge case)
                     _error.value = "User profile not found."
                     auth.signOut()
                }
            } catch (e: Exception) {
                _error.value = "Failed to fetch user data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun routeUser(user: User) {
        if (user.role == UserRole.PLAYER) {
            _navigateDestination.value = "player_home"
        } else {
            _navigateDestination.value = "editor_dashboard"
        }
    }

    fun clearNavigation() {
        _navigateDestination.value = null
    }

    fun logout() {
        auth.signOut()
        _user.value = null
        _navigateDestination.value = "login"
    }

    fun uploadProfileImage(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = auth.currentUser?.uid
            if (userId != null) {
                try {
                    val ref = storage.reference.child("profile_images/$userId.jpg")
                    ref.putFile(uri).await()
                    val downloadUrl = ref.downloadUrl.await().toString()
                    
                    // Update user profile in Firestore
                    db.collection("users").document(userId).update("profileImageUrl", downloadUrl).await()
                    
                    // Update local state
                    val updatedUser = _user.value?.copy(profileImageUrl = downloadUrl)
                    _user.value = updatedUser
                } catch (e: Exception) {
                    _error.value = "Failed to upload image: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
}