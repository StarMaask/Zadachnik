package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
        val name = firebaseUser.displayName ?: "Ковалёв Дмитрий Сергеевич"
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
                    // Even if VPN interrupts Firebase token exchange, sign in with the verified Google Credential!
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
                onAuthError("Не удалось получить токен Google. Воспользуйтесь корпоративным входом ниже.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or interrupted: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError("Сервисы Google недоступны или заблокированы VPN. Войдите по Email ниже.")
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

    var fullNameInput by remember { mutableStateOf("Максим Старшинов") }
    var emailInput by remember { mutableStateOf("m241185m@gmail.com") }
    var headCodeInput by remember { mutableStateOf("") }
    var showCorporateLoginCard by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1D30),
                        Color(0xFF0F2942),
                        Color(0xFF163A5C)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_site_banner_1794330702001),
                        contentDescription = "Панорама управления строительным участком",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0xE60A1D30)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Surface(
                            color = Color(0xFFF59E0B),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "КОРПОРАТИВНЫЙ КОНТРОЛЬ УЧАСТКА • РАБОТА БЕЗ СЕТИ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Корпоративный «Ежедневник»",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Постановка задач, фотоотчёты с камеры, приёмка работ и формирование отчётных документов (PDF, XLSX, DOCX, CSV)",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD5E1)
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

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
                            showCorporateLoginCard = true
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                            errorMessage = "Окно выбора аккаунта закрыто. Вы можете войти одним нажатием через форму ниже."
                            showCorporateLoginCard = true
                        }
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_in_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF59E0B),
                    contentColor = Color(0xFF0F172A)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color(0xFF0F172A),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Проверка корпоративного доступа...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Вход через Google"
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Войти через аккаунт Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Прямой корпоративный вход (по Email и ФИО + определение Руководителя)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15324E)),
                border = BorderStroke(1.dp, Color(0xFF28527A))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCorporateLoginCard = !showCorporateLoginCard }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = "Корпоративный вход",
                                tint = Color(0xFFF59E0B)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Вход по учётной записи участка",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Руководитель определяется по Email или коду 1001, остальные ожидают назначения роли",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    if (showCorporateLoginCard) {
                        HorizontalDivider(color = Color(0xFF28527A))

                        OutlinedTextField(
                            value = fullNameInput,
                            onValueChange = { fullNameInput = it },
                            label = { Text("ФИО сотрудника", color = Color(0xFFCBD5E1)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_fullname_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Электронная почта (Email)", color = Color(0xFFCBD5E1)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_email_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )

                        OutlinedTextField(
                            value = headCodeInput,
                            onValueChange = { headCodeInput = it },
                            label = {
                                Text(
                                    "Код Руководителя (необязательно, по умолчанию 1001)",
                                    color = Color(0xFFCBD5E1)
                                )
                            },
                            placeholder = {
                                Text(
                                    "Оставьте пустым для входа как сотрудник",
                                    color = Color(0xFF64748B)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_head_code_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )

                        val isAutoHeadByEmail = SiteMemberDocument.isRootHeadEmail(emailInput)
                        val isHeadByCode = headCodeInput.trim() == SiteMemberDocument.DEFAULT_HEAD_ACTIVATION_CODE
                        val willEnterAsHead = isAutoHeadByEmail || isHeadByCode

                        Surface(
                            color = if (willEnterAsHead) Color(0xFF064E3B) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (willEnterAsHead) Icons.Default.AdminPanelSettings else Icons.Default.HourglassTop,
                                    contentDescription = "Статус распознавания",
                                    tint = if (willEnterAsHead) Color(0xFF34D399) else Color(0xFFFBBF24),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (willEnterAsHead) {
                                        "Система определила вас как Руководителя участка (полный доступ)"
                                    } else {
                                        "Новый пользователь: после входа откроется ожидание назначения роли руководителем"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val cleanEmail = emailInput.trim().lowercase().ifBlank { "user@site1.ru" }
                                val cleanName = fullNameInput.trim().ifBlank { "Сотрудник участка" }
                                val safeId = "usr_${cleanEmail.replace(Regex("[^a-zA-Z0-9]"), "_").take(40)}"
                                val session = CorporateLoginSession(
                                    userId = safeId,
                                    email = cleanEmail,
                                    fullName = cleanName,
                                    isHeadActivated = willEnterAsHead
                                )
                                saveCorporateSession(context, session)
                                onAuthSuccess(session)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("corporate_direct_login_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color(0xFF052E16)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Войти в систему"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (willEnterAsHead) {
                                    "Войти как Руководитель участка"
                                } else {
                                    "Войти и отправить заявку Руководителю"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    fullNameInput = "Максим Старшинов"
                                    emailInput = "m241185m@gmail.com"
                                    headCodeInput = "1001"
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B))
                            ) {
                                Text(
                                    text = "Профиль Руководителя",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFBBF24)
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    fullNameInput = "Светлана Старшинова"
                                    emailInput = "ritual53rus@gmail.com"
                                    headCodeInput = ""
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8))
                            ) {
                                Text(
                                    text = "Профиль Сотрудника",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15324E))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureHighlightRow(
                        icon = Icons.Default.AccountTree,
                        title = "Иерархия ролей: Руководитель → ИТР → Рабочий",
                        subtitle = "Руководитель сам назначает роли вошедшим сотрудникам и управляет доступом"
                    )
                    FeatureHighlightRow(
                        icon = Icons.Default.CameraAlt,
                        title = "Обязательный фотоотчёт и автоперенос задач",
                        subtitle = "Контроль минимального числа снимков, штамп времени и перенос на следующий день"
                    )
                    FeatureHighlightRow(
                        icon = Icons.Default.Description,
                        title = "Отчётность на 1 или 2 раздельных листах",
                        subtitle = "План работ на завтра и отчёт за смену/период в форматах PDF, XLSX, DOCX, CSV"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Биометрическая защита",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Защищённая облачная БД • Биометрия • Журнал аудита",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8)
                )
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1D30),
                        Color(0xFF0F2942),
                        Color(0xFF163A5C)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = if (member.isRejected) Color(0xFF7F1D1D) else Color(0xFFD97706),
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "Ожидание назначения роли",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = if (member.isRejected) {
                    "Доступ к участку отклонён руководителем"
                } else {
                    "Ожидание назначения роли руководителем"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Вы успешно вошли в систему «Ежедневник». Ваша учётная запись передана Руководителю участка для назначения роли (Рабочий, ИТР или Руководитель) и привязки к бригаде.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD5E1)
            )

            if (bannerMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF065F46)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = bannerMessage,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15324E)),
                border = BorderStroke(1.dp, Color(0xFF28527A))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Карточка вашей заявки на допуск",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ФИО: ${member.fullName}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Электронная почта: ${member.email}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "Текущий статус: ${member.parsedApprovalStatus.titleRu}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.SemiBold
                    )

                    HorizontalDivider(color = Color(0xFF28527A))

                    OutlinedTextField(
                        value = positionText,
                        onValueChange = { positionText = it },
                        label = { Text("Ваша должность / специальность", color = Color(0xFFCBD5E1)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )

                    Text(
                        text = "Запрашиваемая бригада / подразделение:",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFCBD5E1)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CorporateSeedData.workGroups.forEach { grp ->
                            FilterChip(
                                selected = selectedGroup.id == grp.id,
                                onClick = { selectedGroup = grp },
                                label = { Text(grp.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFF59E0B),
                                    selectedLabelColor = Color(0xFF0F172A),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onUpdateRequestDetails(positionText, selectedGroup) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF38BDF8),
                                contentColor = Color(0xFF0F172A)
                            )
                        ) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Обновить данные", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onRefreshStatus,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color(0xFF0F172A)
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Проверить статус", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Блок активации Руководителя участка на этом телефоне
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF475569))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHeadCodeSection = !showHeadCodeSection },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Активация Руководителя",
                                tint = Color(0xFF34D399)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Это телефон Руководителя участка?",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Нажмите, чтобы ввести код руководителя (1001) и управлять ролями",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    if (showHeadCodeSection) {
                        OutlinedTextField(
                            value = headActivationCode,
                            onValueChange = { headActivationCode = it },
                            label = { Text("Код активации Руководителя (по умолчанию 1001)", color = Color(0xFFCBD5E1)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pending_head_code_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF34D399),
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )
                        Button(
                            onClick = {
                                val codeToUse = headActivationCode.ifBlank { "1001" }
                                onActivateHeadWithCode(codeToUse)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("activate_head_role_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color(0xFF052E16)
                            )
                        ) {
                            Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Подтвердить статус Руководителя участка",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF64748B))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Сменить аккаунт",
                    tint = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Выйти и сменить учётную запись",
                    color = Color(0xFFCBD5E1)
                )
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = Color(0xFF1E4976),
            shape = CircleShape,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1)
            )
        }
    }
}
