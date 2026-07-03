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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.mushotoku.app.data.DatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class SecurityController(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val biometricPresence: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
) {
    private val keyManager get() = SecurityGate.keyManager

    var mode by mutableStateOf(currentModeOrDefault())
        private set

    var busy by mutableStateOf(false)
        private set

    var lastError by mutableStateOf<String?>(null)

    var relocked by mutableStateOf(false)
        private set

    var timeoutSeconds: Int = 0
        set(value) {
            field = value
            SecurityGate.relockTimeoutSeconds = value
        }

    private fun currentModeOrDefault(): KeyMode =
        runCatching { keyManager.currentMode() }.getOrDefault(KeyMode.KEYSTORE_NO_LOCK)

    private fun liveDek(): ByteArray =
        DatabaseProvider.currentDek() ?: error("Keine offene Datenbank – DEK nicht verfuegbar")

    fun refresh() {
        mode = currentModeOrDefault()
    }

    val appLockEnabled: Boolean get() = mode.requiresUserPresence

    val hasRecovery: Boolean get() = keyManager.hasRecoveryCode()

    /**
     * Switches to the biometric lock and provisions a recovery code in one step, so a
     * biometric lock can never leave the user without a non-destructive way back in.
     * The freshly generated code is handed to [onRecoveryCode] for one-time display.
     */
    fun enableBiometricLock(onRecoveryCode: (String) -> Unit = {}) = scope.launch {
        busy = true
        lastError = null
        try {
            val dek = liveDek()
            keyManager.rewrapDek(dek, KeyMode.KEYSTORE_LOCK)
            val code = provisionRecoveryCode(dek)
            refresh()
            onRecoveryCode(code)
        } catch (e: Exception) {
            lastError = e.message
        } finally {
            busy = false
        }
    }

    /** Generates and stores a new recovery code (invalidating the previous one). */
    fun regenerateRecoveryCode(onRecoveryCode: (String) -> Unit = {}) = scope.launch {
        busy = true
        lastError = null
        try {
            val code = provisionRecoveryCode(liveDek())
            onRecoveryCode(code)
        } catch (e: Exception) {
            lastError = e.message
        } finally {
            busy = false
        }
    }

    /**
     * Switches from the passphrase lock to the biometric lock, keeping the same
     * recovery code (same DEK). The current passphrase confirms the change.
     */
    fun switchToBiometric(
        currentPassphrase: CharArray,
        onDone: () -> Unit = {},
        onWrong: () -> Unit = {},
    ) = scope.launch {
        busy = true
        lastError = null
        try {
            val dek = keyManager.unlockWithPassphrase(currentPassphrase)
            try {
                keyManager.rewrapDek(dek, KeyMode.KEYSTORE_LOCK)
            } finally {
                dek.wipe()
            }
            refresh()
            onDone()
        } catch (e: WrongPassphraseException) {
            onWrong()
        } catch (e: Exception) {
            lastError = e.message
        } finally {
            busy = false
            currentPassphrase.wipe()
        }
    }

    private suspend fun provisionRecoveryCode(dek: ByteArray): String {
        val code = RecoveryCode.generate()
        val chars = code.toCharArray()
        try {
            keyManager.setRecoveryCode(dek, chars)
        } finally {
            chars.wipe()
        }
        return code
    }

    fun disableLock(
        currentPassphrase: CharArray? = null,
        onNeedPassphrase: () -> Unit = {},
        onDone: () -> Unit = {},
    ) {
        when (mode) {
            KeyMode.KEYSTORE_NO_LOCK -> onDone()
            KeyMode.KEYSTORE_LOCK -> biometricPresence(
                { scope.launch { runOp({ keyManager.rewrapDek(liveDek(), KeyMode.KEYSTORE_NO_LOCK); null }, onDone) } },
                { lastError = it },
            )
            KeyMode.PASSPHRASE -> {
                if (currentPassphrase == null) onNeedPassphrase()
                else scope.launch {
                    try {
                        runOp({ keyManager.switchMode(KeyMode.KEYSTORE_NO_LOCK, currentPassphrase = currentPassphrase) }, onDone)
                    } finally {
                        currentPassphrase.wipe()
                    }
                }
            }
        }
    }

    /**
     * Enables (or switches to) the passphrase lock. A recovery code is provisioned if
     * none exists yet, so a forgotten passphrase is still recoverable; switching from
     * the biometric lock keeps the existing code (same DEK). The freshly generated code,
     * if any, is handed to [onDone] for one-time display.
     */
    fun setupPassphrase(newPassphrase: CharArray, onDone: (String?) -> Unit = {}) {
        when (mode) {
            KeyMode.KEYSTORE_NO_LOCK -> scope.launch { switchToPassphrase(newPassphrase, onDone) }
            KeyMode.KEYSTORE_LOCK -> biometricPresence(
                { scope.launch { switchToPassphrase(newPassphrase, onDone) } },
                { lastError = it; newPassphrase.wipe(); onDone(null) },
            )
            KeyMode.PASSPHRASE -> { newPassphrase.wipe(); onDone(null) }
        }
    }

    private suspend fun switchToPassphrase(newPassphrase: CharArray, onDone: (String?) -> Unit) {
        busy = true
        lastError = null
        try {
            val dek = liveDek()
            keyManager.rewrapDek(dek, KeyMode.PASSPHRASE, newPassphrase)
            val code = if (keyManager.hasRecoveryCode()) null else provisionRecoveryCode(dek)
            refresh()
            onDone(code)
        } catch (e: Exception) {
            lastError = e.message
            onDone(null)
        } finally {
            busy = false
            newPassphrase.wipe()
        }
    }

    fun changePassphrase(
        oldPassphrase: CharArray,
        newPassphrase: CharArray,
        onDone: () -> Unit = {},
        onWrong: () -> Unit = {},
    ) = scope.launch {
        busy = true
        lastError = null
        try {
            keyManager.switchMode(
                KeyMode.PASSPHRASE,
                currentPassphrase = oldPassphrase,
                newPassphrase = newPassphrase,
            ).wipe()
            refresh()
            onDone()
        } catch (e: WrongPassphraseException) {
            onWrong()
        } catch (e: Exception) {
            lastError = e.message
        } finally {
            busy = false
            oldPassphrase.wipe()
            newPassphrase.wipe()
        }
    }

    fun onAppBackgrounded() {
        SecurityGate.onAppBackgrounded()
    }

    fun onAppForegrounded() {
        SecurityGate.evaluateRelock()
        relocked = SecurityGate.relocked
    }

    fun clearRelock() {
        SecurityGate.clearRelock()
        relocked = false
    }

    private suspend fun runOp(block: suspend () -> ByteArray?, onDone: () -> Unit) {
        busy = true
        lastError = null
        try {
            block()?.wipe()
            refresh()
            onDone()
        } catch (e: Exception) {
            lastError = e.message
        } finally {
            busy = false
        }
    }
}

val LocalSecurityController = staticCompositionLocalOf<SecurityController> {
    error("SecurityController nicht bereitgestellt")
}
