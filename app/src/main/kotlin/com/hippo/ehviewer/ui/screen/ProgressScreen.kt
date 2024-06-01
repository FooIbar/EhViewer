package com.hippo.ehviewer.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.big.SadAndroid
import com.hippo.ehviewer.util.displayString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.withUIContext
import moe.tarsin.coroutines.runSuspendCatching

@Destination<RootGraph>
@Composable
fun ProgressScreen(gid: Long, token: String, page: Int, navigator: DestinationsNavigator) {
    val wrong = stringResource(id = R.string.error_something_wrong_happened)
    var error by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(error) {
        if (error.isEmpty()) {
            if (gid == -1L || token == "invalid" || page == -1) {
                error = wrong
            } else {
                runSuspendCatching {
                    EhEngine.getGalleryToken(gid, token, page)
                }.onSuccess {
                    withUIContext {
                        navigator.popBackStack()
                        navigator.navigate(gid asDstPageTo page with it)
                    }
                }.onFailure {
                    error = it.displayString()
                }
            }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (error.isNotBlank()) {
            Column(
                modifier = Modifier.clickable { error = "" },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = EhIcons.Big.Default.SadAndroid,
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = wrong,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        } else {
            CircularProgressIndicator()
        }
    }
}
