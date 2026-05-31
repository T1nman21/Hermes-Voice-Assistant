//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoService {
    /**
     * Cryptographic primitives. Mirrors the iOS `crypto` tool signature:
     *   - [data] — input (plain text for hash/hmac/encrypt; hex for decrypt/base64-decode callers pass the encoded form)
     *   - [key] — hex-encoded key for HMAC / AES
     *   - [iv]  — hex-encoded IV for AES-CBC (16 bytes)
     *
     * Output formats:
     *   - Hashes / HMAC → hex
     *   - AES encrypt → hex ciphertext
     *   - AES decrypt → UTF-8 plaintext (input is hex)
     *   - base64_encode → base64 of input UTF-8 bytes
     *   - base64_decode → UTF-8 string of decoded bytes
     */
    fun execute(operation: String, data: String, key: String?, iv: String?): String {
        return try {
            when (operation) {
                "sha256" -> {
                    val digest = MessageDigest.getInstance("SHA-256")
                    digest.digest(data.toByteArray()).toHex()
                }
                "md5" -> {
                    val digest = MessageDigest.getInstance("MD5")
                    digest.digest(data.toByteArray()).toHex()
                }
                "hmac-sha256", "hmac_sha256" -> {
                    val k = key ?: return "Key required for HMAC"
                    val mac = Mac.getInstance("HmacSHA256")
                    mac.init(SecretKeySpec(hexOrUtf8(k), "HmacSHA256"))
                    mac.doFinal(data.toByteArray()).toHex()
                }
                "base64-encode", "base64_encode" -> Base64.encodeToString(data.toByteArray(), Base64.NO_WRAP)
                "base64-decode", "base64_decode" -> String(Base64.decode(data, Base64.DEFAULT))
                "aes-encrypt", "aes_encrypt" -> {
                    val k = key ?: return "Key required for AES"
                    val ivBytes = parseIv(iv)
                    val keyBytes = hexOrUtf8(k).copyOf(16)
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(ivBytes))
                    cipher.doFinal(data.toByteArray()).toHex()
                }
                "aes-decrypt", "aes_decrypt" -> {
                    val k = key ?: return "Key required for AES"
                    val ivBytes = parseIv(iv)
                    val keyBytes = hexOrUtf8(k).copyOf(16)
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(ivBytes))
                    String(cipher.doFinal(hexDecode(data)))
                }
                else -> "Unknown operation: $operation. Supported: sha256, md5, hmac_sha256, aes_encrypt, aes_decrypt, base64_encode, base64_decode"
            }
        } catch (e: Exception) {
            "Crypto error: ${e.message}"
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun hexDecode(s: String): ByteArray {
        val clean = s.trim().removePrefix("0x").replace(" ", "")
        require(clean.length % 2 == 0) { "Hex string must have even length" }
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    /** Accept hex-encoded key (iOS convention). Fall back to UTF-8 bytes if the string is not valid hex, for backwards compatibility with the old behavior. */
    private fun hexOrUtf8(s: String): ByteArray =
        runCatching { hexDecode(s) }.getOrNull() ?: s.toByteArray()

    /** iOS expects hex-encoded 16-byte IV. If caller omits IV, use a zero block (matches prior Android behavior). */
    private fun parseIv(iv: String?): ByteArray {
        if (iv.isNullOrBlank()) return ByteArray(16)
        val bytes = runCatching { hexDecode(iv) }.getOrNull() ?: iv.toByteArray()
        return bytes.copyOf(16)
    }
}
