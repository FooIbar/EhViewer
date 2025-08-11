package com.ehviewer.core.mainthread

import android.os.Looper

internal actual val isMainThread: Boolean
    get() = Looper.getMainLooper().isCurrentThread
