package com.isep.kotlinproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _authResult = MutableLiveData<Result<Unit>>()
    val authResult: LiveData<Result<Unit>> get() = _authResult

    fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authResult.value = Result.success(Unit)
                } else {
                    _authResult.value = Result.failure(task.exception ?: Exception("Login failed"))
                }
            }
    }

    fun signup(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authResult.value = Result.success(Unit)
                } else {
                    _authResult.value = Result.failure(task.exception ?: Exception("Signup failed"))
                }
            }
    }

    fun logout() {
        auth.signOut()
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}