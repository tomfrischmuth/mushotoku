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

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.TemporalAdjusters

object DeHolidays {
    private val neujahr = Fixed(Month.JANUARY, 1, "holiday_de_neujahr")
    private val karfreitag = EasterRelative(-2, "holiday_de_karfreitag")
    private val ostermontag = EasterRelative(1, "holiday_de_ostermontag")
    private val tagDerArbeit = Fixed(Month.MAY, 1, "holiday_de_tag_der_arbeit")
    private val himmelfahrt = EasterRelative(39, "holiday_de_christi_himmelfahrt")
    private val pfingstmontag = EasterRelative(50, "holiday_de_pfingstmontag")
    private val einheit = Fixed(Month.OCTOBER, 3, "holiday_de_einheit")
    private val weihnachten1 = Fixed(Month.DECEMBER, 25, "holiday_de_weihnachten_1")
    private val weihnachten2 = Fixed(Month.DECEMBER, 26, "holiday_de_weihnachten_2")

    val national = listOf(
        neujahr, karfreitag, ostermontag, tagDerArbeit,
        himmelfahrt, pfingstmontag, einheit, weihnachten1, weihnachten2
    )

    private val dreikoenige = Fixed(Month.JANUARY, 6, "holiday_de_dreikoenige")
    private val frauentag = Fixed(Month.MARCH, 8, "holiday_de_frauentag")
    private val fronleichnam = EasterRelative(60, "holiday_de_fronleichnam")
    private val mariaeHimmelfahrt = Fixed(Month.AUGUST, 15, "holiday_de_mariae_himmelfahrt")
    private val weltkindertag = Fixed(Month.SEPTEMBER, 20, "holiday_de_weltkindertag")
    private val reformationstag = Fixed(Month.OCTOBER, 31, "holiday_de_reformationstag")
    private val allerheiligen = Fixed(Month.NOVEMBER, 1, "holiday_de_allerheiligen")
    private val bussUndBettag = Custom("holiday_de_buss_und_bettag") { y ->
        LocalDate.of(y, 11, 23).with(TemporalAdjusters.previous(DayOfWeek.WEDNESDAY))
    }

    val regions: List<HolidayRegion> = listOf(
        region("DE-BW", "region_de_bw", dreikoenige, fronleichnam, allerheiligen),
        region("DE-BY", "region_de_by", dreikoenige, fronleichnam, allerheiligen, mariaeHimmelfahrt),
        region("DE-BE", "region_de_be", frauentag),
        region("DE-BB", "region_de_bb", reformationstag),
        region("DE-HB", "region_de_hb", reformationstag),
        region("DE-HH", "region_de_hh", reformationstag),
        region("DE-HE", "region_de_he", fronleichnam),
        region("DE-MV", "region_de_mv", frauentag, reformationstag),
        region("DE-NI", "region_de_ni", reformationstag),
        region("DE-NW", "region_de_nw", fronleichnam, allerheiligen),
        region("DE-RP", "region_de_rp", fronleichnam, allerheiligen),
        region("DE-SL", "region_de_sl", fronleichnam, mariaeHimmelfahrt, allerheiligen),
        region("DE-SN", "region_de_sn", reformationstag, bussUndBettag),
        region("DE-ST", "region_de_st", dreikoenige, reformationstag),
        region("DE-SH", "region_de_sh", reformationstag),
        region("DE-TH", "region_de_th", weltkindertag, reformationstag),
    )

    private fun region(iso: String, labelKey: String, vararg extra: HolidayRule) =
        HolidayRegion("DE", iso, labelKey, national + extra.toList())
}

object AtHolidays {
    val rules = listOf(
        Fixed(Month.JANUARY, 1, "holiday_at_neujahr"),
        Fixed(Month.JANUARY, 6, "holiday_at_dreikoenige"),
        EasterRelative(1, "holiday_at_ostermontag"),
        Fixed(Month.MAY, 1, "holiday_at_staatsfeiertag"),
        EasterRelative(39, "holiday_at_himmelfahrt"),
        EasterRelative(50, "holiday_at_pfingstmontag"),
        EasterRelative(60, "holiday_at_fronleichnam"),
        Fixed(Month.AUGUST, 15, "holiday_at_mariae_himmelfahrt"),
        Fixed(Month.OCTOBER, 26, "holiday_at_nationalfeiertag"),
        Fixed(Month.NOVEMBER, 1, "holiday_at_allerheiligen"),
        Fixed(Month.DECEMBER, 8, "holiday_at_mariae_empfaengnis"),
        Fixed(Month.DECEMBER, 25, "holiday_at_christtag"),
        Fixed(Month.DECEMBER, 26, "holiday_at_stefanitag"),
    )
}

