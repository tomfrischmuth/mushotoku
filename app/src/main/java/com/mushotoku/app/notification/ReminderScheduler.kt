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

package com.mushotoku.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.mushotoku.app.R
import com.mushotoku.app.data.Task
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {

    const val CHANNEL_ID = "appointment_reminders"

    const val ACTION_REMIND = "com.mushotoku.app.action.APPOINTMENT_REMINDER"
    const val EXTRA_TITLE   = "extra_title"
    const val EXTRA_TEXT    = "extra_text"
    const val EXTRA_ID      = "extra_id"

    private const val PREFS   = "appointment_reminders"
    private const val KEY_IDS = "scheduled_ids"

    fun sync(
        context: Context,
        appointments: List<Task>,
        enabled: Boolean,
        leadMinutes: Int,
    ) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        prefs.getStringSet(KEY_IDS, emptySet()).orEmpty().forEach { idStr ->
            idStr.toLongOrNull()?.let { id ->
                am.cancel(buildPendingIntent(context, id, null, null))
            }
        }

        val scheduled = mutableSetOf<String>()
        if (enabled) {
            val now = System.currentTimeMillis()
            appointments.forEach { task ->
                if (!task.isAppointment || task.time.isBlank() || task.isDone) return@forEach
                val triggerAt = (appointmentMillis(task) ?: return@forEach) - leadMinutes * 60_000L
                if (triggerAt <= now) return@forEach
                val text = reminderText(context, task.time, leadMinutes)
                schedule(am, context, task.id, task.title, text, triggerAt)
                scheduled += task.id.toString()
            }
        }
        prefs.edit().putStringSet(KEY_IDS, scheduled).apply()
    }

    private fun schedule(
        am: AlarmManager,
        context: Context,
        id: Long,
        title: String,
        text: String,
        triggerAt: Long,
    ) {
        val pi = buildPendingIntent(context, id, title, text)
        if (am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun buildPendingIntent(
        context: Context,
        id: Long,
        title: String?,
        text: String?,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_ID, id)
            if (title != null) putExtra(EXTRA_TITLE, title)
            if (text != null) putExtra(EXTRA_TEXT, text)
        }
        var flags = PendingIntent.FLAG_IMMUTABLE
        flags = flags or PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getBroadcast(context, id.toInt(), intent, flags)
    }

    private fun appointmentMillis(task: Task): Long? {
        val parts = task.time.split(":")
        val hour   = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalDate.ofEpochDay(task.date)
            .atTime(LocalTime.of(hour, minute))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun reminderText(context: Context, time: String, leadMinutes: Int): String = when {
        leadMinutes <= 0 -> context.getString(R.string.reminder_now, time)
        leadMinutes % 60 == 0 -> {
            val h = leadMinutes / 60
            context.resources.getQuantityString(R.plurals.reminder_in_hours, h, h, time)
        }
        else -> context.resources.getQuantityString(R.plurals.reminder_in_minutes, leadMinutes, leadMinutes, time)
    }
}
