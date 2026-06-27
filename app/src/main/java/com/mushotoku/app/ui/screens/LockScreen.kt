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
import com.mushotoku.app.ui.components.soundClick

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.security.KeyMode
import com.mushotoku.app.ui.brand.MushotokuWordmark
import com.mushotoku.app.ui.theme.DarkAppColors
import androidx.compose.ui.platform.LocalContext
import com.mushotoku.app.R

@Composable
fun LockScreen(
    mode: KeyMode,
    errorText: String?,
    keyInvalidated: Boolean,
    onRequestBiometric: () -> Unit,
    onSubmitPassphrase: (CharArray) -> Unit,
    onReset: () -> Unit,
) {
    val context = LocalContext.current
    val s = remember(context) { lockStrings(context) }
    val accent = DarkAppColors.accent

    LaunchedEffect(mode, keyInvalidated) {
        if (mode == KeyMode.KEYSTORE_LOCK && !keyInvalidated) onRequestBiometric()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkAppColors.background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MushotokuWordmark(
            modifier = Modifier.fillMaxWidth(0.6f),
            letterColor = DarkAppColors.onSurface,
        )
        Spacer(Modifier.height(48.dp))

        Icon(
            imageVector = if (mode == KeyMode.PASSPHRASE) Icons.Filled.Lock else Icons.Filled.Fingerprint,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.height(40.dp),
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = s.title,
            color = DarkAppColors.onSurface,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (mode == KeyMode.PASSPHRASE) s.passphraseHint else s.biometricHint,
            color = DarkAppColors.onSurfaceSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        when {
            keyInvalidated -> InvalidatedRecovery(s, accent, onReset)
            mode == KeyMode.PASSPHRASE -> PassphraseEntry(s, accent, onSubmitPassphrase)
            else -> Button(
                onClick = soundClick(onRequestBiometric),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
            ) { Text(s.unlock) }
        }

        if (errorText != null && !keyInvalidated) {
            Spacer(Modifier.height(16.dp))
            Text(errorText, color = Color(0xFFFF6B6B), fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PassphraseEntry(
    s: LockStrings,
    accent: Color,
    onSubmit: (CharArray) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    val submit = {
        if (value.isNotEmpty()) {
            val chars = value.toCharArray()
            onSubmit(chars)
            value = ""
        }
    }
    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        label = { Text(s.passphraseLabel, color = DarkAppColors.onSurfaceSecondary) },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = soundClick(submit),
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
    ) { Text(s.unlock) }
}

@Composable
private fun InvalidatedRecovery(
    s: LockStrings,
    accent: Color,
    onReset: () -> Unit,
) {
    Text(
        text = s.invalidatedMessage,
        color = Color(0xFFFFB74D),
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))
    TextButton(onClick = soundClick(onReset)) {
        Text(s.resetAction, color = accent)
    }
}

private class LockStrings(
    val title: String,
    val biometricHint: String,
    val passphraseHint: String,
    val passphraseLabel: String,
    val unlock: String,
    val invalidatedMessage: String,
    val resetAction: String,
)

private fun lockStrings(ctx: android.content.Context) = LockStrings(
    title = ctx.getString(R.string.lock_title),
    biometricHint = ctx.getString(R.string.lock_biometric_hint),
    passphraseHint = ctx.getString(R.string.lock_passphrase_hint),
    passphraseLabel = ctx.getString(R.string.lock_passphrase_label),
    unlock = ctx.getString(R.string.lock_unlock),
    invalidatedMessage = ctx.getString(R.string.lock_invalidated_message),
    resetAction = ctx.getString(R.string.lock_reset_action),
)
