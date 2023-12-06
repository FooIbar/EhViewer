package com.hippo.ehviewer.util

import android.annotation.SuppressLint
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

@SuppressLint("CustomX509TrustManager")
object NoopX509TrustManager : X509TrustManager {
    override fun checkClientTrusted(p0: Array<out X509Certificate>, p1: String) = Unit
    override fun checkServerTrusted(p0: Array<out X509Certificate>, p1: String) = Unit
    override fun getAcceptedIssuers() = emptyArray<X509Certificate>()
}
