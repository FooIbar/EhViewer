package com.hippo.ehviewer.ui.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.ui.StartDestination
import com.hippo.ehviewer.ui.destinations.SelectSiteScreenDestination
import com.hippo.ehviewer.ui.screen.popNavigate
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.ui.tools.LocalWindowSizeClass
import com.hippo.ehviewer.util.ExceptionUtils
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withNonCancellableContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Destination
@Composable
fun CookieSignInScene(navigator: DestinationsNavigator) {
    val windowSizeClass = LocalWindowSizeClass.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var isProgressIndicatorVisible by rememberSaveable { mutableStateOf(false) }

    var ipbMemberId by rememberSaveable { mutableStateOf("") }
    var ipbPassHash by rememberSaveable { mutableStateOf("") }
    var igneous by rememberSaveable { mutableStateOf("") }

    var ipbMemberIdErrorState by rememberSaveable { mutableStateOf(false) }
    var ipbPassHashErrorState by rememberSaveable { mutableStateOf(false) }

    var signInJob by remember { mutableStateOf<Job?>(null) }

    val dialogState = LocalDialogState.current

    val noCookies = stringResource(R.string.from_clipboard_error)

    fun storeCookie(id: String, hash: String, igneous: String) {
        EhUtils.signOut()
        EhCookieStore.addCookie(EhCookieStore.KEY_IPB_MEMBER_ID, id, EhUrl.DOMAIN_E)
        EhCookieStore.addCookie(EhCookieStore.KEY_IPB_PASS_HASH, hash, EhUrl.DOMAIN_E)
        if (igneous.isNotBlank() && igneous != "mystery") {
            EhCookieStore.addCookie(EhCookieStore.KEY_IGNEOUS, igneous, EhUrl.DOMAIN_EX)
        }
    }

    fun login() {
        if (signInJob?.isActive == true) return
        if (ipbMemberId.isBlank()) {
            ipbMemberIdErrorState = true
            return
        } else {
            ipbMemberIdErrorState = false
        }
        if (ipbPassHash.isBlank()) {
            ipbPassHashErrorState = true
            return
        } else {
            ipbPassHashErrorState = false
        }
        focusManager.clearFocus()
        isProgressIndicatorVisible = true
        signInJob = coroutineScope.launchIO {
            runCatching {
                storeCookie(ipbMemberId, ipbPassHash, igneous)
                EhEngine.getProfile()
            }.onSuccess {
                val canEx = withNonCancellableContext { postLogin() }
                withUIContext { navigator.popNavigate(if (canEx) SelectSiteScreenDestination else StartDestination) }
            }.onFailure {
                EhCookieStore.signOut()
                dialogState.awaitPermissionOrCancel(
                    confirmText = R.string.get_it,
                    showCancelButton = false,
                    title = R.string.sign_in_failed,
                    text = {
                        Text(
                            """
                            ${ExceptionUtils.getReadableString(it)}
                            ${stringResource(R.string.wrong_cookie_warning)}
                            """.trimIndent(),
                        )
                    },
                )
                isProgressIndicatorVisible = false
            }
        }
    }

    fun fillCookiesFromClipboard() {
        focusManager.clearFocus()
        val text = clipboardManager.getText()
        if (text == null) {
            coroutineScope.launch { snackbarHostState.showSnackbar(noCookies) }
            return
        }
        runCatching {
            val kvs: Array<String> = if (text.contains(";")) {
                text.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            } else if (text.contains("\n")) {
                text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            } else {
                coroutineScope.launch { snackbarHostState.showSnackbar(noCookies) }
                return
            }
            if (kvs.size < 2) {
                coroutineScope.launch { snackbarHostState.showSnackbar(noCookies) }
                return
            }
            for (s in kvs) {
                val kv: Array<String> = if (s.contains("=")) {
                    s.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                } else if (s.contains(":")) {
                    s.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                } else {
                    continue
                }
                if (kv.size != 2) {
                    continue
                }
                when (kv[0].trim { it <= ' ' }.lowercase(Locale.getDefault())) {
                    "ipb_member_id" -> ipbMemberId = kv[1].trim { it <= ' ' }
                    "ipb_pass_hash" -> ipbPassHash = kv[1].trim { it <= ' ' }
                    "igneous" -> igneous = kv[1].trim { it <= ' ' }
                }
            }
            login()
        }.onFailure {
            it.printStackTrace()
            coroutineScope.launch { snackbarHostState.showSnackbar(noCookies) }
        }
    }

    @Composable
    fun CookiesTextField() {
        OutlinedTextField(
            value = ipbMemberId,
            onValueChange = { ipbMemberId = it.trim { char -> char <= ' ' } },
            modifier = Modifier.width(dimensionResource(id = R.dimen.single_max_width)),
            label = { Text(text = "ipb_member_id") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            supportingText = { if (ipbMemberIdErrorState) Text(stringResource(R.string.text_is_empty)) },
            trailingIcon = {
                if (ipbMemberIdErrorState) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                    )
                }
            },
            isError = ipbMemberIdErrorState,
            singleLine = true,
        )
        OutlinedTextField(
            value = ipbPassHash,
            onValueChange = { ipbPassHash = it.trim { char -> char <= ' ' } },
            modifier = Modifier.width(dimensionResource(id = R.dimen.single_max_width)),
            label = { Text(text = "ipb_pass_hash") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            supportingText = { if (ipbPassHashErrorState) Text(stringResource(R.string.text_is_empty)) },
            trailingIcon = {
                if (ipbPassHashErrorState) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                    )
                }
            },
            isError = ipbPassHashErrorState,
            singleLine = true,
        )
        OutlinedTextField(
            value = igneous,
            onValueChange = { igneous = it.trim { char -> char <= ' ' } },
            modifier = Modifier.width(dimensionResource(id = R.dimen.single_max_width)),
            label = { Text(text = "igneous") },
            keyboardActions = KeyboardActions(onDone = { login() }),
            singleLine = true,
        )
    }

    @Composable
    fun FillCookiesButton(modifier: Modifier) = TextButton(
        onClick = ::fillCookiesFromClipboard,
        modifier = modifier,
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append(stringResource(id = R.string.from_clipboard))
                }
            },
        )
    }

    Box(contentAlignment = Alignment.Center) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
            when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact, WindowWidthSizeClass.Medium -> {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(dimensionResource(id = R.dimen.keyline_margin)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cookie,
                            contentDescription = null,
                            modifier = Modifier.padding(dimensionResource(id = R.dimen.keyline_margin)).size(48.dp),
                            tint = Color(0xff795548),
                        )
                        Text(
                            text = stringResource(id = R.string.cookie_explain),
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = dimensionResource(id = R.dimen.keyline_margin)),
                            fontSize = 16.sp,
                        )
                        CookiesTextField()
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = ::login,
                            modifier = Modifier.fillMaxWidth().padding(top = dimensionResource(R.dimen.keyline_margin)),
                        ) {
                            Text(text = stringResource(id = android.R.string.ok))
                        }
                        FillCookiesButton(modifier = Modifier.padding(horizontal = 8.dp))
                    }
                }
                WindowWidthSizeClass.Expanded -> {
                    Row(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(dimensionResource(id = R.dimen.keyline_margin)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.width(dimensionResource(id = R.dimen.signinscreen_landscape_caption_frame_width)).padding(dimensionResource(id = R.dimen.keyline_margin)),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cookie,
                                contentDescription = null,
                                modifier = Modifier.padding(dimensionResource(id = R.dimen.keyline_margin)).size(48.dp),
                                tint = Color(0xff795548),
                            )
                            Text(
                                text = stringResource(id = R.string.cookie_explain),
                                modifier = Modifier.widthIn(max = dimensionResource(id = R.dimen.signinscreen_landscape_caption_text_width)).padding(top = 24.dp),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            CookiesTextField()
                            Spacer(modifier = Modifier.height(16.dp))
                            Row {
                                Button(
                                    onClick = ::login,
                                    modifier = Modifier.padding(horizontal = 4.dp).width(128.dp),
                                ) {
                                    Text(text = stringResource(id = android.R.string.ok))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                FillCookiesButton(modifier = Modifier.padding(horizontal = 4.dp))
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
