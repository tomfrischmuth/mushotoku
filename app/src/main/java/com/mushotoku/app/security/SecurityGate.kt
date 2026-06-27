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
import android.os.SystemClock
import com.mushotoku.app.data.DatabaseProvider
import com.mushotoku.app.data.PlaintextDbMigration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SecurityGate {

    @Volatile
    lateinit var keyManager: KeyManager
        private set

    fun install(keyManager: KeyManager) {
        this.keyManager = keyManager
    }

    @Volatile var relockTimeoutSeconds: Int = 0

    @Volatile private var backgroundedAt: Long = 0L

    @Volatile var relocked: Boolean = false
        private set

    private fun gateReady(): Boolean = ::keyManager.isInitialized && keyManager.isInitialized()

    fun onAppBackgrounded() {
        if (!relocked) backgroundedAt = SystemClock.elapsedRealtime()
    }

    fun evaluateRelock() {
        if (!gateReady() || !keyManager.requiresUserPresence()) {
            relocked = false
            backgroundedAt = 0L
            return
        }
        if (backgroundedAt == 0L) return
        val elapsed = SystemClock.elapsedRealtime() - backgroundedAt
        if (elapsed >= relockTimeoutSeconds * 1000L) relocked = true
    }

    fun clearRelock() {
        relocked = false
        backgroundedAt = 0L
    }

    enum class StartGate {
        UNLOCKED,

        NEEDS_BIOMETRIC,

        NEEDS_PASSPHRASE,
    }

    suspend fun prepare(context: Context): StartGate = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        if (DatabaseProvider.isOpen) {
            if (gateReady() && keyManager.requiresUserPresence() && relocked) {
                return@withContext when (keyManager.currentMode()) {
                    KeyMode.KEYSTORE_LOCK -> StartGate.NEEDS_BIOMETRIC
                    KeyMode.PASSPHRASE -> StartGate.NEEDS_PASSPHRASE
                    KeyMode.KEYSTORE_NO_LOCK -> StartGate.UNLOCKED
                }
            }
            return@withContext StartGate.UNLOCKED
        }

        if (!keyManager.isInitialized()) {
            val dek = keyManager.initialize(KeyMode.KEYSTORE_NO_LOCK)
            try {
                PlaintextDbMigration.migrate(appCtx, dek)
            } catch (t: Throwable) {
                keyManager.wipeKeys()
                dek.wipe()
                throw t
            }
            DatabaseProvider.open(appCtx, dek)
            return@withContext StartGate.UNLOCKED
        }

        if (!keyManager.requiresUserPresence()) {
            val dek = keyManager.unlockWithoutPrompt()
            DatabaseProvider.open(appCtx, dek)
            return@withContext StartGate.UNLOCKED
        }

        when (keyManager.currentMode()) {
            KeyMode.KEYSTORE_LOCK -> StartGate.NEEDS_BIOMETRIC
            KeyMode.PASSPHRASE -> StartGate.NEEDS_PASSPHRASE
            KeyMode.KEYSTORE_NO_LOCK -> StartGate.UNLOCKED
        }
    }

    fun unlockWithKeystore(context: Context, cryptoCipher: javax.crypto.Cipher) {
        val dek = keyManager.unlockWithKeystore(cryptoCipher)
        DatabaseProvider.open(context.applicationContext, dek)
    }

    suspend fun unlockWithPassphrase(context: Context, passphrase: CharArray) {
        val dek = keyManager.unlockWithPassphrase(passphrase)
        DatabaseProvider.open(context.applicationContext, dek)
    }

    suspend fun resetAndReinitialize(context: Context): Unit = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        DatabaseProvider.close()
        keyManager.wipeKeys()
        listOf("", "-wal", "-shm", "-journal").forEach {
            appCtx.getDatabasePath("mushotoku.db$it").delete()
        }
        val dek = keyManager.initialize(KeyMode.KEYSTORE_NO_LOCK)
        DatabaseProvider.open(appCtx, dek)
    }
}
