package io.github.jan.supabase.common.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.common.ChatViewModel
import io.github.jan.supabase.common.ui.components.OTPDialog
import io.github.jan.supabase.common.ui.components.OTPDialogState
import io.github.jan.supabase.common.ui.components.PasswordField
import io.github.jan.supabase.common.ui.components.PasswordRecoveryDialog
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberEmailVerifier
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.compose.auth.ui.ProviderButtonContent
import io.github.jan.supabase.compose.auth.ui.ProviderIcon
import io.github.jan.supabase.compose.auth.ui.annotations.AuthUiExperimental

@OptIn(ExperimentalMaterial3Api::class, SupabaseExperimental::class, AuthUiExperimental::class)
@Composable
fun LoginScreen(viewModel: ChatViewModel) {
    var signUp by remember { mutableStateOf(false) }
    val loginAlert by viewModel.alert.collectAsState()
    var email by remember { mutableStateOf("") }
    var otpDialogState by remember { mutableStateOf<OTPDialogState>(OTPDialogState.Invisible) }
    var showPasswordRecoveryDialog by remember { mutableStateOf(false) }
    val emailVerifier = viewModel.supabaseClient.composeAuth.rememberEmailVerifier(
        onResult = { result ->
            result.fold(
                onSuccess = { userInfo ->
                    viewModel.verifyEmail(userInfo.token)
                    Logger.d("LoginScreen - Verified SD-JWT: ${userInfo.token}")
                },
                onFailure = { error ->
                    Logger.d("LoginScreen - Verification failed")
                }
            )
        }
    )
    val action = viewModel.supabaseClient.composeAuth.rememberSignInWithGoogle(
        onResult = { result ->
            when (result) {
                is NativeSignInResult.Success -> {
                    viewModel.handleSignInWithGoogleResult()
                    Logger.d("LoginScreen - Success")
                }

                is NativeSignInResult.ClosedByUser -> {
                    Logger.d("LoginScreen - ClosedByUser")
                }

                is NativeSignInResult.Error -> {
                    Logger.d("LoginScreen - Error")
                }

                is NativeSignInResult.NetworkError -> {
                    Logger.d("LoginScreen - Network Error")
                }
            }
        },
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var password by remember { mutableStateOf("") }
        val passwordFocus = remember { FocusRequester() }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            singleLine = true,
            label = { Text("E-Mail") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
            leadingIcon = { Icon(Icons.Filled.Mail, "Mail") },
        )
        PasswordField(
            password = password,
            onPasswordChanged = { password = it },
            modifier = Modifier.focusRequester(passwordFocus)
                .padding(top = 10.dp),
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = {
                authenticate(signUp, viewModel, email, password)
            }),
            onForgotPassword = {
                showPasswordRecoveryDialog = true
            }
        )

        Button(
            onClick = { authenticate(signUp, viewModel, email, password) },
            modifier = Modifier.padding(top = 10.dp),
            enabled = email.isNotBlank() && password.isNotBlank()
        ) {
            Text(if (signUp) "Register" else "Login")
        }

        if (signUp) {
            Button(
                onClick = { emailVerifier.verify(nonce = "nonce-value") },
                modifier = Modifier.padding(top = 10.dp),
                enabled = email.isNotBlank()
            ) {
                Text("Verify email")
            }
        }

        OutlinedButton(
            onClick = {
                viewModel.loginWithGoogle()
            }
        ) {
            ProviderButtonContent(Google, text = if (signUp) "Sign Up with Google" else "Login with Google")
        }
        Button(
            onClick = {
                action.startFlow()
            },
        ) {
            ProviderIcon(Google, contentDescription = null)
            Text("Native Sign In with Google")
        }
        TextButton(
            onClick = { otpDialogState = OTPDialogState.Visible(email) }
        ) {
            Text("Login with an OTP")
        }

    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        TextButton(onClick = { signUp = !signUp }) {
            Text(if (signUp) "Already have an account? Login" else "Not registered? Register")
        }
    }

    if(otpDialogState is OTPDialogState.Visible) {
        val state = (otpDialogState as OTPDialogState.Visible)
        OTPDialog(
            email = state.email,
            title = state.title,
            onDismiss = { otpDialogState = OTPDialogState.Invisible },
            onConfirm = { email, code ->
                viewModel.loginWithOTP(email, code, state.resetFlow)
            }
        )
    }

    if(showPasswordRecoveryDialog) {
        PasswordRecoveryDialog(
            onDismiss = { showPasswordRecoveryDialog = false },
            onConfirm = { email ->
                viewModel.resetPassword(email)
                otpDialogState = OTPDialogState.Visible(title = "Password recovery", email = email, resetFlow = true)
            }
        )
    }

    if(loginAlert != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.alert.value = null
            },
            text = {
                Text(loginAlert!!)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.alert.value = null
                }) {
                    Text("Ok")
                }
            }
        )
    }
}

fun authenticate(signUp: Boolean, viewModel: ChatViewModel, email: String, password: String) {
    if (signUp) {
        viewModel.signUp(email, password)
    } else {
        viewModel.login(email, password)
    }
}