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

import androidx.room.withTransaction
import com.mushotoku.app.data.AppDatabase
import com.mushotoku.app.data.Expense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class BackupRepository(private val db: AppDatabase) {

    private val dao = db.backupDao()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun buildPayload(appVersion: String): BackupPayload = withContext(Dispatchers.IO) {
        BackupPayload(
            schemaVersion = SCHEMA_VERSION,
            appVersion = appVersion,
            createdAt = System.currentTimeMillis(),
            tasks = dao.allTasks(),
            notes = dao.allNotes(),
            expenses = dao.allExpenses(),
            categories = dao.allCategories(),
            settings = dao.settings(),
            habits = dao.allHabits(),
            habitLogs = dao.allHabitLogs(),
            gratitude = dao.allGratitude(),
            moods = dao.allMoods(),
            caffeineDoses = dao.allCaffeine(),
            recurringCostHistory = dao.allRecurringCostHistory(),
            additionalIncomes = dao.allAdditionalIncomes(),
        )
    }

    fun encode(payload: BackupPayload): ByteArray =
        json.encodeToString(BackupPayload.serializer(), payload).toByteArray(Charsets.UTF_8)

    fun decode(bytes: ByteArray): BackupPayload =
        json.decodeFromString(BackupPayload.serializer(), bytes.decodeToString())

    suspend fun restore(payload: BackupPayload, replace: Boolean) = db.withTransaction {
        if (replace) {
            dao.clearTasks(); dao.clearNotes(); dao.clearExpenses(); dao.clearCategories()
            dao.clearHabits(); dao.clearHabitLogs(); dao.clearGratitude(); dao.clearMoods()
            dao.clearCaffeine(); dao.clearRecurringCostHistory(); dao.clearAdditionalIncomes()
        }
        dao.insertTasks(payload.tasks)
        dao.insertNotes(payload.notes)
        if (replace) dao.insertExpenses(payload.expenses) else mergeExpenses(payload.expenses)
        dao.insertCategories(payload.categories)
        dao.insertHabits(payload.habits)
        dao.insertHabitLogs(payload.habitLogs)
        dao.insertGratitude(payload.gratitude)
        dao.insertMoods(payload.moods)
        dao.insertCaffeine(payload.caffeineDoses)
        dao.insertRecurringCostHistory(payload.recurringCostHistory)
        dao.insertAdditionalIncomes(payload.additionalIncomes)
        payload.settings?.let { dao.insertSettings(it) }
    }

    private suspend fun mergeExpenses(incoming: List<Expense>) {
        if (incoming.isEmpty()) return
        val existing = dao.allExpenses().associateBy { it.date to it.category }
        val merged = incoming
            .groupBy { it.date to it.category }
            .map { (key, rows) ->
                val sum = rows.sumOf { it.amount }
                existing[key]?.let { live -> live.copy(amount = live.amount + sum) }
                    ?: Expense(date = key.first, category = key.second, amount = sum)
            }
        dao.insertExpenses(merged)
    }

    companion object {
        const val SCHEMA_VERSION = 28
    }
}
