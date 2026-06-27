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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mushotoku.app.data.Category
import com.mushotoku.app.data.Expense
import com.mushotoku.app.data.RecurringCostHistory
import com.mushotoku.app.ui.theme.LocalAppColors
import com.mushotoku.app.ui.LocalAppCurrency
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun FinanceOverviewDialog(
    historicalExpenses: ImmutableList<Expense>,
    categories: ImmutableList<Category>,
    recurringCostHistory: ImmutableList<RecurringCostHistory>,
    yearExpenses: ImmutableList<Expense>,
    onDismiss: () -> Unit
) {
    val strings      = LocalAppStrings.current
    val currency     = LocalAppCurrency.current
    val monthFmt     = DateTimeFormatter.ofPattern("MMMM yyyy", strings.locale)
    val categoryById = remember(categories) { categories.associateBy { it.id } }

    val byMonth = remember(historicalExpenses) {
        historicalExpenses
            .groupBy { val d = LocalDate.ofEpochDay(it.date); YearMonth.of(d.year, d.monthValue) }
            .entries.sortedByDescending { it.key }
    }

    val currentMonthEntry    = byMonth.firstOrNull()
    val currentMonthExpenses = currentMonthEntry?.value ?: emptyList()
    val currentMonth         = currentMonthEntry?.key ?: YearMonth.now()
    val totalRecurring       = remember(currentMonth, recurringCostHistory) {
        recurringCostForMonth(currentMonth, recurringCostHistory)
    }

    val currentMonthByGroup: List<Pair<String, Double>> = remember(currentMonthExpenses, currentMonth, recurringCostHistory) {
        val byCategory = mutableMapOf<String, Double>()
        currentMonthExpenses.forEach { exp ->
            byCategory[exp.category] = (byCategory[exp.category] ?: 0.0) + exp.amount
        }
        val monthStr = "%04d-%02d".format(currentMonth.year, currentMonth.monthValue)
        recurringCostHistory
            .filter { e -> e.startMonth <= monthStr && (e.endMonth == null || e.endMonth >= monthStr) }
            .forEach { e -> byCategory[e.categoryId] = (byCategory[e.categoryId] ?: 0.0) + e.amount }
        byCategory.entries
            .filter { it.value > 0.0 }
            .groupBy { (id, _) -> categoryById[id]?.group ?: "Sonstiges" }
            .mapValues { (_, entries) -> entries.sumOf { it.value } }
            .entries.sortedByDescending { it.value }
            .map { it.key to it.value }
    }

    val previousMonths: List<Triple<YearMonth, Double, Float>> = remember(byMonth, recurringCostHistory) {
        if (byMonth.size <= 1) emptyList()
        else {
            val maxTotal = byMonth.maxOfOrNull { (m, exps) ->
                exps.sumOf { it.amount } + recurringCostForMonth(m, recurringCostHistory)
            } ?: 1.0
            byMonth.drop(1).map { (month, monthExpenses) ->
                val monthTotal = monthExpenses.sumOf { it.amount } + recurringCostForMonth(month, recurringCostHistory)
                Triple(month, monthTotal, (monthTotal / maxTotal).toFloat().coerceIn(0f, 1f))
            }
        }
    }

    var showReport   by remember { mutableStateOf(false) }
    var showSavings  by remember { mutableStateOf(false) }
    val colors       = LocalAppColors.current

    GlassDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f)
    ) {
        val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.90f).dp
        Column(modifier = Modifier.heightIn(max = maxHeight)) {

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(strings.monthlyOverviewTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(
                    currentMonthEntry?.key?.format(monthFmt)?.replaceFirstChar { it.uppercase() }
                        ?: strings.currentMonthFallback,
                    fontSize = 13.sp, color = colors.onSurfaceSecondary
                )
                Spacer(Modifier.height(24.dp))

                if (currentMonthByGroup.isNotEmpty()) {
                    val chartData = currentMonthByGroup.map { (group, amt) -> groupColor(group) to amt.toFloat() }
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        DonutChart(data = chartData, modifier = Modifier.size(180.dp))
                        val monthTotal = currentMonthByGroup.sumOf { it.second }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(currency.format(monthTotal), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                            Text(strings.totalLabel, fontSize = 11.sp, color = colors.onSurfaceSecondary)
                        }
                    }
                    if (totalRecurring > 0.0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            strings.inclRecurringCosts(currency.format(totalRecurring)),
                            fontSize = 12.sp, color = colors.onSurfaceSecondary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    currentMonthByGroup.forEach { (group, amt) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(12.dp).background(groupColor(group), CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Text(strings.groupName(group), fontSize = 14.sp, color = colors.onSurface, modifier = Modifier.weight(1f))
                            Text(currency.format(amt), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                        }
                    }
                } else {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text(strings.noExpensesThisMonth, color = colors.onSurfaceSecondary, fontSize = 14.sp)
                    }
                }

                if (previousMonths.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = colors.divider)
                    Spacer(Modifier.height(16.dp))
                    Text(strings.previousMonths, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    Spacer(Modifier.height(12.dp))
                    previousMonths.forEach { (month, monthTotal, fraction) ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(month.format(monthFmt).replaceFirstChar { it.uppercase() }, fontSize = 13.sp, color = colors.onSurfaceSecondary)
                                Text(currency.format(monthTotal), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                            }
                            Spacer(Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(colors.divider, RoundedCornerShape(2.dp))) {
                                Box(modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight().background(colors.accent, RoundedCornerShape(2.dp)))
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick  = soundClick { showReport = true },
                    modifier = Modifier.weight(1f)
                ) { Text(strings.inDetailBtn, maxLines = 1) }
                OutlinedButton(
                    onClick  = soundClick { showSavings = true },
                    modifier = Modifier.weight(1f)
                ) { Text(strings.savingsPotentialBtn, maxLines = 1) }
                Button(
                    onClick  = soundClick(onDismiss),
                    modifier = Modifier.weight(1f)
                ) { Text(strings.close, maxLines = 1) }
            }
        }
    }

    if (showReport) {
        FinanceReportDialog(
            historicalExpenses   = historicalExpenses,
            categories           = categories,
            recurringCostHistory = recurringCostHistory,
            onDismiss            = { showReport = false }
        )
    }

    if (showSavings) {
        SavingsPotentialDialog(
            yearExpenses = yearExpenses,
            categories   = categories,
            onDismiss    = { showSavings = false }
        )
    }
}
