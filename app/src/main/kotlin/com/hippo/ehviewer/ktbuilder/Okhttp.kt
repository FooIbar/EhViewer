package com.hippo.ehviewer.ktbuilder

import okhttp3.OkHttpClient

inline fun httpClient(builder: OkHttpClient.Builder.() -> Unit): OkHttpClient = OkHttpClient.Builder().apply(builder).build()
