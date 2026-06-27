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

internal class SecurityStrings(
    val sectionAppLock: String,
    val appLockTitle: String,
    val appLockHint: String,
    val protectionLevel: String,
    val levelNoLock: String,
    val levelBiometric: String,
    val levelPassphrase: String,
    val timeoutTitle: String,
    val timeoutImmediate: String,
    val timeoutOneMin: String,
    val timeoutFiveMin: String,
    val setupPassphrase: String,
    val setupPassphraseHint: String,
    val changePassphrase: String,
    val enableWarnTitle: String,
    val enableWarnMessage: String,
    val activate: String,
    val cancel: String,
    val confirm: String,
    val save: String,
    val passphraseNoResetWarning: String,
    val newPassphrase: String,
    val confirmPassphrase: String,
    val currentPassphrase: String,
    val tooShort: String,
    val mismatch: String,
    val disableLockTitle: String,
    val disableViaPassphrase: String,
    val lockBusyTitle: String,
    val lockBusyHint: String,
    val sectionScreen: String,
    val blockScreenshotsTitle: String,
    val blockScreenshotsHint: String,
    val sectionBackup: String,
    val createBackup: String,
    val createBackupHint: String,
    val restoreBackup: String,
    val restoreBackupHint: String,
    val backupPasswordTitle: String,
    val backupPasswordWarning: String,
    val backupPassword: String,
    val confirmBackupPassword: String,
    val enterBackupPassword: String,
    val importModeTitle: String,
    val importModeMessage: String,
    val replaceData: String,
    val mergeData: String,
    val backupExported: String,
    val backupImported: String,
    val backupExportFailed: String,
    val wrongBackupPassword: String,
    val corruptBackup: String,
    val incompatibleBackup: String,
)

internal fun securityStrings(ctx: Context): SecurityStrings = SecurityStrings(
    sectionAppLock = ctx.getString(R.string.sec_section_app_lock),
    appLockTitle = ctx.getString(R.string.sec_app_lock_title),
    appLockHint = ctx.getString(R.string.sec_app_lock_hint),
    protectionLevel = ctx.getString(R.string.sec_protection_level),
    levelNoLock = ctx.getString(R.string.sec_level_no_lock),
    levelBiometric = ctx.getString(R.string.sec_level_biometric),
    levelPassphrase = ctx.getString(R.string.sec_level_passphrase),
    timeoutTitle = ctx.getString(R.string.sec_timeout_title),
    timeoutImmediate = ctx.getString(R.string.sec_timeout_immediate),
    timeoutOneMin = ctx.getString(R.string.sec_timeout_one_min),
    timeoutFiveMin = ctx.getString(R.string.sec_timeout_five_min),
    setupPassphrase = ctx.getString(R.string.sec_setup_passphrase),
    setupPassphraseHint = ctx.getString(R.string.sec_setup_passphrase_hint),
    changePassphrase = ctx.getString(R.string.sec_change_passphrase),
    enableWarnTitle = ctx.getString(R.string.sec_enable_warn_title),
    enableWarnMessage = ctx.getString(R.string.sec_enable_warn_message),
    activate = ctx.getString(R.string.sec_activate),
    cancel = ctx.getString(R.string.sec_cancel),
    confirm = ctx.getString(R.string.sec_confirm),
    save = ctx.getString(R.string.sec_save),
    passphraseNoResetWarning = ctx.getString(R.string.sec_passphrase_no_reset_warning),
    newPassphrase = ctx.getString(R.string.sec_new_passphrase),
    confirmPassphrase = ctx.getString(R.string.sec_confirm_passphrase),
    currentPassphrase = ctx.getString(R.string.sec_current_passphrase),
    tooShort = ctx.getString(R.string.sec_too_short),
    mismatch = ctx.getString(R.string.sec_mismatch),
    disableLockTitle = ctx.getString(R.string.sec_disable_lock_title),
    disableViaPassphrase = ctx.getString(R.string.sec_disable_via_passphrase),
    lockBusyTitle = ctx.getString(R.string.sec_lock_busy_title),
    lockBusyHint = ctx.getString(R.string.sec_lock_busy_hint),
    sectionScreen = ctx.getString(R.string.sec_section_screen),
    blockScreenshotsTitle = ctx.getString(R.string.sec_block_screenshots_title),
    blockScreenshotsHint = ctx.getString(R.string.sec_block_screenshots_hint),
    sectionBackup = ctx.getString(R.string.sec_section_backup),
    createBackup = ctx.getString(R.string.sec_create_backup),
    createBackupHint = ctx.getString(R.string.sec_create_backup_hint),
    restoreBackup = ctx.getString(R.string.sec_restore_backup),
    restoreBackupHint = ctx.getString(R.string.sec_restore_backup_hint),
    backupPasswordTitle = ctx.getString(R.string.sec_backup_password_title),
    backupPasswordWarning = ctx.getString(R.string.sec_backup_password_warning),
    backupPassword = ctx.getString(R.string.sec_backup_password),
    confirmBackupPassword = ctx.getString(R.string.sec_confirm_backup_password),
    enterBackupPassword = ctx.getString(R.string.sec_enter_backup_password),
    importModeTitle = ctx.getString(R.string.sec_import_mode_title),
    importModeMessage = ctx.getString(R.string.sec_import_mode_message),
    replaceData = ctx.getString(R.string.sec_replace_data),
    mergeData = ctx.getString(R.string.sec_merge_data),
    backupExported = ctx.getString(R.string.sec_backup_exported),
    backupImported = ctx.getString(R.string.sec_backup_imported),
    backupExportFailed = ctx.getString(R.string.sec_backup_export_failed),
    wrongBackupPassword = ctx.getString(R.string.sec_wrong_backup_password),
    corruptBackup = ctx.getString(R.string.sec_corrupt_backup),
    incompatibleBackup = ctx.getString(R.string.sec_incompatible_backup),
)
