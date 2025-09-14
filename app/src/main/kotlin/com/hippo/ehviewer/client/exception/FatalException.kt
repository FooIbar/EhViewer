package com.hippo.ehviewer.client.exception

import androidx.annotation.StringRes
import com.ehviewer.core.i18n.R

sealed class FatalException : EhException {
    constructor(message: String) : super(message)
    constructor(@StringRes messageId: Int) : super(messageId)
}

class IpBannedException(message: String) : FatalException(message)
class InsufficientGpException : FatalException(R.string.insufficient_gp)
class QuotaExceededException : FatalException(R.string.error_509)
