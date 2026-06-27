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

package com.mushotoku.app.holidays

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

object HolidayDefaults {

    fun resolveCountry(context: Context, chosenCountry: String): SupportedCountry {
        HolidayCatalog.byIso(chosenCountry)?.let { return it }
        HolidayCatalog.byIso(detectCountryIso(context))?.let { return it }
        return HolidayCatalog.countries.first()
    }

    fun detectCountryIso(context: Context): String? {
        val locale = Locale.getDefault()
        locale.country.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)?.let { iso ->
            if (HolidayCatalog.byIso(iso) != null) return iso
        }
        countryForLanguage(locale.language)?.let { return it }
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val sim = tm?.simCountryIso?.takeIf { it.isNotBlank() }
        val network = tm?.networkCountryIso?.takeIf { it.isNotBlank() }
        return (sim ?: network)?.uppercase(Locale.ROOT)
    }

    private fun countryForLanguage(language: String): String? = when (language.lowercase(Locale.ROOT)) {
        "de" -> "DE"
        "fr" -> "FR"
        "it" -> "IT"
        "es" -> "ES"
        "nl" -> "NL"
        "pl" -> "PL"
        "pt" -> "PT"
        "en" -> "US"
        else -> null
    }

    fun resolveRegion(context: Context, chosenCountry: String, chosenRegion: String): HolidayRegion {
        val country = resolveCountry(context, chosenCountry)
        val subdivision = chosenRegion.takeIf { it.isNotBlank() }
        return country.regionFor(subdivision)
    }
}
