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

package com.mushotoku.app.screenshot

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mushotoku.app.data.AppDatabase
import com.mushotoku.app.data.Note
import com.mushotoku.app.data.NoteType
import com.mushotoku.app.security.KeyManager
import com.mushotoku.app.security.KeyMode
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Fills the notes table with the scene used for the README screenshot, so the
 * dates, colours and tags no longer have to be typed in by hand with the system
 * clock moved back and forth.
 *
 * Not a test of anything — run it on purpose:
 *   ./gradlew :app:installDebug :app:installDebugAndroidTest
 *   adb shell am instrument -w \
 *       -e class com.mushotoku.app.screenshot.SeedNotesForScreenshot \
 *       com.mushotoku.app.test/androidx.test.runner.AndroidJUnitRunner
 *
 * Install and run separately like this, not via connectedDebugAndroidTest —
 * that task uninstalls both APKs when it finishes and takes the seeded data
 * with them.
 *
 * Every note is dated relative to the moment it is written, so the scene never
 * goes stale, and nothing lands on today — a note from today would show a clock
 * time instead of a date and give away the faked status bar.
 */
@RunWith(AndroidJUnit4::class)
class SeedNotesForScreenshot {

    @Test fun seed() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val keys = KeyManager(context)
        val dek = if (keys.isInitialized()) {
            keys.unlockWithoutPrompt()
        } else {
            keys.initialize(KeyMode.KEYSTORE_NO_LOCK)
        }

        val db = AppDatabase.build(context, dek)
        try {
            val backup = db.backupDao()
            backup.clearNotes()
            backup.insertNotes(scene())
        } finally {
            db.close()
        }
    }

    private fun scene(): List<Note> {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000

        // Colour palette indices: 0 none, 1 red, 2 orange, 3 yellow,
        // 4 green, 5 blue, 6 purple.
        fun note(
            id: Long,
            title: String,
            content: String,
            color: Int,
            daysAgo: Long,
            pinned: Boolean = false,
            type: NoteType = NoteType.NOTE,
        ) = Note(
            id = id,
            title = title,
            content = content,
            type = type,
            createdAt = now - (daysAgo + 4) * day,
            updatedAt = now - daysAgo * day,
            isPinned = pinned,
            color = color,
        )

        // Written the way the editor's timestamp button writes them, and derived
        // from the note's own date so the two never drift apart. The quick filter
        // for stamped notes only shows up once a note carries one.
        fun stamp(daysAgo: Long, hour: Int, minute: Int, withDate: Boolean): String {
            val at = LocalDateTime
                .ofInstant(Instant.ofEpochMilli(now - daysAgo * day), ZoneId.systemDefault())
                .withHour(hour).withMinute(minute)
            val time = at.format(
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.ENGLISH)
            )
            if (!withDate) return time
            val date = at.format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.ENGLISH)
            )
            return "$date · $time"
        }

        return listOf(
            note(
                id = 1,
                title = "2FA backup",
                content = "The codes for when I lose my phone",
                color = 5,
                daysAgo = 3,
                pinned = true,
            ),
            note(
                id = 2,
                title = "Grocery list",
                content = "- [x] Coffee beans - the expensive ones\n" +
                    "- [/] Something for Sunday\n" +
                    "- [ ] Oat milk\n\n#shopping",
                color = 4,
                daysAgo = 1,
                type = NoteType.LIST,
            ),
            note(
                id = 3,
                title = "Night sky",
                content = "Perseids peak on the 12th. Drive out past the fields\n\n#stars",
                color = 3,
                daysAgo = 2,
            ),
            note(
                id = 4,
                title = "Kyoto trip",
                content = "- [x] Book the flight\n" +
                    "- [ ] Ryokan in Gion\n" +
                    "- [ ] Rail pass\n\n#travel",
                color = 2,
                daysAgo = 4,
                type = NoteType.LIST,
            ),
            note(
                id = 5,
                title = "Router login",
                content = "admin/admin. Changing it this weekend",
                color = 1,
                daysAgo = 5,
            ),
            note(
                id = 6,
                title = "Birthday gift",
                content = "Something thoughtful. It's tomorrow\n\n#gifts",
                color = 6,
                daysAgo = 6,
            ),
            note(
                id = 7,
                title = "Signal handle",
                content = "For the three people who'll switch. Zero so far",
                color = 0,
                daysAgo = 7,
            ),
            note(
                id = 8,
                title = "Reading list",
                content = "- Shobogenzo\n\n#books",
                color = 0,
                daysAgo = 9,
                type = NoteType.LIST,
            ),
            note(
                id = 9,
                title = "Sesshin diary",
                content = "${stamp(11, 4, 40, withDate = true)}\n" +
                    "First bell. Colder in the hall than I expected.\n\n" +
                    "${stamp(11, 11, 15, withDate = false)}\n" +
                    "Walking meditation in the courtyard. The rain stopped.\n\n" +
                    "${stamp(11, 20, 5, withDate = false)}\n" +
                    "Tea with the teacher. Said less than yesterday.",
                color = 5,
                daysAgo = 11,
            ),
        )
    }
}
