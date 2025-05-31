package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.openBrowser
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.android.rememberLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.LicenseScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.license)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        val libraries by rememberLibraries(R.raw.aboutlibraries)
        LibrariesContainer(
            libraries = libraries,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
            padding = LibraryDefaults.libraryPadding(licensePadding = LibraryDefaults.chipPadding(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp))),
            textStyles = LibraryDefaults.libraryTextStyles(licensesTextStyle = MaterialTheme.typography.labelSmall),
            onLibraryClick = { library ->
                library.website?.let { openBrowser(it) }
            },
        )
    }
}
