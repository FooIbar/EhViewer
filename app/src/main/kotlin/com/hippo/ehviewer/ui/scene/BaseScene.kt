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
package com.hippo.ehviewer.ui.scene

import android.content.res.Resources.Theme
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.SoftwareKeyboardControllerCompat
import androidx.fragment.app.Fragment
import com.hippo.ehviewer.ui.MainActivity
import rikka.core.res.resolveDrawable

abstract class BaseScene : Fragment() {
    val isDrawerLocked
        get() = mainActivity?.drawerLocked == true

    fun lockDrawer() {
        mainActivity?.drawerLocked = true
    }

    fun unlockDrawer() {
        mainActivity?.drawerLocked = false
    }

    fun openDrawer() = mainActivity?.openDrawer()

    fun showTip(message: CharSequence?, length: Int) {
        mainActivity?.showTip(message!!, length)
    }

    fun showTip(@StringRes id: Int, length: Int) {
        mainActivity?.showTip(id, length)
    }

    open val enableDrawerGestures = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.background = requireActivity().theme.resolveDrawable(android.R.attr.windowBackground)

        // Update left drawer locked state
        if (enableDrawerGestures) {
            unlockDrawer()
        } else {
            lockDrawer()
        }
        hideSoftInput()
        mainActivity?.recompose()
    }

    val mainActivity: MainActivity?
        get() = activity as? MainActivity

    private fun hideSoftInput() = activity?.window?.decorView?.run {
        SoftwareKeyboardControllerCompat(this)
    }?.hide()

    val theme: Theme
        get() = requireActivity().theme

    companion object {
        const val LENGTH_SHORT = 0
        const val LENGTH_LONG = 1
        const val KEY_DRAWER_VIEW_STATE = "com.hippo.ehviewer.ui.scene.BaseScene:DRAWER_VIEW_STATE"
    }
}
