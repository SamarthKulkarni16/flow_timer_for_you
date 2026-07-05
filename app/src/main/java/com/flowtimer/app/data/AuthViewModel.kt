package com.flowtimer.app.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flowtimer.app.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    /** Emitted right after a successful sign-in/sign-up so the UI can show the welcome animation. */
    data class Success(val isNewAccount: Boolean) : AuthUiState()
}

class AuthViewModel : ViewModel() {

    private val auth = SupabaseClientProvider.auth

    val sessionStatus: StateFlow<SessionStatus> = auth.sessionStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionStatus.Initializing)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    val currentUserEmail: String?
        get() = auth.currentUserOrNull()?.email

    fun isSignedIn(): Boolean = auth.currentUserOrNull() != null

    /**
     * Single entry point for email auth. Tries signing in first; if that fails
     * (most likely because no account exists yet with this email), attempts to
     * create one. This removes the need for a separate "create account" mode -
     * the user just enters their details and continues.
     */
    fun continueWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Enter an email and password")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
                _uiState.value = AuthUiState.Success(isNewAccount = false)
            } catch (signInError: Exception) {
                if (password.length < 6) {
                    _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
                    return@launch
                }
                try {
                    auth.signUpWith(Email) {
                        this.email = email.trim()
                        this.password = password
                    }
                    if (auth.currentUserOrNull() != null) {
                        _uiState.value = AuthUiState.Success(isNewAccount = true)
                    } else {
                        // Project has email confirmation enabled - no session yet.
                        _uiState.value = AuthUiState.Error("Check your email to confirm your account, then continue")
                    }
                } catch (signUpError: Exception) {
                    // Sign-in failed AND sign-up failed (email already registered) -> wrong password.
                    _uiState.value = AuthUiState.Error("Incorrect password")
                }
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val rawNonce = UUID.randomUUID().toString()
                val hashedNonce = sha256(rawNonce)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .setNonce(hashedNonce)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val credentialManager = CredentialManager.create(context)
                val result = credentialManager.getCredential(context, request)
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)

                auth.signInWith(IDToken) {
                    idToken = googleIdTokenCredential.idToken
                    provider = Google
                    nonce = rawNonce
                }
                _uiState.value = AuthUiState.Success(isNewAccount = isNewAccount(auth.currentUserOrNull()))
            } catch (e: GetCredentialException) {
                _uiState.value = AuthUiState.Error("Google sign-in cancelled or unavailable")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Google sign-in failed")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
            } catch (_: Exception) {
                // Best-effort; local session is cleared regardless.
            }
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    /** Called once the welcome animation finishes playing. */
    fun completeAuthFlow() {
        if (_uiState.value is AuthUiState.Success) {
            _uiState.value = AuthUiState.Idle
        }
    }

    /** A user is "new" if their account was created within a few seconds of this sign-in. */
    private fun isNewAccount(user: UserInfo?): Boolean {
        val created = user?.createdAt ?: return false
        val lastSignIn = user.lastSignInAt ?: return true
        return (lastSignIn - created).inWholeSeconds < 5
    }

    private fun sha256(input: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

class AuthViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
