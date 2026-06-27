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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseEncryptionTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val sidecars = listOf("", "-wal", "-shm", "-journal")

    @Before fun clean() {
        DatabaseProvider.close()
        sidecars.forEach { context.getDatabasePath("mushotoku.db$it").delete() }
    }

    @After fun cleanup() {
        sidecars.forEach { context.getDatabasePath("mushotoku.db$it").delete() }
    }

    @Test fun opensWithCorrectDek() = runBlocking {
        val dek = ByteArray(32) { it.toByte() }
        val db = AppDatabase.build(context, dek)
        db.appSettingsDao().insertDefault(AppSettings(id = 1, salary = 1234.0))
        val settings = db.appSettingsDao().getOnce()
        assertNotNull(settings)
        assertEquals(1234.0, settings!!.salary, 0.0001)
        db.close()
    }

    @Test fun failsWithWrongDek() = runBlocking {
        val dek = ByteArray(32) { it.toByte() }
        val db = AppDatabase.build(context, dek)
        db.appSettingsDao().insertDefault(AppSettings(id = 1))
        db.close()

        val wrongDek = ByteArray(32) { (it + 7).toByte() }
        val db2 = AppDatabase.build(context, wrongDek)
        assertThrows(Exception::class.java) {
            runBlocking { db2.appSettingsDao().getOnce() }
        }
        db2.close()
    }
}