object ChHolidays {
    val nationalCommon = listOf(
        Fixed(Month.JANUARY, 1, "holiday_ch_neujahr"),
        EasterRelative(-2, "holiday_ch_karfreitag"),
        EasterRelative(1, "holiday_ch_ostermontag"),
        EasterRelative(39, "holiday_ch_auffahrt"),
        EasterRelative(50, "holiday_ch_pfingstmontag"),
        Fixed(Month.AUGUST, 1, "holiday_ch_bundesfeier"),
        Fixed(Month.DECEMBER, 25, "holiday_ch_weihnachten"),
        Fixed(Month.DECEMBER, 26, "holiday_ch_stephanstag"),
    )
}

object FrHolidays {
    val rules = listOf(
        Fixed(Month.JANUARY, 1, "holiday_fr_jour_de_lan"),
        EasterRelative(1, "holiday_fr_lundi_paques"),
        Fixed(Month.MAY, 1, "holiday_fr_fete_du_travail"),
        Fixed(Month.MAY, 8, "holiday_fr_victoire_1945"),
        EasterRelative(39, "holiday_fr_ascension"),
        EasterRelative(50, "holiday_fr_lundi_pentecote"),
        Fixed(Month.JULY, 14, "holiday_fr_fete_nationale"),
        Fixed(Month.AUGUST, 15, "holiday_fr_assomption"),
        Fixed(Month.NOVEMBER, 1, "holiday_fr_toussaint"),
        Fixed(Month.NOVEMBER, 11, "holiday_fr_armistice"),
        Fixed(Month.DECEMBER, 25, "holiday_fr_noel"),
    )
}

object ItHolidays {
    val rules = listOf(
        Fixed(Month.JANUARY, 1, "holiday_it_capodanno"),
        Fixed(Month.JANUARY, 6, "holiday_it_epifania"),
        EasterRelative(1, "holiday_it_pasquetta"),
        Fixed(Month.APRIL, 25, "holiday_it_liberazione"),
        Fixed(Month.MAY, 1, "holiday_it_lavoro"),
        Fixed(Month.JUNE, 2, "holiday_it_repubblica"),
        Fixed(Month.AUGUST, 15, "holiday_it_ferragosto"),
        Fixed(Month.NOVEMBER, 1, "holiday_it_ognissanti"),
        Fixed(Month.DECEMBER, 8, "holiday_it_immacolata"),
        Fixed(Month.DECEMBER, 25, "holiday_it_natale"),
        Fixed(Month.DECEMBER, 26, "holiday_it_santo_stefano"),
    )
}

object EsHolidays {
    val national = listOf(
        Fixed(Month.JANUARY, 1, "holiday_es_ano_nuevo"),
        Fixed(Month.JANUARY, 6, "holiday_es_reyes"),
        EasterRelative(-2, "holiday_es_viernes_santo"),
        Fixed(Month.MAY, 1, "holiday_es_trabajo"),
        Fixed(Month.AUGUST, 15, "holiday_es_asuncion"),
        Fixed(Month.OCTOBER, 12, "holiday_es_fiesta_nacional"),
        Fixed(Month.NOVEMBER, 1, "holiday_es_todos_los_santos"),
        Fixed(Month.DECEMBER, 6, "holiday_es_constitucion"),
        Fixed(Month.DECEMBER, 8, "holiday_es_inmaculada"),
        Fixed(Month.DECEMBER, 25, "holiday_es_navidad"),
    )
}

