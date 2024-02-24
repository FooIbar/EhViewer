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
package com.hippo.ehviewer.util

import com.hippo.ehviewer.R
import eu.kanade.tachiyomi.util.system.logcat
import java.io.IOException
import java.net.MalformedURLException
import java.net.ProtocolException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import splitties.init.appCtx

fun Throwable.displayString(): String {
    logcat(this)
    val cause = cause
    return when {
        cause != null && this is IOException -> getReadableStringInternal(cause)
        else -> getReadableStringInternal(this)
    }
}

private fun getReadableStringInternal(e: Throwable) = when (e) {
    is MalformedURLException -> appCtx.getString(R.string.error_invalid_url)
    is SocketTimeoutException -> appCtx.getString(R.string.error_timeout)
    is UnknownHostException -> appCtx.getString(R.string.error_unknown_host)
    is StatusCodeException -> buildString {
        append(appCtx.getString(R.string.error_bad_status_code, e.responseCode))
        if (e.isIdentifiedResponseCode) {
            append(", ").append(e.message)
        }
    }
    is ProtocolException -> if (e.message!!.startsWith("Too many follow-up requests:")) {
        appCtx.getString(R.string.error_redirection)
    } else {
        appCtx.getString(R.string.error_socket)
    }
    is SocketException, is SSLException -> appCtx.getString(R.string.error_socket)
    else -> e.message ?: appCtx.getString(R.string.error_unknown)
}

object ExceptionUtils {
    fun throwIfFatal(t: Throwable) {
        // values here derived from https://github.com/ReactiveX/RxJava/issues/748#issuecomment-32471495
        when (t) {
            is VirtualMachineError, is ThreadDeath, is LinkageError -> {
                throw t
            }
        }
    }
}
