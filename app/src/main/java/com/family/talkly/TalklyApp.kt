package com.family.talkly

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class TalklyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ensureFirebaseInitialized()
    }

    private fun ensureFirebaseInitialized() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                if (app == null) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:688875089801:android:07f27e3cf40ca2af913b58")
                        .setGcmSenderId("688875089801")
                        .setProjectId("familycallapp-e6b21")
                        .setApiKey("AIzaSyCmmYWBqRREKmhNaBvc1drcTJib0EuMgF0")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                    Log.d("TalklyApp", "FirebaseApp initialized with fallback options")
                } else {
                    Log.d("TalklyApp", "FirebaseApp initialized successfully: ${app.name}")
                }
            } else {
                Log.d("TalklyApp", "FirebaseApp auto-initialized by Google Services ContentProvider")
            }
        } catch (e: Exception) {
            Log.e("TalklyApp", "Error during default FirebaseApp.initializeApp: ${e.message}, applying explicit fallback options")
            try {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:688875089801:android:07f27e3cf40ca2af913b58")
                    .setGcmSenderId("688875089801")
                    .setProjectId("familycallapp-e6b21")
                    .setApiKey("AIzaSyCmmYWBqRREKmhNaBvc1drcTJib0EuMgF0")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("TalklyApp", "FirebaseApp initialized with explicit fallback options")
            } catch (ex: Exception) {
                Log.e("TalklyApp", "Critical: Failed to initialize FirebaseApp with fallback: ${ex.message}", ex)
            }
        }
    }
}
