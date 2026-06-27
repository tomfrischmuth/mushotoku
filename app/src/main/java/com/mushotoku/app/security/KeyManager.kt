/*
 * Mushotoku — a privacy-focused, offline productivity app.
 * Copyright (C) 2026 Tom Frischmuth
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.mushotoku.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

class KeyManager(context: Context) {

    private val storage = KeyStorage(context)
    private val secureRandom = SecureRandom()

    fun isInitialized(): Boolean = storage.isInitialized

    fun currentMode(): KeyMode =
        storage.mode ?: throw IllegalStateException("KeyManager ist nicht initialisiert")

    fun requiresUserPresence(): Boolean = currentMode().requiresUserPresence

    suspend fun initialize(
        mode: KeyMode = KeyMode.KEYSTORE_NO_LOCK,
        passphrase: CharArray? = null,
    ): ByteArray {
        check(!storage.isInitialized) { "KeyManager ist bereits initialisiert" }
        val dek = randomBytes(DEK_LENGTH_BYTES)
        persistWrap(dek, mode, passphrase)
        return dek
    }

    fun unlockWithoutPrompt(): ByteArray {
        requireMode(KeyMode.KEYSTORE_NO_LOCK, "unlockWithoutPrompt")
        val wrapped = storage.wrappedDek ?: error("Kein gewrappter DEK vorhanden")
        val iv = storage.dekIv ?: error("Kein IV vorhanden")
        val cipher = Cipher.getInstance(TRANSFORM_AES)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateNoLockKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(wrapped)
    }

    fun unlockWithKeystore(cryptoCipher: Cipher): ByteArray {
        requireMode(KeyMode.KEYSTORE_LOCK, "unlockWithKeystore")
        val wrapped = storage.wrappedDek ?: error("Kein gewrappter DEK vorhanden")
        return try {
            cryptoCipher.doFinal(wrapped)
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw KeyInvalidatedException(e)
        }
    }

    suspend fun unlockWithPassphrase(passphrase: CharArray): ByteArray {
        requireMode(KeyMode.PASSPHRASE, "unlockWithPassphrase")
        val wrapped = storage.wrappedDek ?: error("Kein gewrappter DEK vorhanden")
        val iv = storage.dekIv ?: error("Kein IV vorhanden")
        val salt = storage.passphraseSalt ?: error("Kein Salt vorhanden")
        val kek = derivePassphraseKek(passphrase, salt)
        return try {
            val cipher = Cipher.getInstance(TRANSFORM_AES)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(kek, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(wrapped)
        } catch (e: AEADBadTagException) {
            throw WrongPassphraseException(e)
        } finally {
            kek.wipe()
        }
    }

    fun getCipherForBiometricPrompt(): Cipher {
        requireMode(KeyMode.KEYSTORE_LOCK, "getCipherForBiometricPrompt")
        val privateKey = androidKeyStore().getKey(ALIAS_LOCK, null) as PrivateKey
        val cipher = Cipher.getInstance(TRANSFORM_RSA)
        return try {
            cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepSpec())
            cipher
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw KeyInvalidatedException(e)
        }
    }

    suspend fun switchMode(
        newMode: KeyMode,
        authenticatedCipher: Cipher? = null,
        currentPassphrase: CharArray? = null,
        newPassphrase: CharArray? = null,
    ): ByteArray {
        val dek = when (currentMode()) {
            KeyMode.KEYSTORE_NO_LOCK -> unlockWithoutPrompt()
            KeyMode.KEYSTORE_LOCK -> unlockWithKeystore(
                requireNotNull(authenticatedCipher) {
                    "Wechsel aus KEYSTORE_LOCK benoetigt einen per Biometrie freigeschalteten Cipher"
                }
            )
            KeyMode.PASSPHRASE -> unlockWithPassphrase(
                requireNotNull(currentPassphrase) { "Wechsel aus PASSPHRASE benoetigt die aktuelle Passphrase" }
            )
        }
        try {
            persistWrap(dek, newMode, newPassphrase)
            cleanupUnusedKeystoreAliases(newMode)
        } catch (t: Throwable) {
            dek.wipe()
            throw t
        }
        return dek
    }

    suspend fun rewrapDek(dek: ByteArray, newMode: KeyMode, newPassphrase: CharArray? = null) {
        persistWrap(dek, newMode, newPassphrase)
        cleanupUnusedKeystoreAliases(newMode)
    }

    fun wipeKeys() {
        storage.clear()
        val ks = androidKeyStore()
        listOf(ALIAS_NOLOCK, ALIAS_LOCK).forEach { if (ks.containsAlias(it)) ks.deleteEntry(it) }
    }

    private suspend fun persistWrap(dek: ByteArray, mode: KeyMode, passphrase: CharArray?) = withContext(Dispatchers.Default) {
        when (mode) {
            KeyMode.KEYSTORE_NO_LOCK -> {
                val cipher = Cipher.getInstance(TRANSFORM_AES)
                cipher.init(Cipher.ENCRYPT_MODE, getOrCreateNoLockKey())
                storage.wrappedDek = cipher.doFinal(dek)
                storage.dekIv = cipher.iv
                storage.passphraseSalt = null
            }
            KeyMode.KEYSTORE_LOCK -> {
                storage.wrappedDek = wrapWithLockKey(dek)
                storage.dekIv = null
                storage.passphraseSalt = null
            }
            KeyMode.PASSPHRASE -> {
                requireNotNull(passphrase) { "PASSPHRASE-Modus benoetigt eine Passphrase" }
                val salt = randomBytes(Argon2Kdf.SALT_LENGTH_BYTES)
                val kek = derivePassphraseKek(passphrase, salt)
                try {
                    val cipher = Cipher.getInstance(TRANSFORM_AES)
                    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(kek, "AES"))
                    storage.wrappedDek = cipher.doFinal(dek)
                    storage.dekIv = cipher.iv
                    storage.passphraseSalt = salt
                    storage.argonMemoryKiB = Argon2Kdf.MEMORY_KIB
                    storage.argonIterations = Argon2Kdf.ITERATIONS
                    storage.argonParallelism = Argon2Kdf.PARALLELISM
                } finally {
                    kek.wipe()
                }
            }
        }
        storage.mode = mode
    }

    private fun wrapWithLockKey(dek: ByteArray): ByteArray {
        getOrCreateLockKey()
        val publicKey = androidKeyStore().getCertificate(ALIAS_LOCK).publicKey
        val unrestricted = KeyFactory.getInstance(publicKey.algorithm)
            .generatePublic(X509EncodedKeySpec(publicKey.encoded))
        val cipher = Cipher.getInstance(TRANSFORM_RSA)
        cipher.init(Cipher.ENCRYPT_MODE, unrestricted, oaepSpec())
        return cipher.doFinal(dek)
    }

    private suspend fun derivePassphraseKek(passphrase: CharArray, salt: ByteArray): ByteArray {
        val pwBytes = passphrase.toUtf8Bytes()
        return try {
            Argon2Kdf.deriveKey(
                password = pwBytes,
                salt = salt,
                memoryKiB = storage.argonMemoryKiB,
                iterations = storage.argonIterations,
                parallelism = storage.argonParallelism,
            )
        } finally {
            pwBytes.wipe()
        }
    }

    private fun getOrCreateNoLockKey(): SecretKey {
        val ks = androidKeyStore()
        (ks.getKey(ALIAS_NOLOCK, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        fun spec(strongBox: Boolean) = KeyGenParameterSpec.Builder(
            ALIAS_NOLOCK,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .apply { if (strongBox) setIsStrongBoxBacked(true) }
            .build()
        return try {
            generator.init(spec(strongBox = true)); generator.generateKey()
        } catch (e: StrongBoxUnavailableException) {
            generator.init(spec(strongBox = false)); generator.generateKey()
        }
    }

    private fun getOrCreateLockKey() {
        val ks = androidKeyStore()
        if (ks.containsAlias(ALIAS_LOCK)) return
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
        fun spec(strongBox: Boolean) = KeyGenParameterSpec.Builder(
            ALIAS_LOCK,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        ).setKeySize(2048)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setMgf1Digests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
            )
            .setInvalidatedByBiometricEnrollment(false)
            .apply { if (strongBox) setIsStrongBoxBacked(true) }
            .build()
        try {
            generator.initialize(spec(strongBox = true)); generator.generateKeyPair()
        } catch (e: StrongBoxUnavailableException) {
            generator.initialize(spec(strongBox = false)); generator.generateKeyPair()
        }
    }

    private fun cleanupUnusedKeystoreAliases(newMode: KeyMode) {
        val ks = androidKeyStore()
        val keep = when (newMode) {
            KeyMode.KEYSTORE_NO_LOCK -> setOf(ALIAS_NOLOCK)
            KeyMode.KEYSTORE_LOCK -> setOf(ALIAS_LOCK)
            KeyMode.PASSPHRASE -> emptySet()
        }
        listOf(ALIAS_NOLOCK, ALIAS_LOCK).forEach {
            if (it !in keep && ks.containsAlias(it)) ks.deleteEntry(it)
        }
    }

    private fun androidKeyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun oaepSpec() = OAEPParameterSpec(
        "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT,
    )

    private fun randomBytes(size: Int): ByteArray =
        ByteArray(size).also { secureRandom.nextBytes(it) }

    private fun requireMode(expected: KeyMode, op: String) {
        val actual = currentMode()
        if (actual != expected) throw WrongModeException("$op ist nur im Modus $expected erlaubt (aktuell: $actual)")
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALIAS_NOLOCK = "mushotoku_kek_nolock"
        private const val ALIAS_LOCK = "mushotoku_kek_lock"
        private const val TRANSFORM_AES = "AES/GCM/NoPadding"
        private const val TRANSFORM_RSA = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        private const val DEK_LENGTH_BYTES = 32
        private const val GCM_TAG_BITS = 128
    }
}
