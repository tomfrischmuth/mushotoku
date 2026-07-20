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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.ui.AppCurrency
import com.mushotoku.app.ui.CUSTOM_SYMBOL_MAX_LENGTH
import com.mushotoku.app.ui.customCurrency
import com.mushotoku.app.ui.encode
import com.mushotoku.app.ui.sanitizeCustomSymbol
import com.mushotoku.app.ui.strings.LocalAppStrings
import com.mushotoku.app.ui.theme.LocalAppColors

private const val PREVIEW_AMOUNT = 1234.56

/**
 * Lets people define a currency the curated list does not cover: symbol,
 * where it sits, and how the amount is written. The preview shows the exact
 * result before saving.
 */
@Composable
internal fun CustomCurrencyDialog(
    initial: AppCurrency?,
    onConfirm: (encoded: String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val colors  = LocalAppColors.current

    var symbol       by remember { mutableStateOf(initial?.symbol ?: "") }
    var symbolBefore by remember { mutableStateOf(initial?.symbolBefore ?: true) }
    var symbolSpace  by remember { mutableStateOf(initial?.symbolSpace ?: false) }
    var withDecimals by remember { mutableStateOf((initial?.decimalDigits ?: 2) != 0) }
    var commaSep     by remember { mutableStateOf(initial?.decimalSeparator == ',') }

    val cleanSymbol = sanitizeCustomSymbol(symbol)
    val preview = remember(cleanSymbol, symbolBefore, symbolSpace, withDecimals, commaSep) {
        customCurrency(
            symbol           = cleanSymbol.ifEmpty { "¤" },
            symbolBefore     = symbolBefore,
            symbolSpace      = symbolSpace,
            decimalDigits    = if (withDecimals) 2 else 0,
            decimalSeparator = if (commaSep) ',' else '.',
        ).format(PREVIEW_AMOUNT)
    }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.currencyCustomTitle) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it.take(CUSTOM_SYMBOL_MAX_LENGTH * 2) },
                    label = { Text(strings.currencyCustomSymbol) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OptionGroup(strings.currencyCustomPosition) {
                    OptionRow(strings.currencyCustomPositionBefore, symbolBefore) { symbolBefore = true }
                    OptionRow(strings.currencyCustomPositionAfter, !symbolBefore) { symbolBefore = false }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        strings.currencyCustomSpace,
                        fontSize = 15.sp,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = symbolSpace,
                        onCheckedChange = soundCheck { symbolSpace = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.accent)
                    )
                }

                OptionGroup(strings.currencyCustomDecimals) {
                    OptionRow(strings.currencyCustomDecimalsTwo, withDecimals) { withDecimals = true }
                    OptionRow(strings.currencyCustomDecimalsNone, !withDecimals) { withDecimals = false }
                }

                if (withDecimals) {
                    OptionGroup(strings.currencyCustomSeparator) {
                        OptionRow(strings.currencyCustomSeparatorDot, !commaSep) { commaSep = false }
                        OptionRow(strings.currencyCustomSeparatorComma, commaSep) { commaSep = true }
                    }
                }

                Spacer(Modifier.padding(top = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.currencyCustomPreview, fontSize = 13.sp, color = colors.onSurfaceSecondary)
                    Spacer(Modifier.width(12.dp))
                    Text(preview, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = soundClick {
                    onConfirm(
                        customCurrency(
                            symbol           = cleanSymbol,
                            symbolBefore     = symbolBefore,
                            symbolSpace      = symbolSpace,
                            decimalDigits    = if (withDecimals) 2 else 0,
                            decimalSeparator = if (commaSep) ',' else '.',
                        ).encode()
                    )
                },
                enabled = cleanSymbol.isNotEmpty()
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = soundClick(onDismiss)) { Text(strings.cancel) } }
    )
}

@Composable
private fun OptionGroup(label: String, content: @Composable () -> Unit) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).selectableGroup()) {
        Text(label, fontSize = 13.sp, color = colors.onSurfaceSecondary)
        content()
    }
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = soundClick(onSelect),
            colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
        )
        Text(label, fontSize = 15.sp, color = colors.onSurface)
    }
}
