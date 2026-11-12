package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.data.local.LocalWorkspaceStore
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
    val isHeadActivated: Boolean,
    val requestedSubdivision: String = "",
    val requestedSiteName: String = "",
    val requestedPosition: String = ""
)

private const val PREFS_AUTH_NAME = "ezhednevnik_corporate_auth"
private const val KEY_SAVED_USER_ID = "saved_user_id"
private const val KEY_SAVED_EMAIL = "saved_email"
private const val KEY_SAVED_NAME = "saved_name"
private const val KEY_SAVED_SUBDIVISION = "saved_subdivision"
private const val KEY_SAVED_SITE = "saved_site"
private const val KEY_SAVED_POSITION = "saved_position"
private const val KEY_SAVED_ADMIN_FLAG = "saved_admin_flag"
private const val KEY_EXPLICIT_SIGNED_OUT = "explicit_signed_out"

fun isExplicitlySignedOut(context: Context): Boolean {
    return context.applicationContext
        .getSharedPreferences(PREFS_AUTH_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_EXPLICIT_SIGNED_OUT, false)
}

fun saveCorporateSession(context: Context, session: CorporateLoginSession) {
    val cleanEmail = session.email.trim().lowercase()
    val canonicalId = SiteMemberDocument.canonicalMemberId(session.userId, cleanEmail)
    val isAdmin = session.isHeadActivated || SiteMemberDocument.isRootAdminEmail(cleanEmail)
    context.applicationContext
        .getSharedPreferences(PREFS_AUTH_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_SAVED_USER_ID, canonicalId)
        .putString(KEY_SAVED_EMAIL, cleanEmail)
        .putString(KEY_SAVED_NAME, session.fullName)
        .putString(KEY_SAVED_SUBDIVISION, session.requestedSubdivision)
        .putString(KEY_SAVED_SITE, session.requestedSiteName)
        .putString(KEY_SAVED_POSITION, session.requestedPosition)
        .putBoolean(KEY_SAVED_ADMIN_FLAG, isAdmin)
        .putBoolean(KEY_EXPLICIT_SIGNED_OUT, false)
        .apply()
}

fun loadSavedCorporateSession(context: Context): CorporateLoginSession? {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_AUTH_NAME, Context.MODE_PRIVATE)
    if (prefs.getBoolean(KEY_EXPLICIT_SIGNED_OUT, false)) {
        return null
    }
    val savedId = prefs.getString(KEY_SAVED_USER_ID, null) ?: return null
    val savedEmail = prefs.getString(KEY_SAVED_EMAIL, "")?.trim()?.lowercase().orEmpty()
    val savedSub = prefs.getString(KEY_SAVED_SUBDIVISION, "") ?: ""
    val savedSite = prefs.getString(KEY_SAVED_SITE, "") ?: ""
    val savedPos = prefs.getString(KEY_SAVED_POSITION, "") ?: ""

    val firebaseUser = Firebase.auth.currentUser
    val resolvedEmail = savedEmail.ifBlank { firebaseUser?.email?.trim()?.lowercase().orEmpty() }
    val isAdmin = if (resolvedEmail.isNotBlank()) {
        SiteMemberDocument.isRootAdminEmail(resolvedEmail)
    } else {
        prefs.getBoolean(KEY_SAVED_ADMIN_FLAG, false)
    }
    val canonicalId = SiteMemberDocument.canonicalMemberId(savedId, resolvedEmail)
    val savedName = prefs.getString(KEY_SAVED_NAME, null)
        ?: firebaseUser?.displayName
        ?: if (isAdmin) "Администратор" else "Сотрудник участка"

    return CorporateLoginSession(
        userId = canonicalId,
        email = resolvedEmail,
        fullName = savedName,
        isHeadActivated = isAdmin,
        requestedSubdivision = savedSub,
        requestedSiteName = savedSite,
        requestedPosition = savedPos
    )
}

