package io.github.jan.supabase.common


import co.touchlab.kermit.Logger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.common.net.AuthApi
import io.github.jan.supabase.common.net.Message
import io.github.jan.supabase.common.net.MessageApi
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

expect open class MPViewModel() {

    val coroutineScope: CoroutineScope

}
class ChatViewModel(
    val supabaseClient: SupabaseClient,
    private val messageApi: MessageApi,
    private val authApi: AuthApi
) : MPViewModel() {

    val sessionStatus = authApi.sessionStatus().stateIn(coroutineScope, SharingStarted.Eagerly, SessionStatus.NotAuthenticated(false))
    val alert = MutableStateFlow<String?>(null)
    val messages = MutableStateFlow<List<Message>>(emptyList())
    val passwordReset = MutableStateFlow<Boolean>(false)

    //Auth

    fun signUp(email: String, password: String) {
        coroutineScope.launch {
            kotlin.runCatching {
                authApi.signUp(email, password)
            }.onSuccess {
                alert.value = "Successfully registered! Check your E-Mail to verify your account."
            }.onFailure {
                alert.value = "There was an error while registering: ${it.message}"
            }
        }
    }

    fun login(email: String, password: String) {
        coroutineScope.launch {
            kotlin.runCatching {
                authApi.signIn(email, password)
            }.onFailure {
                it.printStackTrace()
                alert.value = "There was an error while logging in. Check your credentials and try again."
            }
        }
    }

    fun loginWithGoogle() {
        coroutineScope.launch {
            kotlin.runCatching {
                authApi.signInWithGoogle()
            }
        }
    }

    fun loginWithOTP(email: String, code: String, reset: Boolean) {
        coroutineScope.launch {
            kotlin.runCatching {
                authApi.verifyOtp(email, code)
            }.onSuccess {
                passwordReset.value = reset
            }.onFailure {
                alert.value = "There was an error while verifying the OTP: ${it.message}"
            }
        }
    }

    fun resetPassword(email: String) {
        coroutineScope.launch {
            kotlin.runCatching {
                authApi.resetPassword(email)
            }
        }
    }

    fun changePassword(password: String) {
        coroutineScope.launch {
            kotlin.runCatching {
                authApi.changePassword(password)
            }.onSuccess {
                alert.value = "Password changed successfully!"
            }.onFailure {
                alert.value = "There was an error while changing the password: ${it.message}"
            }
        }
    }

    fun handleSignInWithGoogleResult() {
        alert.value = "Successfully signed in with Google!"
    }

    fun verifyEmail(token: String) {
        coroutineScope.launch {
            runCatching {
                supabaseClient.functions.invoke("email-verification", body = buildJsonObject {
                    put("token", token)
                })
            }.onSuccess {
                alert.value = "Verify email successfully"
            }.onFailure {
                alert.value = "Verify email failed"
            }
        }
    }

    fun logout() {
        coroutineScope.launch {
            kotlin.runCatching {
                authApi.signOut()
                messages.value = emptyList()
            }
        }
    }

    //Realtime
    fun retrieveMessages() {
        coroutineScope.launch {
            kotlin.runCatching {
                messageApi.retrieveMessages()
                    .onEach {
                        messages.value = it
                    }
                    .launchIn(coroutineScope)
            }.onFailure {
                Logger.e(it) { "Error while retrieving messages" }
            }
        }
    }

    //Interacting with the message api
    fun createMessage(message: String) {
        coroutineScope.launch {
            kotlin.runCatching {
                messageApi.createMessage(message)
            }.onFailure {
                Logger.e(it) { "Error while creating message" }
            }
        }
    }

    fun deleteMessage(id: Int) {
        coroutineScope.launch {
            kotlin.runCatching {
                messageApi.deleteMessage(id)
            }.onFailure {
                Logger.e(it) { "Error while deleting message" }
            }
        }
    }

    fun disconnectFromRealtime() {
        coroutineScope.launch {
            kotlin.runCatching {
                supabaseClient.realtime.removeAllChannels()
            }
        }
    }

}