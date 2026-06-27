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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mushotoku.app.data.Category
import com.mushotoku.app.data.Expense
import com.mushotoku.app.data.RecurringCostHistory
import com.mushotoku.app.ui.theme.LocalAppColors
import com.mushotoku.app.ui.LocalAppCurrency
import com.mushotoku.app.export.FinanceReportPdf
import com.mushotoku.app.export.HtmlToPdfRenderer
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FinanceReportDialog(
    historicalExpenses: ImmutableList<Expense>,
    categories: ImmutableList<Category>,
    recurringCostHistory: ImmutableList<RecurringCostHistory>,
    onDismiss: () -> Unit
) {
    val strings      = LocalAppStrings.current
    val currency     = LocalAppCurrency.current
    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()
    val monthFmt     = DateTimeFormatter.ofPattern("MMMM yyyy", strings.locale)
    val dateFmt      = DateTimeFormatter.ofPattern("d. MMMM yyyy", strings.locale)
    val categoryById = remember(categories) { categories.associateBy { it.id } }
    var exporting    by remember { mutableStateOf(false) }

    val availableMonths: List<YearMonth> = remember(historicalExpenses) {
        val set = historicalExpenses.map {
            val d = LocalDate.ofEpochDay(it.date); YearMonth.of(d.year, d.monthValue)
        }.toMutableSet()
        set.add(YearMonth.now())
        set.sortedDescending()
    }

    var selectedMonth    by remember { mutableStateOf(availableMonths.firstOrNull() ?: YearMonth.now()) }
    var fromFifteenth    by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val fromDate = if (fromFifteenth) LocalDate.of(selectedMonth.year, selectedMonth.month, 15)
                  else selectedMonth.atDay(1)
    val toDate   = if (fromFifteenth) fromDate.plusMonths(1).minusDays(1)
                  else selectedMonth.atEndOfMonth()

    val periodByGroup: List<ReportGroupData> = remember(historicalExpenses, selectedMonth, fromFifteenth, recurringCostHistory) {
        val from = if (fromFifteenth) LocalDate.of(selectedMonth.year, selectedMonth.month, 15) else selectedMonth.atDay(1)
        val to   = if (fromFifteenth) from.plusMonths(1).minusDays(1) else selectedMonth.atEndOfMonth()

        val byCategory = mutableMapOf<String, Double>()
        historicalExpenses
            .filter { val d = LocalDate.ofEpochDay(it.date); !d.isBefore(from) && !d.isAfter(to) }
            .forEach { exp -> byCategory[exp.category] = (byCategory[exp.category] ?: 0.0) + exp.amount }
        val monthStr = "%04d-%02d".format(selectedMonth.year, selectedMonth.monthValue)
        recurringCostHistory
            .filter { e -> e.startMonth <= monthStr && (e.endMonth == null || e.endMonth >= monthStr) }
            .forEach { e -> byCategory[e.categoryId] = (byCategory[e.categoryId] ?: 0.0) + e.amount }

        byCategory.entries
            .filter { it.value > 0.0 }
            .groupBy { (id, _) -> categoryById[id]?.group ?: "Sonstiges" }
            .map { (group, entries) ->
                ReportGroupData(
                    group = group,
                    total = entries.sumOf { it.value },
                    cats  = entries.sortedByDescending { it.value }.map { it.key to it.value }
                )
            }
            .sortedByDescending { it.total }
    }

    val totalRecurring = remember(selectedMonth, recurringCostHistory) {
        recurringCostForMonth(selectedMonth, recurringCostHistory)
    }
    val periodTotal = periodByGroup.sumOf { it.total }

    val colors = LocalAppColors.current

    GlassDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.92f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 16.dp)) {
                Text(strings.detailedReportTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                Spacer(Modifier.height(16.dp))

                Text(strings.reportMonthLabel, fontSize = 12.sp, color = colors.onSurfaceSecondary)
                Spacer(Modifier.height(6.dp))
                ExposedDropdownMenuBox(expanded = dropdownExpanded, onExpandedChange = { dropdownExpanded = !dropdownExpanded }) {
                    OutlinedTextField(
                        value = selectedMonth.format(monthFmt).replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = colors.accent,
                            unfocusedBorderColor = colors.divider
                        )
                    )
                    ExposedDropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                        availableMonths.forEach { month ->
                            DropdownMenuItem(
                                text    = { Text(month.format(monthFmt).replaceFirstChar { it.uppercase() }) },
                                onClick = soundClick { selectedMonth = month; dropdownExpanded = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !fromFifteenth,
                        onClick  = soundClick { fromFifteenth = false },
                        label    = { Text(strings.reportFrom1st) }
                    )
                    FilterChip(
                        selected = fromFifteenth,
                        onClick  = soundClick { fromFifteenth = true },
                        label    = { Text(strings.reportFrom15th) }
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${fromDate.format(dateFmt)} – ${toDate.format(dateFmt)}",
                    fontSize = 12.sp, color = colors.onSurfaceSecondary
                )
            }

            HorizontalDivider(color = colors.divider)

            if (periodByGroup.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    item(key = "chart") {
                        Column {
                            val chartData = periodByGroup.map { groupColor(it.group) to it.total.toFloat() }
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                                DonutChart(data = chartData, modifier = Modifier.size(160.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(currency.format(periodTotal), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                                    Text(strings.totalLabel, fontSize = 10.sp, color = colors.onSurfaceSecondary)
                                }
                            }
                            if (totalRecurring > 0.0) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    strings.inclRecurringCosts(currency.format(totalRecurring)),
                                    fontSize = 12.sp, color = colors.onSurfaceSecondary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                        }
                    }

                    periodByGroup.forEach { groupData ->
                        item(key = "grp_${groupData.group}") {
                            val gColor = groupColor(groupData.group)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(11.dp).background(gColor, CircleShape))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    strings.groupName(groupData.group),
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface, modifier = Modifier.weight(1f)
                                )
                                Text(currency.format(groupData.total), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                            }
                        }

                        items(groupData.cats, key = { (catId, _) -> "cat_${groupData.group}_$catId" }) { (catId, catAmt) ->
                            val gColor  = groupColor(groupData.group)
                            val cat     = categoryById[catId]
                            val catName = cat?.let { strings.categoryName(it.id, it.name) } ?: catId
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 21.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(width = 2.dp, height = 12.dp).background(gColor.copy(alpha = 0.35f), RoundedCornerShape(1.dp)))
                                Spacer(Modifier.width(10.dp))
                                Text(catName, fontSize = 13.sp, color = colors.onSurfaceSecondary, modifier = Modifier.weight(1f))
                                Text(currency.format(catAmt), fontSize = 13.sp, color = colors.onSurface)
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Text(strings.noDataForPeriod, color = colors.onSurfaceSecondary, fontSize = 14.sp)
                }
            }

            HorizontalDivider(color = colors.divider)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    enabled = !exporting && periodByGroup.isNotEmpty(),
                    onClick = soundClick {
                        val es = exportStrings(context)
                        val section = FinanceReportPdf.Section(
                            heading   = "",
                            recurring = totalRecurring,
                            total     = periodTotal,
                            groups    = periodByGroup.map { g ->
                                FinanceReportPdf.Group(
                                    name  = strings.groupName(g.group),
                                    total = g.total,
                                    items = g.cats.map { (catId, amt) ->
                                        val cat = categoryById[catId]
                                        (cat?.let { strings.categoryName(it.id, it.name) } ?: catId) to amt
                                    }
                                )
                            }
                        )
                        val html = FinanceReportPdf.html(
                            title    = es.financeReportHeading,
                            subtitle = "${fromDate.format(dateFmt)} – ${toDate.format(dateFmt)}",
                            sections = listOf(section),
                            currency = currency,
                            monthTotalLabel       = es.financeMonthTotal,
                            inclRecurringTemplate = es.financeInclRecurring,
                        )
                        exporting = true
                        scope.launch {
                            try {
                                val file = File(context.cacheDir, "finanzbericht.pdf")
                                withContext(Dispatchers.IO) {
                                    file.outputStream().use { HtmlToPdfRenderer(context).render(html, it) }
                                }
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(intent, es.shareAction).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                )
                            } catch (_: Exception) {
                            } finally {
                                exporting = false
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.exportReport)
                }
                Button(onClick = soundClick(onDismiss)) { Text(strings.close) }
            }
        }
    }

}
