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

package com.mushotoku.app.ui.strings

import android.content.Context
import com.mushotoku.app.R

internal class NotificationStrings(
    val menu: String,
    val menuSubtitle: String,
    val section: String,
    val enableLabel: String,
    val enableHint: String,
    val leadLabel: String,
    val permissionHint: String,
    val leadAtTime: String,
    val lead5Min: String,
    val lead10Min: String,
    val lead15Min: String,
    val lead30Min: String,
    val lead1Hour: String,
)

internal fun notificationStrings(ctx: Context): NotificationStrings = NotificationStrings(
    menu = ctx.getString(R.string.notif_menu),
    menuSubtitle = ctx.getString(R.string.notif_menu_subtitle),
    section = ctx.getString(R.string.notif_section),
    enableLabel = ctx.getString(R.string.notif_enable_label),
    enableHint = ctx.getString(R.string.notif_enable_hint),
    leadLabel = ctx.getString(R.string.notif_lead_label),
    permissionHint = ctx.getString(R.string.notif_permission_hint),
    leadAtTime = ctx.getString(R.string.notif_lead_at_time),
    lead5Min = ctx.getString(R.string.notif_lead_5min),
    lead10Min = ctx.getString(R.string.notif_lead_10min),
    lead15Min = ctx.getString(R.string.notif_lead_15min),
    lead30Min = ctx.getString(R.string.notif_lead_30min),
    lead1Hour = ctx.getString(R.string.notif_lead_1hour),
)
