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

package com.mushotoku.app.ui.dialogs
import com.mushotoku.app.ui.components.*

import com.mushotoku.app.ui.strings.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mushotoku.app.ui.theme.LocalAppColors

@Composable
internal fun AddIncomeDialog(
    onConfirm: (label: String, amount: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val strings      = LocalAppStrings.current
    val colors       = LocalAppColors.current
    val focusManager = LocalFocusManager.current
    var label  by remember { mutableStateOf("") }
    var amtText by remember { mutableStateOf("") }

    val parsed   = amtText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val canSave  = label.isNotBlank() && parsed > 0.0

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text(strings.addIncomeTitle) },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = label,
                    onValueChange = { label = it },
                    label         = { Text(strings.incomeLabelHint) },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape   = RoundedCornerShape(12.dp),
                    colors  = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color(0xFF2E7D32),
                        unfocusedBorderColor = Color(0xFFDDDDDD)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = amtText,
                    onValueChange = { amtText = it },
                    label         = { Text(strings.budgetSalaryRow) },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction    = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        if (canSave) onConfirm(label, parsed)
                    }),
                    shape   = RoundedCornerShape(12.dp),
                    colors  = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color(0xFF2E7D32),
                        unfocusedBorderColor = Color(0xFFDDDDDD)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = soundClick { onConfirm(label, parsed) }, enabled = canSave) {
                Text(strings.add)
            }
        },
        dismissButton = {
            TextButton(onClick = soundClick(onDismiss)) { Text(strings.cancel) }
        }
    )
}
