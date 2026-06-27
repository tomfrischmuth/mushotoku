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

object Recurrence {
    const val DAILY           = "DAILY"
    const val EVERY_OTHER_DAY = "EVERY_OTHER_DAY"
    const val WEEKLY          = "WEEKLY"
    const val BIWEEKLY        = "BIWEEKLY"
    const val MONTHLY         = "MONTHLY"
    val ALL = listOf(DAILY, EVERY_OTHER_DAY, WEEKLY, BIWEEKLY, MONTHLY)
}

@Serializable
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey val id: String,
    val name: String,
    val recurrence: String = Recurrence.DAILY,
    val createdAtDay: Long = 0L,
    val sortOrder: Int = 0
)

fun Habit.isScheduledFor(epochDay: Long): Boolean {
    val delta = epochDay - createdAtDay
    if (delta < 0) return false
    return when (recurrence) {
        Recurrence.DAILY           -> true
        Recurrence.EVERY_OTHER_DAY -> delta % 2 == 0L
        Recurrence.WEEKLY          -> delta % 7 == 0L
        Recurrence.BIWEEKLY        -> delta % 14 == 0L
        Recurrence.MONTHLY         -> {
            val created = java.time.LocalDate.ofEpochDay(createdAtDay)
            val current = java.time.LocalDate.ofEpochDay(epochDay)
            current.dayOfMonth == created.dayOfMonth
        }
        else -> true
    }
}

@Serializable
@Entity(
    tableName = "habit_logs",
    indices = [
        androidx.room.Index(value = ["habitId", "date"], unique = true),
        androidx.room.Index(value = ["date"]),
    ]
)
data class HabitLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: String,
    val date: Long
)