fun clearSavedCorporateSession(context: Context) {
    context.applicationContext
        .getSharedPreferences(PREFS_AUTH_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove(KEY_SAVED_USER_ID)
        .remove(KEY_SAVED_EMAIL)
        .remove(KEY_SAVED_NAME)
        .remove(KEY_SAVED_SUBDIVISION)
        .remove(KEY_SAVED_SITE)
        .remove(KEY_SAVED_POSITION)
        .remove(KEY_SAVED_ADMIN_FLAG)
        .putBoolean(KEY_EXPLICIT_SIGNED_OUT, true)
        .commit()
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: (CorporateLoginSession) -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (isExplicitlySignedOut(context)) {
        onUnauthenticated()
        return
    }
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
                val email = (user?.email ?: tokenCred.id).trim().lowercase()
                val isAdmin = SiteMemberDocument.isRootAdminEmail(email)
                val fullName = user?.displayName ?: tokenCred.displayName
                    ?: if (isAdmin) "Администратор" else "Сотрудник участка"
                val canonicalId = SiteMemberDocument.canonicalMemberId(user?.uid ?: "", email)
                val session = CorporateLoginSession(
                    userId = canonicalId,
                    email = email,
                    fullName = fullName,
                    isHeadActivated = isAdmin
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
    selectedSubdivision: String = "",
    selectedSiteName: String = "",
    selectedPosition: String = "",
    onAuthSuccess: (CorporateLoginSession) -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Конфигурация Google не найдена. Используйте форму входа/регистрации ниже.")
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
                val email = tokenCred.id.trim().lowercase()
                val isAdmin = SiteMemberDocument.isRootAdminEmail(email)
                val displayName = tokenCred.displayName
                    ?: if (isAdmin) "Администратор" else "Сотрудник участка"
                try {
                    val authCredential = GoogleAuthProvider.getCredential(tokenCred.idToken, null)
                    val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                    val fbUser = authResult.user
                    val resolvedEmail = (fbUser?.email ?: email).trim().lowercase()
                    val resolvedAdmin = SiteMemberDocument.isRootAdminEmail(resolvedEmail)
                    val canonicalId = SiteMemberDocument.canonicalMemberId(fbUser?.uid ?: "", resolvedEmail)
                    val session = CorporateLoginSession(
                        userId = canonicalId,
                        email = resolvedEmail,
                        fullName = fbUser?.displayName ?: displayName,
                        isHeadActivated = resolvedAdmin,
                        requestedSubdivision = selectedSubdivision,
                        requestedSiteName = selectedSiteName,
                        requestedPosition = selectedPosition
                    )
                    saveCorporateSession(context, session)
                    onAuthSuccess(session)
                } catch (firebaseEx: Exception) {
                    val canonicalId = SiteMemberDocument.canonicalMemberId("", email)
                    val session = CorporateLoginSession(
                        userId = canonicalId,
                        email = email,
                        fullName = displayName,
                        isHeadActivated = isAdmin,
                        requestedSubdivision = selectedSubdivision,
                        requestedSiteName = selectedSiteName,
                        requestedPosition = selectedPosition
                    )
                    saveCorporateSession(context, session)
                    onAuthSuccess(session)
                }
            } else {
                onAuthError("Не удалось получить токен Google. Воспользуйтесь формой входа ниже.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled: ${e.message}")
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError("Сервисы Google недоступны. Воспользуйтесь формой входа по учётной записи ниже.")
        }
    }
}

