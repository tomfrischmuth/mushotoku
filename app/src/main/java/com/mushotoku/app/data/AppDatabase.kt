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
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [Task::class, Note::class, Expense::class, Category::class, AppSettings::class, Habit::class, HabitLog::class, GratitudeEntry::class, MoodEntry::class, CaffeineDose::class, RecurringCostHistory::class, AdditionalIncome::class],
    version = 3,
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
        /**
         * Everything this release adds to the schema, in one step: the note
         * colour and the two notes preferences. Written out rather than left to
         * the destructive fallback, which would wipe every note on update.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE notes ADD COLUMN color INTEGER NOT NULL DEFAULT 0")
                connection.execSQL(
                    "ALTER TABLE app_settings ADD COLUMN newNoteStartsWithTitle INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        /**
         * The switch between lists and notes is gone, and with it its setting.
         * Its column only exists where the unreleased step above once wrote it,
         * so the drop is asked for rather than assumed.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                val hasColumn = connection.prepare(
                    "SELECT COUNT(*) FROM pragma_table_info('app_settings') WHERE name = 'noteTypeFilterEnabled'"
                ).use { it.step() && it.getLong(0) > 0 }
                if (hasColumn) {
                    connection.execSQL("ALTER TABLE app_settings DROP COLUMN noteTypeFilterEnabled")
                }
            }
        }

        fun build(context: Context, dek: ByteArray): AppDatabase {
            val factory = SupportOpenHelperFactory(SqlCipherKey.rawKeyBytes(dek))
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "mushotoku.db")
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
        }

        fun getInstance(context: Context): AppDatabase = DatabaseProvider.requireDatabase()
    }
}
