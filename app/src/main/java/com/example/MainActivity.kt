package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SiteMemberDocument
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.CorporateLoginSession
import com.example.ui.auth.loadSavedCorporateSession
import com.example.ui.auth.saveCorporateSession
import com.example.ui.screens.MainFeatureScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var localCorporateSession by remember {
        mutableStateOf(loadSavedCorporateSession(context))
    }

    LaunchedEffect(currentUser) {
        if (com.example.ui.auth.isExplicitlySignedOut(context)) {
            return@LaunchedEffect
        }
        val saved = loadSavedCorporateSession(context)
        if (saved != null) {
            localCorporateSession = saved
            return@LaunchedEffect
        }
        val fbUser = currentUser
        if (fbUser != null) {
            val email = fbUser.email?.trim()?.lowercase() ?: ""
            val canonicalId = SiteMemberDocument.canonicalMemberId(fbUser.uid, email)
            val resolvedSession = CorporateLoginSession(
                userId = canonicalId,
                email = email,
                fullName = fbUser.displayName?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore("@").ifBlank { "Сотрудник участка" },
                isHeadActivated = SiteMemberDocument.isRootHeadEmail(email)
            )
            saveCorporateSession(context, resolvedSession)
            localCorporateSession = resolvedSession
        }
    }

    val activeSession = localCorporateSession

    if (activeSession == null) {
        AuthScreen(
            onAuthSuccess = { newSession ->
                saveCorporateSession(context, newSession)
                localCorporateSession = newSession
            }
        )
    } else {
        key(activeSession.userId, activeSession.email, activeSession.isHeadActivated) {
            MainFeatureScreen(
                session = activeSession,
                onSignOutComplete = {
                    localCorporateSession = null
                }
            )
        }
    }
}
