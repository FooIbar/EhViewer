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
import android.os.Parcelable
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.SoftwareKeyboardControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.android.material.sidesheet.SideSheetDialog
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.util.getSparseParcelableArrayCompat
import rikka.core.res.resolveDrawable

abstract class BaseScene : Fragment() {
    private var drawerView: View? = null
    private var drawerViewState: SparseArray<Parcelable>? = null
    private var sideSheetDialog: SideSheetDialog? = null

    val isDrawerLocked
        get() = mainActivity?.drawerLocked == true

    fun lockDrawer() {
        mainActivity?.drawerLocked = true
    }

    fun unlockDrawer() {
        mainActivity?.drawerLocked = false
    }

    fun openDrawer() = mainActivity?.openDrawer()

    fun openSideSheet() = sideSheetDialog!!.show()
    fun closeSideSheet() = sideSheetDialog!!.dismiss()

    fun showTip(message: CharSequence?, length: Int) {
        mainActivity?.showTip(message!!, length)
    }

    fun showTip(@StringRes id: Int, length: Int) {
        mainActivity?.showTip(id, length)
    }

    open val enableDrawerGestures = false

    private fun createDrawerView(savedInstanceState: Bundle?): View? {
        drawerView = onCreateDrawerView(layoutInflater)
        if (drawerView != null) {
            val saved = drawerViewState ?: savedInstanceState?.getSparseParcelableArrayCompat(KEY_DRAWER_VIEW_STATE)
            saved?.let {
                drawerView!!.restoreHierarchyState(saved)
            }
        }
        return drawerView
    }

    open fun onCreateDrawerView(inflater: LayoutInflater): View? {
        return null
    }

    private fun destroyDrawerView() {
        if (drawerView != null) {
            drawerViewState = SparseArray()
            drawerView!!.saveHierarchyState(drawerViewState)
        }
        onDestroyDrawerView()
        drawerView = null
    }

    open fun onDestroyDrawerView() {
        sideSheetDialog?.dismiss()
        sideSheetDialog = null
    }

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
        createDrawerView(savedInstanceState)?.let {
            sideSheetDialog = SideSheetDialog(requireContext()).apply {
                window?.decorView?.apply {
                    val owner = viewLifecycleOwner
                    setViewTreeLifecycleOwner(owner)
                    setViewTreeViewModelStoreOwner(owner as ViewModelStoreOwner)
                    setViewTreeSavedStateRegistryOwner(owner as SavedStateRegistryOwner)
                }
                setContentView(it)
            }
        }
        mainActivity?.recompose()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        destroyDrawerView()
    }

    val mainActivity: MainActivity?
        get() = activity as? MainActivity

    fun hideSoftInput() = activity?.window?.decorView?.run { SoftwareKeyboardControllerCompat(this) }?.hide()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (drawerView != null) {
            drawerViewState = SparseArray()
            drawerView!!.saveHierarchyState(drawerViewState)
            outState.putSparseParcelableArray(KEY_DRAWER_VIEW_STATE, drawerViewState)
        }
    }

    val theme: Theme
        get() = requireActivity().theme

    companion object {
        const val LENGTH_SHORT = 0
        const val LENGTH_LONG = 1
        const val KEY_DRAWER_VIEW_STATE = "com.hippo.ehviewer.ui.scene.BaseScene:DRAWER_VIEW_STATE"
    }
}
