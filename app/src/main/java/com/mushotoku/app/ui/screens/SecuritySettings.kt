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

package com.mushotoku.app.ui.screens

import com.mushotoku.app.ui.strings.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.AppSettings
import com.mushotoku.app.security.KeyMode
import com.mushotoku.app.security.LocalSecurityController
import com.mushotoku.app.ui.theme.LocalAppColors

@Composable
internal fun SicherheitSection(
    settings: AppSettings,
    onSetAppLockTimeout: (Int) -> Unit,
    onSetBlockScreenshots: (Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    val controller = LocalSecurityController.current
    val context = LocalContext.current
    val s = remember(LocalAppStrings.current.locale) { securityStrings(context) }

    var showEnableWarning by remember { mutableStateOf(false) }
    var showSetupPassphrase by remember { mutableStateOf(false) }
    var showChangePassphrase by remember { mutableStateOf(false) }
    var showDisableViaPassphrase by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        SecSectionLabel(s.sectionAppLock)

        SecCard {
            SecToggleRow(
                title = s.appLockTitle,
                subtitle = s.appLockHint,
                checked = controller.appLockEnabled,
                onCheckedChange = { enable ->
                    if (enable) {
                        if (controller.mode == KeyMode.KEYSTORE_NO_LOCK) showEnableWarning = true
                    } else {
                        controller.disableLock(onNeedPassphrase = { showDisableViaPassphrase = true })
                    }
                },
            )
        }

        Spacer(Modifier.height(10.dp))
        SecCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(s.protectionLevel, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = when (controller.mode) {
                            KeyMode.KEYSTORE_NO_LOCK -> s.levelNoLock
                            KeyMode.KEYSTORE_LOCK -> s.levelBiometric
                            KeyMode.PASSPHRASE -> s.levelPassphrase
                        },
                        color = colors.onSurfaceSecondary,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        if (controller.appLockEnabled) {
            Spacer(Modifier.height(10.dp))
            SecCard {
                Column(Modifier.padding(16.dp)) {
                    Text(s.timeoutTitle, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    SecSegmented(
                        options = listOf(0 to s.timeoutImmediate, 60 to s.timeoutOneMin, 300 to s.timeoutFiveMin),
                        selected = settings.appLockTimeoutSeconds,
                        onSelect = onSetAppLockTimeout,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        SecCard {
            if (controller.mode == KeyMode.PASSPHRASE) {
                SecClickRow(title = s.changePassphrase, onClick = { showChangePassphrase = true })
            } else {
                SecClickRow(title = s.setupPassphrase, subtitle = s.setupPassphraseHint, onClick = { showSetupPassphrase = true })
            }
        }

        controller.lastError?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Color(0xFFFF6B6B), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
        }

        Spacer(Modifier.height(24.dp))
        SecSectionLabel(s.sectionScreen)
        SecCard {
            SecToggleRow(
                title = s.blockScreenshotsTitle,
                subtitle = s.blockScreenshotsHint,
                checked = settings.blockScreenshots,
                onCheckedChange = onSetBlockScreenshots,
            )
        }

        Spacer(Modifier.height(24.dp))
        SecSectionLabel(s.sectionBackup)
        BackupCard(s)

        Spacer(Modifier.height(32.dp))
    }

    if (controller.busy) {
        SecBusyDialog(s.lockBusyTitle, s.lockBusyHint)
    }

    if (showEnableWarning) {
        SecConfirmDialog(
            title = s.enableWarnTitle,
            message = s.enableWarnMessage,
            confirmLabel = s.activate,
            dismissLabel = s.cancel,
            onConfirm = { showEnableWarning = false; controller.enableBiometricLock() },
            onDismiss = { showEnableWarning = false },
        )
    }

    if (showSetupPassphrase) {
        NewPassphraseDialog(
            title = s.setupPassphrase,
            warning = s.passphraseNoResetWarning,
            newLabel = s.newPassphrase,
            confirmLabel = s.confirmPassphrase,
            s = s,
            onConfirm = { newChars ->
                showSetupPassphrase = false
                controller.setupPassphrase(newChars)
            },
            onDismiss = { showSetupPassphrase = false },
        )
    }

    if (showChangePassphrase) {
        ChangePassphraseDialog(
            s = s,
            onConfirm = { oldChars, newChars ->
                controller.changePassphrase(oldChars, newChars, onDone = { showChangePassphrase = false })
            },
            onDismiss = { showChangePassphrase = false },
        )
    }

    if (showDisableViaPassphrase) {
        ConfirmPassphraseDialog(
            title = s.disableLockTitle,
            message = s.disableViaPassphrase,
            s = s,
            onConfirm = { chars ->
                showDisableViaPassphrase = false
                controller.disableLock(currentPassphrase = chars)
            },
            onDismiss = { showDisableViaPassphrase = false },
        )
    }
}
