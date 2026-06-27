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

package com.mushotoku.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.mushotoku.app.notification.ReminderScheduler
import com.mushotoku.app.security.KeyManager
import com.mushotoku.app.security.SecurityGate

class MushotokuApp : Application() {

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
        SecurityGate.install(KeyManager(this))
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            ReminderScheduler.CHANNEL_ID,
            getString(R.string.notif_channel_appointments),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.notif_channel_appointments_desc)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
