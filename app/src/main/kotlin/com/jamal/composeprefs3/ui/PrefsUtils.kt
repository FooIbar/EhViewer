package com.jamal.composeprefs3.ui

import androidx.compose.runtime.Composable

fun Any?.ifNotNullThen(content: @Composable () -> Unit): (@Composable () -> Unit)? = if (this != null) content else null

fun Boolean.ifTrueThen(content: @Composable () -> Unit): (@Composable () -> Unit)? = if (this) content else null
