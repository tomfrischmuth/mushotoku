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
import java.util.Locale
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
            // Locale.ROOT keeps the raw output at '.' so the currency's own
            // separator decides, independent of the device locale.
            String.format(Locale.ROOT, "%.${decimalDigits}f", amount)
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
    AppCurrency("ILS", "₪",    true,  false, 2, '.'),
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

/** Code of a user-defined currency; never collides with a real ISO 4217 code. */
const val CUSTOM_CURRENCY_CODE = "CUSTOM"

private const val CUSTOM_PREFIX = "CUSTOM:"
private const val CUSTOM_SEP = '|'
const val CUSTOM_SYMBOL_MAX_LENGTH = 6

/**
 * A user-defined currency is stored in the very same settings column as a
 * built-in code, encoded as `CUSTOM:<symbol>|<B|A>|<S|N>|<digits>|<separator>`.
 * That keeps the whole feature free of a schema change.
 */
fun customCurrency(
    symbol: String,
    symbolBefore: Boolean,
    symbolSpace: Boolean,
    decimalDigits: Int,
    decimalSeparator: Char,
): AppCurrency = AppCurrency(
    code             = CUSTOM_CURRENCY_CODE,
    symbol           = sanitizeCustomSymbol(symbol),
    symbolBefore     = symbolBefore,
    symbolSpace      = symbolSpace,
    decimalDigits    = if (decimalDigits == 0) 0 else 2,
    decimalSeparator = if (decimalSeparator == ',') ',' else '.',
)

fun sanitizeCustomSymbol(symbol: String): String =
    symbol.filterNot { it == CUSTOM_SEP || it.isWhitespace() }.take(CUSTOM_SYMBOL_MAX_LENGTH)

fun AppCurrency.encode(): String =
    if (code != CUSTOM_CURRENCY_CODE) code
    else CUSTOM_PREFIX + listOf(
        symbol,
        if (symbolBefore) "B" else "A",
        if (symbolSpace) "S" else "N",
        decimalDigits.toString(),
        decimalSeparator.toString(),
    ).joinToString(CUSTOM_SEP.toString())

fun isCustomCurrency(stored: String): Boolean = stored.startsWith(CUSTOM_PREFIX)

private fun decodeCustom(stored: String): AppCurrency? {
    val parts = stored.removePrefix(CUSTOM_PREFIX).split(CUSTOM_SEP)
    if (parts.size != 5) return null
    val symbol = sanitizeCustomSymbol(parts[0])
    if (symbol.isEmpty()) return null
    return customCurrency(
        symbol           = symbol,
        symbolBefore     = parts[1] == "B",
        symbolSpace      = parts[2] == "S",
        decimalDigits    = parts[3].toIntOrNull() ?: 2,
        decimalSeparator = parts[4].firstOrNull() ?: '.',
    )
}

fun currencyByCode(code: String): AppCurrency =
    if (isCustomCurrency(code)) decodeCustom(code) ?: DEFAULT_CURRENCY
    else ALL_CURRENCIES.find { it.code == code } ?: DEFAULT_CURRENCY

val LocalAppCurrency = staticCompositionLocalOf { DEFAULT_CURRENCY }