object NlHolidays {
    val rules = listOf(
        Fixed(Month.JANUARY, 1, "holiday_nl_nieuwjaar"),
        EasterRelative(-2, "holiday_nl_goede_vrijdag"),
        EasterRelative(0, "holiday_nl_eerste_paasdag"),
        EasterRelative(1, "holiday_nl_tweede_paasdag"),
        Custom("holiday_nl_koningsdag") { y ->
            val d = LocalDate.of(y, 4, 27)
            if (d.dayOfWeek == DayOfWeek.SUNDAY) d.minusDays(1) else d
        },
        EasterRelative(39, "holiday_nl_hemelvaart"),
        EasterRelative(49, "holiday_nl_eerste_pinksterdag"),
        EasterRelative(50, "holiday_nl_tweede_pinksterdag"),
        Fixed(Month.DECEMBER, 25, "holiday_nl_eerste_kerstdag"),
        Fixed(Month.DECEMBER, 26, "holiday_nl_tweede_kerstdag"),
    )
}

object PlHolidays {
    val rules = listOf(
        Fixed(Month.JANUARY, 1, "holiday_pl_nowy_rok"),
        Fixed(Month.JANUARY, 6, "holiday_pl_trzech_kroli"),
        EasterRelative(0, "holiday_pl_wielkanoc"),
        EasterRelative(1, "holiday_pl_poniedzialek_wielkanocny"),
        Fixed(Month.MAY, 1, "holiday_pl_swieto_pracy"),
        Fixed(Month.MAY, 3, "holiday_pl_konstytucji"),
        EasterRelative(49, "holiday_pl_zielone_swiatki"),
        EasterRelative(60, "holiday_pl_boze_cialo"),
        Fixed(Month.AUGUST, 15, "holiday_pl_wniebowziecie"),
        Fixed(Month.NOVEMBER, 1, "holiday_pl_wszystkich_swietych"),
        Fixed(Month.NOVEMBER, 11, "holiday_pl_niepodleglosci"),
        Fixed(Month.DECEMBER, 25, "holiday_pl_boze_narodzenie_1"),
        Fixed(Month.DECEMBER, 26, "holiday_pl_boze_narodzenie_2"),
    )
}

object GbHolidays {
    val englandWales = listOf(
        Custom("holiday_gb_new_year") { y -> LocalDate.of(y, 1, 1).ukSubstitute() },
        EasterRelative(-2, "holiday_gb_good_friday"),
        EasterRelative(1, "holiday_gb_easter_monday"),
        NthWeekday(Month.MAY, DayOfWeek.MONDAY, 1, "holiday_gb_early_may"),
        LastWeekday(Month.MAY, DayOfWeek.MONDAY, "holiday_gb_spring_bank"),
        LastWeekday(Month.AUGUST, DayOfWeek.MONDAY, "holiday_gb_summer_bank"),
        Custom("holiday_gb_christmas") { y -> LocalDate.of(y, 12, 25).ukSubstitute() },
        Custom("holiday_gb_boxing_day") { y -> LocalDate.of(y, 12, 26).ukSubstitute() },
    )
}

object UsHolidays {
    val federal = listOf(
        Custom("holiday_us_new_year") { y -> LocalDate.of(y, 1, 1).observedUsStyle() },
        NthWeekday(Month.JANUARY, DayOfWeek.MONDAY, 3, "holiday_us_mlk"),
        NthWeekday(Month.FEBRUARY, DayOfWeek.MONDAY, 3, "holiday_us_presidents"),
        LastWeekday(Month.MAY, DayOfWeek.MONDAY, "holiday_us_memorial"),
        Custom("holiday_us_juneteenth") { y -> LocalDate.of(y, 6, 19).observedUsStyle() },
        Custom("holiday_us_independence") { y -> LocalDate.of(y, 7, 4).observedUsStyle() },
        NthWeekday(Month.SEPTEMBER, DayOfWeek.MONDAY, 1, "holiday_us_labor"),
        NthWeekday(Month.OCTOBER, DayOfWeek.MONDAY, 2, "holiday_us_columbus"),
        Custom("holiday_us_veterans") { y -> LocalDate.of(y, 11, 11).observedUsStyle() },
        NthWeekday(Month.NOVEMBER, DayOfWeek.THURSDAY, 4, "holiday_us_thanksgiving"),
        Custom("holiday_us_christmas") { y -> LocalDate.of(y, 12, 25).observedUsStyle() },
    )
}

