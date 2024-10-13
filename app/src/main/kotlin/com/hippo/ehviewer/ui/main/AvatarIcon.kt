package com.hippo.ehviewer.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import arrow.core.Either
import arrow.core.Some
import arrow.core.left
import arrow.core.none
import arrow.core.right
import coil3.compose.AsyncImage
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.parser.HomeParser
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.util.displayString
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching

private val limitScope = CoroutineScope(Dispatchers.IO)
private val refreshEvent = MutableSharedFlow<Unit>()
private suspend fun refresh() = refreshEvent.emit(Unit)
private fun <T> Flow<T>.keepIf(predicate: (old: T, new: T) -> Boolean) = distinctUntilChanged(predicate)

private val limitFlow = refreshEvent.conflate().map { EhEngine.getImageLimits().right() }
    .retryWhen<Either<String, HomeParser.Result>> { e, _ -> emit(e.displayString().left()).let { true } }
    .map(::Some).let { merge(it, refreshEvent.map { none() }) }
    .keepIf { o, n -> o.isSome { it.isRight() } && n.isNone() }
    .stateIn(limitScope, SharingStarted.Eagerly, none())

context(CoroutineScope, DialogState, SnackbarHostState, DestinationsNavigator)
@Composable
fun AvatarIcon() {
    val hasSignedIn by Settings.hasSignedIn.collectAsState()
    if (hasSignedIn) {
        val placeholder = stringResource(id = R.string.please_wait)
        val resetImageLimitSucceed = stringResource(id = R.string.reset_limits_succeed)
        val result by limitFlow.collectAsState()
        IconButton(
            onClick = {
                launch {
                    refresh()
                    awaitConfirmationOrCancel(
                        confirmText = R.string.reset,
                        title = R.string.image_limits,
                        confirmButtonEnabled = result.isSome { it.isRight { it.limits.resetCost != 0 } },
                    ) {
                        result.onNone {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
                                Text(text = placeholder)
                            }
                        }
                        result.onSome { current ->
                            when (current) {
                                is Either.Left -> Text(text = current.value)
                                is Either.Right -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val (limits, funds) = current.value
                                    if (limits.maximum > 0) {
                                        val value by animateFloatAsState(limits.current.toFloat() / limits.maximum)
                                        LinearProgressIndicator(
                                            progress = { value },
                                            modifier = Modifier.height(12.dp).fillMaxWidth(),
                                        )
                                    }
                                    Text(
                                        text = when (limits.maximum) {
                                            -1 -> stringResource(id = R.string.image_limits_restricted)
                                            0 -> stringResource(id = R.string.image_limits_normal)
                                            else -> stringResource(id = R.string.image_limits_summary, limits.current, limits.maximum)
                                        },
                                    )
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
                            }
                        }
                    }
                    runSuspendCatching {
                        EhEngine.resetImageLimits()
                    }.onSuccess {
                        refresh()
                        showSnackbar(resetImageLimitSucceed)
                    }
                }
            },
        ) {
            val avatar by Settings.avatar.collectAsState()
            AnimatedContent(targetState = avatar == null) { noAvatar ->
                if (noAvatar) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null)
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
        IconButton(
            onClick = {
                launch {
                    awaitConfirmationOrCancel(
                        confirmText = R.string.sign_in,
                        showCancelButton = false,
                        text = { Text(text = stringResource(id = R.string.settings_eh_identity_cookies_guest)) },
                    )
                    EhUtils.signOut()
                }
            },
        ) {
            Icon(imageVector = Icons.Default.NoAccounts, contentDescription = null)
        }
    }
}
