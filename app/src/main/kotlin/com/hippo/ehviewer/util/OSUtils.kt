/*
 * Copyright 2015 Hippo Seven
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
package com.hippo.ehviewer.util

import android.os.Looper
import eu.kanade.tachiyomi.util.system.logcat
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

object OSUtils {
    private const val PROCFS_MEMFILE = "/proc/meminfo"
    private val PROCFS_MEMFILE_FORMAT = Regex("^([a-zA-Z]*):[ \t]*([0-9]*)[ \t]kB")
    private const val MEMTOTAL_STRING = "MemTotal"

    val appAllocatedMemory: Long
        get() = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

    val appMaxMemory: Long
        get() = Runtime.getRuntime().maxMemory()

    val totalMemory by lazy {
        runCatching {
            File(PROCFS_MEMFILE).useLines {
                it.forEach { line ->
                    PROCFS_MEMFILE_FORMAT.find(line)?.destructured?.let { (k, v) ->
                        if (k == MEMTOTAL_STRING) {
                            return@runCatching v.toLong() * 1024
                        }
                    }
                }
            }
            -1L
        }.getOrElse {
            logcat(it)
            -1L
        }
    }
}

val isMainThread: Boolean
    get() = Looper.getMainLooper().thread === Thread.currentThread()

fun assertNotMainThread() {
    check(!isMainThread) { "Cannot access database on the main thread since" + " it may potentially lock the UI for a long period of time." }
}

fun <T> runAssertingNotMainThread(block: suspend CoroutineScope.() -> T) = assertNotMainThread().run { runBlocking(block = block) }
