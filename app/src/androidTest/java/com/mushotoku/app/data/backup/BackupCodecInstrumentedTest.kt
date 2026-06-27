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

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class BackupCodecInstrumentedTest {

    @Test fun encrypt_then_decrypt_roundtrip() {
        runBlocking {
            val payload = "{\"hello\":\"welt\",\"n\":42}".toByteArray(Charsets.UTF_8)
            val bos = ByteArrayOutputStream()
            BackupCodec.encrypt(bos, "s3cret-pass".toCharArray(), payload)

            val restored = BackupCodec.decrypt(ByteArrayInputStream(bos.toByteArray()), "s3cret-pass".toCharArray())
            assertArrayEquals(payload, restored)
        }
    }

    @Test fun decrypt_wrongPassword_throws() {
        val bos = ByteArrayOutputStream()
        runBlocking { BackupCodec.encrypt(bos, "right-pass".toCharArray(), "daten".toByteArray()) }

        assertThrows(WrongBackupPasswordException::class.java) {
            runBlocking {
                BackupCodec.decrypt(ByteArrayInputStream(bos.toByteArray()), "wrong-pass".toCharArray())
            }
        }
    }
}
