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

object ExportFilenameSanitizer {

    private const val MAX_LENGTH = 100

    private val FORBIDDEN = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')

    fun sanitize(title: String, fallbackPrefix: String, fallbackIndex: Int): String {
        val cleaned = buildString {
            for (ch in title) {
                when {
                    ch.isISOControl() -> append(' ')
                    ch in FORBIDDEN   -> append(' ')
                    else              -> append(ch)
                }
            }
        }
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.')
            .trim()

        val base = cleaned.ifBlank { "$fallbackPrefix$fallbackIndex" }
        return if (base.length > MAX_LENGTH) base.substring(0, MAX_LENGTH).trim() else base
    }

    fun uniqueFileName(baseName: String, ext: String, taken: MutableSet<String>): String {
        var candidate = "$baseName.$ext"
        var counter = 2
        while (!taken.add(candidate.lowercase())) {
            candidate = "$baseName ($counter).$ext"
            counter++
        }
        return candidate
    }
}
