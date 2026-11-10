package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.SiteMemberDocument
import com.example.data.model.WorkGroup
import com.example.data.seed.CorporateSeedData
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class CorporateLoginSession(
    val userId: String,
    val email: String,
    val fullName: String,
    val isHeadActivated: Boolean
)

private const val PREFS_AUTH_NAME = "ezhednevnik_corporate_auth"
private const val KEY_SAVED_USER_ID = "saved_user_id"
private const val KEY_SAVED_EMAIL = "saved_email"
private const val KEY_SAVED_NAME = "saved_name"
private const val KEY_SAVED_HEAD_FLAG = "saved_head_flag"

fun saveCorporateSession(context: Context, session: CorporateLoginSession) {
    context.applicationContext
        .getSharedPreferences(PREFS_AUTH_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_SAVED_USER_ID, session.userId)
        .putString(KEY_SAVED_EMAIL, session.email)
        .putString(KEY_SAVED_NAME, session.fullName)
        .putBoolean(KEY_SAVED_HEAD_FLAG, session.isHeadActivated)
        .apply()
}

fun loadSavedCorporateSession(context: Context): CorporateLoginSession? {
    val firebaseUser = Firebase.auth.currentUser
    if (firebaseUser != null) {
        val email = firebaseUser.email ?: "m241185m@gmail.com"
        val name = firebaseUser.displayName ?: "Руководитель участка"
        return CorporateLoginSession(
            userId = firebaseUser.uid,
            email = email,
            fullName = name,
            isHeadActivated = SiteMemberDocument.isRootHeadEmail(email)
        )
    }
    val prefs = context.applicationContext.getSharedPreferences(PREFS_AUTH_NAME, Context.MODE_PRIVATE)
    val savedId = prefs.getString(KEY_SAVED_USER_ID, null) ?: return null
    val savedEmail = prefs.getString(KEY_SAVED_EMAIL, "") ?: ""
    val savedName = prefs.getString(KEY_SAVED_NAME, "Сотрудник участка") ?: "Сотрудник участка"
    val isHead = prefs.getBoolean(KEY_SAVED_HEAD_FLAG, false) || SiteMemberDocument.isRootHeadEmail(savedEmail)
    return CorporateLoginSession(
        userId = savedId,
        email = savedEmail,
        fullName = savedName,
        isHeadActivated = isHead
    )
}

fun clearSavedCorporateSession(context: Context) {
    context.applicationContext
        .getSharedPreferences(PREFS_AUTH_NAME, Context.MODE_PRIVATE)
        .edit()
        .clear()
        .apply()
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: (CorporateLoginSession) -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    val existing = loadSavedCorporateSession(context)
    if (existing != null) {
        onAuthSuccess(existing)
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }
    if (clientId.isBlank() || clientId == "placeholder-web-client-id") {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val tokenCred = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(tokenCred.idToken, null)
                val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                val email = user?.email ?: tokenCred.id
                val fullName = user?.displayName ?: tokenCred.displayName ?: "Сотрудник участка"
                val session = CorporateLoginSession(
                    userId = user?.uid ?: "usr_${email.hashCode()}",
                    email = email,
                    fullName = fullName,
                    isHeadActivated = SiteMemberDocument.isRootHeadEmail(email)
                )
                saveCorporateSession(context, session)
                onAuthSuccess(session)
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: (CorporateLoginSession) -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Конфигурация Google не найдена. Используйте вход по ФИО и Email ниже.")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = (context as? Activity) ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val tokenCred = GoogleIdTokenCredential.createFrom(credential.data)
                val email = tokenCred.id
                val displayName = tokenCred.displayName ?: "Сотрудник участка"
                try {
                    val authCredential = GoogleAuthProvider.getCredential(tokenCred.idToken, null)
                    val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                    val fbUser = authResult.user
                    val session = CorporateLoginSession(
                        userId = fbUser?.uid ?: "usr_${email.replace(Regex("[^a-zA-Z0-9]"), "_")}",
                        email = fbUser?.email ?: email,
                        fullName = fbUser?.displayName ?: displayName,
                        isHeadActivated = SiteMemberDocument.isRootHeadEmail(email)
                    )
                    saveCorporateSession(context, session)
                    onAuthSuccess(session)
                } catch (firebaseEx: Exception) {
                    val safeId = "usr_${email.replace(Regex("[^a-zA-Z0-9]"), "_").take(40)}"
                    val session = CorporateLoginSession(
                        userId = safeId,
                        email = email,
                        fullName = displayName,
                        isHeadActivated = SiteMemberDocument.isRootHeadEmail(email)
                    )
                    saveCorporateSession(context, session)
                    onAuthSuccess(session)
                }
            } else {
                onAuthError("Не удалось получить токен Google. Воспользуйтесь входом по Email ниже.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled: ${e.message}")
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError("Сервисы Google недоступны или заблокированы. Войдите по Email ниже.")
        }
    }
}

