package de.westnordost.streetcomplete.util.creds_manager

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@RequiresApi(Build.VERSION_CODES.M)
object SecureCredentialStorage {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "my_app_secure_key"
    private const val PREFS_NAME = "secure_prefs"
    private const val PREF_IV = "encrypted_iv"
    private const val PREF_DATA = "encrypted_data"
    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_LENGTH = 128

    private val json = Json { ignoreUnknownKeys = true }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

        val keyGenParams = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(keyGenParams)
            generateKey()
        }
    }

    private fun encrypt(secretKey: SecretKey, plaintext: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return Pair(cipher.iv, cipher.doFinal(plaintext))
    }

    private fun decrypt(secretKey: SecretKey, iv: ByteArray, ciphertext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_MODE)
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(ciphertext)
    }

    private fun saveEncryptedJson(context: Context, jsonString: String) {
        val key = getOrCreateSecretKey()
        val (iv, ciphertext) = encrypt(key, jsonString.toByteArray(Charsets.UTF_8))

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit() {
            putString(PREF_IV, Base64.encodeToString(iv, Base64.DEFAULT))
                .putString(PREF_DATA, Base64.encodeToString(ciphertext, Base64.DEFAULT))
        }
    }

    private fun loadDecryptedJson(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ivBase64 = prefs.getString(PREF_IV, null) ?: return null
        val dataBase64 = prefs.getString(PREF_DATA, null) ?: return null

        val iv = Base64.decode(ivBase64, Base64.DEFAULT)
        val ciphertext = Base64.decode(dataBase64, Base64.DEFAULT)

        val key = getOrCreateSecretKey()
        val decrypted = decrypt(key, iv, ciphertext)
        return decrypted.toString(Charsets.UTF_8)
    }

    fun saveCredentials(context: Context, credentials: Map<String, EnvCredentials>) {
        val jsonString = json.encodeToString(credentials)
        saveEncryptedJson(context, jsonString)
    }

    fun loadCredentials(context: Context): MutableMap<String, EnvCredentials> {
        val decryptedJson = loadDecryptedJson(context) ?: return mutableMapOf()
        return try {
            json.decodeFromString<Map<String, EnvCredentials>>(decryptedJson).toMutableMap()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    fun saveCredential(context: Context, env: String, username: String, password: String) {
        val allCreds = loadCredentials(context)
        allCreds[env] = EnvCredentials(username, password)
        saveCredentials(context, allCreds)
    }

    fun getCredential(context: Context, env: String): EnvCredentials? {
        return loadCredentials(context)[env]
    }

    fun deleteCredential(context: Context, env: String) {
        val allCreds = loadCredentials(context)
        if (allCreds.remove(env) != null) saveCredentials(context, allCreds)
    }
}
