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

import com.mushotoku.app.ui.components.*
import com.mushotoku.app.ui.strings.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.ui.theme.LocalAppColors

internal const val MIN_PASSPHRASE_LENGTH = 8

@Composable
internal fun SecSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = LocalAppColors.current.onSurfaceSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
internal fun SecCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = LocalAppColors.current.surface),
    ) { content() }
}

@Composable
internal fun SecToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = colors.onSurfaceSecondary, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = soundCheck(onCheckedChange),
            colors = SwitchDefaults.colors(checkedTrackColor = colors.accent),
        )
    }
}

@Composable
internal fun SecClickRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, color = colors.onSurfaceSecondary, fontSize = 13.sp)
        }
        Icon(
            Icons.Default.ChevronRight, contentDescription = null,
            tint = colors.onSurfaceTertiary, modifier = Modifier.width(20.dp),
        )
    }
}

@Composable
internal fun SecSegmented(
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val isSel = value == selected
            Text(
                text = label,
                color = if (isSel) Color.White else colors.onSurfaceSecondary,
                fontSize = 13.sp,
                fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .clickable { onSelect(value) }
                    .background(
                        color = if (isSel) colors.accent else colors.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
internal fun SecBusyDialog(title: String, hint: String) {
    val colors = LocalAppColors.current
    GlassAlertDialog(
        onDismissRequest = {},
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 3.dp, color = colors.accent)
                Spacer(Modifier.width(12.dp))
                Text(title, color = colors.onSurface)
            }
        },
        text = { Text(hint, color = colors.onSurfaceSecondary, fontSize = 14.sp) },
    )
}

@Composable
internal fun SecConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = colors.onSurface) },
        text = { Text(message, color = colors.onSurfaceSecondary, fontSize = 14.sp) },
        confirmButton = { TextButton(onClick = soundClick(onConfirm)) { Text(confirmLabel, color = colors.accent) } },
        dismissButton = { TextButton(onClick = soundClick(onDismiss)) { Text(dismissLabel, color = colors.onSurfaceSecondary) } },
    )
}

@Composable
internal fun NewPassphraseDialog(
    title: String,
    warning: String,
    newLabel: String,
    confirmLabel: String,
    s: SecurityStrings,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = colors.onSurface) },
        text = {
            Column {
                Text(warning, color = Color(0xFFFFB74D), fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                PassphraseField(p1, { p1 = it }, newLabel)
                Spacer(Modifier.height(8.dp))
                PassphraseField(p2, { p2 = it }, confirmLabel)
                error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = Color(0xFFFF6B6B), fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = soundClick {
                when {
                    p1.length < MIN_PASSPHRASE_LENGTH -> error = s.tooShort
                    p1 != p2 -> error = s.mismatch
                    else -> onConfirm(p1.toCharArray())
                }
            }) { Text(s.save, color = colors.accent) }
        },
        dismissButton = { TextButton(onClick = soundClick(onDismiss)) { Text(s.cancel, color = colors.onSurfaceSecondary) } },
    )
}

@Composable
internal fun ChangePassphraseDialog(
    s: SecurityStrings,
    onConfirm: (CharArray, CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    var oldP by remember { mutableStateOf("") }
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.changePassphrase, color = colors.onSurface) },
        text = {
            Column {
                PassphraseField(oldP, { oldP = it }, s.currentPassphrase)
                Spacer(Modifier.height(8.dp))
                PassphraseField(p1, { p1 = it }, s.newPassphrase)
                Spacer(Modifier.height(8.dp))
                PassphraseField(p2, { p2 = it }, s.confirmPassphrase)
                error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = Color(0xFFFF6B6B), fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = soundClick {
                when {
                    p1.length < MIN_PASSPHRASE_LENGTH -> error = s.tooShort
                    p1 != p2 -> error = s.mismatch
                    else -> onConfirm(oldP.toCharArray(), p1.toCharArray())
                }
            }) { Text(s.save, color = colors.accent) }
        },
        dismissButton = { TextButton(onClick = soundClick(onDismiss)) { Text(s.cancel, color = colors.onSurfaceSecondary) } },
    )
}

@Composable
internal fun ConfirmPassphraseDialog(
    title: String,
    message: String,
    s: SecurityStrings,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    var p by remember { mutableStateOf("") }
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = colors.onSurface) },
        text = {
            Column {
                Text(message, color = colors.onSurfaceSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                PassphraseField(p, { p = it }, s.currentPassphrase)
            }
        },
        confirmButton = {
            TextButton(onClick = soundClick { if (p.isNotEmpty()) onConfirm(p.toCharArray()) }) {
                Text(s.confirm, color = colors.accent)
            }
        },
        dismissButton = { TextButton(onClick = soundClick(onDismiss)) { Text(s.cancel, color = colors.onSurfaceSecondary) } },
    )
}

@Composable
internal fun PassphraseField(value: String, onValueChange: (String) -> Unit, label: String) {
    val colors = LocalAppColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        label = { Text(label, color = colors.onSurfaceSecondary) },
        modifier = Modifier.fillMaxWidth(),
    )
}
