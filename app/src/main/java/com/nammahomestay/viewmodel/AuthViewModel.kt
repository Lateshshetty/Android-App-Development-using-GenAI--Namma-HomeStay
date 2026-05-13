package com.nammahomestay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.nammahomestay.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val loading: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
    val role: String? = null,
    val authComplete: Boolean = false
)

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepository(app)
    private val _state = MutableStateFlow(AuthState(role = repo.userRole()))
    val state: StateFlow<AuthState> = _state

    fun isLoggedIn() = repo.isLoggedIn()
    fun role() = repo.userRole()
    fun logout() {
        repo.logout()
        _state.value = AuthState()
    }

    fun signInWithGoogle(account: GoogleSignInAccount) = viewModelScope.launch {
        _state.value = AuthState(loading = true)
        repo.signInWithGoogle(account).fold(
            onSuccess = { role -> _state.value = AuthState(done = true, role = role, authComplete = true) },
            onFailure = { _state.value = AuthState(error = it.message) }
        )
    }

    fun saveRole(role: String) = viewModelScope.launch {
        runCatching { repo.saveRole(role) }
            .onSuccess { _state.value = AuthState(done = true, role = role, authComplete = true) }
            .onFailure { _state.value = AuthState(error = it.message) }
    }
}
