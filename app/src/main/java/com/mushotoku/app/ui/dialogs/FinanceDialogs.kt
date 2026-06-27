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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mushotoku.app.data.Category
import com.mushotoku.app.data.RecurringCostHistory
import com.mushotoku.app.finance.SavingsCeiling
import com.mushotoku.app.ui.theme.LocalAppColors
import java.time.YearMonth

internal data class ReportGroupData(
    val group: String,
    val total: Double,
    val cats: List<Pair<String, Double>>
)

internal fun recurringCostForMonth(month: YearMonth, history: List<RecurringCostHistory>): Double {
    val monthStr = "%04d-%02d".format(month.year, month.monthValue)
    return history.filter { e -> e.startMonth <= monthStr && (e.endMonth == null || e.endMonth >= monthStr) }
        .sumOf { it.amount }
}

@Composable
internal fun BudgetRow(label: String, value: String, valueColor: Color, bold: Boolean = false) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = colors.onSurfaceSecondary)
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

internal sealed class SavingsRow {
    abstract val ceiling: SavingsCeiling
    data class Single(val category: Category, override val ceiling: SavingsCeiling) : SavingsRow()
    data class Group(val groupKey: String, override val ceiling: SavingsCeiling) : SavingsRow()
}

@Composable
internal fun DonutChart(data: List<Pair<Color, Float>>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val total = data.sumOf { it.second.toDouble() }.toFloat()
        if (total == 0f) return@Canvas
        val strokeW = size.minDimension * 0.18f
        val inset   = strokeW / 2f
        val arcSize = Size(size.width - strokeW, size.height - strokeW)
        var startAngle = -90f
        data.forEach { (color, value) ->
            val sweep = value / total * 360f
            drawArc(
                color = color, startAngle = startAngle, sweepAngle = sweep - 2f,
                useCenter = false, topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
            startAngle += sweep
        }
    }
}
