//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-04-11
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.providers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.TimeUnit

@Serializable
data class OpenAIOAuthCredential(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMillis: Long,
    val accountID: String,
    val authorizedAtEpochMillis: Long
) {
    val isExpired: Boolean
        get() = expiresAtEpochMillis <= System.currentTimeMillis()

    val maskedAccessToken: String
        get() {
            if (accessToken.length < 10) return "••••"
            return "${accessToken.take(8)}••••${accessToken.takeLast(4)}"
        }
}

object OpenAIOAuthService {
    private const val TAG = "OpenAIOAuthService"
    private const val CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
    private const val AUTHORIZE_URL = "https://auth.openai.com/oauth/authorize"
    private const val TOKEN_URL = "https://auth.openai.com/oauth/token"
    private const val REDIRECT_HOST = "127.0.0.1"
    private const val REDIRECT_PORT = 1455
    private const val REDIRECT_URI = "http://localhost:1455/auth/callback"
    private const val CALLBACK_BASE_URI = "http://localhost:1455"
    private const val SCOPE = "openid profile email offline_access api.connectors.read api.connectors.invoke"
    private const val JWT_AUTH_CLAIM_PATH = "https://api.openai.com/auth"
    private val json = Json { ignoreUnknownKeys = true }
    private val secureRandom = SecureRandom()
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun signIn(context: Context, originator: String = "openrocky") = withContext(Dispatchers.IO) {
        val flow = makeAuthorizationFlow(originator)
        Log.i(TAG, "Starting OpenAI OAuth sign-in, originator=$originator")
        val callbackURL = waitForLoopbackCallback(context, flow.url)
        val callbackUri = Uri.parse(callbackURL)
        Log.i(TAG, "Received OpenAI OAuth callback")

        val returnedState = callbackUri.getQueryParameter("state")
        if (returnedState != flow.state) {
            Log.w(TAG, "OpenAI OAuth state mismatch")
            throw OpenAIOAuthException.StateMismatch
        }

        val oauthError = callbackUri.getQueryParameter("error")
        if (!oauthError.isNullOrBlank()) {
            val desc = callbackUri.getQueryParameter("error_description").orEmpty()
            Log.w(TAG, "OpenAI OAuth authorization failed, error=$oauthError")
            throw OpenAIOAuthException.AuthorizationFailed(oauthError, desc)
        }

        val code = callbackUri.getQueryParameter("code")
        if (code.isNullOrBlank()) {
            Log.w(TAG, "OpenAI OAuth missing authorization code")
            throw OpenAIOAuthException.MissingAuthorizationCode
        }

        val token = exchangeAuthorizationCode(code = code, verifier = flow.verifier)
        val accountID = accountIDFromAccessToken(token.accessToken)
            ?: throw OpenAIOAuthException.MissingAccountID
        val now = System.currentTimeMillis()
        Log.i(TAG, "OpenAI OAuth sign-in completed, account=$accountID")

        OpenAIOAuthCredential(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            expiresAtEpochMillis = now + token.expiresIn * 1000L,
            accountID = accountID,
            authorizedAtEpochMillis = now
        )
    }

    suspend fun refresh(credential: OpenAIOAuthCredential): OpenAIOAuthCredential = withContext(Dispatchers.IO) {
        Log.i(TAG, "Refreshing OpenAI OAuth access token for account=${credential.accountID}")
        val token = refreshAccessToken(credential.refreshToken)
        val accountID = accountIDFromAccessToken(token.accessToken)
            ?: throw OpenAIOAuthException.MissingAccountID
        val now = System.currentTimeMillis()
        Log.i(TAG, "OpenAI OAuth refresh completed for account=$accountID")

        OpenAIOAuthCredential(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            expiresAtEpochMillis = now + token.expiresIn * 1000L,
            accountID = accountID,
            authorizedAtEpochMillis = credential.authorizedAtEpochMillis
        )
    }

    suspend fun refreshIfNeeded(
        credential: OpenAIOAuthCredential,
        leewaySeconds: Long = 60
    ): OpenAIOAuthCredential {
        val remains = credential.expiresAtEpochMillis - System.currentTimeMillis()
        if (remains > leewaySeconds * 1000L) {
            return credential
        }
        return refresh(credential)
    }

    fun accountIDFromAccessToken(accessToken: String): String? {
        val segments = accessToken.split(".")
        if (segments.size != 3) return null
        val payload = decodeBase64URL(segments[1]) ?: return null
        val root = runCatching { json.parseToJsonElement(payload.decodeToString()) }.getOrNull()
            ?: return null
        val authObj = root.jsonObject[JWT_AUTH_CLAIM_PATH]?.jsonObject ?: return null
        val accountID = authObj["chatgpt_account_id"]?.jsonPrimitive?.contentOrNull ?: return null
        return accountID.ifBlank { null }
    }

