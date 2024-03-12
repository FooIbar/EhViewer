package com.hippo.ehviewer.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.ui.LockDrawer
import com.hippo.ehviewer.ui.StartDestination
import com.hippo.ehviewer.ui.destinations.WebViewSignInScreenDestination
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.ui.screen.popNavigate
import com.hippo.ehviewer.ui.tools.LocalWindowSizeClass
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@RootNavGraph(start = true)
@Destination
@Composable
fun SignInScreen(navigator: DestinationsNavigator) {
    LockDrawer(true)
    val windowSizeClass = LocalWindowSizeClass.current
    val context = LocalContext.current

    Box(contentAlignment = Alignment.Center) {
        when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact, WindowWidthSizeClass.Medium -> {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).systemBarsPadding().padding(dimensionResource(id = R.dimen.keyline_margin)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.padding(dimensionResource(id = R.dimen.keyline_margin)),
                    )
                    Text(
                        text = stringResource(id = R.string.app_waring),
                        modifier = Modifier.widthIn(max = dimensionResource(id = R.dimen.single_max_width)).padding(top = 24.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(id = R.string.app_waring_2),
                        modifier = Modifier.widthIn(max = dimensionResource(id = R.dimen.single_max_width)).padding(top = 12.dp),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.keyline_margin)))
                    Row(modifier = Modifier.padding(top = dimensionResource(R.dimen.keyline_margin))) {
                        FilledTonalButton(
                            onClick = { context.openBrowser(EhUrl.URL_REGISTER) },
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        ) {
                            Text(text = stringResource(id = R.string.register))
                        }
                        Button(
                            onClick = { navigator.navigate(WebViewSignInScreenDestination) },
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        ) {
                            Text(text = stringResource(id = R.string.sign_in))
                        }
                    }
                    TextButton(
                        onClick = {
                            Settings.needSignIn = false
                            Settings.gallerySite = EhUrl.SITE_E
                            navigator.popNavigate(StartDestination)
                        },
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(textDecoration = TextDecoration.Underline),
                                ) {
                                    append(stringResource(id = R.string.guest_mode))
                                }
                            },
                        )
                    }
                }
            }
            WindowWidthSizeClass.Expanded -> {
                Row(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).systemBarsPadding().padding(dimensionResource(id = R.dimen.keyline_margin)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.width(dimensionResource(id = R.dimen.signinscreen_landscape_caption_frame_width)).padding(dimensionResource(id = R.dimen.keyline_margin)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            alignment = Alignment.Center,
                            modifier = Modifier.padding(dimensionResource(id = R.dimen.keyline_margin)),
                        )
                        Text(
                            text = stringResource(id = R.string.app_waring),
                            modifier = Modifier.widthIn(max = 360.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(id = R.string.app_waring_2),
                            modifier = Modifier.widthIn(max = 360.dp),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(48.dp))
                        Row(horizontalArrangement = Arrangement.Center) {
                            Button(
                                onClick = { navigator.navigate(WebViewSignInScreenDestination) },
                                modifier = Modifier.padding(horizontal = 4.dp).width(128.dp),
                            ) {
                                Text(text = stringResource(id = R.string.sign_in))
                            }
                            FilledTonalButton(
                                onClick = { context.openBrowser(EhUrl.URL_REGISTER) },
                                modifier = Modifier.padding(horizontal = 4.dp).width(128.dp),
                            ) {
                                Text(text = stringResource(id = R.string.register))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                Settings.needSignIn = false
                                Settings.gallerySite = EhUrl.SITE_E
                                navigator.popNavigate(StartDestination)
                            },
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        style = SpanStyle(textDecoration = TextDecoration.Underline),
                                    ) {
                                        append(stringResource(id = R.string.guest_mode))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
