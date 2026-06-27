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

package com.mushotoku.app.export

import java.io.OutputStream

enum class ExportCategory { KALENDER, JOURNAL, NOTIZEN, FINANZBERICHT }

enum class ExportFormat { PDF, ZIP_TXT, ZIP_PDF }

data class ExportOptions(
    val includeMood: Boolean = false
)

interface Exporter {
    val category: ExportCategory

    fun supportedFormats(): List<ExportFormat>

    fun mimeType(format: ExportFormat): String

    fun defaultBaseName(): String

    suspend fun hasData(): Boolean

    suspend fun write(
        out: OutputStream,
        format: ExportFormat,
        options: ExportOptions,
        onProgress: (current: Int, total: Int) -> Unit
    )
}

fun ExportFormat.extension(): String = when (this) {
    ExportFormat.PDF -> "pdf"
    ExportFormat.ZIP_TXT, ExportFormat.ZIP_PDF -> "zip"
}
