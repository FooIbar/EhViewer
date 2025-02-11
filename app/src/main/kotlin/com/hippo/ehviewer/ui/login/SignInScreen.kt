package com.hippo.ehviewer.ui.login

import androidx.compose.animation.AnimatedVisibilityScope
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.destinations.WebViewSignInScreenDestination
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.ui.tools.LocalWindowSizeClass
import com.hippo.ehviewer.ui.tools.thenIf
import com.hippo.ehviewer.util.displayString
import com.jamal.composeprefs3.ui.ifTrueThen
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.Job

@Destination<RootGraph>(start = true)
@Composable
fun AnimatedVisibilityScope.SignInScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val windowSizeClass = LocalWindowSizeClass.current
    val focusManager = LocalFocusManager.current
    var isProgressIndicatorVisible by rememberSaveable { mutableStateOf(false) }
    var showUsernameError by rememberSaveable { mutableStateOf(false) }
    var showPasswordError by rememberSaveable { mutableStateOf(false) }
    val username = rememberTextFieldState()
    val password = rememberTextFieldState()
    var passwordHidden by rememberSaveable { mutableStateOf(true) }
    var signInJob by remember { mutableStateOf<Job?>(null) }

    fun signIn() {
        if (signInJob?.isActive == true) return
        if (username.text.isEmpty()) {
            showUsernameError = true
            return
        } else {
            showUsernameError = false
        }
        if (password.text.isEmpty()) {
            showPasswordError = true
            return
        } else {
            showPasswordError = false
        }
        focusManager.clearFocus()
        isProgressIndicatorVisible = true

        EhUtils.signOut()
        signInJob = launchIO {
            runCatching {
                EhEngine.signIn(username.text.toString(), password.text.toString())
            }.onFailure {
                withUIContext {
                    focusManager.clearFocus()
                    isProgressIndicatorVisible = false
                    awaitConfirmationOrCancel(
                        confirmText = R.string.get_it,
                        title = R.string.sign_in_failed,
                        showCancelButton = false,
                        text = {
                            Text(
                                """
                                ${it.displayString()}
                                ${stringResource(R.string.sign_in_failed_tip, stringResource(R.string.sign_in_via_webview))}
                                """.trimIndent(),
                            )
                        },
                    )
                }
            }.onSuccess {
                postLogin()
            }
        }
    }

    @Composable
    fun UsernameAndPasswordTextField() {
        OutlinedTextField(
            state = username,
            modifier = Modifier.width(dimensionResource(id = R.dimen.single_max_width))
                .semantics { contentType = ContentType.Username }
                .thenIf(!showUsernameError) { padding(bottom = 16.dp) },
            label = { Text(stringResource(R.string.username)) },
            supportingText = showUsernameError.ifTrueThen { Text(stringResource(R.string.error_username_cannot_empty)) },
            trailingIcon = showUsernameError.ifTrueThen { Icon(imageVector = Icons.Filled.Info, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            lineLimits = TextFieldLineLimits.SingleLine,
            isError = showUsernameError,
        )
        OutlinedSecureTextField(
            state = password,
            modifier = Modifier.width(dimensionResource(id = R.dimen.single_max_width))
                .semantics { contentType = ContentType.Password }
                .thenIf(!showPasswordError) { padding(bottom = 16.dp) },
            label = { Text(stringResource(R.string.password)) },
            supportingText = showPasswordError.ifTrueThen { Text(stringResource(R.string.error_password_cannot_empty)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            onKeyboardAction = { signIn() },
            trailingIcon = {
                if (showPasswordError) {
                    Icon(imageVector = Icons.Filled.Info, contentDescription = null)
                } else {
                    IconButton(onClick = { passwordHidden = !passwordHidden }) {
                        val visibilityIcon = if (passwordHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        Icon(imageVector = visibilityIcon, contentDescription = null)
                    }
                }
            },
            isError = showPasswordError,
            textObfuscationMode = if (passwordHidden) TextObfuscationMode.RevealLastTyped else TextObfuscationMode.Visible,
        )
    }

    Box(contentAlignment = Alignment.Center) {
        when (windowSizeClass.windowWidthSizeClass) {
            WindowWidthSizeClass.COMPACT, WindowWidthSizeClass.MEDIUM -> {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).systemBarsPadding().padding(dimensionResource(id = R.dimen.keyline_margin)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.padding(dimensionResource(id = R.dimen.keyline_margin)),
                    )
                    UsernameAndPasswordTextField()
                    Text(
                        text = stringResource(id = R.string.app_waring),
                        modifier = Modifier.widthIn(max = dimensionResource(id = R.dimen.single_max_width)).padding(top = 24.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(id = R.string.app_waring_2),
                        modifier = Modifier.widthIn(max = dimensionResource(id = R.dimen.single_max_width)).padding(top = 12.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.keyline_margin)))
                    Row(modifier = Modifier.padding(top = dimensionResource(R.dimen.keyline_margin))) {
                        FilledTonalButton(
                            onClick = { openBrowser(EhUrl.URL_REGISTER) },
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        ) {
                            Text(text = stringResource(id = R.string.register))
                        }
                        Button(
                            onClick = ::signIn,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        ) {
                            Text(text = stringResource(id = R.string.sign_in))
                        }
                    }
                    Row(modifier = Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { navigator.navigate(WebViewSignInScreenDestination) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        style = SpanStyle(textDecoration = TextDecoration.Underline),
                                    ) {
                                        append(stringResource(id = R.string.sign_in_via_webview))
                                    }
                                },
                            )
                        }
                        TextButton(
                            onClick = {
                                Settings.gallerySite.value = EhUrl.SITE_E
                                Settings.needSignIn.value = false
                            },
                            modifier = Modifier.weight(1f),
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
            WindowWidthSizeClass.EXPANDED -> {
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
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        UsernameAndPasswordTextField()
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.Center) {
                            Button(
                                onClick = ::signIn,
                                modifier = Modifier.padding(horizontal = 4.dp).width(128.dp),
                            ) {
                                Text(text = stringResource(id = R.string.sign_in))
                            }
                            FilledTonalButton(
                                onClick = { openBrowser(EhUrl.URL_REGISTER) },
                                modifier = Modifier.padding(horizontal = 4.dp).width(128.dp),
                            ) {
                                Text(text = stringResource(id = R.string.register))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.Center) {
                            TextButton(
                                onClick = { navigator.navigate(WebViewSignInScreenDestination) },
                                modifier = Modifier.padding(horizontal = 4.dp).width(128.dp),
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = SpanStyle(textDecoration = TextDecoration.Underline),
                                        ) {
                                            append(stringResource(id = R.string.sign_in_via_webview))
                                        }
                                    },
                                )
                            }
                            TextButton(
                                onClick = {
                                    Settings.gallerySite.value = EhUrl.SITE_E
                                    Settings.needSignIn.value = false
                                },
                                modifier = Modifier.padding(horizontal = 4.dp).width(128.dp),
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
        if (isProgressIndicatorVisible) {
            CircularProgressIndicator()
        }
    }
}
