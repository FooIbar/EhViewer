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
package com.hippo.ehviewer.ui.legacy

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class AutoStaggeredGridLayoutManager(columnSize: Int, orientation: Int) : StaggeredGridLayoutManager(1, orientation) {
    private var mColumnSize = columnSize
    private var mColumnSizeChanged = true

    fun setColumnSize(columnSize: Int) {
        if (columnSize == mColumnSize) {
            return
        }
        mColumnSize = columnSize
        mColumnSizeChanged = true
    }

    override fun supportsPredictiveItemAnimations() = false

    override fun onMeasure(recycler: RecyclerView.Recycler, state: RecyclerView.State, widthSpec: Int, heightSpec: Int) {
        if (mColumnSizeChanged && mColumnSize > 0) {
            val totalSpace = if (orientation == VERTICAL) {
                View.MeasureSpec.getSize(widthSpec) - paddingRight - paddingLeft
            } else {
                View.MeasureSpec.getSize(heightSpec) - paddingTop - paddingBottom
            }
            spanCount = (totalSpace / mColumnSize).coerceAtLeast(1)
            mColumnSizeChanged = false
        }
        super.onMeasure(recycler, state, widthSpec, heightSpec)
    }
}
