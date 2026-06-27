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

package com.mushotoku.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.roundToLong

data class AppCurrency(
    val code: String,
    val symbol: String,
    val symbolBefore: Boolean,
    val symbolSpace: Boolean,
    val decimalDigits: Int,
    val decimalSeparator: Char,
) {
    fun format(amount: Double): String {
        val formatted = if (decimalDigits == 0) {
            amount.roundToLong().toString()
        } else {
            "%.${decimalDigits}f".format(amount)
                .let { if (decimalSeparator == ',') it.replace('.', ',') else it }
        }
        val space = if (symbolSpace) " " else ""
        return if (symbolBefore) "$symbol$space$formatted" else "$formatted$space$symbol"
    }

    fun formatWhole(amount: Double): String {
        val whole = amount.toLong().toString()
        val space = if (symbolSpace) " " else ""
        return if (symbolBefore) "$symbol$space$whole" else "$whole$space$symbol"
    }
}

val ALL_CURRENCIES: List<AppCurrency> = listOf(
    AppCurrency("EUR", "€",    false, true,  2, ','),
    AppCurrency("USD", "$",    true,  false, 2, '.'),
    AppCurrency("GBP", "£",    true,  false, 2, '.'),
    AppCurrency("CHF", "Fr.",  true,  true,  2, '.'),
    AppCurrency("CAD", "CA$",  true,  false, 2, '.'),
    AppCurrency("AUD", "A$",   true,  false, 2, '.'),
    AppCurrency("INR", "₹",    true,  false, 2, '.'),
    AppCurrency("BRL", "R$",   true,  false, 2, ','),
    AppCurrency("MXN", "MX$",  true,  false, 2, '.'),
    AppCurrency("SEK", "kr",   false, true,  2, ','),
    AppCurrency("NOK", "kr",   false, true,  2, ','),
    AppCurrency("DKK", "kr.",  false, true,  2, ','),
    AppCurrency("PLN", "zł",   false, true,  2, ','),
    AppCurrency("CZK", "Kč",   false, true,  2, ','),
    AppCurrency("SGD", "S$",   true,  false, 2, '.'),
)

val DEFAULT_CURRENCY: AppCurrency = ALL_CURRENCIES.first()

fun currencyByCode(code: String): AppCurrency =
    ALL_CURRENCIES.find { it.code == code } ?: DEFAULT_CURRENCY

val LocalAppCurrency = staticCompositionLocalOf { DEFAULT_CURRENCY }
