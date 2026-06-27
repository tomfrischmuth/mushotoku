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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.Category
import com.mushotoku.app.data.Expense
import com.mushotoku.app.finance.ExpenseEntry
import com.mushotoku.app.finance.ExpenseProjection
import com.mushotoku.app.finance.REISEN_CATEGORY_IDS
import com.mushotoku.app.finance.SAVINGS_ELIGIBLE_CATEGORY_IDS
import com.mushotoku.app.ui.theme.LocalAppColors
import com.mushotoku.app.ui.LocalAppCurrency
import java.time.LocalDate
import kotlin.math.roundToLong
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun SavingsPotentialDialog(
    yearExpenses: ImmutableList<Expense>,
    categories: ImmutableList<Category>,
    onDismiss: () -> Unit
) {
    val strings  = LocalAppStrings.current
    val currency = LocalAppCurrency.current
    val colors   = LocalAppColors.current
    val today    = remember { LocalDate.now() }

    val rows: List<SavingsRow> = remember(yearExpenses, categories) {
        val result = mutableListOf<SavingsRow>()

        categories
            .filter { it.isEnabled && it.id in SAVINGS_ELIGIBLE_CATEGORY_IDS }
            .forEach { cat ->
                val entries = yearExpenses
                    .filter { it.category == cat.id }
                    .map { exp -> ExpenseEntry(LocalDate.ofEpochDay(exp.date), (exp.amount * 100.0).roundToLong()) }
                if (entries.size >= 7) {
                    val ceiling = ExpenseProjection.annualSavingsCeiling(entries, emptyList(), today)
                    if (ceiling.lowerCents > 0L) result += SavingsRow.Single(cat, ceiling)
                }
            }

        val reisenEntries = yearExpenses
            .filter { it.category in REISEN_CATEGORY_IDS }
            .map { exp -> ExpenseEntry(LocalDate.ofEpochDay(exp.date), (exp.amount * 100.0).roundToLong()) }
        if (reisenEntries.size >= 7) {
            val ceiling = ExpenseProjection.annualSavingsCeiling(reisenEntries, emptyList(), today)
            if (ceiling.lowerCents > 0L) result += SavingsRow.Group("Reisen", ceiling)
        }

        result.sortedByDescending { it.ceiling.lowerCents }
    }

    val grandLower  = rows.sumOf { it.ceiling.lowerCents }
    val anyMissing  = rows.any { it.ceiling.hasMissingWeekdays }
    val maxCents    = rows.firstOrNull()?.ceiling?.lowerCents?.coerceAtLeast(1L) ?: 1L

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
                Text(strings.savingsPotentialTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(strings.savingsPotentialSubtitle, fontSize = 13.sp, color = colors.onSurfaceSecondary)
                Spacer(Modifier.height(20.dp))

                if (rows.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Text(strings.savingsNoData, fontSize = 14.sp, color = colors.onSurfaceSecondary, textAlign = TextAlign.Center)
                    }
                } else {
                    Text(
                        strings.savingsUpToPerYear(currency.formatWhole(grandLower / 100.0)),
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.onSurface
                    )
                    if (anyMissing) {
                        Spacer(Modifier.height(4.dp))
                        Text(strings.savingsConfidenceHint, fontSize = 11.sp, color = colors.onSurfaceSecondary)
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = colors.divider)

                    Spacer(Modifier.height(20.dp))

                    Text(strings.savingsBreakdownSection, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    Spacer(Modifier.height(10.dp))

                    rows.forEach { row ->
                        val label = when (row) {
                            is SavingsRow.Single -> strings.categoryName(row.category.id, row.category.name)
                            is SavingsRow.Group  -> strings.groupName(row.groupKey)
                        }
                        val ceiling  = row.ceiling
                        val fraction = (ceiling.lowerCents.toFloat() / maxCents).coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                label,
                                fontSize = 13.sp,
                                color    = colors.onSurface,
                                modifier = Modifier.width(100.dp),
                                maxLines = 2
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .background(colors.divider, RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .fillMaxHeight()
                                        .background(colors.accent, RoundedCornerShape(3.dp))
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                currency.format(ceiling.lowerCents / 100.0),
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = colors.onSurface
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = soundClick(onDismiss)) { Text(strings.close) }
            }
        }
    }
}
