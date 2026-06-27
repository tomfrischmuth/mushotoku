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
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.io.RandomAccessFile

object PlaintextDbMigration {

    private const val DB_NAME = "mushotoku.db"
    private const val TMP_NAME = "mushotoku_enc_tmp.db"

    private val SQLITE_HEADER = "SQLite format 3".toByteArray(Charsets.US_ASCII) + 0.toByte()

    fun isPlaintextDbPresent(context: Context): Boolean {
        val file = context.getDatabasePath(DB_NAME)
        return file.exists() && file.length() >= 16L && readHeader(file).contentEquals(SQLITE_HEADER)
    }

    fun migrate(context: Context, dek: ByteArray) {
        if (!isPlaintextDbPresent(context)) return

        val plain = context.getDatabasePath(DB_NAME)
        val encTmp = context.getDatabasePath(TMP_NAME)
        deleteDbFileSet(encTmp)

        val oldVersion = readUserVersion(plain)
        val keyBytes = rawKeyBytes(dek)
        var enc: SQLiteDatabase? = null
        try {
            enc = SQLiteDatabase.openOrCreateDatabase(encTmp, keyBytes, null, null)
            val escapedPlainPath = plain.absolutePath.replace("'", "''")
            enc.rawExecSQL("ATTACH DATABASE '$escapedPlainPath' AS plaintext KEY ''")
            enc.rawExecSQL("SELECT sqlcipher_export('main', 'plaintext')")
            enc.rawExecSQL("DETACH DATABASE plaintext")
            enc.rawExecSQL("PRAGMA wal_checkpoint(TRUNCATE)")
            enc.version = oldVersion
            enc.close()
            enc = null

            deleteDbFileSet(plain)
            moveDbFileSet(encTmp, plain)
        } catch (t: Throwable) {
            enc?.close()
            deleteDbFileSet(encTmp)
            throw t
        } finally {
            keyBytes.fill(0)
        }
    }

    private val SIDECARS = listOf("", "-wal", "-shm", "-journal")

    private fun deleteDbFileSet(base: File) {
        SIDECARS.forEach { File(base.path + it).delete() }
    }

    private fun moveDbFileSet(from: File, to: File) {
        SIDECARS.forEach { suffix ->
            val src = File(from.path + suffix)
            if (src.exists()) {
                val dst = File(to.path + suffix)
                if (!src.renameTo(dst)) {
                    src.copyTo(dst, overwrite = true)
                    src.delete()
                }
            }
        }
    }

    private fun readHeader(file: File): ByteArray {
        val header = ByteArray(16)
        RandomAccessFile(file, "r").use { it.readFully(header) }
        return header
    }

    private fun readUserVersion(file: File): Int {
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(60)
            val b = ByteArray(4)
            raf.readFully(b)
            return ((b[0].toInt() and 0xFF) shl 24) or
                ((b[1].toInt() and 0xFF) shl 16) or
                ((b[2].toInt() and 0xFF) shl 8) or
                (b[3].toInt() and 0xFF)
        }
    }

    private fun rawKeyBytes(dek: ByteArray): ByteArray =
        SqlCipherKey.rawKeyBytes(dek)
}
