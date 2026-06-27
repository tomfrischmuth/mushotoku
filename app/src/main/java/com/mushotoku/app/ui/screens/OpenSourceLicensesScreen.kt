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

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import com.mushotoku.app.BuildConfig
import com.mushotoku.app.R
import com.mushotoku.app.ui.theme.LocalAppColors
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Composable
internal fun OpenSourceLicensesScreen() {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val groups = remember {
        val aboutJson = context.resources.openRawResource(R.raw.aboutlibraries)
            .bufferedReader().use { it.readText() }
        val libs = Libs.Builder().withJson(aboutJson).build()
        val notices = runCatching {
            val raw = context.resources.openRawResource(R.raw.legal_notices)
                .bufferedReader().use { it.readText() }
            LENIENT_JSON.decodeFromString<LegalNotices>(raw).notices
        }.getOrDefault(emptyMap())
        val years = runCatching {
            val raw = context.resources.openRawResource(R.raw.copyright_years)
                .bufferedReader().use { it.readText() }
            LENIENT_JSON.decodeFromString<CopyrightYears>(raw).years
        }.getOrDefault(emptyMap())
        buildLicenseGroups(libs, notices, years)
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(stringResource(R.string.lic_intro), color = colors.onSurfaceSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))
        groups.forEach { group ->
            LicenseGroupBlock(group)
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun LicenseGroupBlock(group: LicenseGroup) {
    val colors = LocalAppColors.current
    var textExpanded by remember { mutableStateOf(false) }
    val noticeExpanded = remember { mutableStateMapOf<String, Boolean>() }

    Text(group.displayName, color = colors.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(2.dp))
    Text(group.spdxId, color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)

    if (!group.attributionRequired) {
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.lic_public_domain_note),
            color = colors.onSurfaceTertiary, fontSize = 12.sp,
        )
    }

    if (group.licenseText.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = (if (textExpanded) "▾  " else "▸  ") + stringResource(R.string.lic_license_text),
            color = colors.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { textExpanded = !textExpanded }
                .padding(vertical = 6.dp),
        )
        if (textExpanded) {
            Text(
                group.licenseText,
                color = colors.onSurfaceSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(R.string.lic_components, group.entries.size),
        color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(6.dp))

    group.entries.forEach { entry ->
        val title = entry.version?.let { "${entry.name} $it" } ?: entry.name
        Text(title, color = colors.onSurface, fontSize = 13.sp)
        entry.copyright?.let { holder ->
            Text("© $holder", color = colors.onSurfaceSecondary, fontSize = 12.sp)
        }
        if (entry.notice != null) {
            val key = entry.name + (entry.version ?: "")
            val expanded = noticeExpanded[key] == true
            Text(
                text = (if (expanded) "▾  " else "▸  ") + stringResource(R.string.lic_notice),
                color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { noticeExpanded[key] = !expanded }
                    .padding(vertical = 4.dp),
            )
            if (expanded) {
                Text(
                    entry.notice,
                    color = colors.onSurfaceSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

private data class LibEntry(
    val name: String,
    val version: String?,
    val copyright: String?,
    val notice: String?,
)

private data class LicenseGroup(
    val spdxId: String,
    val displayName: String,
    val licenseText: String,
    val attributionRequired: Boolean,
    val entries: List<LibEntry>,
)

@Serializable
private data class LegalNotices(val notices: Map<String, NoticeRecord> = emptyMap())

@Serializable
private data class NoticeRecord(val notice: String? = null, val embeddedLicense: String? = null)

@Serializable
private data class CopyrightYears(val years: Map<String, String> = emptyMap())

private const val SPDX_PUBLIC_DOMAIN = "Public-Domain"

private val LENIENT_JSON = Json { ignoreUnknownKeys = true }

private fun buildLicenseGroups(
    libs: Libs,
    notices: Map<String, NoticeRecord>,
    years: Map<String, String>,
): List<LicenseGroup> {
    data class Raw(val entry: LibEntry, val spdx: String, val license: License?, val coord: String?)

    if (BuildConfig.DEBUG) warnIncompleteMetadata(libs, years)

    val gradleRaws = libs.libraries.map { lib ->
        val license = lib.licenses.firstOrNull()
        val spdx = resolveSpdx(license)
        Raw(
            entry = LibEntry(
                name = lib.name,
                version = lib.artifactVersion,
                copyright = FULL_COPYRIGHT[lib.uniqueId]
                    ?: copyrightLine(holder = holderFor(lib), year = years[lib.uniqueId]),
                notice = notices[lib.uniqueId]?.notice,
            ),
            spdx = spdx,
            license = license,
            coord = lib.uniqueId,
        )
    }
    val nativeRaws = NATIVE_COMPONENTS.map { Raw(it.entry, it.spdx, null, null) }

    return (gradleRaws + nativeRaws)
        .groupBy { it.spdx }
        .map { (spdx, items) ->
            val text = items.firstNotNullOfOrNull { it.license?.licenseContent?.takeIf(String::isNotBlank) }
                ?.let(::stripCopyrightPlaceholder)
                ?: FALLBACK_LICENSE_TEXTS[spdx]
                ?: items.firstNotNullOfOrNull { it.coord?.let { c -> notices[c]?.embeddedLicense } }
                ?: ""
            LicenseGroup(
                spdxId = spdx,
                displayName = displayName(spdx, items.firstNotNullOfOrNull { it.license }),
                licenseText = text,
                attributionRequired = spdx != "CC0-1.0" && spdx != SPDX_PUBLIC_DOMAIN,
                entries = items.map { it.entry }.sortedBy { it.name.lowercase() },
            )
        }
        .sortedBy { it.displayName.lowercase() }
}

private fun warnIncompleteMetadata(libs: Libs, years: Map<String, String>) {
    libs.libraries.forEach { lib ->
        if (lib.uniqueId in FULL_COPYRIGHT) return@forEach
        val holder = holderFor(lib)
        when {
            lib.licenses.isEmpty() ->
                Log.w("Licenses", "Keine Lizenz erkannt für ${lib.uniqueId} – Override ergänzen.")
            holder == null ->
                Log.w("Licenses", "Keine Copyright-Zeile für ${lib.uniqueId} – HOLDER_OVERRIDES ergänzen.")
            years[lib.uniqueId] == null ->
                Log.w("Licenses", "Kein Copyright-Jahr für ${lib.uniqueId} – MANUAL_COPYRIGHT_YEARS/FULL_COPYRIGHT ergänzen.")
        }
    }
}

private fun copyrightLine(holder: String?, year: String?): String? =
    holder?.let { if (year != null) "$year $it" else it }

private fun holderFor(lib: Library): String? = HOLDER_OVERRIDES[lib.uniqueId]
    ?: if (lib.uniqueId.startsWith("org.jetbrains.compose") || lib.uniqueId.startsWith("org.jetbrains.androidx"))
        "The Android Open Source Project"
    else copyrightHolder(lib)

private fun copyrightHolder(lib: Library): String? =
    lib.developers.firstNotNullOfOrNull { it.name?.takeIf(String::isNotBlank) }
        ?: lib.organization?.name?.takeIf { it.isNotBlank() }

private fun stripCopyrightPlaceholder(text: String): String =
    text.replace(Regex("(?m)^Copyright \\(c\\) <year> <copyright holders>\\n+"), "")

private fun resolveSpdx(license: License?): String = when {
    license == null -> "Unknown"
    !license.spdxId.isNullOrBlank() -> license.spdxId!!
    license.url?.contains("zetetic", ignoreCase = true) == true -> "BSD-3-Clause"
    license.name.isNotBlank() -> license.name
    else -> "Unknown"
}

private fun displayName(spdx: String, license: License?): String = when (spdx) {
    "Apache-2.0" -> "Apache License 2.0"
    "MIT" -> "MIT License"
    "BSD-2-Clause" -> "BSD 2-Clause License"
    "BSD-3-Clause" -> "BSD 3-Clause License"
    "CC0-1.0" -> "Creative Commons Zero 1.0"
    SPDX_PUBLIC_DOMAIN -> "Public Domain"
    else -> license?.name?.takeIf { it.isNotBlank() } ?: spdx
}

private val FULL_COPYRIGHT = mapOf(
    "net.zetetic:sqlcipher-android" to "2008–2024 Zetetic, LLC",
    "com.lambdapioneer.argon2kt:argon2kt" to "Daniel Hugenroth",
    "com.google.guava:listenablefuture" to "2007 The Guava Authors",
    "org.jspecify:jspecify" to "2018–2020 The JSpecify Authors",
    "dev.chrisbanes.haze:haze-android" to "2023 Chris Banes",
    "com.mikepenz:aboutlibraries-core-android" to "2015–2024 Mike Penz",
    "org.jetbrains:annotations" to "2000–2024 JetBrains s.r.o.",
    "org.jetbrains.kotlin:kotlin-stdlib" to "2010–2024 JetBrains s.r.o. and Kotlin Programming Language contributors",
    "org.jetbrains.kotlin:kotlin-stdlib-common" to "2010–2024 JetBrains s.r.o. and Kotlin Programming Language contributors",
    "org.jetbrains.kotlin:kotlin-stdlib-jdk7" to "2010–2024 JetBrains s.r.o. and Kotlin Programming Language contributors",
    "org.jetbrains.kotlin:kotlin-stdlib-jdk8" to "2010–2024 JetBrains s.r.o. and Kotlin Programming Language contributors",
    "org.jetbrains.kotlinx:kotlinx-coroutines-android" to "2016–2024 JetBrains s.r.o. and contributors",
    "org.jetbrains.kotlinx:kotlinx-coroutines-bom" to "2016–2024 JetBrains s.r.o. and contributors",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm" to "2016–2024 JetBrains s.r.o. and contributors",
    "org.jetbrains.kotlinx:kotlinx-serialization-bom" to "2017–2024 JetBrains s.r.o. and contributors",
    "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm" to "2017–2024 JetBrains s.r.o. and contributors",
    "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm" to "2017–2024 JetBrains s.r.o. and contributors",
    "org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm" to "2016–2026 JetBrains s.r.o. and contributors",
)

private val HOLDER_OVERRIDES = mapOf(
    "androidx.compose:compose-bom" to "The Android Open Source Project",
)

private class NativeComponent(val entry: LibEntry, val spdx: String)

private val NATIVE_COMPONENTS = listOf(
    NativeComponent(
        LibEntry(
            "Argon2 (phc-winner-argon2)", null,
            "2015 Daniel Dinu, Dmitry Khovratovich, Jean-Philippe Aumasson, Samuel Neves", null,
        ),
        "Apache-2.0",
    ),
    NativeComponent(LibEntry("LibTomCrypt", null, null, null), SPDX_PUBLIC_DOMAIN),
    NativeComponent(LibEntry("SQLite", null, null, null), SPDX_PUBLIC_DOMAIN),
)

private val FALLBACK_LICENSE_TEXTS = mapOf(
    "BSD-3-Clause" to (
        "Redistribution and use in source and binary forms, with or without modification, are " +
            "permitted provided that the following conditions are met:\n" +
            "  * Redistributions of source code must retain the above copyright notice, this list " +
            "of conditions and the following disclaimer.\n" +
            "  * Redistributions in binary form must reproduce the above copyright notice, this " +
            "list of conditions and the following disclaimer in the documentation and/or other " +
            "materials provided with the distribution.\n" +
            "  * Neither the name of the copyright holder nor the names of its contributors may be " +
            "used to endorse or promote products derived from this software without specific prior " +
            "written permission.\n\n" +
            "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\" AND ANY " +
            "EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES " +
            "OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT " +
            "SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, " +
            "INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED " +
            "TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR " +
            "BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN " +
            "CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN " +
            "ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH " +
            "DAMAGE."
        ),
)
