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
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [Task::class, Note::class, Expense::class, Category::class, AppSettings::class, Habit::class, HabitLog::class, GratitudeEntry::class, MoodEntry::class, CaffeineDose::class, RecurringCostHistory::class, AdditionalIncome::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun habitDao(): HabitDao
    abstract fun gratitudeDao(): GratitudeDao
    abstract fun moodDao(): MoodDao
    abstract fun caffeineDoseDao(): CaffeineDoseDao
    abstract fun recurringCostHistoryDao(): RecurringCostHistoryDao
    abstract fun additionalIncomeDao(): AdditionalIncomeDao
    abstract fun backupDao(): BackupDao

    companion object {
        fun build(context: Context, dek: ByteArray): AppDatabase {
            val factory = SupportOpenHelperFactory(SqlCipherKey.rawKeyBytes(dek))
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "mushotoku.db")
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
        }

        fun getInstance(context: Context): AppDatabase = DatabaseProvider.requireDatabase()
    }
}
