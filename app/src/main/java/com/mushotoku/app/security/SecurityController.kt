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

    fun enableBiometricLock(onDone: () -> Unit = {}) = scope.launch {
        runOp({ keyManager.rewrapDek(liveDek(), KeyMode.KEYSTORE_LOCK); null }, onDone)
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

    fun setupPassphrase(newPassphrase: CharArray, onDone: () -> Unit = {}) {
        when (mode) {
            KeyMode.KEYSTORE_NO_LOCK -> scope.launch {
                try {
                    runOp({ keyManager.rewrapDek(liveDek(), KeyMode.PASSPHRASE, newPassphrase); null }, onDone)
                } finally {
                    newPassphrase.wipe()
                }
            }
            KeyMode.KEYSTORE_LOCK -> biometricPresence(
                {
                    scope.launch {
                        try {
                            runOp({ keyManager.rewrapDek(liveDek(), KeyMode.PASSPHRASE, newPassphrase); null }, onDone)
                        } finally {
                            newPassphrase.wipe()
                        }
                    }
                },
                { lastError = it; newPassphrase.wipe() },
            )
            KeyMode.PASSPHRASE -> { newPassphrase.wipe(); onDone() }
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
