package com.hippo.ehviewer.ui.screen

import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

@Composable
fun listItemOnDrawerColor() = ListItemDefaults.colors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
)

@Composable
fun topBarOnDrawerColor() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
)
