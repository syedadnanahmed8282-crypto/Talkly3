package com.family.talkly.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.family.talkly.data.models.UserProfile
import com.family.talkly.util.PhoneUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
    object InitialCheck : AuthState()
    object Unauthenticated : AuthState()
    data class VerificationInProgress(val message: String = "Authenticating...") : AuthState()
    data class ProfileSetupRequired(val uid: String, val phoneNumber: String) : AuthState()
    data class Authenticated(val profile: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthManager(private val context: Context) {

    companion object {
        private const val TAG = "AuthManager"
        private const val PREFS_NAME = "talkly_auth_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_UID = "user_uid"
        private const val KEY_NAME = "user_name"
        private const val KEY_PHONE = "user_phone"
        private const val KEY_PROFILE_PIC = "user_profile_pic"
        private const val KEY_BIO = "user_bio"

        /**
         * Converts phone number into a deterministic internal email address for Firebase Auth
         */
        fun getInternalEmail(phoneNumber: String): String {
            val cleanNumber = phoneNumber.replace("+", "").replace(" ", "").replace("-", "").trim()
            return "${cleanNumber}@talkly.app"
        }
    }

    private fun ensureFirebase() {
        if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
            try {
                com.google.firebase.FirebaseApp.initializeApp(context)
            } catch (e: Exception) {
                try {
                    val options = com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:688875089801:android:07f27e3cf40ca2af913b58")
                        .setGcmSenderId("688875089801")
                        .setProjectId("familycallapp-e6b21")
                        .setApiKey("AIzaSyCmmYWBqRREKmhNaBvc1drcTJib0EuMgF0")
                        .build()
                    com.google.firebase.FirebaseApp.initializeApp(context, options)
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed fallback Firebase init in AuthManager: ${ex.message}")
                }
            }
        }
    }

    private fun getFirebaseAuth(): FirebaseAuth {
        ensureFirebase()
        return FirebaseAuth.getInstance()
    }

    private fun getFirestore(): FirebaseFirestore {
        ensureFirebase()
        return FirebaseFirestore.getInstance()
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.InitialCheck)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentSession()
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    /**
     * Checks local session and Firebase Auth current user to resume session
     */
    fun checkCurrentSession() {
        try {
            val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
            val savedUid = prefs.getString(KEY_UID, null)
            val firebaseUser = try { getFirebaseAuth().currentUser } catch (e: Exception) { null }

            if (isLoggedIn && !savedUid.isNullOrEmpty()) {
                val name = prefs.getString(KEY_NAME, "") ?: ""
                val phone = prefs.getString(KEY_PHONE, "") ?: ""
                val pic = prefs.getString(KEY_PROFILE_PIC, "") ?: ""
                val bio = prefs.getString(KEY_BIO, "Available on Talkly 💬") ?: "Available on Talkly 💬"

                if (name.isNotBlank()) {
                    val profile = UserProfile(
                        uid = savedUid,
                        name = name,
                        phoneNumber = phone,
                        profilePicUrl = pic,
                        bio = bio
                    )
                    _authState.value = AuthState.Authenticated(profile)
                } else {
                    _authState.value = AuthState.ProfileSetupRequired(savedUid, phone)
                }
            } else if (firebaseUser != null) {
                val uid = firebaseUser.uid
                val phone = firebaseUser.phoneNumber ?: prefs.getString(KEY_PHONE, "") ?: ""
                checkUserProfileInFirestore(uid, phone)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking current session: ${e.message}")
            _authState.value = AuthState.Unauthenticated
        }
    }

    /**
     * Registers a new user with Mobile Phone Number and Password
     */
    fun signUpWithPhoneAndPassword(
        phoneNumber: String,
        password: String,
        name: String,
        profilePicUrl: String = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&auto=format&fit=crop",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (phoneNumber.isBlank() || password.isBlank() || name.isBlank()) {
            val err = "Please enter your name, phone number, and password."
            _authState.value = AuthState.Error(err)
            onError(err)
            return
        }

        if (password.length < 6) {
            val err = "Password must be at least 6 characters long."
            _authState.value = AuthState.Error(err)
            onError(err)
            return
        }

        val internalEmail = getInternalEmail(phoneNumber)
        _authState.value = AuthState.VerificationInProgress("Creating user account...")

        try {
            getFirebaseAuth().createUserWithEmailAndPassword(internalEmail, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        val uid = user?.uid
                        if (uid != null) {
                            Log.d(TAG, "Registration successful for phone $phoneNumber ($internalEmail), UID: $uid")
                            saveUserProfileAndAuthenticate(uid, name, phoneNumber, profilePicUrl, onSuccess, onError)
                        } else {
                            val err = "Account created, but user session was null."
                            _authState.value = AuthState.Error(err)
                            onError(err)
                        }
                    } else {
                        val rawErr = task.exception?.message ?: "Registration failed."
                        val formatted = when {
                            rawErr.contains("already in use", ignoreCase = true) ||
                            rawErr.contains("email-already-in-use", ignoreCase = true) ->
                                "An account with this phone number already exists. Please sign in instead."
                            rawErr.contains("badly formatted", ignoreCase = true) ||
                            rawErr.contains("invalid-email", ignoreCase = true) ->
                                "Invalid mobile phone number format."
                            rawErr.contains("weak-password", ignoreCase = true) ->
                                "Password is too weak. Please use at least 6 characters."
                            else -> rawErr
                        }
                        _authState.value = AuthState.Error(formatted)
                        onError(formatted)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Sign up exception: ${e.localizedMessage}", e)
            val err = e.localizedMessage ?: "Registration error. Please try again."
            _authState.value = AuthState.Error(err)
            onError(err)
        }
    }

    /**
     * Signs in an existing user with Mobile Phone Number and Password
     */
    fun signInWithPhoneAndPassword(
        phoneNumber: String,
        password: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (phoneNumber.isBlank() || password.isBlank()) {
            val err = "Please enter both mobile phone number and password."
            _authState.value = AuthState.Error(err)
            onError(err)
            return
        }

        val internalEmail = getInternalEmail(phoneNumber)
        _authState.value = AuthState.VerificationInProgress("Signing in...")

        try {
            getFirebaseAuth().signInWithEmailAndPassword(internalEmail, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        val uid = user?.uid
                        if (uid != null) {
                            Log.d(TAG, "Sign in successful for phone $phoneNumber ($internalEmail), UID: $uid")
                            checkUserProfileInFirestore(uid, phoneNumber)
                            onSuccess()
                        } else {
                            val err = "Authentication succeeded but user session is null."
                            _authState.value = AuthState.Error(err)
                            onError(err)
                        }
                    } else {
                        val rawErr = task.exception?.message ?: "Authentication failed."
                        val formatted = when {
                            rawErr.contains("no user record", ignoreCase = true) ||
                            rawErr.contains("user-not-found", ignoreCase = true) ->
                                "No account found with this phone number. Please register first."
                            rawErr.contains("invalid-credential", ignoreCase = true) ||
                            rawErr.contains("wrong-password", ignoreCase = true) ||
                            rawErr.contains("invalid password", ignoreCase = true) ->
                                "Incorrect password. Please try again."
                            else -> rawErr
                        }
                        _authState.value = AuthState.Error(formatted)
                        onError(formatted)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in exception: ${e.localizedMessage}", e)
            val err = e.localizedMessage ?: "Sign in error. Please try again."
            _authState.value = AuthState.Error(err)
            onError(err)
        }
    }

    /**
     * Triggers Firebase sendPasswordResetEmail using mapped internal email address for phone number
     */
    fun sendPasswordResetForPhone(
        phoneNumber: String,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (phoneNumber.isBlank()) {
            val err = "Please enter your mobile phone number."
            onError(err)
            return
        }

        val internalEmail = getInternalEmail(phoneNumber)

        try {
            getFirebaseAuth().sendPasswordResetEmail(internalEmail)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Password reset email sent to internal email: $internalEmail for phone $phoneNumber")
                        onSuccess("Password reset instructions sent for account linked to $phoneNumber.")
                    } else {
                        val rawErr = task.exception?.message ?: "Password reset failed."
                        val formatted = when {
                            rawErr.contains("no user record", ignoreCase = true) ||
                            rawErr.contains("user-not-found", ignoreCase = true) ->
                                "No registered account found with mobile number $phoneNumber."
                            rawErr.contains("invalid-email", ignoreCase = true) ->
                                "Invalid phone number format."
                            else -> rawErr
                        }
                        onError(formatted)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Password reset exception: ${e.localizedMessage}", e)
            onError(e.localizedMessage ?: "Failed to request password reset. Please try again.")
        }
    }

    private fun saveUserProfileAndAuthenticate(
        uid: String,
        name: String,
        phoneNumber: String,
        profilePicUrl: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val bio = "Available on Talkly 💬"
        val phoneSuffix = PhoneUtils.extractPhoneSuffix(phoneNumber)
        saveLocalSession(uid, name, phoneNumber, profilePicUrl, bio)
        val profile = UserProfile(
            uid = uid,
            name = name,
            phoneNumber = phoneNumber,
            phoneSuffix = phoneSuffix,
            profilePicUrl = profilePicUrl,
            bio = bio
        )
        _authState.value = AuthState.Authenticated(profile)
        onSuccess()

        val profileMap = mapOf(
            "uid" to uid,
            "name" to name,
            "phoneNumber" to phoneNumber,
            "phoneSuffix" to phoneSuffix,
            "email" to getInternalEmail(phoneNumber),
            "profilePicUrl" to profilePicUrl,
            "bio" to bio,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        try {
            getFirestore().collection("users").document(uid)
                .set(profileMap, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "User profile saved to Firestore successfully")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to write user profile to Firestore: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore write exception: ${e.localizedMessage}")
        }
    }

    /**
     * Checks Firestore 'users/{uid}' collection to see if user has completed profile setup
     */
    private fun checkUserProfileInFirestore(uid: String, phoneNumber: String) {
        // Upsert user phone and suffix on login
        val digitsOnly = phoneNumber.filter { it.isDigit() }
        val suffix = if (digitsOnly.length >= 10) digitsOnly.takeLast(10) else digitsOnly

        val userData = mapOf(
            "uid" to uid,
            "phoneNumber" to phoneNumber,
            "phoneSuffix" to suffix,
            "updatedAt" to System.currentTimeMillis()
        )

        try {
            getFirestore().collection("users").document(uid)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Upserted login phone info for uid $uid (phone: $phoneNumber, suffix: $suffix)")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed upserting login phone info: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Login upsert exception: ${e.localizedMessage}")
        }

        try {
            val db = getFirestore()
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists() && !doc.getString("name").isNullOrBlank()) {
                        val name = doc.getString("name") ?: ""
                        val phone = doc.getString("phoneNumber") ?: phoneNumber
                        val docSuffix = doc.getString("phoneSuffix") ?: PhoneUtils.extractPhoneSuffix(phone)
                        val pic = doc.getString("profilePicUrl") ?: ""
                        val bio = doc.getString("bio") ?: "Available on Talkly 💬"
                        val profile = UserProfile(
                            uid = uid,
                            name = name,
                            phoneNumber = phone,
                            phoneSuffix = docSuffix,
                            profilePicUrl = pic,
                            bio = bio
                        )
                        saveLocalSession(uid, name, phone, pic, bio)
                        _authState.value = AuthState.Authenticated(profile)
                    } else {
                        saveLocalSession(uid, "", phoneNumber, "", "")
                        _authState.value = AuthState.ProfileSetupRequired(uid, phoneNumber)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Firestore user profile read failed: ${e.localizedMessage}")
                    saveLocalSession(uid, "", phoneNumber, "", "")
                    _authState.value = AuthState.ProfileSetupRequired(uid, phoneNumber)
                }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore user profile exception: ${e.localizedMessage}")
            saveLocalSession(uid, "", phoneNumber, "", "")
            _authState.value = AuthState.ProfileSetupRequired(uid, phoneNumber)
        }
    }

    /**
     * Saves name, bio and profile picture to Firestore 'users' collection and local session
     */
    fun saveUserProfile(
        name: String,
        profilePicUrl: String,
        bio: String = "Available on Talkly 💬",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentState = _authState.value
        var uid = ""
        var phone = ""

        if (currentState is AuthState.ProfileSetupRequired) {
            uid = currentState.uid
            phone = currentState.phoneNumber
        } else {
            uid = prefs.getString(KEY_UID, "") ?: ""
            phone = prefs.getString(KEY_PHONE, "") ?: ""
        }

        if (uid.isBlank()) {
            val err = "User session invalid. Please sign in again."
            _authState.value = AuthState.Error(err)
            onError(err)
            return
        }

        val phoneSuffix = PhoneUtils.extractPhoneSuffix(phone)
        saveLocalSession(uid, name, phone, profilePicUrl, bio)
        val profile = UserProfile(
            uid = uid,
            name = name,
            phoneNumber = phone,
            phoneSuffix = phoneSuffix,
            profilePicUrl = profilePicUrl,
            bio = bio
        )
        _authState.value = AuthState.Authenticated(profile)
        onSuccess()

        val profileMap = mapOf(
            "uid" to uid,
            "name" to name,
            "phoneNumber" to phone,
            "phoneSuffix" to phoneSuffix,
            "email" to getInternalEmail(phone),
            "profilePicUrl" to profilePicUrl,
            "bio" to bio,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        try {
            getFirestore().collection("users").document(uid)
                .set(profileMap, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Saved user profile to Firestore successfully")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Firestore user profile write failed: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore user save exception: ${e.localizedMessage}")
        }
    }

    private fun saveLocalSession(uid: String, name: String, phone: String, pic: String, bio: String = "Available on Talkly 💬") {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_UID, uid)
            .putString(KEY_NAME, name)
            .putString(KEY_PHONE, phone)
            .putString(KEY_PROFILE_PIC, pic)
            .putString(KEY_BIO, bio)
            .apply()
    }

    fun logout() {
        try {
            getFirebaseAuth().signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Sign out exception: ${e.localizedMessage}")
        }
        prefs.edit().clear().apply()
        _authState.value = AuthState.Unauthenticated
    }
}
