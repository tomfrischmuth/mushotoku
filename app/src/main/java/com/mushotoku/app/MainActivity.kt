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

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import android.os.SystemClock
import android.view.ViewTreeObserver
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.platform.LocalContext
import com.mushotoku.app.security.KeyInvalidatedException
import com.mushotoku.app.security.KeyMode
import com.mushotoku.app.security.LocalSecurityController
import com.mushotoku.app.security.SecurityController
import com.mushotoku.app.security.SecurityGate
import com.mushotoku.app.security.WrongPassphraseException
import com.mushotoku.app.security.wipe
import com.mushotoku.app.ui.screens.LockScreen
import javax.crypto.Cipher
import java.util.Locale as JavaLocale
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mushotoku.app.ui.components.*
import com.mushotoku.app.ui.theme.MushotokuTheme
import com.mushotoku.app.viewmodel.AppViewModel
import com.mushotoku.app.viewmodel.MeditationViewModel
import com.mushotoku.app.viewmodel.*
import com.mushotoku.app.ui.dialogs.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : FragmentActivity() {

    private val _addTaskTrigger  = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _openTodayTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    companion object {
        const val ACTION_ADD_TASK   = "com.mushotoku.app.ACTION_ADD_TASK"
        const val ACTION_OPEN_TODAY = "com.mushotoku.app.ACTION_OPEN_TODAY"

        /** smallestScreenWidthDp at/above which the screen counts as "large"
         *  (e.g. a foldable's unfolded inner display) and orientation is freed. */
        const val LARGE_SCREEN_SW_DP = 600
    }

    private val securityController by lazy {
        SecurityController(
            appContext = applicationContext,
            scope = lifecycleScope,
            biometricPresence = { onSuccess, onError -> authenticateBiometricPresence(onSuccess, onError) },
        )
    }

    override fun onStart() {
        super.onStart()
        securityController.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        securityController.onAppBackgrounded()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when (intent.action) {
            ACTION_ADD_TASK   -> _addTaskTrigger.tryEmit(Unit)
            ACTION_OPEN_TODAY -> _openTodayTrigger.tryEmit(Unit)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyAdaptiveOrientation(newConfig)
    }

    /**
     * Keep portrait locked on phone-sized screens — including a foldable's
     * narrow cover/folded display — but allow free orientation once the screen
     * is large (the unfolded inner display). Driven at runtime because the lock
     * depends on the current configuration, not a static manifest value.
     */
    private fun applyAdaptiveOrientation(config: Configuration) {
        requestedOrientation = if (config.smallestScreenWidthDp >= LARGE_SCREEN_SW_DP) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyAdaptiveOrientation(resources.configuration)
        when (intent?.action) {
            ACTION_ADD_TASK   -> _addTaskTrigger.tryEmit(Unit)
            ACTION_OPEN_TODAY -> _openTodayTrigger.tryEmit(Unit)
        }

        val startTime = SystemClock.elapsedRealtime()
        var contentReady = false

        val decorView = window.decorView
        decorView.filterTouchesWhenObscured = true
        decorView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                return if (contentReady && SystemClock.elapsedRealtime() - startTime >= 500L) {
                    decorView.viewTreeObserver.removeOnPreDrawListener(this)
                    true
                } else false
            }
        })

        enableEdgeToEdge()
        setContent {
            SideEffect { contentReady = true }
            AppLockGate {
                UnlockedRoot()
            }
        }
    }

    @Composable
    private fun UnlockedRoot() {
        val vm: AppViewModel = viewModel()
        val meditationVm: MeditationViewModel = viewModel()
        val settingsVm: SettingsViewModel = viewModel()
        val settings by settingsVm.settings.collectAsStateWithLifecycle()
        LaunchedEffect(settings.appLockTimeoutSeconds) {
            securityController.timeoutSeconds = settings.appLockTimeoutSeconds
        }
        LaunchedEffect(settings.blockScreenshots) {
            if (settings.blockScreenshots) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        MushotokuTheme(
            themeMode = settings.themeMode,
            fontScale  = settings.fontScale
        ) {
            CompositionLocalProvider(LocalSecurityController provides securityController) {
                Box(Modifier.fillMaxSize()) {
                    MushotokuApp(
                        vm              = vm,
                        meditationVm    = meditationVm,
                        addTaskTrigger  = _addTaskTrigger,
                        openTodayTrigger = _openTodayTrigger
                    )

                    var showBrandSplash by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        delay(1100)
                        showBrandSplash = false
                    }
                    AnimatedVisibility(visible = showBrandSplash, exit = fadeOut()) {
                        BrandSplash()
                    }

                    if (securityController.relocked) {
                        ReLockOverlay()
                    }
                }
            }
        }
    }

    @Composable
    private fun ReLockOverlay() {
        val context = LocalContext.current
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        MushotokuTheme(themeMode = "DARK") {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent().changes.forEach { it.consume() }
                            }
                        }
                    }
            ) {
            LockScreen(
                mode = securityController.mode,
                errorText = error,
                keyInvalidated = false,
                onRequestBiometric = {
                    error = null
                    authenticateBiometric(
                        onSuccess = { securityController.clearRelock() },
                        onInvalidated = { error = lockErrorWrongPassphrase() },
                        onError = { msg -> error = msg },
                    )
                },
                onSubmitPassphrase = { chars ->
                    error = null
                    scope.launch {
                        try {
                            SecurityGate.unlockWithPassphrase(context, chars)
                            securityController.clearRelock()
                        } catch (e: WrongPassphraseException) {
                            error = lockErrorWrongPassphrase()
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            chars.wipe()
                        }
                    }
                },
                onReset = { securityController.clearRelock() },
            )
            }
        }
    }

    @Composable
    private fun AppLockGate(content: @Composable () -> Unit) {
        val context = LocalContext.current
        var gate by remember { mutableStateOf<GateState>(GateState.Loading) }
        var error by remember { mutableStateOf<String?>(null) }
        var invalidated by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            gate = runCatching { SecurityGate.prepare(context) }
                .map {
                    when (it) {
                        SecurityGate.StartGate.UNLOCKED -> GateState.Unlocked
                        SecurityGate.StartGate.NEEDS_BIOMETRIC -> GateState.Locked(KeyMode.KEYSTORE_LOCK)
                        SecurityGate.StartGate.NEEDS_PASSPHRASE -> GateState.Locked(KeyMode.PASSPHRASE)
                    }
                }
                .getOrElse { GateState.Failed(it.message) }
        }

        when (val g = gate) {
            GateState.Loading -> MushotokuTheme(themeMode = "DARK") { BrandSplash() }
            GateState.Unlocked -> content()
            is GateState.Failed -> MushotokuTheme(themeMode = "DARK") { BrandSplash() }
            is GateState.Locked -> MushotokuTheme(themeMode = "DARK") {
                LockScreen(
                    mode = g.mode,
                    errorText = error,
                    keyInvalidated = invalidated,
                    onRequestBiometric = {
                        error = null
                        authenticateBiometric(
                            onSuccess = { cipher ->
                                runCatching { SecurityGate.unlockWithKeystore(context, cipher) }
                                    .onSuccess { securityController.clearRelock(); gate = GateState.Unlocked }
                                    .onFailure { e ->
                                        if (e is KeyInvalidatedException) invalidated = true
                                        else error = e.message
                                    }
                            },
                            onInvalidated = { invalidated = true },
                            onError = { msg -> error = msg },
                        )
                    },
                    onSubmitPassphrase = { chars ->
                        error = null
                        scope.launch {
                            try {
                                SecurityGate.unlockWithPassphrase(context, chars)
                                securityController.clearRelock()
                                gate = GateState.Unlocked
                            } catch (e: WrongPassphraseException) {
                                error = lockErrorWrongPassphrase()
                            } catch (e: Exception) {
                                error = e.message
                            } finally {
                                chars.wipe()
                            }
                        }
                    },
                    onReset = {
                        scope.launch {
                            SecurityGate.resetAndReinitialize(context)
                            securityController.clearRelock()
                            gate = GateState.Unlocked
                        }
                    },
                )
            }
        }
    }

    private fun authenticateBiometric(
        onSuccess: (Cipher) -> Unit,
        onInvalidated: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val cipher = try {
            SecurityGate.keyManager.getCipherForBiometricPrompt()
        } catch (e: KeyInvalidatedException) {
            onInvalidated(); return
        } catch (e: Exception) {
            onError(e.message ?: "Fehler"); return
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.bio_unlock_title))
            .setSubtitle(getString(R.string.bio_confirm_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val c = result.cryptoObject?.cipher
                    if (c != null) onSuccess(c) else onError("Kein freigeschalteter Cipher")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        onError(errString.toString())
                    }
                }
            },
        )
        prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    private fun authenticateBiometricPresence(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.bio_confirm_title))
            .setSubtitle(getString(R.string.bio_confirm_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        onError(errString.toString())
                    }
                }
            },
        )
        prompt.authenticate(info)
    }

    private fun lockErrorWrongPassphrase(): String = getString(R.string.wrong_passphrase)
}

internal fun localeListForSetting(setting: String): LocaleList = when (setting) {
    "", "AUTO" -> LocaleList.getEmptyLocaleList()
    "DE" -> LocaleList.forLanguageTags("de")
    "EN" -> LocaleList.forLanguageTags("en")
    else -> LocaleList.forLanguageTags(setting)
}

private sealed interface GateState {
    data object Loading : GateState
    data object Unlocked : GateState
    data class Locked(val mode: KeyMode) : GateState
    data class Failed(val message: String?) : GateState
}
