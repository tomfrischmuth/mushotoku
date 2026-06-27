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

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import android.content.Context
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDb

@RunWith(AndroidJUnit4::class)
class PlaintextMigrationInstrumentedTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val sidecars = listOf("", "-wal", "-shm", "-journal")

    @Before fun clean() {
        DatabaseProvider.close()
        sidecars.forEach { context.getDatabasePath("mushotoku.db$it").delete() }
    }

    @After fun cleanup() {
        sidecars.forEach { context.getDatabasePath("mushotoku.db$it").delete() }
    }

    @Test fun plaintextDb_isMigrated_andDataSurvives() {
        val dbFile = context.getDatabasePath("mushotoku.db")
        dbFile.parentFile?.mkdirs()

        val plain = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        plain.execSQL("CREATE TABLE app_settings (id INTEGER PRIMARY KEY, salary REAL)")
        plain.execSQL("INSERT INTO app_settings (id, salary) VALUES (1, 4242.0)")
        plain.version = 28
        plain.close()

        assertTrue(PlaintextDbMigration.isPlaintextDbPresent(context))

        val dek = ByteArray(32) { (it * 7 + 1).toByte() }
        PlaintextDbMigration.migrate(context, dek)

        assertFalse(PlaintextDbMigration.isPlaintextDbPresent(context))

        val enc = CipherDb.openOrCreateDatabase(dbFile, SqlCipherKey.rawKeyBytes(dek), null, null)
        try {
            enc.rawQuery("SELECT salary FROM app_settings WHERE id = 1", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(4242.0, c.getDouble(0), 0.0001)
            }
            assertEquals(28, enc.version)
        } finally {
            enc.close()
        }
    }
}
