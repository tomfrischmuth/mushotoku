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
import com.mushotoku.app.ui.*

import com.mushotoku.app.ui.strings.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
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
import com.mushotoku.app.data.AppSettings
import com.mushotoku.app.data.Category
import com.mushotoku.app.ui.theme.LocalAppColors
import androidx.compose.material.icons.filled.ExpandMore
import kotlinx.collections.immutable.ImmutableList

internal val ALL_GROUPS = listOf(
    "Wohnen", "Lebensmittel", "Essen & Trinken", "Transport", "Gesundheit & Körper",
    "Kleidung & Accessoires", "Freizeit", "Sport", "Reisen", "Digitales", "Bildung",
    "Soziales", "Haustiere", "Finanzen & Vorsorge", "Familie & Kinder", "Beruf & Büro",
    "Sonstiges"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FinanzenSection(
    categories: ImmutableList<Category>,
    settings: AppSettings,
    onSetFinanceEnabled: (Boolean) -> Unit,
    onSetCategoryEnabled: (Category, Boolean) -> Unit,
    onSetCategoryRecurringCost: (Category, Double) -> Unit,
    onAddCategory: (name: String, group: String) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onSetSalary: (Double) -> Unit,
    onSetSalaryDay: (String) -> Unit,
    onSetCurrency: (String) -> Unit,
) {
    val strings      = LocalAppStrings.current
    val colors       = LocalAppColors.current
    val focusManager = LocalFocusManager.current
    var showAddDialog by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    val selectedCurrency = remember(settings.currency) { currencyByCode(settings.currency) }
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }

    var salaryText  by remember(settings.salary) {
        mutableStateOf(if (settings.salary == 0.0) "" else "%.2f".format(settings.salary).replace('.', ','))
    }
    var salaryFocused by remember { mutableStateOf(false) }

    val grouped = remember(categories) {
        ALL_GROUPS.mapNotNull { group ->
            val cats = categories.filter { it.group == group }
            if (cats.isEmpty()) null else group to cats
        } + categories.filter { it.group !in ALL_GROUPS }
            .groupBy { it.group }.entries.map { it.key to it.value }
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(16.dp))

        SectionLabel(strings.currencyLabel)
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = currencyExpanded,
                onExpandedChange = { currencyExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selectedCurrency.code}  ${selectedCurrency.symbol}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = colors.onSurfaceSecondary
                    )
                }
                ExposedDropdownMenu(
                    expanded = currencyExpanded,
                    onDismissRequest = { currencyExpanded = false },
                    containerColor = colors.surface
                ) {
                    ALL_CURRENCIES.sortedBy { it.code }.forEach { currency ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${currency.code}  ${currency.symbol}",
                                    fontSize = 14.sp,
                                    color = if (currency.code == settings.currency) colors.accent else colors.onSurface,
                                    fontWeight = if (currency.code == settings.currency) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onSetCurrency(currency.code)
                                currencyExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(strings.sectionSalary)
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.salaryAmountLabel, fontSize = 15.sp, color = colors.onSurface, modifier = Modifier.weight(1f))
                BasicTextField(
                    value = salaryText,
                    onValueChange = { salaryText = it },
                    modifier = Modifier
                        .widthIn(min = 100.dp)
                        .onFocusChanged { state ->
                            val was = salaryFocused
                            salaryFocused = state.isFocused
                            if (!state.isFocused && was) {
                                val parsed = salaryText.replace(',', '.').toDoubleOrNull() ?: 0.0
                                onSetSalary(parsed.coerceAtLeast(0.0))
                                salaryText = if (parsed == 0.0) "" else "%.2f".format(parsed).replace('.', ',')
                            }
                        },
                    textStyle = TextStyle(
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface, textAlign = TextAlign.End
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    decorationBox = { inner ->
                        if (salaryFocused) inner()
                        else Text(
                            text = if (settings.salary > 0.0) selectedCurrency.format(settings.salary) else selectedCurrency.format(0.0),
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            color = if (settings.salary > 0.0) colors.onSurface else colors.onSurfaceTertiary,
                            textAlign = TextAlign.End
                        )
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = colors.divider)

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(strings.salaryDayLabel, fontSize = 13.sp, color = colors.onSurfaceSecondary)
                Spacer(Modifier.height(8.dp))
                listOf("FIRST" to strings.salaryDayFirst, "FIFTEENTH" to strings.salaryDayFifteenth).forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetSalaryDay(key) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.salaryDay == key,
                            onClick = soundClick { onSetSalaryDay(key) },
                            colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, fontSize = 15.sp, color = colors.onSurface)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        SectionLabel(strings.sectionFinanceTab)
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.showFinanceTab, fontSize = 15.sp, color = colors.onSurface, modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.financeTabEnabled,
                    onCheckedChange = soundCheck(onSetFinanceEnabled),
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.accent)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        SectionLabel(strings.sectionCategories)
        grouped.forEach { (group, cats) ->
            val expanded = expandedGroups[group] == true
            CategoryGroupHeader(
                title        = strings.groupName(group),
                enabledCount = cats.count { it.isEnabled },
                totalCount   = cats.size,
                expanded     = expanded,
                onToggle     = { expandedGroups[group] = !expanded },
            )
            if (expanded) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    cats.forEachIndexed { i, category ->
                        CategorySettingsRow(
                            category           = category,
                            onSetEnabled       = { onSetCategoryEnabled(category, it) },
                            onSetRecurringCost = { onSetCategoryRecurringCost(category, it) },
                            onDelete           = if (!category.isDefault) ({ onDeleteCategory(category) }) else null
                        )
                        if (i < cats.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = colors.divider)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        OutlinedButton(
            onClick = soundClick { showAddDialog = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(strings.addCategory)
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showAddDialog) {
        AddCategoryDialog(
            onConfirm = { name, group -> onAddCategory(name, group); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }
}
