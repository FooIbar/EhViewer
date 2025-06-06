/*
 * Copyright 2018 Hippo Seven
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
package com.hippo.ehviewer.shortcuts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hippo.ehviewer.download.DownloadService

/**
 * Created by onlymash on 3/25/18.
 */
class ShortcutsActivity : AppCompatActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (val action = intent?.action) {
            DownloadService.ACTION_START_ALL, DownloadService.ACTION_STOP_ALL -> DownloadService.startService(action)
        }
        finish()
    }
}
