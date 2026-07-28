package com.family.talkly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.family.talkly.data.auth.AuthManager
import com.family.talkly.data.auth.AuthState
import com.family.talkly.data.firebase.FirebaseChatRepository
import com.family.talkly.data.zego.ZegoCallEngineManager
import com.family.talkly.ui.components.AuthLoadingState
import com.family.talkly.ui.screens.MainScreen
import com.family.talkly.ui.screens.auth.PhonePasswordAuthScreen
import com.family.talkly.ui.screens.auth.ProfileSetupScreen
import com.family.talkly.ui.theme.TalklyTheme
import com.family.talkly.ui.theme.ThemePreferences
import com.family.talkly.ui.theme.WhatsappGreen
import com.family.talkly.ui.theme.WhatsappTeal
import com.family.talkly.workers.DeleteExpiredMessagesWorker
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var chatRepository: FirebaseChatRepository
    private lateinit var zegoManager: ZegoCallEngineManager
    private lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            if (FirebaseApp.getApps(applicationContext).isEmpty()) {
                FirebaseApp.initializeApp(applicationContext)
                android.util.Log.d("MainActivity", "FirebaseApp initialized in MainActivity.onCreate")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "FirebaseApp initialization check in MainActivity: ${e.localizedMessage}")
        }

        authManager = AuthManager(applicationContext)
        chatRepository = FirebaseChatRepository(applicationContext)
        zegoManager = ZegoCallEngineManager(applicationContext)
        themePreferences = ThemePreferences(applicationContext)

        // Schedule WorkManager job for deleting expired Firestore messages (>48 hours old)
        DeleteExpiredMessagesWorker.schedulePeriodicCleanup(applicationContext)

        setContent {
            val currentThemeMode by themePreferences.themeMode.collectAsState()

            TalklyTheme(themeMode = currentThemeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authState by authManager.authState.collectAsState()

                    when (val state = authState) {
                        is AuthState.InitialCheck -> {
                            AuthLoadingState(
                                message = "Talkly Family Messenger",
                                subMessage = "Checking authentication session..."
                            )
                        }

                        is AuthState.Unauthenticated -> {
                            PhonePasswordAuthScreen(
                                isLoading = false,
                                errorMessage = null,
                                onSignIn = { phone, password ->
                                    authManager.signInWithPhoneAndPassword(phone, password)
                                },
                                onSignUp = { phone, password, name ->
                                    authManager.signUpWithPhoneAndPassword(
                                        phoneNumber = phone,
                                        password = password,
                                        name = name
                                    )
                                },
                                onForgotPassword = { phone, onSuccess, onError ->
                                    authManager.sendPasswordResetForPhone(
                                        phoneNumber = phone,
                                        onSuccess = onSuccess,
                                        onError = onError
                                    )
                                },
                                onClearError = {
                                    authManager.clearError()
                                }
                            )
                        }

                        is AuthState.VerificationInProgress -> {
                            AuthLoadingState(
                                message = state.message,
                                subMessage = "Please wait a moment while we process your request"
                            )
                        }

                        is AuthState.ProfileSetupRequired -> {
                            ProfileSetupScreen(
                                phoneNumber = state.phoneNumber,
                                isLoading = false,
                                errorMessage = null,
                                onSaveProfile = { name, picUrl ->
                                    authManager.saveUserProfile(
                                        name = name,
                                        profilePicUrl = picUrl,
                                        onSuccess = {},
                                        onError = {}
                                    )
                                }
                            )
                        }

                        is AuthState.Authenticated -> {
                            MainScreen(
                                chatRepository = chatRepository,
                                zegoManager = zegoManager,
                                currentUserProfile = state.profile,
                                currentThemeMode = currentThemeMode,
                                onThemeModeChange = { mode ->
                                    themePreferences.setThemeMode(mode)
                                },
                                onLogout = {
                                    authManager.logout()
                                },
                                onSaveProfile = { name, bio, picUrl ->
                                    authManager.saveUserProfile(
                                        name = name,
                                        profilePicUrl = picUrl,
                                        bio = bio
                                    )
                                }
                            )
                        }

                        is AuthState.Error -> {
                            PhonePasswordAuthScreen(
                                isLoading = false,
                                errorMessage = state.message,
                                onSignIn = { phone, password ->
                                    authManager.signInWithPhoneAndPassword(phone, password)
                                },
                                onSignUp = { phone, password, name ->
                                    authManager.signUpWithPhoneAndPassword(
                                        phoneNumber = phone,
                                        password = password,
                                        name = name
                                    )
                                },
                                onForgotPassword = { phone, onSuccess, onError ->
                                    authManager.sendPasswordResetForPhone(
                                        phoneNumber = phone,
                                        onSuccess = onSuccess,
                                        onError = onError
                                    )
                                },
                                onClearError = {
                                    authManager.clearError()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
