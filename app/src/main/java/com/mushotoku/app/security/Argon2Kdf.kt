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

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Argon2Kdf {

    const val MEMORY_KIB = 65536
    const val ITERATIONS = 3
    const val PARALLELISM = 1
    const val HASH_LENGTH_BYTES = 32
    const val SALT_LENGTH_BYTES = 16

    private val argon2 by lazy { Argon2Kt() }

    suspend fun deriveKey(
        password: ByteArray,
        salt: ByteArray,
        memoryKiB: Int = MEMORY_KIB,
        iterations: Int = ITERATIONS,
        parallelism: Int = PARALLELISM,
        hashLengthBytes: Int = HASH_LENGTH_BYTES,
    ): ByteArray = withContext(Dispatchers.Default) {
        val result = argon2.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = password,
            salt = salt,
            tCostInIterations = iterations,
            mCostInKibibyte = memoryKiB,
            parallelism = parallelism,
            hashLengthInBytes = hashLengthBytes,
        )
        result.rawHashAsByteArray()
    }
}
