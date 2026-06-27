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
import android.util.Base64

class KeyStorage(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var mode: KeyMode?
        get() = prefs.getString(KEY_MODE, null)?.let { runCatching { KeyMode.valueOf(it) }.getOrNull() }
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_MODE) else putString(KEY_MODE, value.name)
        }.let { it.apply() }

    var wrappedDek: ByteArray?
        get() = getBytes(KEY_WRAPPED_DEK)
        set(value) = setBytes(KEY_WRAPPED_DEK, value)

    var dekIv: ByteArray?
        get() = getBytes(KEY_DEK_IV)
        set(value) = setBytes(KEY_DEK_IV, value)

    var passphraseSalt: ByteArray?
        get() = getBytes(KEY_SALT)
        set(value) = setBytes(KEY_SALT, value)

    var argonMemoryKiB: Int
        get() = prefs.getInt(KEY_ARGON_MEM, Argon2Kdf.MEMORY_KIB)
        set(value) = prefs.edit().putInt(KEY_ARGON_MEM, value).apply()

    var argonIterations: Int
        get() = prefs.getInt(KEY_ARGON_ITER, Argon2Kdf.ITERATIONS)
        set(value) = prefs.edit().putInt(KEY_ARGON_ITER, value).apply()

    var argonParallelism: Int
        get() = prefs.getInt(KEY_ARGON_PAR, Argon2Kdf.PARALLELISM)
        set(value) = prefs.edit().putInt(KEY_ARGON_PAR, value).apply()

    val isInitialized: Boolean
        get() = mode != null && wrappedDek != null

    fun clear() = prefs.edit().clear().apply()

    private fun getBytes(key: String): ByteArray? =
        prefs.getString(key, null)?.let { Base64.decode(it, Base64.NO_WRAP) }

    private fun setBytes(key: String, value: ByteArray?) = prefs.edit().apply {
        if (value == null) remove(key) else putString(key, Base64.encodeToString(value, Base64.NO_WRAP))
    }.apply()

    companion object {
        private const val PREFS_NAME = "mushotoku_keys"
        private const val KEY_MODE = "mode"
        private const val KEY_WRAPPED_DEK = "wrapped_dek"
        private const val KEY_DEK_IV = "dek_iv"
        private const val KEY_SALT = "passphrase_salt"
        private const val KEY_ARGON_MEM = "argon_mem_kib"
        private const val KEY_ARGON_ITER = "argon_iterations"
        private const val KEY_ARGON_PAR = "argon_parallelism"
    }
}
