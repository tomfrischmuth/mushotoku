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

import com.mushotoku.app.security.Argon2Kdf
import com.mushotoku.app.security.toUtf8Bytes
import com.mushotoku.app.security.wipe
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object BackupCodec {

    private val MAGIC = byteArrayOf('A'.code.toByte(), 'P'.code.toByte(), 'P'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
    const val FORMAT_VERSION = 1
    private const val GCM_TAG_BITS = 128
    private const val NONCE_LEN = 12

    private const val MIN_MEMORY_KIB = 8
    private const val MAX_MEMORY_KIB = 1_048_576
    private const val MIN_ITERATIONS = 1
    private const val MAX_ITERATIONS = 50
    private const val MIN_PARALLELISM = 1
    private const val MAX_PARALLELISM = 16

    private val secureRandom = SecureRandom()

    suspend fun encrypt(out: OutputStream, password: CharArray, plaintextJson: ByteArray) {
        val salt = randomBytes(Argon2Kdf.SALT_LENGTH_BYTES)
        val key = deriveKey(password, salt, Argon2Kdf.MEMORY_KIB, Argon2Kdf.ITERATIONS, Argon2Kdf.PARALLELISM)
        try {
            val nonce = randomBytes(NONCE_LEN)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
            val ciphertext = cipher.doFinal(gzip(plaintextJson))

            val dos = DataOutputStream(BufferedOutputStream(out))
            dos.write(MAGIC)
            dos.writeByte(FORMAT_VERSION)
            dos.write(salt)
            dos.writeInt(Argon2Kdf.MEMORY_KIB)
            dos.writeInt(Argon2Kdf.ITERATIONS)
            dos.writeInt(Argon2Kdf.PARALLELISM)
            dos.write(nonce)
            dos.write(ciphertext)
            dos.flush()
        } finally {
            key.wipe()
        }
    }

    suspend fun decrypt(input: InputStream, password: CharArray): ByteArray {
        val dis = DataInputStream(BufferedInputStream(input))
        val magic = ByteArray(MAGIC.size)
        try {
            dis.readFully(magic)
        } catch (e: Exception) {
            throw BackupCorruptException("Datei zu kurz / kein gültiger Header", e)
        }
        if (!magic.contentEquals(MAGIC)) throw BackupCorruptException("Ungültige Signatur")
        val version = dis.readByte().toInt()
        if (version != FORMAT_VERSION) throw BackupCorruptException("Unbekannte Container-Version $version")

        val salt = ByteArray(Argon2Kdf.SALT_LENGTH_BYTES)
        dis.readFully(salt)
        val memKiB = dis.readInt().also {
            if (it !in MIN_MEMORY_KIB..MAX_MEMORY_KIB) throw BackupCorruptException("Argon2-Speicherparameter ausserhalb des gueltigen Bereichs: $it")
        }
        val iterations = dis.readInt().also {
            if (it !in MIN_ITERATIONS..MAX_ITERATIONS) throw BackupCorruptException("Argon2-Iterationen ausserhalb des gueltigen Bereichs: $it")
        }
        val parallelism = dis.readInt().also {
            if (it !in MIN_PARALLELISM..MAX_PARALLELISM) throw BackupCorruptException("Argon2-Parallelitaet ausserhalb des gueltigen Bereichs: $it")
        }
        val nonce = ByteArray(NONCE_LEN)
        dis.readFully(nonce)
        val ciphertext = dis.readBytes()

        val key = deriveKey(password, salt, memKiB, iterations, parallelism)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
            val gz = try {
                cipher.doFinal(ciphertext)
            } catch (e: AEADBadTagException) {
                throw WrongBackupPasswordException(e)
            }
            return gunzip(gz)
        } finally {
            key.wipe()
        }
    }

    private suspend fun deriveKey(password: CharArray, salt: ByteArray, mem: Int, iters: Int, par: Int): ByteArray {
        val pw = password.toUtf8Bytes()
        return try {
            Argon2Kdf.deriveKey(pw, salt, mem, iters, par)
        } finally {
            pw.wipe()
        }
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also { secureRandom.nextBytes(it) }

    private fun gzip(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }

    private fun gunzip(data: ByteArray): ByteArray =
        GZIPInputStream(data.inputStream()).use { it.readBytes() }
}
