package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.signOut
import com.example.ui.navigation.LkStoreMainShell
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.LkStoreTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LkStoreTheme {
                LkStoreRoot()
            }
        }
    }
}

@Composable
fun LkStoreRoot() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {}
        )
    } else {
        key(user.uid) {
            LkStoreMainShell(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: "LK Store User",
                userEmail = user.email,
                userPhotoUrl = user.photoUrl?.toString(),
                onSignOut = {
                    signOut(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}