object PtHolidays {
    val rules = listOf(
        Fixed(Month.JANUARY, 1, "holiday_pt_ano_novo"),
        EasterRelative(-2, "holiday_pt_sexta_feira_santa"),
        EasterRelative(0, "holiday_pt_pascoa"),
        Fixed(Month.APRIL, 25, "holiday_pt_dia_da_liberdade"),
        Fixed(Month.MAY, 1, "holiday_pt_dia_do_trabalhador"),
        EasterRelative(60, "holiday_pt_corpo_de_deus"),
        Fixed(Month.JUNE, 10, "holiday_pt_dia_de_portugal"),
        Fixed(Month.AUGUST, 15, "holiday_pt_assuncao"),
        Fixed(Month.OCTOBER, 5, "holiday_pt_implantacao_republica"),
        Fixed(Month.NOVEMBER, 1, "holiday_pt_todos_os_santos"),
        Fixed(Month.DECEMBER, 1, "holiday_pt_restauracao_independencia"),
        Fixed(Month.DECEMBER, 8, "holiday_pt_imaculada_conceicao"),
        Fixed(Month.DECEMBER, 25, "holiday_pt_natal"),
    )
}

object BrHolidays {
    val rules = listOf(
        Fixed(Month.JANUARY, 1, "holiday_br_confraternizacao"),
        EasterRelative(-2, "holiday_br_sexta_feira_santa"),
        Fixed(Month.APRIL, 21, "holiday_br_tiradentes"),
        Fixed(Month.MAY, 1, "holiday_br_dia_do_trabalho"),
        Fixed(Month.SEPTEMBER, 7, "holiday_br_independencia"),
        Fixed(Month.OCTOBER, 12, "holiday_br_aparecida"),
        Fixed(Month.NOVEMBER, 2, "holiday_br_finados"),
        Fixed(Month.NOVEMBER, 15, "holiday_br_proclamacao_republica"),
        Custom("holiday_br_consciencia_negra") { y ->
            if (y >= 2024) LocalDate.of(y, 11, 20) else null
        },
        Fixed(Month.DECEMBER, 25, "holiday_br_natal"),
    )
    val pontosFacultativos = listOf(
        EasterRelative(-48, "holiday_br_carnaval_segunda"),
        EasterRelative(-47, "holiday_br_carnaval_terca"),
        EasterRelative(60, "holiday_br_corpus_christi"),
    )
}

data class SupportedCountry(
    val iso: String,
    val labelKey: String,
    val nationalRules: List<HolidayRule>,
    val subdivisions: List<HolidayRegion> = emptyList(),
) {
    val hasSubdivisions: Boolean get() = subdivisions.isNotEmpty()

    fun regionFor(subdivisionIso: String?): HolidayRegion {
        if (subdivisionIso != null) {
            subdivisions.firstOrNull { it.subdivisionIso == subdivisionIso }?.let { return it }
        }
        return HolidayRegion(iso, null, "region_nationwide", nationalRules)
    }
}

object HolidayCatalog {
    val countries: List<SupportedCountry> = listOf(
        SupportedCountry("DE", "country_de", DeHolidays.national, DeHolidays.regions),
        SupportedCountry("AT", "country_at", AtHolidays.rules),
        SupportedCountry("CH", "country_ch", ChHolidays.nationalCommon),
        SupportedCountry("FR", "country_fr", FrHolidays.rules),
        SupportedCountry("IT", "country_it", ItHolidays.rules),
        SupportedCountry("ES", "country_es", EsHolidays.national),
        SupportedCountry("NL", "country_nl", NlHolidays.rules),
        SupportedCountry("PL", "country_pl", PlHolidays.rules),
        SupportedCountry("GB", "country_gb", GbHolidays.englandWales),
        SupportedCountry("US", "country_us", UsHolidays.federal),
        SupportedCountry("PT", "country_pt", PtHolidays.rules),
        SupportedCountry("BR", "country_br", BrHolidays.rules),
    )

    fun byIso(iso: String?): SupportedCountry? =
        iso?.let { code -> countries.firstOrNull { it.iso.equals(code, ignoreCase = true) } }
}
