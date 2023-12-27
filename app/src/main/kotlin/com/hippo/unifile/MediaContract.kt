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
package com.hippo.unifile

import android.net.Uri
import android.provider.MediaStore

object MediaContract {
    fun getName(self: Uri) = Contracts.queryForString(self, MediaStore.MediaColumns.DISPLAY_NAME, null)

    fun getType(self: Uri) = Contracts.queryForString(self, MediaStore.MediaColumns.MIME_TYPE, null)

    fun lastModified(self: Uri) = Contracts.queryForLong(self, MediaStore.MediaColumns.DATE_MODIFIED, 0)

    fun length(self: Uri) = Contracts.queryForLong(self, MediaStore.MediaColumns.SIZE, 0)
}
