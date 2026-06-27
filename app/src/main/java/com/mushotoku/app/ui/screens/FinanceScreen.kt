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
import com.mushotoku.app.ui.dialogs.*
import com.mushotoku.app.ui.*

import com.mushotoku.app.ui.strings.*

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.AdditionalIncome
import com.mushotoku.app.data.Category
import com.mushotoku.app.data.Expense
import com.mushotoku.app.data.RecurringCostHistory
import com.mushotoku.app.ui.theme.LocalAppColors
import com.mushotoku.app.ui.LocalAppCurrency
import java.time.LocalDate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun groupColor(group: String): Color = when (group) {
    "Wohnen"                 -> Color(0xFF1976D2)
    "Lebensmittel"           -> Color(0xFF43A047)
    "Essen & Trinken"        -> Color(0xFFE53935)
    "Transport"              -> Color(0xFF546E7A)
    "Gesundheit & Körper"    -> Color(0xFF00ACC1)
    "Kleidung & Accessoires" -> Color(0xFF8E24AA)
    "Freizeit"               -> Color(0xFFFB8C00)
    "Sport"                  -> Color(0xFFD84315)
    "Reisen"                 -> Color(0xFF039BE5)
    "Digitales"              -> Color(0xFF3949AB)
    "Bildung"                -> Color(0xFF00897B)
    "Soziales"               -> Color(0xFFD81B60)
    "Haustiere"              -> Color(0xFF6D4C41)
    "Finanzen & Vorsorge"    -> Color(0xFF2E7D32)
    "Familie & Kinder"       -> Color(0xFFFF6D00)
    "Beruf & Büro"           -> Color(0xFF37474F)
    "Sonstiges"              -> Color(0xFF757575)
    else                     -> Color(0xFF9E9E9E)
}

