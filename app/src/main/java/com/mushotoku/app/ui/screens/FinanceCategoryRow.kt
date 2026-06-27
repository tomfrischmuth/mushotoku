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

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.Category
import com.mushotoku.app.ui.LocalAppCurrency
import com.mushotoku.app.ui.strings.LocalAppStrings
import com.mushotoku.app.ui.theme.LocalAppColors
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
internal fun CategoryRow(
    category: Category,
    amount: Double,
    selectedDate: LocalDate,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onSetAmount: (Double) -> Unit
) {
    val strings      = LocalAppStrings.current
    val currency     = LocalAppCurrency.current
    val color        = groupColor(category.group)
    val colors       = LocalAppColors.current
    val focusManager = LocalFocusManager.current
    var editText         by remember(selectedDate) { mutableStateOf("") }
    var isFocused        by remember(selectedDate) { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customInput      by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 36.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(12.dp))
        Text(text = strings.categoryName(category.id, category.name), fontSize = 15.sp, color = colors.onSurface, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(48.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    // ripple() (no sound) + soundClick on onClick: short tap clicks,
                    // long-press (onLongClick) stays silent on release.
                    indication  = ripple(),
                    enabled     = amount > 0.0,
                    onClick     = soundClick(onRemove),
                    onLongClick = { onSetAmount(0.0) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = null,
                tint = if (amount > 0.0) Color(0xFF3D5AFE) else Color(0xFFCCCCCC),
                modifier = Modifier.size(20.dp)
            )
        }
        BasicTextField(
            value = editText,
            onValueChange = { editText = it },
            modifier = Modifier
                .widthIn(min = 72.dp)
                .onFocusChanged { state ->
                    val wasFocused = isFocused
                    isFocused = state.isFocused
                    if (state.isFocused && !wasFocused) {
                        editText = if (amount == 0.0) "" else "%.2f".format(amount).replace('.', ',')
                    } else if (!state.isFocused && wasFocused) {
                        val raw     = editText.replace(',', '.').toDoubleOrNull() ?: 0.0
                        val rounded = (raw / 0.5).roundToInt() * 0.5
                        onSetAmount(rounded.coerceAtLeast(0.0))
                        editText = ""
                    }
                },
            textStyle = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            decorationBox = { inner ->
                if (isFocused) inner()
                else Text(
                    text = currency.format(amount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (amount > 0.0) colors.onSurface else colors.onSurfaceTertiary,
                    textAlign = TextAlign.Center
                )
            }
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    // ripple() (no sound) + soundClick on onClick: short tap clicks,
                    // long-press (onLongClick) stays silent on release.
                    indication  = ripple(),
                    onClick     = soundClick(onAdd),
                    onLongClick = { customInput = ""; showCustomDialog = true }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF3D5AFE), modifier = Modifier.size(20.dp))
        }
    }

    if (showCustomDialog) {
        val focusManagerDialog = LocalFocusManager.current
        GlassAlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text(strings.addCustomAmountTitle) },
            text = {
                BasicTextField(
                    value = customInput,
                    onValueChange = { customInput = it },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = LocalAppColors.current.onSurface,
                        textAlign = TextAlign.Start
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(Color(0xFF3D5AFE)),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManagerDialog.clearFocus() }),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LocalAppColors.current.background, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (customInput.isEmpty()) {
                                Text("0,00", color = LocalAppColors.current.onSurfaceTertiary, fontSize = 16.sp)
                            }
                            inner()
                        }
                    }
                )
            },
            confirmButton = {
                Button(onClick = soundClick {
                    val parsed  = customInput.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val rounded = (parsed / 0.5).roundToInt() * 0.5
                    if (rounded > 0.0) onSetAmount(amount + rounded)
                    showCustomDialog = false
                }) {
                    Text(strings.add)
                }
            },
            dismissButton = {
                TextButton(onClick = soundClick { showCustomDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}
