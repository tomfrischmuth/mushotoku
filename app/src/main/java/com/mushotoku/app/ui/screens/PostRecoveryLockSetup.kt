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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.security.SecurityController
import com.mushotoku.app.ui.components.soundClick
import com.mushotoku.app.ui.strings.securityStrings
import com.mushotoku.app.ui.theme.DarkAppColors

/**
 * Shown right after a successful recovery-code unlock. The old lock has been removed,
 * and the user picks how to protect the app from here on — a fresh passphrase, the
 * biometric lock, or no lock. Any crypto work shows a busy dialog so the user sees that
 * something is happening.
 */
@Composable
fun PostRecoveryLockSetup(
    controller: SecurityController,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val s = remember(context) { securityStrings(context) }
    var showPassphrase by remember { mutableStateOf(false) }
    var recoveryCode by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkAppColors.background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = s.newLockTitle,
            color = DarkAppColors.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = s.newLockMessage,
            color = DarkAppColors.onSurfaceSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = soundClick { showPassphrase = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = DarkAppColors.accent, contentColor = Color.White),
        ) { Text(s.setupPassphrase) }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = soundClick { controller.enableBiometricLock { code -> recoveryCode = code } },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = DarkAppColors.accent, contentColor = Color.White),
        ) { Text(s.newLockBiometric) }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = soundClick(onDone)) {
            Text(s.newLockNone, color = DarkAppColors.onSurfaceSecondary)
        }
    }

    if (showPassphrase) {
        NewPassphraseDialog(
            title = s.setupPassphrase,
            warning = s.passphraseNoResetWarning,
            newLabel = s.newPassphrase,
            confirmLabel = s.confirmPassphrase,
            s = s,
            onConfirm = { chars ->
                showPassphrase = false
                controller.setupPassphrase(chars) { code ->
                    if (code != null) recoveryCode = code else onDone()
                }
            },
            onDismiss = { showPassphrase = false },
        )
    }

    recoveryCode?.let { code ->
        RecoveryCodeDialog(code = code, onDone = { recoveryCode = null; onDone() })
    }

    if (controller.busy) {
        SecBusyDialog(s.lockBusyTitle, s.lockBusyHint)
    }
}
