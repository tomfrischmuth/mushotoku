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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryCodeTest {

    private val allowed = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toSet()

    @Test fun generate_hasExpectedShapeAndCharset() {
        val code = RecoveryCode.generate()
        val symbols = code.filter { it != '-' }
        assertEquals(RecoveryCode.LENGTH, symbols.length)
        assertTrue(symbols.all { it in allowed })
        assertTrue(code.contains('-'))
    }

    @Test fun normalize_ofGeneratedCode_hasCanonicalLength() {
        val normalized = RecoveryCode.normalize(RecoveryCode.generate().toCharArray())
        assertEquals(RecoveryCode.LENGTH, normalized?.size)
        assertTrue(normalized!!.all { it in allowed })
    }

    @Test fun normalize_isCaseAndSeparatorInsensitive() {
        val code = RecoveryCode.generate()
        val canonical = String(RecoveryCode.normalize(code.toCharArray())!!)
        val messy = code.lowercase().replace("-", " - ")
        val fromMessy = String(RecoveryCode.normalize(messy.toCharArray())!!)
        assertEquals(canonical, fromMessy)
    }

    @Test fun normalize_mapsAmbiguousLetters() {
        assertTrue(RecoveryCode.normalize(CharArray(RecoveryCode.LENGTH) { 'O' })!!.all { it == '0' })
        assertTrue(RecoveryCode.normalize(CharArray(RecoveryCode.LENGTH) { 'I' })!!.all { it == '1' })
        assertTrue(RecoveryCode.normalize(CharArray(RecoveryCode.LENGTH) { 'l' })!!.all { it == '1' })
    }

    @Test fun normalize_returnsNullForTooShortInput() {
        assertNull(RecoveryCode.normalize("ABC".toCharArray()))
    }
}
