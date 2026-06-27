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
import com.mushotoku.app.ui.components.soundCheck

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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

@Composable
internal fun CategoryGroupHeader(
    title: String,
    enabledCount: Int,
    totalCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = colors.onSurface, modifier = Modifier.weight(1f)
        )
        Text(
            text = "$enabledCount/$totalCount", fontSize = 12.sp,
            color = colors.onSurfaceSecondary
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = colors.onSurfaceSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
internal fun CategorySettingsRow(
    category: Category,
    onSetEnabled: (Boolean) -> Unit,
    onSetRecurringCost: (Double) -> Unit,
    onDelete: (() -> Unit)?
) {
    val strings      = LocalAppStrings.current
    val currency     = LocalAppCurrency.current
    val color        = groupColor(category.group)
    val colors       = LocalAppColors.current
    val focusManager = LocalFocusManager.current
    var editText  by remember(category.id) { mutableStateOf("") }
    var isFocused by remember(category.id) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(width = 4.dp, height = 20.dp).background(color, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(12.dp))
            Text(
                text = strings.categoryName(category.id, category.name), fontSize = 15.sp,
                color = if (category.isEnabled) colors.onSurface else colors.onSurfaceTertiary,
                modifier = Modifier.weight(1f)
            )
            if (onDelete != null) {
                IconButton(onClick = soundClick(onDelete), modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                }
            }
            Checkbox(
                checked = category.isEnabled,
                onCheckedChange = soundCheck(onSetEnabled),
                colors = CheckboxDefaults.colors(checkedColor = colors.accent)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.recurringCostsLabel, fontSize = 13.sp, color = colors.onSurfaceSecondary, modifier = Modifier.weight(1f))
            BasicTextField(
                value = editText,
                onValueChange = { editText = it },
                modifier = Modifier
                    .widthIn(min = 90.dp)
                    .onFocusChanged { state ->
                        val wasFocused = isFocused
                        isFocused = state.isFocused
                        if (state.isFocused && !wasFocused) {
                            editText = if (category.recurringCost == 0.0) ""
                            else "%.2f".format(category.recurringCost).replace('.', ',')
                        } else if (!state.isFocused && wasFocused) {
                            val parsed = editText.replace(',', '.').toDoubleOrNull() ?: 0.0
                            val rounded = (Math.round(parsed * 2) / 2.0).coerceAtLeast(0.0)
                            onSetRecurringCost(rounded)
                            editText = ""
                        }
                    },
                textStyle = TextStyle(
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface, textAlign = TextAlign.End
                ),
                singleLine = true,
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                decorationBox = { inner ->
                    if (isFocused) inner()
                    else Text(
                        text = currency.format(if (category.recurringCost > 0.0) category.recurringCost else 0.0),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = if (category.recurringCost > 0.0) colors.onSurface else colors.onSurfaceTertiary,
                        textAlign = TextAlign.End
                    )
                }
            )
            Text(" ${strings.perMonth}", fontSize = 12.sp, color = colors.onSurfaceSecondary)
        }
    }
}
