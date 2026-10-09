package com.example.imagetopdf.features.auth.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imagetopdf.core.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun login(onSuccess: (String) -> Unit) {
        val state = _uiState.value
        if (state.email.isBlank() || !state.email.contains("@")) {
            _uiState.value = state.copy(error = "Enter a valid email address")
            return
        }
        if (state.password.length < 4) {
            _uiState.value = state.copy(error = "Password must be at least 4 characters")
            return
        }
        val stored = UserPreferences.getLocalPassword(context)
        if (stored != null && state.password != stored) {
            _uiState.value = state.copy(error = "Incorrect password")
            return
        }

        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            if (UserPreferences.getLocalPassword(context) == null) {
                UserPreferences.setLocalPassword(context, state.password)
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoggedIn = true
            )
            onSuccess(state.email)
        }
    }

    fun loginWithGoogle(onSuccess: (String) -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            val email = _uiState.value.email.ifBlank { "user@gmail.com" }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoggedIn = true,
                email = email
            )
            onSuccess(email)
        }
    }

    fun reset() {
        _uiState.value = AuthUiState()
    }
}
