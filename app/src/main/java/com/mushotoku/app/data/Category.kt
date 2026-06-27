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

package com.mushotoku.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String,
    val name: String,
    val group: String,
    val isEnabled: Boolean = false,
    val recurringCost: Double = 0.0,
    val isDefault: Boolean = true,
    val sortOrder: Int = 0
)

val DEFAULT_CATEGORIES: List<Category> = listOf(
    Category("miete",            "Miete",                      "Wohnen",                isEnabled = false, sortOrder =   0),
    Category("nebenkosten",      "Nebenkosten",                "Wohnen",                isEnabled = false, sortOrder =   1),
    Category("strom",            "Strom",                      "Wohnen",                isEnabled = false, sortOrder =   2),
    Category("wasser",           "Wasser",                     "Wohnen",                isEnabled = false, sortOrder =   3),
    Category("heizung",          "Heizung",                    "Wohnen",                isEnabled = false, sortOrder =   4),
    Category("internet",         "Internet / Telefon",         "Wohnen",                isEnabled = false, sortOrder =   5),
    Category("reparaturen",      "Reparaturen",                "Wohnen",                isEnabled = false, sortOrder =   6),
    Category("haushalt",         "Haushalt",                   "Wohnen",                isEnabled = false, sortOrder =   7),
    Category("gartenarbeit",     "Gartenarbeit",               "Wohnen",                isEnabled = false, sortOrder =   8),
    Category("supermarkt",       "Supermarkt",                 "Lebensmittel",          isEnabled = true,  sortOrder =   9),
    Category("wochenmarkt",      "Wochenmarkt",                "Lebensmittel",          isEnabled = false, sortOrder =  10),
    Category("bio_laden",        "Bio-Laden",                  "Lebensmittel",          isEnabled = false, sortOrder =  11),
    Category("getraenke",        "Getränke",                   "Lebensmittel",          isEnabled = true,  sortOrder =  12),
    Category("snacks",           "Snacks",                     "Lebensmittel",          isEnabled = true,  sortOrder =  13),
    Category("restaurant",       "Restaurant",                 "Essen & Trinken",       isEnabled = true,  sortOrder =  14),
    Category("cafe",             "Café",                       "Essen & Trinken",       isEnabled = true,  sortOrder =  15),
    Category("takeaway",         "Takeaway",                   "Essen & Trinken",       isEnabled = true,  sortOrder =  16),
    Category("lieferdienst",     "Lieferdienst",               "Essen & Trinken",       isEnabled = true,  sortOrder =  17),
    Category("kantine",          "Kantine",                    "Essen & Trinken",       isEnabled = true,  sortOrder =  18),
    Category("bar",              "Bar",                        "Essen & Trinken",       isEnabled = true,  sortOrder =  19),
    Category("fahrzeugleasing",  "Fahrzeugleasing",            "Transport",             isEnabled = false, sortOrder =  20),
    Category("tanken",           "Tanken / Laden",             "Transport",             isEnabled = true,  sortOrder =  21),
    Category("parkgebuehren",    "Parkgebühren",               "Transport",             isEnabled = true,  sortOrder =  22),
    Category("kfz_versicherung", "KFZ-Versicherung",           "Transport",             isEnabled = false, sortOrder =  23),
    Category("kfz_steuer",       "KFZ-Steuer",                 "Transport",             isEnabled = false, sortOrder =  24),
    Category("werkstatt",        "Werkstatt",                  "Transport",             isEnabled = false, sortOrder =  25),
    Category("oepnv",            "ÖPNV",                       "Transport",             isEnabled = true,  sortOrder =  26),
    Category("fahrdienste",      "Fahrdienste",                "Transport",             isEnabled = false, sortOrder =  27),
    Category("fahrrad",          "Fahrrad",                    "Transport",             isEnabled = false, sortOrder =  28),
    Category("escooter",         "E-Scooter",                  "Transport",             isEnabled = false, sortOrder =  29),
    Category("medikamente",      "Medikamente",                "Gesundheit & Körper",   isEnabled = true,  sortOrder =  30),
    Category("arztbesuch",       "Arztbesuch",                 "Gesundheit & Körper",   isEnabled = false, sortOrder =  31),
    Category("zahnarzt",         "Zahnarzt",                   "Gesundheit & Körper",   isEnabled = false, sortOrder =  32),
    Category("krankenkasse",     "Krankenversicherung",        "Gesundheit & Körper",   isEnabled = false, sortOrder =  33),
    Category("brille",           "Brille / Kontaktlinsen",     "Gesundheit & Körper",   isEnabled = false, sortOrder =  34),
    Category("koerperpflege",    "Körperpflege",               "Gesundheit & Körper",   isEnabled = true,  sortOrder =  35),
    Category("kosmetik",         "Kosmetik",                   "Gesundheit & Körper",   isEnabled = false, sortOrder =  36),
    Category("friseur",          "Friseur",                    "Gesundheit & Körper",   isEnabled = false, sortOrder =  37),
    Category("wellness",         "Wellness",                   "Gesundheit & Körper",   isEnabled = false, sortOrder =  38),
    Category("kleidung",         "Kleidung",                   "Kleidung & Accessoires",isEnabled = true,  sortOrder =  39),
    Category("schuhe",           "Schuhe",                     "Kleidung & Accessoires",isEnabled = false, sortOrder =  40),
    Category("taschen",          "Taschen",                    "Kleidung & Accessoires",isEnabled = false, sortOrder =  41),
    Category("accessoires",      "Accessoires",                "Kleidung & Accessoires",isEnabled = true,  sortOrder =  42),
    Category("sportkleidung",    "Sportkleidung",              "Kleidung & Accessoires",isEnabled = false, sortOrder =  43),
    Category("reinigung",        "Reinigung / Schneiderei",    "Kleidung & Accessoires",isEnabled = false, sortOrder =  44),
    Category("events",           "Events",                     "Freizeit",              isEnabled = true,  sortOrder =  45),
    Category("ausstellungen",    "Ausstellungen",              "Freizeit",              isEnabled = false, sortOrder =  46),
    Category("sportveranstalt",  "Sportveranstaltungen",       "Freizeit",              isEnabled = false, sortOrder =  47),
    Category("lesestoff",        "Lesestoff",                  "Freizeit",              isEnabled = true,  sortOrder =  48),
    Category("kulturveranstalt", "Kulturveranstaltung",        "Freizeit",              isEnabled = false, sortOrder =  49),
    Category("fitnessstudio",    "Fitnessstudio",              "Sport",                 isEnabled = false, sortOrder =  50),
    Category("sportkurse",       "Sportkurse",                 "Sport",                 isEnabled = false, sortOrder =  51),
    Category("sportverein",      "Sportverein",                "Sport",                 isEnabled = false, sortOrder =  52),
    Category("sportausruestung", "Sportausrüstung",            "Sport",                 isEnabled = false, sortOrder =  53),
    Category("schwimmbad",       "Schwimmbad",                 "Sport",                 isEnabled = true,  sortOrder =  54),
    Category("outdoor_sport",    "Outdoor-Sport",              "Sport",                 isEnabled = false, sortOrder =  55),
    Category("indoor_sport",     "Indoor-Sport",               "Sport",                 isEnabled = true,  sortOrder =  56),
    Category("fluege",           "Flüge",                      "Reisen",                isEnabled = false, sortOrder =  57),
    Category("unterkunft",       "Unterkunft",                 "Reisen",                isEnabled = false, sortOrder =  58),
    Category("mietwagen",        "Mietwagen",                  "Reisen",                isEnabled = false, sortOrder =  59),
    Category("reiseversicherung","Reiseversicherung",          "Reisen",                isEnabled = false, sortOrder =  60),
    Category("aktivitaeten",     "Aktivitäten vor Ort",        "Reisen",                isEnabled = false, sortOrder =  61),
    Category("souvenirs",        "Souvenirs",                  "Reisen",                isEnabled = false, sortOrder =  62),
    Category("reisegepaeck",     "Reisegepäck",                "Reisen",                isEnabled = false, sortOrder =  63),
    Category("smartphone",       "Smartphone / Tablet",        "Digitales",             isEnabled = false, sortOrder =  64),
    Category("computer",         "Computer / Zubehör",         "Digitales",             isEnabled = false, sortOrder =  65),
    Category("mobilfunk",        "Mobilfunk-Vertrag",          "Digitales",             isEnabled = false, sortOrder =  66),
    Category("software",         "Software / Apps",            "Digitales",             isEnabled = false, sortOrder =  67),
    Category("abonnements",      "Abonnements",                "Digitales",             isEnabled = false, sortOrder =  68),
    Category("streaming",        "Streaming",                  "Digitales",             isEnabled = false, sortOrder =  69),
    Category("gaming",           "Gaming",                     "Digitales",             isEnabled = false, sortOrder =  70),
    Category("fachliteratur",    "Fachliteratur",              "Bildung",               isEnabled = false, sortOrder =  71),
    Category("schule_uni",       "Schule / Universität",       "Bildung",               isEnabled = false, sortOrder =  72),
    Category("seminare",         "Seminare / Workshops",       "Bildung",               isEnabled = false, sortOrder =  73),
    Category("sprachen",         "Sprachen lernen",            "Bildung",               isEnabled = false, sortOrder =  74),
    Category("online_kurse",     "Online-Kurse",               "Bildung",               isEnabled = false, sortOrder =  75),
    Category("spenden",          "Spenden",                    "Soziales",              isEnabled = true,  sortOrder =  76),
    Category("geschenke",        "Geschenke",                  "Soziales",              isEnabled = true,  sortOrder =  77),
    Category("vereinsbeitraege", "Vereinsbeiträge",            "Soziales",              isEnabled = false, sortOrder =  78),
    Category("hochzeiten",       "Hochzeiten / Feiern",        "Soziales",              isEnabled = false, sortOrder =  79),
    Category("tierfutter",       "Futter",                     "Haustiere",             isEnabled = false, sortOrder =  80),
    Category("tierarzt",         "Tierarzt",                   "Haustiere",             isEnabled = false, sortOrder =  81),
    Category("tier_zubehoer",    "Zubehör / Spielzeug",        "Haustiere",             isEnabled = false, sortOrder =  82),
    Category("tierpension",      "Tierpension / Tiersitter",   "Haustiere",             isEnabled = false, sortOrder =  83),
    Category("tierversicherung", "Tierversicherung",           "Haustiere",             isEnabled = false, sortOrder =  84),
    Category("hundesteuer",      "Hundesteuer",                "Haustiere",             isEnabled = false, sortOrder =  85),
    Category("krankenversicherung","Krankenversicherung",      "Finanzen & Vorsorge",   isEnabled = false, sortOrder =  86),
    Category("lebensversicherung","Lebensversicherung",        "Finanzen & Vorsorge",   isEnabled = false, sortOrder =  87),
    Category("haftpflicht",      "Haftpflichtversicherung",    "Finanzen & Vorsorge",   isEnabled = false, sortOrder =  88),
    Category("altersvorsorge",   "Rentenvorsorge / Altersvorsorge","Finanzen & Vorsorge",isEnabled = false,sortOrder =  89),
    Category("investitionen",    "Investitionen / ETFs / Aktien","Finanzen & Vorsorge", isEnabled = false, sortOrder =  90),
    Category("sparbetrag",       "Sparbetrag",                 "Finanzen & Vorsorge",   isEnabled = false, sortOrder =  91),
    Category("kredit",           "Kredittilgung",              "Finanzen & Vorsorge",   isEnabled = false, sortOrder =  92),
    Category("steuernachzahlung","Steuernachzahlung",          "Finanzen & Vorsorge",   isEnabled = false, sortOrder =  93),
    Category("steuerberater",    "Steuerberater",              "Finanzen & Vorsorge",   isEnabled = false, sortOrder =  94),
    Category("kinderbetreuung",  "Kinderbetreuung",            "Familie & Kinder",      isEnabled = false, sortOrder =  95),
    Category("schulbedarf",      "Schule / Schulbedarf",       "Familie & Kinder",      isEnabled = false, sortOrder =  96),
    Category("essensgeld",       "Essensgeld",                 "Familie & Kinder",      isEnabled = false, sortOrder =  97),
    Category("nachhilfe",        "Nachhilfe",                  "Familie & Kinder",      isEnabled = false, sortOrder =  98),
    Category("vereine_kinder",   "Vereine",                    "Familie & Kinder",      isEnabled = false, sortOrder =  99),
    Category("spielzeug",        "Spielzeug",                  "Familie & Kinder",      isEnabled = false, sortOrder = 100),
    Category("freizeitaktivit",  "Freizeitaktivitäten",        "Familie & Kinder",      isEnabled = false, sortOrder = 101),
    Category("kinderkleidung",   "Kinderkleidung",             "Familie & Kinder",      isEnabled = false, sortOrder = 102),
    Category("windeln",          "Windeln / Pflegeprodukte",   "Familie & Kinder",      isEnabled = false, sortOrder = 103),
    Category("babynahrung",      "Babynahrung",                "Familie & Kinder",      isEnabled = false, sortOrder = 104),
    Category("babymoebel",       "Babymöbel",                  "Familie & Kinder",      isEnabled = false, sortOrder = 105),
    Category("babysitter",       "Babysitter",                 "Familie & Kinder",      isEnabled = false, sortOrder = 106),
    Category("taschengeld",      "Taschengeld",                "Familie & Kinder",      isEnabled = false, sortOrder = 107),
    Category("arbeitsmaterial",  "Arbeitsmaterial",            "Beruf & Büro",          isEnabled = false, sortOrder = 108),
    Category("homeoffice",       "Homeoffice-Ausstattung",     "Beruf & Büro",          isEnabled = false, sortOrder = 109),
    Category("berufskleidung",   "Berufskleidung",             "Beruf & Büro",          isEnabled = false, sortOrder = 110),
    Category("weiterbildung",    "Weiterbildung (beruflich)",  "Beruf & Büro",          isEnabled = false, sortOrder = 111),
    Category("bussgelder",       "Gebühren / Bußgelder",       "Sonstiges",             isEnabled = false, sortOrder = 112),
    Category("rechtsanwalt",     "Rechtsanwalt / Notar",       "Sonstiges",             isEnabled = false, sortOrder = 113),
    Category("unterhalt",        "Unterhaltszahlungen",        "Sonstiges",             isEnabled = false, sortOrder = 114),
    Category("sonstiges",        "Sonstiges",                  "Sonstiges",             isEnabled = false, sortOrder = 115),
)
