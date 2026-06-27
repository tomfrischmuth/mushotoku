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
import com.mushotoku.app.ui.screens.groupColor

import com.mushotoku.app.ui.strings.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mushotoku.app.data.Category
import com.mushotoku.app.data.Expense
import com.mushotoku.app.ui.theme.LocalAppColors
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OneTimeExpenseDialog(
    categories: ImmutableList<Category>,
    amountById: Map<String, Double>,
    onConfirm: (Category, Double) -> Unit,
    onDismiss: () -> Unit
) {
    val strings      = LocalAppStrings.current
    val colors       = LocalAppColors.current
    val focusManager = LocalFocusManager.current

    val groups = remember(categories) {
        categories.map { it.group }.distinct().sortedBy { grp -> categories.first { it.group == grp }.sortOrder }
    }

    var selectedGroup    by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var amountInput      by remember { mutableStateOf("") }
    var groupExpanded    by remember { mutableStateOf(false) }
    var catExpanded      by remember { mutableStateOf(false) }

    val catsInGroup = remember(selectedGroup, categories) {
        if (selectedGroup == null) emptyList() else categories.filter { it.group == selectedGroup }
    }

    val canConfirm = selectedCategory != null &&
        (amountInput.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.oneTimeExpenseBtn) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = groupExpanded,
                    onExpandedChange = { groupExpanded = !groupExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedGroup?.let { strings.groupName(it) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text(strings.selectGroupHint, color = colors.onSurfaceTertiary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(groupExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Color(0xFF3D5AFE),
                            unfocusedBorderColor = Color(0xFFDDDDDD)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = groupExpanded,
                        onDismissRequest = { groupExpanded = false }
                    ) {
                        groups.forEach { grp ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).background(groupColor(grp), CircleShape))
                                        Spacer(Modifier.width(8.dp))
                                        Text(strings.groupName(grp), fontSize = 14.sp)
                                    }
                                },
                                onClick = soundClick {
                                    if (grp != selectedGroup) {
                                        selectedGroup    = grp
                                        selectedCategory = null
                                    }
                                    groupExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedGroup != null) {
                    ExposedDropdownMenuBox(
                        expanded = catExpanded,
                        onExpandedChange = { catExpanded = !catExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.let { strings.categoryName(it.id, it.name) } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text(strings.selectCategoryHint, color = colors.onSurfaceTertiary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = Color(0xFF3D5AFE),
                                unfocusedBorderColor = Color(0xFFDDDDDD)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = catExpanded,
                            onDismissRequest = { catExpanded = false }
                        ) {
                            catsInGroup.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(strings.categoryName(cat.id, cat.name), fontSize = 14.sp) },
                                    onClick = soundClick { selectedCategory = cat; catExpanded = false }
                                )
                            }
                        }
                    }
                }

                BasicTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = colors.onSurface,
                        textAlign = TextAlign.Start
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(Color(0xFF3D5AFE)),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.background, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (amountInput.isEmpty()) {
                                Text("0,00", color = colors.onSurfaceTertiary, fontSize = 16.sp)
                            }
                            inner()
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = soundClick {
                    val cat     = selectedCategory ?: return@soundClick
                    val parsed  = amountInput.replace(',', '.').toDoubleOrNull() ?: return@soundClick
                    val rounded = (parsed / 0.5).roundToInt() * 0.5
                    if (rounded > 0.0) {
                        val existing = amountById[cat.id] ?: 0.0
                        onConfirm(cat, existing + rounded)
                    }
                    onDismiss()
                },
                enabled = canConfirm
            ) {
                Text(strings.add)
            }
        },
        dismissButton = {
            TextButton(onClick = soundClick(onDismiss)) { Text(strings.cancel) }
        }
    )
}
