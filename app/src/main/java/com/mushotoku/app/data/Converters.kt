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

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromTaskStatus(s: TaskStatus): String = s.name
    @TypeConverter fun toTaskStatus(s: String): TaskStatus = TaskStatus.valueOf(s)
    @TypeConverter fun fromNoteType(t: NoteType): String = t.name
    @TypeConverter fun toNoteType(t: String): NoteType = NoteType.valueOf(t)
    @TypeConverter fun fromTaskCategory(c: TaskCategory): String = c.name
    @TypeConverter fun toTaskCategory(s: String): TaskCategory = TaskCategory.valueOf(s)
}
