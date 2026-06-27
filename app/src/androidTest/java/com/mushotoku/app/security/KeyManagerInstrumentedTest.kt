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

package com.mushotoku.app.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyManagerInstrumentedTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private lateinit var km: KeyManager

    @Before fun setup() {
        km = KeyManager(context)
        km.wipeKeys()
    }

    @After fun tearDown() {
        km.wipeKeys()
    }

    @Test fun noLock_initialize_then_unlockWithoutPrompt_roundtrip() {
        runBlocking {
            val dek = km.initialize(KeyMode.KEYSTORE_NO_LOCK)
            assertEquals(32, dek.size)
            assertFalse(km.requiresUserPresence())
            assertArrayEquals(dek, km.unlockWithoutPrompt())
        }
    }

    @Test fun passphrase_roundtrip_and_wrongPassphrase_fails() {
        runBlocking {
            val dek = km.initialize(KeyMode.PASSPHRASE, "correct horse battery".toCharArray())
            assertTrue(km.requiresUserPresence())
            assertArrayEquals(dek, km.unlockWithPassphrase("correct horse battery".toCharArray()))
        }
        assertThrows(WrongPassphraseException::class.java) {
            runBlocking { km.unlockWithPassphrase("wrong".toCharArray()) }
        }
    }

    @Test fun switchMode_noLock_to_passphrase_keepsDek() {
        runBlocking {
            val dek = km.initialize(KeyMode.KEYSTORE_NO_LOCK)
            val same = km.switchMode(KeyMode.PASSPHRASE, newPassphrase = "passphrase1".toCharArray())
            assertArrayEquals(dek, same)
            assertEquals(KeyMode.PASSPHRASE, km.currentMode())
            assertArrayEquals(dek, km.unlockWithPassphrase("passphrase1".toCharArray()))
        }
    }

    @Test fun switchMode_passphrase_to_noLock_keepsDek() {
        runBlocking {
            val dek = km.initialize(KeyMode.PASSPHRASE, "passphrase1".toCharArray())
            val same = km.switchMode(KeyMode.KEYSTORE_NO_LOCK, currentPassphrase = "passphrase1".toCharArray())
            assertArrayEquals(dek, same)
            assertEquals(KeyMode.KEYSTORE_NO_LOCK, km.currentMode())
            assertArrayEquals(dek, km.unlockWithoutPrompt())
        }
    }

    @Test fun changePassphrase_keepsDek_andOldFails() {
        runBlocking {
            val dek = km.initialize(KeyMode.PASSPHRASE, "old-passphrase".toCharArray())
            km.switchMode(
                KeyMode.PASSPHRASE,
                currentPassphrase = "old-passphrase".toCharArray(),
                newPassphrase = "new-passphrase".toCharArray(),
            )
            assertArrayEquals(dek, km.unlockWithPassphrase("new-passphrase".toCharArray()))
        }
        assertThrows(WrongPassphraseException::class.java) {
            runBlocking { km.unlockWithPassphrase("old-passphrase".toCharArray()) }
        }
    }

    @Test fun unlockWithoutPrompt_inPassphraseMode_throwsWrongMode() {
        runBlocking { km.initialize(KeyMode.PASSPHRASE, "passphrase1".toCharArray()) }
        assertThrows(WrongModeException::class.java) { km.unlockWithoutPrompt() }
    }

    @Test fun rewrap_enableLock_thenDisable_keepsDek_withoutBiometric() {
        runBlocking {
            val dek = km.initialize(KeyMode.KEYSTORE_NO_LOCK)
            km.rewrapDek(dek, KeyMode.KEYSTORE_LOCK)
            assertEquals(KeyMode.KEYSTORE_LOCK, km.currentMode())
            assertTrue(km.requiresUserPresence())
            km.rewrapDek(dek, KeyMode.KEYSTORE_NO_LOCK)
            assertEquals(KeyMode.KEYSTORE_NO_LOCK, km.currentMode())
            assertFalse(km.requiresUserPresence())
            assertArrayEquals(dek, km.unlockWithoutPrompt())
        }
    }

    @Test fun requiresUserPresence_matchesMode() {
        runBlocking {
            km.initialize(KeyMode.KEYSTORE_NO_LOCK)
            assertFalse(km.requiresUserPresence())
            km.switchMode(KeyMode.PASSPHRASE, newPassphrase = "passphrase1".toCharArray())
            assertTrue(km.requiresUserPresence())
        }
    }
}
