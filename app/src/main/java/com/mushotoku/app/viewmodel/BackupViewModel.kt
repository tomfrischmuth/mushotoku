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

package com.mushotoku.app.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.mushotoku.app.data.AppDatabase
import com.mushotoku.app.data.backup.BackupCodec
import com.mushotoku.app.data.backup.BackupCorruptException
import com.mushotoku.app.data.backup.BackupRepository
import com.mushotoku.app.data.backup.IncompatibleSchemaException
import com.mushotoku.app.data.backup.WrongBackupPasswordException
import com.mushotoku.app.security.wipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface ImportOutcome {
    data object Success : ImportOutcome
    data object WrongPassword : ImportOutcome
    data object Corrupt : ImportOutcome
    data class Incompatible(val foundVersion: Int) : ImportOutcome
    data class Error(val message: String?) : ImportOutcome
}

sealed interface ExportOutcome {
    data object Success : ExportOutcome
    data class Error(val message: String?) : ExportOutcome
}

class BackupViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = BackupRepository(AppDatabase.getInstance(app))
    private val appVersion: String =
        runCatching {
            app.packageManager.getPackageInfo(app.packageName, 0).versionName
        }.getOrNull() ?: "?"

    suspend fun export(context: Context, uri: Uri, password: CharArray): ExportOutcome = try {
        val payload = repo.buildPayload(appVersion)
        val jsonBytes = repo.encode(payload)
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                BackupCodec.encrypt(os, password, jsonBytes)
            } ?: throw IllegalStateException("Konnte Datei nicht zum Schreiben öffnen")
        }
        jsonBytes.fill(0)
        ExportOutcome.Success
    } catch (e: Exception) {
        ExportOutcome.Error(e.message)
    } finally {
        password.wipe()
    }

    suspend fun import(context: Context, uri: Uri, password: CharArray, replace: Boolean): ImportOutcome = try {
        val jsonBytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { ins ->
                BackupCodec.decrypt(ins, password)
            } ?: throw IllegalStateException("Konnte Datei nicht zum Lesen öffnen")
        }
        val payload = repo.decode(jsonBytes)
        jsonBytes.fill(0)
        if (payload.schemaVersion > BackupRepository.SCHEMA_VERSION) {
            ImportOutcome.Incompatible(payload.schemaVersion)
        } else {
            repo.restore(payload, replace)
            ImportOutcome.Success
        }
    } catch (e: WrongBackupPasswordException) {
        ImportOutcome.WrongPassword
    } catch (e: BackupCorruptException) {
        ImportOutcome.Corrupt
    } catch (e: IncompatibleSchemaException) {
        ImportOutcome.Incompatible(e.foundVersion)
    } catch (e: Exception) {
        ImportOutcome.Error(e.message)
    } finally {
        password.wipe()
    }
}
