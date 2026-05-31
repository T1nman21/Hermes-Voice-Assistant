//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-04-11
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.providers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.providers.OpenAIOAuthCredential
import com.xnu.rocky.providers.OpenAIOAuthService
import com.xnu.rocky.providers.ProviderInstance
import com.xnu.rocky.providers.ProviderKind
import com.xnu.rocky.ui.theme.OpenRockyPalette
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ProviderInstanceEditorView(
    existingInstance: ProviderInstance?,
    existingCredential: String,
    existingOpenAIOAuthCredential: OpenAIOAuthCredential?,
    onSave: (ProviderInstance, String, OpenAIOAuthCredential?) -> Unit,
    onTest: (ProviderInstance, String, OpenAIOAuthCredential?, (String) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isNew = existingInstance == null

    var name by remember { mutableStateOf(existingInstance?.name ?: "") }
    var selectedKind by remember { mutableStateOf(existingInstance?.kind ?: ProviderKind.OPENAI) }
    var modelID by remember { mutableStateOf(existingInstance?.modelID ?: "") }
    var credential by remember { mutableStateOf(existingCredential) }
    var openAIAuthMethod by remember {
        mutableStateOf(
            if (selectedKind == ProviderKind.OPENAI && existingOpenAIOAuthCredential != null) {
                OpenAIAuthMethod.OAUTH
            } else {
                OpenAIAuthMethod.API_KEY
            }
        )
    }
    var openAIOAuthCredential by remember { mutableStateOf(existingOpenAIOAuthCredential) }
    var oauthState by remember { mutableStateOf<OpenAIOAuthState>(OpenAIOAuthState.IDLE) }

    var azureResourceName by remember { mutableStateOf(existingInstance?.azureResourceName ?: "") }
    var azureAPIVersion by remember { mutableStateOf(existingInstance?.azureAPIVersion ?: "2024-02-15-preview") }
    var aiProxyServiceURL by remember { mutableStateOf(existingInstance?.aiProxyServiceURL ?: "") }
    var customHost by remember { mutableStateOf(existingInstance?.customHost ?: "") }
    var showPassword by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var showKindDropdown by remember { mutableStateOf(false) }

    fun resolvedCredential(): String {
        val manual = credential.trim()
        if (manual.isNotEmpty()) return manual
        if (selectedKind == ProviderKind.OPENAI && openAIAuthMethod == OpenAIAuthMethod.OAUTH) {
            return openAIOAuthCredential?.accessToken.orEmpty()
        }
        return manual
    }

    val canSave = modelID.trim().isNotEmpty() && resolvedCredential().isNotEmpty()

    LaunchedEffect(selectedKind) {
        if (selectedKind != ProviderKind.OPENAI) {
            openAIAuthMethod = OpenAIAuthMethod.API_KEY
            openAIOAuthCredential = null
            oauthState = OpenAIOAuthState.IDLE
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Add Provider" else "Edit Provider", color = OpenRockyPalette.text) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val instance = (existingInstance ?: ProviderInstance()).copy(
                                name = name,
                                kind = selectedKind,
                                modelID = modelID.ifBlank { selectedKind.defaultModel },
                                customHost = customHost,
                                azureResourceName = azureResourceName,
                                azureAPIVersion = azureAPIVersion,
                                aiProxyServiceURL = aiProxyServiceURL
                            )
                            onSave(
                                instance,
                                credential.trim(),
                                if (selectedKind == ProviderKind.OPENAI && openAIAuthMethod == OpenAIAuthMethod.OAUTH) {
                                    openAIOAuthCredential
                                } else {
                                    null
                                }
                            )
                            onBack()
                        },
                        enabled = canSave
                    ) {
                        Text("Save", color = if (canSave) OpenRockyPalette.accent else OpenRockyPalette.label)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OpenRockyPalette.background)
            )
        },
        containerColor = OpenRockyPalette.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text("Provider", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                Spacer(Modifier.height(4.dp))
                ExposedDropdownMenuBox(expanded = showKindDropdown, onExpandedChange = { showKindDropdown = it }) {
                    OutlinedTextField(
                        value = selectedKind.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showKindDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = rockyTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(
                        expanded = showKindDropdown,
                        onDismissRequest = { showKindDropdown = false }
                    ) {
                        ProviderKind.entries.forEach { kind ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(kind.displayName, color = OpenRockyPalette.text)
                                        Text(kind.summary, fontSize = 11.sp, color = OpenRockyPalette.muted)
                                    }
                                },
                                onClick = {
                                    selectedKind = kind
                                    if (modelID.isBlank() || ProviderKind.entries.any { it.defaultModel == modelID }) {
                                        modelID = kind.defaultModel
                                    }
                                    showKindDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text("Name (optional)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(selectedKind.displayName, color = OpenRockyPalette.label) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = rockyTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            if (selectedKind == ProviderKind.OPENAI) {
                item {
                    Text("Authentication", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = openAIAuthMethod == OpenAIAuthMethod.API_KEY,
                            onClick = { openAIAuthMethod = OpenAIAuthMethod.API_KEY },
                            label = { Text("API Key") }
                        )
                        FilterChip(
                            selected = openAIAuthMethod == OpenAIAuthMethod.OAUTH,
                            onClick = { openAIAuthMethod = OpenAIAuthMethod.OAUTH },
                            label = { Text("OAuth") }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "This setting is for chat provider only. Voice uses Realtime Provider settings.",
                        color = OpenRockyPalette.label,
                        fontSize = 11.sp
                    )
                }

                if (openAIAuthMethod == OpenAIAuthMethod.OAUTH) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.cardElevated),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                when (val state = oauthState) {
                                    is OpenAIOAuthState.FAILED -> {
                                        Text(state.message, color = OpenRockyPalette.error, fontSize = 12.sp)
                                    }
                                    OpenAIOAuthState.AUTHENTICATING -> {
                                        Text("Signing in with OpenAI…", color = OpenRockyPalette.muted, fontSize = 12.sp)
                                    }
                                    else -> Unit
                                }

                                if (openAIOAuthCredential == null) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                oauthState = OpenAIOAuthState.AUTHENTICATING
                                                runCatching {
                                                    OpenAIOAuthService.signIn(context, originator = "openrocky")
                                                }.onSuccess { auth ->
                                                    openAIOAuthCredential = auth
                                                    oauthState = OpenAIOAuthState.AUTHENTICATED
                                                }.onFailure { error ->
                                                    oauthState = OpenAIOAuthState.FAILED(error.message ?: "OpenAI OAuth failed")
                                                }
                                            }
                                        },
                                        enabled = oauthState != OpenAIOAuthState.AUTHENTICATING,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Sign in with OpenAI")
                                    }
                                } else {
                                    val oauth = openAIOAuthCredential!!
                                    Text(
                                        text = if (oauth.isExpired) "OAuth Expired" else "Authenticated",
                                        color = if (oauth.isExpired) OpenRockyPalette.warning else OpenRockyPalette.success,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text("Token: ${oauth.maskedAccessToken}", color = OpenRockyPalette.muted, fontSize = 12.sp)
                                    Text("Account: ${oauth.accountID}", color = OpenRockyPalette.muted, fontSize = 12.sp)
                                    Text(
                                        "Authorized: ${formatEpochTime(oauth.authorizedAtEpochMillis)}",
                                        color = OpenRockyPalette.muted,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "Expires: ${formatEpochTime(oauth.expiresAtEpochMillis)}",
                                        color = OpenRockyPalette.muted,
                                        fontSize = 12.sp
                                    )

                                    TextButton(onClick = {
                                        openAIOAuthCredential = null
                                        oauthState = OpenAIOAuthState.IDLE
                                    }) {
                                        Text("Sign Out", color = OpenRockyPalette.error)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("Manual Token Override (optional)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = credential,
                            onValueChange = { credential = it },
                            placeholder = { Text("sk-...", color = OpenRockyPalette.label) },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        null,
                                        tint = OpenRockyPalette.muted
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = rockyTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Text(
                            "If set, this token overrides OAuth for chat requests.",
                            color = OpenRockyPalette.label,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    item {
                        Text("API Key", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = credential,
                            onValueChange = { credential = it },
                            placeholder = { Text(selectedKind.apiKeyPlaceholder, color = OpenRockyPalette.label) },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        null,
                                        tint = OpenRockyPalette.muted
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = rockyTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            } else {
                item {
                    Text("API Key", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = credential,
                        onValueChange = { credential = it },
                        placeholder = { Text(selectedKind.apiKeyPlaceholder, color = OpenRockyPalette.label) },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null,
                                    tint = OpenRockyPalette.muted
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = rockyTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            item {
                Text("Model", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = modelID,
                    onValueChange = { modelID = it },
                    placeholder = { Text(selectedKind.defaultModel, color = OpenRockyPalette.label) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = rockyTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedKind.suggestedModels.take(3).forEach { model ->
                        SuggestionChip(
                            onClick = { modelID = model },
                            label = { Text(model, fontSize = 11.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = OpenRockyPalette.cardElevated,
                                labelColor = OpenRockyPalette.muted
                            )
                        )
                    }
                }
            }

            item {
                Text("Custom Host (optional)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = customHost,
                    onValueChange = { customHost = it },
                    placeholder = { Text(selectedKind.baseUrl.ifBlank { "https://api.example.com/v1/" }, color = OpenRockyPalette.label) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = rockyTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Text("Leave empty to use default endpoint", fontSize = 11.sp, color = OpenRockyPalette.label, modifier = Modifier.padding(top = 4.dp))
            }

            if (selectedKind == ProviderKind.AZURE_OPENAI) {
                item {
                    Text("Azure Resource Name", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = azureResourceName,
                        onValueChange = { azureResourceName = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = rockyTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
                item {
                    Text("API Version", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = azureAPIVersion,
                        onValueChange = { azureAPIVersion = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = rockyTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            if (selectedKind == ProviderKind.AIPROXY) {
                item {
                    Text("Service URL", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = aiProxyServiceURL,
                        onValueChange = { aiProxyServiceURL = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = rockyTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        isTesting = true
                        testResult = null
                        val instance = (existingInstance ?: ProviderInstance()).copy(
                            kind = selectedKind,
                            modelID = modelID.ifBlank { selectedKind.defaultModel },
                            customHost = customHost,
                            azureResourceName = azureResourceName,
                            azureAPIVersion = azureAPIVersion,
                            aiProxyServiceURL = aiProxyServiceURL
                        )
                        onTest(
                            instance,
                            credential.trim(),
                            if (selectedKind == ProviderKind.OPENAI && openAIAuthMethod == OpenAIAuthMethod.OAUTH) {
                                openAIOAuthCredential
                            } else {
                                null
                            }
                        ) { result ->
                            testResult = result
                            isTesting = false
                        }
                    },
                    enabled = canSave && !isTesting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.cardElevated),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OpenRockyPalette.accent, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isTesting) "Testing…" else "Test Connection", color = OpenRockyPalette.text)
                }

                testResult?.let { result ->
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.startsWith("Connected")) OpenRockyPalette.success.copy(alpha = 0.1f) else OpenRockyPalette.error.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            result,
                            fontSize = 13.sp,
                            color = if (result.startsWith("Connected")) OpenRockyPalette.success else OpenRockyPalette.error,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun rockyTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = OpenRockyPalette.text,
    unfocusedTextColor = OpenRockyPalette.text,
    cursorColor = OpenRockyPalette.accent,
    focusedBorderColor = OpenRockyPalette.accent,
    unfocusedBorderColor = OpenRockyPalette.stroke,
    focusedContainerColor = OpenRockyPalette.cardElevated,
    unfocusedContainerColor = OpenRockyPalette.card,
)

private fun formatEpochTime(epochMillis: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))
}

private enum class OpenAIAuthMethod {
    API_KEY,
    OAUTH
}

private sealed class OpenAIOAuthState {
    data object IDLE : OpenAIOAuthState()
    data object AUTHENTICATING : OpenAIOAuthState()
    data object AUTHENTICATED : OpenAIOAuthState()
    data class FAILED(val message: String) : OpenAIOAuthState()
}
