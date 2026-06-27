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

package com.mushotoku.app.data.backup

import com.mushotoku.app.data.AppSettings
import com.mushotoku.app.data.Category
import com.mushotoku.app.data.Expense
import com.mushotoku.app.data.MoodEntry
import com.mushotoku.app.data.Note
import com.mushotoku.app.data.NoteType
import com.mushotoku.app.data.Task
import com.mushotoku.app.data.TaskCategory
import com.mushotoku.app.data.TaskStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun payload_jsonRoundtrip_isLossless() {
        val original = BackupPayload(
            schemaVersion = 28,
            appVersion = "1.0",
            createdAt = 1_700_000_000_000L,
            tasks = listOf(
                Task(id = 1, title = "Aufgabe", date = 19_900L, status = TaskStatus.GREEN, category = TaskCategory.TOP),
                Task(id = 2, title = "Termin", date = 19_901L, isAppointment = true, time = "09:30", linkedNoteId = 5),
            ),
            notes = listOf(
                Note(id = 5, title = "Notiz", content = "Inhalt", type = NoteType.LIST, isPinned = true),
            ),
            expenses = listOf(Expense(id = 1, date = 19_900L, category = "supermarkt", amount = 12.5)),
            categories = listOf(Category(id = "miete", name = "Miete", group = "Wohnen", isEnabled = true)),
            settings = AppSettings(salary = 3000.0, appLockTimeoutSeconds = 60),
            moods = listOf(MoodEntry(date = 19_900L, mood = 4)),
        )

        val encoded = json.encodeToString(BackupPayload.serializer(), original)
        val decoded = json.decodeFromString(BackupPayload.serializer(), encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun olderPayload_missingNewField_usesDefault() {
        val olderJson = """
            {"schemaVersion":27,"appVersion":"0.9","createdAt":1,
             "settings":{"id":1,"salary":100.0}}
        """.trimIndent()

        val decoded = json.decodeFromString(BackupPayload.serializer(), olderJson)
        assertEquals(0, decoded.settings?.appLockTimeoutSeconds)
        assertEquals(100.0, decoded.settings?.salary!!, 0.0001)
    }
}
