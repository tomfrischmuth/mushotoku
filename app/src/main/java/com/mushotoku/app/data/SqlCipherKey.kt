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

internal object SqlCipherKey {

    private val HEX = "0123456789abcdef".map { it.code.toByte() }.toByteArray()
    private val QUOTE = '\''.code.toByte()
    private val X = 'x'.code.toByte()

    fun rawKeyBytes(dek: ByteArray): ByteArray {
        val out = ByteArray(2 + dek.size * 2 + 1)
        out[0] = X
        out[1] = QUOTE
        for (i in dek.indices) {
            val v = dek[i].toInt() and 0xFF
            out[2 + i * 2] = HEX[v ushr 4]
            out[2 + i * 2 + 1] = HEX[v and 0x0F]
        }
        out[out.size - 1] = QUOTE
        return out
    }
}
