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

import android.content.Context
import com.mushotoku.app.security.wipe

object DatabaseProvider {

    @Volatile private var database: AppDatabase? = null
    @Volatile private var dekRef: ByteArray? = null

    val isOpen: Boolean get() = database != null

    fun open(context: Context, dek: ByteArray): AppDatabase {
        database?.let { dek.wipe(); return it }
        return synchronized(this) {
            val existing = database
            if (existing != null) {
                dek.wipe()
                existing
            } else {
                AppDatabase.build(context.applicationContext, dek).also {
                    database = it
                    dekRef = dek
                }
            }
        }
    }

    fun requireDatabase(): AppDatabase =
        database ?: throw IllegalStateException("Datenbank ist gesperrt – DEK liegt noch nicht vor")

    fun currentDek(): ByteArray? = dekRef

    fun close() = synchronized(this) {
        database?.close()
        database = null
        dekRef?.wipe()
        dekRef = null
    }
}
