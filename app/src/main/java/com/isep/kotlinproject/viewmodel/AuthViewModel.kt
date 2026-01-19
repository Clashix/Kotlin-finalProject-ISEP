package com.isep.kotlinproject.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.isep.kotlinproject.model.User
import com.isep.kotlinproject.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel for authentication operations.
 * Handles login, signup, Google Sign-In, and session management.
 */
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

    /**
     * Login with email and password
     */
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

    /**
     * Sign up with email and password
     */
    fun signup(name: String, email: String, password: String, role: UserRole) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                authResult.user?.let { firebaseUser ->
                    val newUser = User.createNew(
                        uid = firebaseUser.uid,
                        displayName = name,
                        email = email,
                        userRole = role,
                        photoURL = firebaseUser.photoUrl?.toString() ?: "",
                        locale = "en"
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

    /**
     * Sign in with Google credential
     */
    fun signInWithGoogle(idToken: String, role: UserRole? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                authResult.user?.let { firebaseUser ->
                    // Check if user already exists
                    val existingDoc = db.collection("users").document(firebaseUser.uid).get().await()
                    
                    if (existingDoc.exists()) {
                        // Existing user - fetch data
                        fetchUserData(firebaseUser.uid)
                    } else {
                        // New user - create document
                        val newUser = User.createNew(
                            uid = firebaseUser.uid,
                            displayName = firebaseUser.displayName ?: "User",
                            email = firebaseUser.email ?: "",
                            userRole = role ?: UserRole.PLAYER,
                            photoURL = firebaseUser.photoUrl?.toString() ?: "",
                            locale = "en"
                        )
                        saveUserToFirestore(newUser)
                    }
                }
            } catch (e: Exception) {
                _error.value = "Google sign-in failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear navigation destination
     */
    fun clearNavigation() {
        _navigateDestination.value = null
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Set error message (for external error handling)
     */
    fun setError(message: String) {
        _error.value = message
    }

    /**
     * Logout current user
     */
    fun logout() {
        auth.signOut()
        _user.value = null
        _navigateDestination.value = null
    }

    /**
     * Upload profile image
     */
    fun uploadProfileImage(uri: Uri) {
        val currentUser = auth.currentUser
        if (currentUser == null) return
        
        val userId = currentUser.uid
        val storageRef = storage.reference.child("profile_images/$userId")

        viewModelScope.launch {
            _isLoading.value = true
            try {
                storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                
                db.collection("users").document(userId)
                    .update(
                        mapOf(
                            "photoURL" to downloadUrl,
                            "profileImageUrl" to downloadUrl // Keep legacy field
                        )
                    ).await()
                
                _user.value = _user.value?.copy(photoURL = downloadUrl)
            } catch (e: Exception) {
                _error.value = "Upload failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update user display name
     */
    fun updateDisplayName(name: String) {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                db.collection("users").document(userId)
                    .update(
                        mapOf(
                            "displayName" to name,
                            "name" to name // Keep legacy field
                        )
                    ).await()
                
                _user.value = _user.value?.copy(displayName = name, name = name)
            } catch (e: Exception) {
                _error.value = "Update failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update user locale preference
     */
    fun updateLocale(locale: String) {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .update("locale", locale)
                    .await()
                
                _user.value = _user.value?.copy(locale = locale)
            } catch (e: Exception) {
                _error.value = "Update locale failed: ${e.message}"
            }
        }
    }

    /**
     * Save user document to Firestore
     */
    private suspend fun saveUserToFirestore(user: User) {
        try {
            val userData = mapOf(
                "uid" to user.uid,
                "displayName" to user.displayName,
                "name" to user.displayName, // Legacy field
                "email" to user.email,
                "photoURL" to user.photoURL,
                "profileImageUrl" to user.photoURL, // Legacy field
                "role" to user.userRole.name,
                "locale" to user.locale,
                "friends" to user.friends,
                "likedGames" to user.likedGames,
                "playedGames" to user.playedGames,
                "wishlist" to user.wishlist,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            
            db.collection("users").document(user.uid).set(userData).await()
            _user.value = user
            routeUser(user)
        } catch (e: Exception) {
            _error.value = "Failed to save user data: ${e.message}"
        }
    }

    /**
     * Fetch user data from Firestore
     */
    private fun fetchUserData(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val document = db.collection("users").document(userId).get().await()
                
                if (document.exists()) {
                    val data = document.data ?: return@launch
                    
                    // Parse role from string
                    val roleString = data["role"] as? String ?: "PLAYER"
                    val role = try {
                        UserRole.valueOf(roleString.uppercase())
                    } catch (e: Exception) {
                        UserRole.PLAYER
                    }
                    
                    @Suppress("UNCHECKED_CAST")
                    val fetchedUser = User(
                        uid = userId,
                        displayName = data["displayName"] as? String 
                            ?: data["name"] as? String 
                            ?: "User",
                        displayNameLower = (data["displayNameLower"] as? String) 
                            ?: (data["displayName"] as? String)?.lowercase() 
                            ?: "",
                        email = data["email"] as? String ?: "",
                        photoURL = data["photoURL"] as? String 
                            ?: data["profileImageUrl"] as? String 
                            ?: "",
                        bio = data["bio"] as? String ?: "",
                        role = roleString.lowercase(),
                        locale = data["locale"] as? String ?: "en",
                        friends = data["friends"] as? List<String> ?: emptyList(),
                        likedGames = data["likedGames"] as? List<String> ?: emptyList(),
                        playedGames = data["playedGames"] as? List<String> ?: emptyList(),
                        wishlist = data["wishlist"] as? List<String> ?: emptyList(),
                        wishlistSteamAppIds = data["wishlistSteamAppIds"] as? List<String> ?: emptyList(),
                        followingEditors = data["followingEditors"] as? List<String> ?: emptyList(),
                        themePreference = data["themePreference"] as? String ?: "system",
                        name = data["name"] as? String ?: "",
                        profileImageUrl = data["profileImageUrl"] as? String ?: ""
                    )
                    
                    _user.value = fetchedUser
                    routeUser(fetchedUser)
                } else {
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

    /**
     * Route user to appropriate destination based on role
     */
    private fun routeUser(user: User) {
        if (user.userRole == UserRole.PLAYER) {
            _navigateDestination.value = "player_home"
        } else {
            _navigateDestination.value = "editor_dashboard"
        }
    }
}
