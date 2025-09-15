package com.hippo.ehviewer.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
context(navigator: DestinationsNavigator)
fun NavigationIcon() = IconButton(onClick = { navigator.popBackStack() }, shapes = IconButtonDefaults.shapes()) {
    Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
}
