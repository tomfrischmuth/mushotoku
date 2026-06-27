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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mushotoku.app.data.AdditionalIncome
import com.mushotoku.app.data.Category
import com.mushotoku.app.data.Expense
import com.mushotoku.app.ui.theme.LocalAppColors
import com.mushotoku.app.ui.LocalAppCurrency
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun BudgetOverviewDialog(
    salary: Double,
    salaryDay: String,
    historicalExpenses: ImmutableList<Expense>,
    categories: ImmutableList<Category>,
    today: LocalDate,
    historicalAdditionalIncomes: ImmutableList<AdditionalIncome>,
    onDismiss: () -> Unit
) {
    val strings       = LocalAppStrings.current
    val currency      = LocalAppCurrency.current
    val dateFmt       = DateTimeFormatter.ofPattern("d. MMM yyyy", strings.locale)
    val recurringCosts = remember(categories) { categories.sumOf { it.recurringCost } }

    val periodStart: LocalDate
    val periodEnd:   LocalDate
    if (salaryDay == "FIFTEENTH") {
        if (today.dayOfMonth >= 15) {
            periodStart = today.withDayOfMonth(15)
            periodEnd   = today.plusMonths(1).withDayOfMonth(14)
        } else {
            periodStart = today.minusMonths(1).withDayOfMonth(15)
            periodEnd   = today.withDayOfMonth(14)
        }
    } else {
        periodStart = today.withDayOfMonth(1)
        periodEnd   = YearMonth.of(today.year, today.month).atEndOfMonth()
    }

    val daysLeft  = ChronoUnit.DAYS.between(today, periodEnd).toInt() + 1
    val periodExpenses = remember(historicalExpenses, periodStart, periodEnd) {
        historicalExpenses.filter {
            val d = LocalDate.ofEpochDay(it.date)
            !d.isBefore(periodStart) && !d.isAfter(periodEnd)
        }.sumOf { it.amount }
    }
    val periodAdditionalIncome = remember(historicalAdditionalIncomes, periodStart, periodEnd) {
        historicalAdditionalIncomes.filter {
            val d = LocalDate.ofEpochDay(it.date)
            !d.isBefore(periodStart) && !d.isAfter(periodEnd)
        }.sumOf { it.amount }
    }

    val colors     = LocalAppColors.current
    val remaining  = salary + periodAdditionalIncome - recurringCosts - periodExpenses
    val perDay     = if (daysLeft > 0) remaining / daysLeft else 0.0
    val valueColor = if (remaining >= 0) Color(0xFF2E7D32) else Color(0xFFB71C1C)

    GlassDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(strings.budgetDialogTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(
                "${periodStart.format(dateFmt)} – ${periodEnd.format(dateFmt)}",
                fontSize = 13.sp, color = colors.onSurfaceSecondary
            )

            if (salary <= 0.0 && periodAdditionalIncome <= 0.0) {
                Spacer(Modifier.height(24.dp))
                Text(strings.budgetNoSalary, fontSize = 14.sp, color = colors.onSurfaceSecondary)
            } else {
                Spacer(Modifier.height(20.dp))

                if (salary > 0.0)
                    BudgetRow(label = strings.budgetSalaryRow, value = currency.format(salary), valueColor = colors.onSurface)
                if (periodAdditionalIncome > 0.0)
                    BudgetRow(label = strings.budgetAdditionalIncomeRow, value = "+ ${currency.format(periodAdditionalIncome)}", valueColor = Color(0xFF2E7D32))
                BudgetRow(label = strings.budgetRecurringRow, value = "− ${currency.format(recurringCosts)}", valueColor = colors.onSurfaceSecondary)
                BudgetRow(label = strings.budgetSpentRow,     value = "− ${currency.format(periodExpenses)}", valueColor = colors.onSurfaceSecondary)

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.divider)

                BudgetRow(label = strings.budgetRemainingRow, value = currency.format(remaining), valueColor = valueColor, bold = true)

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(strings.budgetDaysLeftRow(daysLeft), fontSize = 13.sp, color = colors.onSurfaceSecondary)
                        Text(strings.budgetPerDayRow, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    }
                    Text(currency.format(perDay), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = valueColor)
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = soundClick(onDismiss)) { Text(strings.close) }
            }
        }
    }
}
