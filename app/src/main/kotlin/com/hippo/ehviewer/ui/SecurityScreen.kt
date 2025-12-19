/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.hippo.ehviewer.ui

import android.content.Context
import androidx.biometric.AuthenticationRequest.Biometric
import androidx.biometric.AuthenticationRequest.Companion.biometricRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.compose.rememberAuthenticationLauncher
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ehviewer.core.i18n.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

context(ctx: Context)
fun isAuthenticationSupported(): Boolean {
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    return BiometricManager.from(ctx).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
}

@Composable
fun SecurityScreen(onError: () -> Unit, modifier: Modifier) {
    val security by Settings.security.collectAsState()
    var locked by remember { mutableStateOf(security) }
    var isAuthenticating by remember { mutableStateOf(false) }
    val title = stringResource(R.string.settings_privacy_require_unlock)
    val launcher = rememberAuthenticationLauncher { result ->
        isAuthenticating = false
        when (result) {
            is AuthenticationResult.Success -> locked = false
            is AuthenticationResult.Error -> onError()
        }
    }

    @Synchronized
    fun startAuthentication() {
        if (isAuthenticating) return
        isAuthenticating = true
        val request = biometricRequest(
            title = title,
            authFallback = Biometric.Fallback.DeviceCredential,
        ) {}
        launcher.launch(request)
    }

    val securityDelay by Settings.securityDelay.collectAsState { it.minutes }
    var leaveTime by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }
    LifecycleResumeEffect(Unit) {
        if (security && leaveTime.elapsedNow() > securityDelay) {
            locked = true
        }
        onPauseOrDispose {
            if (!locked) {
                leaveTime = TimeSource.Monotonic.markNow()
            }
        }
    }

    if (locked) {
        val windowInfo = LocalWindowInfo.current
        LaunchedEffect(windowInfo) {
            snapshotFlow { windowInfo.isWindowFocused }.collect {
                if (it) startAuthentication()
            }
        }
        Surface(modifier = modifier.fillMaxSize()) {}
    }
}