fun signOut(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    clearSavedCorporateSession(context)
    onSignOutComplete()
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: (CorporateLoginSession) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val availableSubdivisions = remember(context) {
        LocalWorkspaceStore.loadSubdivisions(context)
    }
    val availableSites = remember(context) {
        LocalWorkspaceStore.loadSites(context)
    }

    var fullNameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var positionInput by remember { mutableStateOf("") }

    var selectedSubdivision by remember(availableSubdivisions) {
        mutableStateOf(availableSubdivisions.firstOrNull() ?: "Основное подразделение")
    }
    var subdivisionDropdownExpanded by remember { mutableStateOf(false) }

    var selectedSite by remember(availableSites) {
        mutableStateOf(availableSites.firstOrNull() ?: CorporateSeedData.DEFAULT_SITE_NAME)
    }
    var showSitePickerDialog by remember { mutableStateOf(false) }

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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Ежедневник",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Учёт выполнения объёмов работ, чек-листы и фотоотчёты",
                        style = MaterialTheme.typography.bodySmall,
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

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    selectedSubdivision = selectedSubdivision,
                                    selectedSiteName = selectedSite,
                                    selectedPosition = positionInput.trim(),
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
                                text = "Регистрация и вход по учётной записи",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }

                        OutlinedTextField(
                            value = fullNameInput,
                            onValueChange = { fullNameInput = it },
                            label = { Text("ФИО пользователя") },
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
                            label = { Text("Учётная запись (Email) *") },
                            placeholder = { Text("Введите вашу эл. почту") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_email_input")
                        )

                        val isRootAdmin = SiteMemberDocument.isRootAdminEmail(emailInput)

                        if (!isRootAdmin) {
                            OutlinedTextField(
                                value = positionInput,
                                onValueChange = { positionInput = it },
                                label = { Text("Должность / специальность") },
                                placeholder = { Text("Например: Монтажник, Инженер ПТО") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_position_input")
                            )

                            // Выпадающий список выбора принадлежности к подразделению
                            ExposedDropdownMenuBox(
                                expanded = subdivisionDropdownExpanded,
                                onExpandedChange = { subdivisionDropdownExpanded = !subdivisionDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedSubdivision,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Принадлежность к подразделению") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Apartment,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = subdivisionDropdownExpanded)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                        .testTag("registration_subdivision_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = subdivisionDropdownExpanded,
                                    onDismissRequest = { subdivisionDropdownExpanded = false }
                                ) {
                                    availableSubdivisions.forEach { subdiv ->
                                        DropdownMenuItem(
                                            text = { Text(subdiv) },
                                            onClick = {
                                                selectedSubdivision = subdiv
                                                subdivisionDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Окно выбора принадлежности к участку
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSitePickerDialog = true }
                                    .testTag("registration_site_selector_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Принадлежность к участку (нажмите для выбора)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = selectedSite,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Выбрать",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (emailInput.isNotBlank()) {
                            Surface(
                                color = if (isRootAdmin) {
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
                                        imageVector = if (isRootAdmin) Icons.Default.AdminPanelSettings else Icons.Default.HourglassTop,
                                        contentDescription = null,
                                        tint = if (isRootAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isRootAdmin) {
                                            "Вход под Администратором: все права управления без ограничений"
                                        } else {
                                            "Подразделение: $selectedSubdivision • Участок: $selectedSite"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val cleanEmail = emailInput.trim().lowercase()
                                if (cleanEmail.isBlank()) {
                                    errorMessage = "Укажите адрес электронной почты учётной записи для входа"
                                    return@OutlinedButton
                                }
                                val adminActive = SiteMemberDocument.isRootAdminEmail(cleanEmail)
                                val cleanName = fullNameInput.trim().ifBlank {
                                    if (adminActive) {
                                        "Администратор"
                                    } else {
                                        cleanEmail.substringBefore("@").ifBlank { "Сотрудник участка" }
                                    }
                                }
                                val safeId = "usr_${cleanEmail.replace(Regex("[^a-zA-Z0-9]"), "_").take(40)}"
                                val session = CorporateLoginSession(
                                    userId = safeId,
                                    email = cleanEmail,
                                    fullName = cleanName,
                                    isHeadActivated = adminActive,
                                    requestedSubdivision = selectedSubdivision,
                                    requestedSiteName = selectedSite,
                                    requestedPosition = positionInput.trim()
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
                                imageVector = if (isRootAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Badge,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRootAdmin) "Войти как Администратор" else "Войти / Зарегистрироваться",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSitePickerDialog) {
        SiteSelectionDialog(
            availableSites = availableSites,
            selectedSite = selectedSite,
            onSelectSite = { chosen ->
                selectedSite = chosen
                showSitePickerDialog = false
            },
            onDismiss = { showSitePickerDialog = false }
        )
    }
}

@Composable
fun SiteSelectionDialog(
    availableSites: List<String>,
    selectedSite: String,
    onSelectSite: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Выбор принадлежности к участку",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                availableSites.forEach { siteOption ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (siteOption == selectedSite) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSite(siteOption) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = siteOption == selectedSite,
                                onClick = { onSelectSite(siteOption) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = siteOption,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (siteOption == selectedSite) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingRoleApprovalScreen(
    member: SiteMemberDocument,
    bannerMessage: String?,
    availableSubdivisions: List<String> = LocalWorkspaceStore.DEFAULT_SUBDIVISIONS,
    availableSites: List<String> = LocalWorkspaceStore.DEFAULT_SITES,
    onUpdateRequestDetails: (position: String, group: WorkGroup, siteName: String) -> Unit,
    onRefreshStatus: () -> Unit,
    onSignOut: () -> Unit
) {
    var positionText by remember(member.position) { mutableStateOf(member.position) }
    var selectedSubdivision by remember(member.groupName, availableSubdivisions) {
        mutableStateOf(
            member.groupName.ifBlank {
                availableSubdivisions.firstOrNull() ?: "Основное подразделение"
            }
        )
    }
    var subdivisionExpanded by remember { mutableStateOf(false) }

    var selectedSite by remember(member.effectiveSiteName, availableSites) {
        mutableStateOf(
            member.effectiveSiteName.ifBlank {
                availableSites.firstOrNull() ?: CorporateSeedData.DEFAULT_SITE_NAME
            }
        )
    }
    var showSiteDialog by remember { mutableStateOf(false) }

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
                        "Доступ отклонён"
                    } else {
                        "Ожидание назначения роли"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Ваша учётная запись зарегистрирована. Администратор или Руководитель назначит вам роль в системе.",
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

                        // Выпадающий список подразделения
                        ExposedDropdownMenuBox(
                            expanded = subdivisionExpanded,
                            onExpandedChange = { subdivisionExpanded = !subdivisionExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedSubdivision,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Подразделение") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = subdivisionExpanded)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = subdivisionExpanded,
                                onDismissRequest = { subdivisionExpanded = false }
                            ) {
                                availableSubdivisions.forEach { subdiv ->
                                    DropdownMenuItem(
                                        text = { Text(subdiv) },
                                        onClick = {
                                            selectedSubdivision = subdiv
                                            subdivisionExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Окно выбора принадлежности к участку
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSiteDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Принадлежность к участку",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = selectedSite,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "Изменить",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val resolvedGroup = CorporateSeedData.workGroups.firstOrNull {
                                        it.name.equals(selectedSubdivision, ignoreCase = true)
                                    } ?: WorkGroup(
                                        id = "grp_${selectedSubdivision.hashCode()}",
                                        name = selectedSubdivision,
                                        siteId = "site_1",
                                        siteName = selectedSite,
                                        supervisorId = "emp_head_1",
                                        supervisorName = "Руководитель",
                                        specialization = selectedSubdivision
                                    )
                                    onUpdateRequestDetails(positionText, resolvedGroup, selectedSite)
                                },
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

                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pending_sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Выйти из учётной записи (сменить аккаунт)", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showSiteDialog) {
        SiteSelectionDialog(
            availableSites = availableSites,
            selectedSite = selectedSite,
            onSelectSite = { chosen ->
                selectedSite = chosen
                showSiteDialog = false
            },
            onDismiss = { showSiteDialog = false }
        )
    }
}
