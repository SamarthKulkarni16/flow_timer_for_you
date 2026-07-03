package com.flowtimer.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowtimer.app.data.AuthUiState
import com.flowtimer.app.data.AuthViewModel
import io.github.jan.supabase.auth.SessionStatus

/**
 * Sign in / create account screen. Shown on first launch (skippable - the
 * app is fully usable offline without an account) and reachable later from
 * Home for backup/sync.
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onSkip: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val sessionStatus by viewModel.sessionStatus.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        when (val status = sessionStatus) {
            is SessionStatus.Authenticated -> SignedInContent(
                email = status.session.user?.email,
                onSignOut = { viewModel.signOut() },
                onBack = onBack
            )
            else -> SignedOutContent(
                uiState = uiState,
                onSignIn = { email, password -> viewModel.signInWithEmail(email, password) },
                onSignUp = { email, password -> viewModel.signUpWithEmail(email, password) },
                onGoogleSignIn = { viewModel.signInWithGoogle(context) },
                onSkip = onSkip,
                onBack = onBack,
                onDismissError = { viewModel.clearError() }
            )
        }
    }
}

@Composable
private fun SignedOutContent(
    uiState: AuthUiState,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onSkip: () -> Unit,
    onBack: (() -> Unit)?,
    onDismissError: () -> Unit
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val isLoading = uiState is AuthUiState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "back",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onBack() }
                        .padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        Text(
            text = if (isSignUpMode) "create account" else "sign in",
            color = Color.White,
            fontSize = 26.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "for backup & sync across devices",
            color = Color.Gray,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        AuthTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "email",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        AuthTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "password",
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            isPassword = true,
            onImeAction = {
                focusManager.clearFocus()
                if (isSignUpMode) onSignUp(email, password) else onSignIn(email, password)
            }
        )

        if (uiState is AuthUiState.Error) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = uiState.message,
                color = Color(0xFFFF6B6B),
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismissError() }
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
        } else {
            Text(
                text = if (isSignUpMode) "create account" else "continue",
                color = Color.Black,
                fontSize = 18.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isSignUpMode) onSignUp(email, password) else onSignIn(email, password)
                    }
                    .padding(horizontal = 40.dp, vertical = 14.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "continue with google",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Light,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onGoogleSignIn() }
                    .padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = if (isSignUpMode) "have an account? sign in" else "new here? create an account",
            color = Color.Gray,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { isSignUpMode = !isSignUpMode }
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "skip for now",
            color = Color.DarkGray,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onSkip() }
                .padding(8.dp)
        )
    }
}

@Composable
private fun SignedInContent(
    email: String?,
    onSignOut: () -> Unit,
    onBack: (() -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "signed in",
            color = Color.White,
            fontSize = 24.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = email ?: "",
            color = Color.Gray,
            fontSize = 16.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "sign out",
            color = Color.White,
            fontSize = 16.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onSignOut() }
                .padding(12.dp)
        )
        if (onBack != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "back",
                color = Color.DarkGray,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Light,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBack() }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    isPassword: Boolean = false
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 20.sp,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center
        ),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        cursorBrush = SolidColor(Color.White),
        modifier = Modifier.width(260.dp).padding(vertical = 10.dp),
        decorationBox = { innerTextField ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color.DarkGray,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )
                    }
                    innerTextField()
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.DarkGray)
                )
            }
        }
    )
}