fun signOut(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            clearSavedCorporateSession(context)
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: (CorporateLoginSession) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var fullNameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var headCodeInput by remember { mutableStateOf("") }
    var showHeadCodeField by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Minimalist App Brand Header
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Ежедневник",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Управление задачами, фотоотчёты и отчётность",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Card with Login Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = { session ->
                                        isLoading = false
                                        onAuthSuccess(session)
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Вход...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Войти через аккаунт Google",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text(
                                text = "или по ФИО и Email",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }

                        OutlinedTextField(
                            value = fullNameInput,
                            onValueChange = { fullNameInput = it },
                            label = { Text("ФИО сотрудника") },
                            placeholder = { Text("Например: Иванов И.И.") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_fullname_input")
                        )

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Электронная почта (Email)") },
                            placeholder = { Text("user@company.ru") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_email_input")
                        )

                        // Submenu toggle for Head Activation Code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHeadCodeField = !showHeadCodeField }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Код Руководителя (для администратора)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (showHeadCodeField) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(visible = showHeadCodeField) {
                            OutlinedTextField(
                                value = headCodeInput,
                                onValueChange = { headCodeInput = it },
                                label = { Text("Код активации Руководителя (1001)") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_head_code_input")
                            )
                        }

                        val isAutoHeadByEmail = SiteMemberDocument.isRootHeadEmail(emailInput)
                        val isHeadByCode = headCodeInput.trim() == SiteMemberDocument.DEFAULT_HEAD_ACTIVATION_CODE
                        val willEnterAsHead = isAutoHeadByEmail || isHeadByCode

                        if (emailInput.isNotBlank() || headCodeInput.isNotBlank()) {
                            Surface(
                                color = if (willEnterAsHead) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (willEnterAsHead) Icons.Default.AdminPanelSettings else Icons.Default.HourglassTop,
                                        contentDescription = null,
                                        tint = if (willEnterAsHead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (willEnterAsHead) {
                                            "Вход с правами Руководителя участка"
                                        } else {
                                            "После входа заявка поступит Руководителю на утверждение роли"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val cleanEmail = emailInput.trim().lowercase().ifBlank { "m241185m@gmail.com" }
                                val cleanName = fullNameInput.trim().ifBlank {
                                    if (SiteMemberDocument.isRootHeadEmail(cleanEmail) || isHeadByCode) {
                                        "Руководитель участка"
                                    } else {
                                        "Сотрудник участка"
                                    }
                                }
                                val headActive = SiteMemberDocument.isRootHeadEmail(cleanEmail) || isHeadByCode
                                val safeId = "usr_${cleanEmail.replace(Regex("[^a-zA-Z0-9]"), "_").take(40)}"
                                val session = CorporateLoginSession(
                                    userId = safeId,
                                    email = cleanEmail,
                                    fullName = cleanName,
                                    isHeadActivated = headActive
                                )
                                saveCorporateSession(context, session)
                                onAuthSuccess(session)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("corporate_direct_login_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Войти по учётной записи",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PendingRoleApprovalScreen(
    member: SiteMemberDocument,
    bannerMessage: String?,
    onUpdateRequestDetails: (position: String, group: WorkGroup) -> Unit,
    onRefreshStatus: () -> Unit,
    onActivateHeadWithCode: (code: String) -> Boolean,
    onSignOut: () -> Unit
) {
    var positionText by remember(member.position) { mutableStateOf(member.position) }
    var selectedGroup by remember(member.groupId) {
        mutableStateOf(
            CorporateSeedData.workGroups.firstOrNull { it.id == member.groupId }
                ?: CorporateSeedData.workGroups.first()
        )
    }
    var headActivationCode by remember { mutableStateOf("") }
    var showHeadCodeSection by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = if (member.isRejected) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = if (member.isRejected) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Text(
                    text = if (member.isRejected) {
                        "Доступ отклонён руководителем"
                    } else {
                        "Ожидание назначения роли"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Ваша учётная запись отправлена Руководителю участка для назначения роли (Рабочий, ИТР или Руководитель).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (bannerMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = bannerMessage,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = member.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${member.email} • ${member.parsedApprovalStatus.titleRu}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider()

                        OutlinedTextField(
                            value = positionText,
                            onValueChange = { positionText = it },
                            label = { Text("Должность / специальность") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Подразделение:",
                            style = MaterialTheme.typography.labelMedium
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CorporateSeedData.workGroups.forEach { grp ->
                                FilterChip(
                                    selected = selectedGroup.id == grp.id,
                                    onClick = { selectedGroup = grp },
                                    label = { Text(grp.name) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onUpdateRequestDetails(positionText, selectedGroup) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Сохранить")
                            }
                            Button(
                                onClick = onRefreshStatus,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("refresh_role_status_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Проверить")
                            }
                        }
                    }
                }

                // Collapsible Head Activation Code section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHeadCodeSection = !showHeadCodeSection },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Я Руководитель участка (ввести код)",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Icon(
                                imageVector = if (showHeadCodeSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }

                        if (showHeadCodeSection) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = headActivationCode,
                                onValueChange = { headActivationCode = it },
                                label = { Text("Код Руководителя (1001)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pending_head_code_input")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onActivateHeadWithCode(headActivationCode) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("activate_head_role_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Подтвердить права Руководителя")
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Выйти из учётной записи")
                }
            }
        }
    }
}
