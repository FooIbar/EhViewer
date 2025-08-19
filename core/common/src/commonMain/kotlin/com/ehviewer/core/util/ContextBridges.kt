package com.ehviewer.core.util

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

context(scope: CoroutineScope)
fun launch(context: CoroutineContext = EmptyCoroutineContext, start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) = scope.launch(context, start, block)

context(scope: CoroutineScope)
fun <T> async(context: CoroutineContext = EmptyCoroutineContext, start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> T) = scope.async(context, start, block)

context(_: CoroutineScope)
fun launchIO(block: suspend CoroutineScope.() -> Unit) = launch(Dispatchers.IO, block = block)

context(_: CoroutineScope)
fun launchUI(block: suspend CoroutineScope.() -> Unit) = launch(Dispatchers.Main, block = block)
