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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mushotoku.app.ui.strings.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddCategoryDialog(
    onConfirm: (name: String, group: String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings       = LocalAppStrings.current
    var name          by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf(ALL_GROUPS.first()) }
    var expanded      by remember { mutableStateOf(false) }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addCategory) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(strings.categoryNameHint) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedGroup, onValueChange = {}, readOnly = true,
                        label = { Text(strings.categoryGroupHint) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        ALL_GROUPS.forEach { group ->
                            DropdownMenuItem(text = { Text(group) }, onClick = soundClick { selectedGroup = group; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = soundClick { if (name.isNotBlank()) onConfirm(name, selectedGroup) }, enabled = name.isNotBlank()) {
                Text(strings.add)
            }
        },
        dismissButton = { TextButton(onClick = soundClick(onDismiss)) { Text(strings.cancel) } }
    )
}
