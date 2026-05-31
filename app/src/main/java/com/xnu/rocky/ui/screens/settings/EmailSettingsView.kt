//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-04-13
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.runtime.tools.EmailConfig
import com.xnu.rocky.runtime.tools.EmailProvider
import com.xnu.rocky.ui.screens.providers.rockyTextFieldColors
import com.xnu.rocky.ui.theme.OpenRockyPalette
import kotlinx.coroutines.launch

enum class EmailPreset(val label: String, val config: EmailConfig?) {
    GMAIL("Gmail", EmailConfig.gmailPreset),
    OUTLOOK("Outlook", EmailConfig.outlookPreset),
    QQ("QQ Mail", EmailConfig.qqPreset),
    CUSTOM("Custom SMTP", null);
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailSettingsView(onBack: () -> Unit) {
    val context = LocalContext.current

    var providerType by remember { mutableStateOf(EmailProvider.SMTP) }

    // SMTP state
    var selectedPreset by remember { mutableStateOf(EmailPreset.GMAIL) }
    var smtpHost by remember { mutableStateOf("") }
    var smtpPort by remember { mutableStateOf("465") }
    var smtpUsername by remember { mutableStateOf("") }
    var smtpPassword by remember { mutableStateOf("") }
    var useTLS by remember { mutableStateOf(true) }
    var showPassword by remember { mutableStateOf(false) }

    // Resend state
    var resendApiKey by remember { mutableStateOf("") }
    var resendFromAddress by remember { mutableStateOf(EmailConfig.RESEND_SANDBOX_FROM) }
    var showApiKey by remember { mutableStateOf(false) }

    // Shared state
    var defaultRecipient by remember { mutableStateOf("") }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isConfigured = remember(providerType, smtpHost, smtpUsername, resendFromAddress, testResult) {
        EmailConfig.load(context)?.isConfigured == true && EmailConfig.load(context)?.hasCredential == true
    }
    val canSave = when (providerType) {
        EmailProvider.SMTP -> smtpHost.isNotBlank() && smtpUsername.isNotBlank() && smtpPassword.isNotBlank()
        EmailProvider.RESEND -> resendApiKey.isNotBlank() && resendFromAddress.isNotBlank()
    }

    // Load existing config + restore per-provider credentials so switching tabs
    // doesn't force the user to re-enter them.
    LaunchedEffect(Unit) {
        EmailConfig.loadCredential(EmailProvider.SMTP)?.let { smtpPassword = it }
        EmailConfig.loadCredential(EmailProvider.RESEND)?.let { resendApiKey = it }

        val config = EmailConfig.load(context) ?: return@LaunchedEffect
        providerType = config.provider
        smtpHost = config.smtpHost
        smtpPort = config.smtpPort.toString()
        smtpUsername = config.username
        useTLS = config.useTLS
        if (config.resendFromAddress.isNotBlank()) resendFromAddress = config.resendFromAddress
        defaultRecipient = config.defaultRecipient
        selectedPreset = when (config.smtpHost) {
            "smtp.gmail.com" -> EmailPreset.GMAIL
            "smtp.office365.com" -> EmailPreset.OUTLOOK
            "smtp.qq.com" -> EmailPreset.QQ
            else -> if (config.smtpHost.isBlank()) EmailPreset.GMAIL else EmailPreset.CUSTOM
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Email Setup", color = OpenRockyPalette.text) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OpenRockyPalette.background)
            )
        },
        containerColor = OpenRockyPalette.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Status
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConfigured) OpenRockyPalette.success.copy(alpha = 0.1f)
                        else OpenRockyPalette.warning.copy(alpha = 0.1f)
                    )
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            if (isConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                            null,
                            tint = if (isConfigured) OpenRockyPalette.success else OpenRockyPalette.warning,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(if (isConfigured) "Email Configured" else "Setup Required", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                            Text(
                                if (isConfigured) "Ready to send via ${providerType.displayName}."
                                else "Pick a provider type and fill in the credentials below.",
                                fontSize = 12.sp, color = OpenRockyPalette.muted
                            )
                        }
                    }
                }
            }

            // Provider type
            item {
                Text("Provider Type", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EmailProvider.entries.forEach { type ->
                        FilterChip(
                            selected = providerType == type,
                            onClick = { providerType = type },
                            label = { Text(type.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OpenRockyPalette.accent.copy(alpha = 0.2f),
                                containerColor = OpenRockyPalette.cardElevated
                            )
                        )
                    }
                }
                val hint = when (providerType) {
                    EmailProvider.SMTP -> "SMTP works with Gmail, Outlook, QQ Mail, and other standard providers. Requires an app password."
                    EmailProvider.RESEND -> "Resend is an HTTP email API — sign up at resend.com, grab an API key, and you can start sending in under a minute."
                }
                Text(hint, fontSize = 11.sp, color = OpenRockyPalette.label, modifier = Modifier.padding(top = 4.dp))
            }

            if (providerType == EmailProvider.SMTP) {
                // Provider preset
                item {
                    Text("Provider", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EmailPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = selectedPreset == preset,
                                onClick = {
                                    selectedPreset = preset
                                    preset.config?.let {
                                        smtpHost = it.smtpHost
                                        smtpPort = it.smtpPort.toString()
                                        useTLS = it.useTLS
                                    }
                                },
                                label = { Text(preset.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OpenRockyPalette.accent.copy(alpha = 0.2f),
                                    containerColor = OpenRockyPalette.cardElevated
                                )
                            )
                        }
                    }
                    val hint = when (selectedPreset) {
                        EmailPreset.GMAIL -> "Use a Google App Password (not your Gmail password). Generate one at myaccount.google.com -> Security -> App passwords."
                        EmailPreset.OUTLOOK -> "Use your Outlook/Microsoft account password or an app password if 2FA is enabled."
                        EmailPreset.QQ -> "Use QQ Mail authorization code (not your QQ password). Get it from QQ Mail Settings -> Account -> POP3/SMTP."
                        EmailPreset.CUSTOM -> "Enter your SMTP server details manually."
                    }
                    Text(hint, fontSize = 11.sp, color = OpenRockyPalette.label, modifier = Modifier.padding(top = 4.dp))
                }

                // SMTP Server
                item {
                    Text("SMTP Server", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = smtpHost, onValueChange = { smtpHost = it },
                            placeholder = { Text("smtp.gmail.com", color = OpenRockyPalette.label) },
                            label = { Text("Host") },
                            modifier = Modifier.weight(2f), colors = rockyTextFieldColors(),
                            shape = RoundedCornerShape(12.dp), singleLine = true
                        )
                        OutlinedTextField(
                            value = smtpPort, onValueChange = { smtpPort = it },
                            placeholder = { Text("465", color = OpenRockyPalette.label) },
                            label = { Text("Port") },
                            modifier = Modifier.weight(1f), colors = rockyTextFieldColors(),
                            shape = RoundedCornerShape(12.dp), singleLine = true
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Checkbox(checked = useTLS, onCheckedChange = { useTLS = it }, colors = CheckboxDefaults.colors(checkedColor = OpenRockyPalette.accent))
                        Text("Use TLS", fontSize = 14.sp, color = OpenRockyPalette.text)
                    }
                }

                // Account
                item {
                    Text("Account", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = smtpUsername, onValueChange = { smtpUsername = it },
                        placeholder = { Text("you@gmail.com", color = OpenRockyPalette.label) },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(), colors = rockyTextFieldColors(),
                        shape = RoundedCornerShape(12.dp), singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = smtpPassword, onValueChange = { smtpPassword = it },
                        placeholder = { Text("App password", color = OpenRockyPalette.label) },
                        label = { Text("App Password") },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = OpenRockyPalette.muted)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(), colors = rockyTextFieldColors(),
                        shape = RoundedCornerShape(12.dp), singleLine = true
                    )
                }
            } else {
                // Resend
                item {
                    Text("Resend", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = resendApiKey, onValueChange = { resendApiKey = it },
                        placeholder = { Text("re_...", color = OpenRockyPalette.label) },
                        label = { Text("API Key") },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = OpenRockyPalette.muted)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(), colors = rockyTextFieldColors(),
                        shape = RoundedCornerShape(12.dp), singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resendFromAddress, onValueChange = { resendFromAddress = it },
                        placeholder = { Text(EmailConfig.RESEND_SANDBOX_FROM, color = OpenRockyPalette.label) },
                        label = { Text("From") },
                        modifier = Modifier.fillMaxWidth(), colors = rockyTextFieldColors(),
                        shape = RoundedCornerShape(12.dp), singleLine = true
                    )
                    Text(
                        "Get your API key at resend.com -> API Keys. The default From `${EmailConfig.RESEND_SANDBOX_FROM}` works without domain verification but can only deliver to your Resend account email. To send to anyone, verify your own domain in Resend and use an address on it.",
                        fontSize = 11.sp, color = OpenRockyPalette.label, modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Default recipient
            item {
                Text("Default Recipient", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = defaultRecipient, onValueChange = { defaultRecipient = it },
                    placeholder = { Text("you@example.com", color = OpenRockyPalette.label) },
                    label = { Text("Send To") },
                    modifier = Modifier.fillMaxWidth(), colors = rockyTextFieldColors(),
                    shape = RoundedCornerShape(12.dp), singleLine = true
                )
                Text(
                    "Where to send when you ask Rocky to email something without naming a recipient (e.g. \"write this up and email it to me\"). You can always override it by naming an address out loud.",
                    fontSize = 11.sp, color = OpenRockyPalette.label, modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Actions
            item {
                Button(
                    onClick = {
                        val trimmedRecipient = defaultRecipient.trim()
                        val config: EmailConfig
                        val credential: String
                        when (providerType) {
                            EmailProvider.SMTP -> {
                                config = EmailConfig(
                                    provider = EmailProvider.SMTP,
                                    smtpHost = smtpHost,
                                    smtpPort = smtpPort.toIntOrNull() ?: 465,
                                    username = smtpUsername,
                                    useTLS = useTLS,
                                    resendFromAddress = resendFromAddress,  // preserve other provider's UI state
                                    defaultRecipient = trimmedRecipient
                                )
                                credential = smtpPassword
                            }
                            EmailProvider.RESEND -> {
                                config = EmailConfig(
                                    provider = EmailProvider.RESEND,
                                    smtpHost = smtpHost,
                                    smtpPort = smtpPort.toIntOrNull() ?: 465,
                                    username = smtpUsername,
                                    useTLS = useTLS,
                                    resendFromAddress = resendFromAddress,
                                    defaultRecipient = trimmedRecipient
                                )
                                credential = resendApiKey
                            }
                        }
                        EmailConfig.save(context, config)
                        EmailConfig.saveCredential(config.provider, credential)
                        testResult = "Configuration saved."
                        testSuccess = true
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Configuration")
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isTesting = true
                            testResult = null
                            // SMTP sends the test to itself; Resend sends to its From
                            // (sandbox `onboarding@resend.dev` is routed to the account email).
                            val recipient = when (providerType) {
                                EmailProvider.SMTP -> smtpUsername
                                EmailProvider.RESEND -> resendFromAddress
                            }
                            val result = com.xnu.rocky.runtime.tools.EmailService.sendEmail(
                                context, recipient,
                                "Rocky Email Test",
                                "This is a test email from Rocky. If you received this, your email configuration is working correctly!"
                            )
                            testSuccess = result.startsWith("Email sent successfully")
                            testResult = result
                            isTesting = false
                        }
                    },
                    enabled = isConfigured && !isTesting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = OpenRockyPalette.accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Testing...")
                    } else {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Send Test Email")
                    }
                }

                if (isConfigured) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            EmailConfig.remove(context)
                            smtpHost = ""; smtpPort = "465"; smtpUsername = ""; smtpPassword = ""
                            resendApiKey = ""; resendFromAddress = EmailConfig.RESEND_SANDBOX_FROM
                            defaultRecipient = ""
                            testResult = "Configuration removed."
                            testSuccess = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, null, tint = OpenRockyPalette.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Remove Configuration", color = OpenRockyPalette.error)
                    }
                }
            }

            // Test result
            testResult?.let { msg ->
                item {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (testSuccess) OpenRockyPalette.success.copy(alpha = 0.1f)
                            else OpenRockyPalette.error.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                if (testSuccess) Icons.Default.Check else Icons.Default.Close, null,
                                tint = if (testSuccess) OpenRockyPalette.success else OpenRockyPalette.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(msg, fontSize = 13.sp, color = if (testSuccess) OpenRockyPalette.success else OpenRockyPalette.error)
                        }
                    }
                }
            }
        }
    }
}