    private fun makeAuthorizationFlow(originator: String): AuthorizationFlow {
        val verifier = base64UrlEncode(randomBytes(32))
        val challenge = base64UrlEncode(sha256(verifier.toByteArray(Charsets.UTF_8)))
        val state = randomBytes(16).joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }

        val authUri = Uri.parse(AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("audience", "https://api.openai.com/v1")
            .appendQueryParameter("scope", SCOPE)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .appendQueryParameter("id_token_add_organizations", "true")
            .appendQueryParameter("codex_cli_simplified_flow", "true")
            .appendQueryParameter("originator", originator)
            .build()

        return AuthorizationFlow(url = authUri.toString(), verifier = verifier, state = state)
    }

    private suspend fun waitForLoopbackCallback(context: Context, authURL: String): String {
        val serverSocket = ServerSocket(REDIRECT_PORT, 0, InetAddress.getByName(REDIRECT_HOST))
        serverSocket.soTimeout = 180_000

        try {
            withContext(Dispatchers.Main) {
                val customTabs = CustomTabsIntent.Builder().build()
                customTabs.intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                customTabs.launchUrl(context, Uri.parse(authURL))
            }

            val socket = serverSocket.accept()
            socket.use { client ->
                client.soTimeout = 15_000
                val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
                val writer = client.getOutputStream().bufferedWriter(Charsets.UTF_8)

                val firstLine = reader.readLine().orEmpty()
                val target = firstLine.split(" ").getOrNull(1).orEmpty()
                val callbackURL = "$CALLBACK_BASE_URI$target"

                writer.write(
                    "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html; charset=utf-8\r\n" +
                        "Connection: close\r\n\r\n" +
                        "<html><body><h3>Rocky</h3><p>Authentication complete. You can close this tab.</p></body></html>"
                )
                writer.flush()
                return callbackURL
            }
        } catch (e: Exception) {
            Log.e(TAG, "OpenAI OAuth callback failed: ${e.message}")
            throw OpenAIOAuthException.CallbackFailed(e.message ?: "OAuth callback failed")
        } finally {
            runCatching { serverSocket.close() }
        }
    }

    private fun exchangeAuthorizationCode(code: String, verifier: String): OpenAIOAuthTokenResponse {
        val body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", CLIENT_ID)
            .add("code", code)
            .add("code_verifier", verifier)
            .add("redirect_uri", REDIRECT_URI)
            .build()

        return requestToken(body)
    }

    private fun refreshAccessToken(refreshToken: String): OpenAIOAuthTokenResponse {
        val body = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("client_id", CLIENT_ID)
            .add("refresh_token", refreshToken)
            .build()

        return requestToken(body)
    }

    private fun requestToken(body: FormBody): OpenAIOAuthTokenResponse {
        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(body)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        val response = httpClient.newCall(request).execute()
        response.use { http ->
            val payload = http.body?.string().orEmpty()
            if (!http.isSuccessful) {
                Log.e(TAG, "OpenAI token exchange failed, status=${http.code}")
                throw OpenAIOAuthException.TokenExchangeFailed(http.code, payload)
            }
            val decoded = runCatching { json.decodeFromString<OpenAIOAuthTokenResponse>(payload) }.getOrNull()
                ?: throw OpenAIOAuthException.InvalidTokenResponse
            if (decoded.accessToken.isBlank() || decoded.refreshToken.isBlank()) {
                throw OpenAIOAuthException.InvalidTokenResponse
            }
            return decoded
        }
    }

    private fun randomBytes(length: Int): ByteArray = ByteArray(length).also { secureRandom.nextBytes(it) }

    private fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

    private fun base64UrlEncode(data: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(data)

    private fun decodeBase64URL(value: String): ByteArray? =
        runCatching { Base64.getUrlDecoder().decode(value) }.getOrNull()

    private data class AuthorizationFlow(
        val url: String,
        val verifier: String,
        val state: String
    )

    @Serializable
    private data class OpenAIOAuthTokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String,
        @SerialName("expires_in") val expiresIn: Long
    )
}

sealed class OpenAIOAuthException(message: String) : Exception(message) {
    data object StateMismatch : OpenAIOAuthException("OpenAI OAuth state mismatch.")
    data object MissingAuthorizationCode : OpenAIOAuthException("OpenAI OAuth did not return an authorization code.")
    data object MissingAccountID : OpenAIOAuthException("OpenAI OAuth token is missing account information.")
    data object InvalidTokenResponse : OpenAIOAuthException("OpenAI OAuth returned an invalid token response.")
    data class AuthorizationFailed(val error: String, val description: String) :
        OpenAIOAuthException(
            if (description.isBlank()) "OpenAI OAuth failed: $error"
            else "OpenAI OAuth failed: $error ($description)"
        )
    data class TokenExchangeFailed(val statusCode: Int, val body: String) :
        OpenAIOAuthException("OpenAI token exchange failed ($statusCode): $body")
    data class CallbackFailed(val details: String) : OpenAIOAuthException(details)
}