@Composable
fun FinanceScreen(
    selectedDate: LocalDate,
    expenses: ImmutableList<Expense>,
    historicalExpenses: ImmutableList<Expense>,
    categories: ImmutableList<Category>,
    recurringCostHistory: ImmutableList<RecurringCostHistory>,
    yearExpenses: ImmutableList<Expense>,
    contentPadding: PaddingValues,
    salary: Double,
    salaryDay: String,
    additionalIncomes: ImmutableList<AdditionalIncome>,
    historicalAdditionalIncomes: ImmutableList<AdditionalIncome>,
    onAddIncome: (label: String, amount: Double) -> Unit,
    onDeleteIncome: (AdditionalIncome) -> Unit,
    showOverview: Boolean,
    onDismissOverview: () -> Unit,
    showBudgetOverview: Boolean,
    onDismissBudgetOverview: () -> Unit,
    onAdd: (Category) -> Unit,
    onRemove: (Category) -> Unit,
    onSetAmount: (Category, Double) -> Unit
) {
    val strings         = LocalAppStrings.current
    val currency        = LocalAppCurrency.current
    val enabledCategories = remember(categories) { categories.filter { it.isEnabled } }
    val amountById = remember(expenses) { expenses.associate { it.category to it.amount } }
    val total      = remember(expenses) { expenses.sumOf { it.amount } }

    val grouped = remember(enabledCategories) {
        enabledCategories.groupBy { it.group }
    }

    val colors               = LocalAppColors.current
    val focusManager         = LocalFocusManager.current
    val enabledIds           = remember(enabledCategories) { enabledCategories.map { it.id }.toSet() }
    val oneTimeItems         = remember(expenses, enabledIds, categories) {
        val catById = categories.associateBy { it.id }
        expenses.filter { it.category !in enabledIds }
                .mapNotNull { exp -> catById[exp.category]?.let { it to exp.amount } }
                .sortedBy { (cat, _) -> cat.sortOrder }
    }
    val nonEnabledCategories = remember(categories, enabledIds) {
        categories.filter { it.id !in enabledIds }.sortedBy { it.sortOrder }.toImmutableList()
    }
    var showOneTimeDialog by remember { mutableStateOf(false) }
    var showIncomeDialog  by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
    ) {
            item { Spacer(Modifier.height(contentPadding.calculateTopPadding() + 12.dp)) }

            grouped.entries.forEach { (group, cats) ->
              item(key = "group_$group") {
                Column {
                Text(
                    text = strings.groupName(group),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    cats.forEachIndexed { i, category ->
                        CategoryRow(
                            category     = category,
                            amount       = amountById[category.id] ?: 0.0,
                            selectedDate = selectedDate,
                            onAdd        = { onAdd(category) },
                            onRemove     = { onRemove(category) },
                            onSetAmount  = { onSetAmount(category, it) }
                        )
                        if (i < cats.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = colors.divider
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                }
              }
            }

            item {
              Column {
            if (oneTimeItems.isNotEmpty()) {
                Text(
                    text = strings.oneTimeExpensesSection,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    oneTimeItems.forEachIndexed { i, (cat, amount) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 4.dp, height = 36.dp)
                                    .background(groupColor(cat.group), RoundedCornerShape(2.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.categoryName(cat.id, cat.name), fontSize = 15.sp, color = colors.onSurface)
                                Text(strings.groupName(cat.group), fontSize = 11.sp, color = colors.onSurfaceSecondary)
                            }
                            Text(
                                text = currency.format(amount),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            IconButton(onClick = soundClick { onSetAmount(cat, 0.0) }) {
                                Icon(Icons.Default.Remove, contentDescription = null, tint = Color(0xFF3D5AFE), modifier = Modifier.size(20.dp))
                            }
                        }
                        if (i < oneTimeItems.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = colors.divider)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            TextButton(
                onClick = soundClick { showOneTimeDialog = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF3D5AFE))
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.oneTimeExpenseBtn, fontSize = 14.sp)
            }
            Spacer(Modifier.height(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3D5AFE)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.totalLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(currency.format(total), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (additionalIncomes.isNotEmpty()) {
                Text(
                    text = strings.additionalIncomeSection,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    additionalIncomes.forEachIndexed { i, income ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 4.dp, height = 36.dp)
                                    .background(Color(0xFF2E7D32), RoundedCornerShape(2.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(income.label, fontSize = 15.sp, color = colors.onSurface, modifier = Modifier.weight(1f))
                            Text(
                                text = "+ ${currency.format(income.amount)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            IconButton(onClick = soundClick { onDeleteIncome(income) }) {
                                Icon(Icons.Default.Remove, contentDescription = null, tint = Color(0xFF3D5AFE), modifier = Modifier.size(20.dp))
                            }
                        }
                        if (i < additionalIncomes.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = colors.divider)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            TextButton(
                onClick = soundClick { showIncomeDialog = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.addIncomeBtn, fontSize = 14.sp)
            }

            Spacer(Modifier.height(contentPadding.calculateBottomPadding() + 16.dp))
              }
            }
    }

    if (showOverview) {
        FinanceOverviewDialog(
            historicalExpenses   = historicalExpenses,
            categories           = categories,
            recurringCostHistory = recurringCostHistory,
            yearExpenses         = yearExpenses,
            onDismiss            = onDismissOverview
        )
    }

    if (showBudgetOverview) {
        BudgetOverviewDialog(
            salary                      = salary,
            salaryDay                   = salaryDay,
            historicalExpenses          = historicalExpenses,
            categories                  = categories,
            today                       = selectedDate,
            historicalAdditionalIncomes = historicalAdditionalIncomes,
            onDismiss                   = onDismissBudgetOverview
        )
    }

    if (showOneTimeDialog) {
        OneTimeExpenseDialog(
            categories  = nonEnabledCategories,
            amountById  = amountById,
            onConfirm   = { cat, newAmount -> onSetAmount(cat, newAmount) },
            onDismiss   = { showOneTimeDialog = false }
        )
    }

    if (showIncomeDialog) {
        AddIncomeDialog(
            onConfirm = { label, amount ->
                onAddIncome(label, amount)
                showIncomeDialog = false
            },
            onDismiss = { showIncomeDialog = false }
        )
    }
}
