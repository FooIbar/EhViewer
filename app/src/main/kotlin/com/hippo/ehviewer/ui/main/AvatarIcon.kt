package com.hippo.ehviewer.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.parser.HomeParser
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ui.login.refreshAccountInfo
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.util.displayString
import eu.kanade.tachiyomi.util.lang.withIOContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching

context(CoroutineScope, DialogState, SnackbarHostState)
@Composable
fun AvatarIcon() {
    val hasSignedIn by Settings.hasSignedIn.collectAsState()
    val avatar by Settings.avatar.collectAsState()
    if (hasSignedIn) {
        val placeholder = stringResource(id = R.string.please_wait)
        val resetImageLimitSucceed = stringResource(id = R.string.reset_limits_succeed)
        var result by rememberSaveable { mutableStateOf<HomeParser.Result?>(null) }
        var error by rememberSaveable { mutableStateOf<String?>(null) }
        val summary by rememberUpdatedState(
            result?.run {
                when (limits.maximum) {
                    -1 -> stringResource(id = R.string.image_limits_restricted)
                    0 -> stringResource(id = R.string.image_limits_normal)
                    else -> stringResource(id = R.string.image_limits_summary, limits.current, limits.maximum)
                }
            } ?: placeholder,
        )
        suspend fun getImageLimits() {
            result = EhEngine.getImageLimits()
            error = null
        }
        if (result == null && error == null) {
            LaunchedEffect(Unit) {
                runSuspendCatching {
                    withIOContext { getImageLimits() }
                }.onFailure {
                    error = it.displayString()
                }
            }
        }

        fun showImageLimit() = launch {
            awaitConfirmationOrCancel(
                confirmText = R.string.reset,
                title = R.string.image_limits,
                confirmButtonEnabled = result?.run { limits.resetCost != 0 } == true,
            ) {
                error?.let {
                    Text(text = it)
                } ?: result?.let { (limits, funds) ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (limits.maximum > 0) {
                            LinearProgressIndicator(
                                progress = { limits.current.toFloat() / limits.maximum },
                                modifier = Modifier.height(12.dp).fillMaxWidth(),
                            )
                        }
                        Text(text = summary)
                        if (limits.resetCost > 0) {
                            Text(text = stringResource(id = R.string.reset_cost, limits.resetCost))
                        }
                        Text(text = stringResource(id = R.string.current_funds))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            FundsItem(
                                type = "GP",
                                amount = funds.gp,
                            )
                            FundsItem(
                                type = "C",
                                amount = funds.credit,
                            )
                        }
                    }
                } ?: run {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
                        Text(text = placeholder)
                    }
                }
            }
            runSuspendCatching {
                EhEngine.resetImageLimits()
                getImageLimits()
            }.onSuccess {
                showSnackbar(resetImageLimitSucceed)
            }.onFailure {
                error = it.displayString()
            }
        }
        IconButton(onClick = ::showImageLimit) {
            AnimatedContent(targetState = avatar == null) { needRefresh ->
                if (needRefresh) {
                    LaunchedEffect(Unit) {
                        refreshAccountInfo()
                    }
                    Icon(imageVector = Icons.Default.NoAccounts, contentDescription = null)
                } else {
                    AsyncImage(
                        model = avatar,
                        contentDescription = null,
                        modifier = Modifier.clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    } else {
        IconButton(onClick = {}) {
            Icon(imageVector = Icons.Default.NoAccounts, contentDescription = null)
        }
    }
}
