package com.example.data.secure

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeyStoreManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        createKeyIfNeeded()
    }

    private fun createKeyIfNeeded() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEY_STORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()

                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (_: Exception) {
            // Key store initialization handled gracefully
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        } catch (_: Exception) {
            null
        }
    }

    fun saveApiKey(apiKey: String): Boolean {
        if (apiKey.isBlank()) return false
        val secretKey = getSecretKey() ?: return false
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(apiKey.trim().toByteArray(Charsets.UTF_8))

            val ivString = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encryptedString = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

            prefs.edit()
                .putString(KEY_ENCRYPTED_API_KEY, encryptedString)
                .putString(KEY_IV, ivString)
                .apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getApiKey(): String? {
        val encryptedString = prefs.getString(KEY_ENCRYPTED_API_KEY, null) ?: return null
        val ivString = prefs.getString(KEY_IV, null) ?: return null
        val secretKey = getSecretKey() ?: return null

        return try {
            val iv = Base64.decode(ivString, Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(encryptedString, Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun isApiKeyConfigured(): Boolean {
        val key = getApiKey()
        return !key.isNullOrBlank()
    }

    fun getMaskedApiKey(): String {
        val key = getApiKey()
        if (key.isNullOrBlank()) return "Not Configured"
        return if (key.length > 8) {
            "${key.take(4)}••••••••${key.takeLast(4)}"
        } else {
            "••••••••"
        }
    }

    fun clearApiKey() {
        prefs.edit()
            .remove(KEY_ENCRYPTED_API_KEY)
            .remove(KEY_IV)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "rexyy_secure_vault"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "rexyy_aes_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        private const val KEY_ENCRYPTED_API_KEY = "encrypted_api_key"
        private const val KEY_IV = "encryption_iv"
    }
}
