package com.hippo.ehviewer.ktbuilder

import okhttp3.OkHttpClient

inline fun httpClient(builder: OkHttpClient.Builder.() -> Unit): OkHttpClient = OkHttpClient.Builder().apply(builder).build()
inline fun httpClient(client: OkHttpClient, builder: OkHttpClient.Builder.() -> Unit): OkHttpClient = client.newBuilder().apply(builder).build()
