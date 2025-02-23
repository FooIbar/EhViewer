package com.hippo.ehviewer.ui.screen

import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

@Composable
fun selectedListItemColor() = ListItemDefaults.colors(
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
    headlineColor = MaterialTheme.colorScheme.onSecondaryContainer,
    trailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
)

@Composable
fun listItemOnDrawerColor(selected: Boolean) = if (selected) {
    selectedListItemColor()
} else {
    ListItemDefaults.colors(
        containerColor = DrawerDefaults.modalContainerColor,
    )
}

@Composable
fun topBarOnDrawerColor() = TopAppBarDefaults.topAppBarColors(
    containerColor = DrawerDefaults.modalContainerColor,
    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
)
