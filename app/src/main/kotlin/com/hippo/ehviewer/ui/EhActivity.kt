/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.ui

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ActivityNavigator
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.util.isAtLeastR
import eu.kanade.tachiyomi.util.view.setSecureScreen

abstract class EhActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // https://issuetracker.google.com/204791558
        // Fix system bars insets still exist in fullscreen mode on API < 30
        @Suppress("DEPRECATION")
        if (!isAtLeastR) {
            with(window.decorView) {
                systemUiVisibility = systemUiVisibility and View.SYSTEM_UI_FLAG_LAYOUT_STABLE.inv()
            }
        }
    }

    override fun onResume() {
        interceptSecurityOrReturn()
        super.onResume()
        window.setSecureScreen(Settings.enabledSecurity.value)
    }

    override fun finish() {
        super.finish()
        ActivityNavigator.applyPopAnimationsToPendingTransition(this)
    }
}
