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
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.AuthenticationRequest.Biometric
import androidx.biometric.AuthenticationRequest.Companion.biometricRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.registerForAuthenticationResult
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings

fun Context.isAuthenticationSupported(): Boolean {
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    return BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
}

class SecurityActivity : AppCompatActivity() {
    override fun onResume() {
        super.onResume()
        if (isAuthenticationSupported()) {
            startAuthentication()
        } else {
            onSuccess()
        }
    }

    private val authLauncher = registerForAuthenticationResult { result ->
        when (result) {
            is AuthenticationResult.Success -> onSuccess()
            is AuthenticationResult.Error -> onError()
        }
    }

    @Synchronized
    private fun startAuthentication() {
        if (isAuthenticating) return
        isAuthenticating = true
        val request = biometricRequest(
            title = getString(R.string.settings_privacy_require_unlock),
            authFallback = Biometric.Fallback.DeviceCredential,
        ) {}
        authLauncher.launch(request)
    }

    private fun onSuccess() {
        isAuthenticating = false
        locked = false
        finish()
    }

    private fun onError() {
        moveTaskToBack(true)
        isAuthenticating = false
    }
}

private var isAuthenticating = false
var locked = true
var lockedLastLeaveTime: Long = 0

val lockObserver = object : DefaultLifecycleObserver {
    override fun onPause(owner: LifecycleOwner) {
        if (!locked) {
            lockedLastLeaveTime = System.currentTimeMillis() / 1000
        }
        locked = true
    }
}

fun Context.interceptSecurityOrReturn() {
    val lockedResumeTime = System.currentTimeMillis() / 1000
    val lockedDelayTime = lockedResumeTime - lockedLastLeaveTime
    if (lockedDelayTime < Settings.securityDelay * 60) {
        locked = false
    } else if (Settings.security.value && isAuthenticationSupported() && locked) {
        startActivity(Intent(this, SecurityActivity::class.java))
    }
}
