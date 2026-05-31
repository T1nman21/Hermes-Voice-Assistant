//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-04-13
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.xnu.rocky.runtime.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object EmailService {
    private const val TAG = "EmailService"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sendEmail(context: Context, to: String, subject: String, body: String): String {
        val config = EmailConfig.load(context)
        // Resolve the recipient: an explicit `to` wins; otherwise fall back to the
        // configured default recipient (e.g. "email this article to me").
        val resolved = to.trim().ifBlank { config?.defaultRecipient?.trim().orEmpty() }

        // If a provider is fully configured, send directly through it.
        if (config != null && config.isConfigured && config.hasCredential) {
            val recipients = resolved.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (recipients.isEmpty()) {
                return "No recipient: name an address out loud, or set a Default Recipient in Settings → Features → Send Email."
            }
            return try {
                val messageID = when (config.provider) {
                    EmailProvider.SMTP -> SmtpEmailService(context).send(recipients, subject, body)
                    EmailProvider.RESEND -> sendViaResend(config, recipients, subject, body)
                }
                "Email sent successfully to ${recipients.joinToString()} (Message-ID: $messageID)"
            } catch (e: Exception) {
                "Email send failed: ${e.message}"
            }
        }

        // Fallback: open the system email app (no provider configured).
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                if (resolved.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(resolved))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Email compose opened for: ${resolved.ifBlank { "(no recipient)" }} (no provider configured — opened system mail app)"
        } catch (e: Exception) {
            "Failed to send email: ${e.message}"
        }
    }

    // MARK: - Resend HTTP API

    private suspend fun sendViaResend(
        config: EmailConfig,
        to: List<String>,
        subject: String,
        body: String,
        cc: List<String> = emptyList(),
        bcc: List<String> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = EmailConfig.loadCredential(EmailProvider.RESEND)
            ?: throw Exception("Resend API key not found.")

        val payload = buildJsonObject {
            put("from", config.resendFromAddress)
            putJsonArray("to") { to.forEach { add(it) } }
            put("subject", subject)
            put("text", body)
            if (cc.isNotEmpty()) putJsonArray("cc") { cc.forEach { add(it) } }
            if (bcc.isNotEmpty()) putJsonArray("bcc") { bcc.forEach { add(it) } }
        }

        val request = Request.Builder()
            .url("https://api.resend.com/emails")
            .header("Authorization", "Bearer $apiKey")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        OkHttpClient().newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching {
                    json.parseToJsonElement(responseBody).jsonObject["message"]?.jsonPrimitive?.contentOrNull
                }.getOrNull() ?: responseBody.ifBlank { "HTTP ${response.code}" }
                throw Exception("Resend error: HTTP ${response.code}: $message")
            }
            val id = runCatching {
                json.parseToJsonElement(responseBody).jsonObject["id"]?.jsonPrimitive?.contentOrNull
            }.getOrNull() ?: throw Exception("Resend error: unexpected response body")
            LogManager.info("Resend email sent to ${to.joinToString()} id=$id", TAG)
            id
        }
    }
}
