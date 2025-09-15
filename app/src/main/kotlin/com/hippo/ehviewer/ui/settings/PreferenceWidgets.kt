package com.hippo.ehviewer.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.integerArrayResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.ehviewer.core.ui.component.RollingNumber
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.util.ProgressDialog
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec
import kotlin.concurrent.atomics.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.DropdownListPreference
import me.zhanghai.compose.preference.IntSliderPreference
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.SwitchPreference

@Composable
fun PreferenceHeader(
    icon: ImageVector,
    @StringRes title: Int,
    childRoute: DirectionDestinationSpec,
    navigator: DestinationsNavigator,
) {
    Preference(
        title = { Text(text = stringResource(title)) },
        icon = { Icon(imageVector = icon, contentDescription = null) },
        onClick = { navigator.navigate(childRoute) },
    )
}

@Composable
fun Preference(title: String, summary: String? = null, onClick: () -> Unit = {}) {
    Preference(
        title = { Text(title) },
        summary = summary?.let { { Text(it) } },
        onClick = onClick,
    )
}

@Composable
fun SwitchPreference(title: String, summary: String? = null, state: MutableState<Boolean>, enabled: Boolean = true) {
    SwitchPreference(
        state = state,
        title = { Text(title) },
        summary = summary?.let { { Text(it) } },
        enabled = enabled,
    )
}

@Composable
fun IntSliderPreference(maxValue: Int, minValue: Int = 0, step: Int = maxValue - minValue - 1, title: String, summary: String? = null, state: MutableState<Int>, enabled: Boolean = true, display: (Int) -> Int = { it }) {
    val sliderState = remember { mutableIntStateOf(state.value) }
    IntSliderPreference(
        state = state,
        title = { Text(title) },
        valueRange = minValue..maxValue,
        valueSteps = step,
        sliderState = sliderState,
        enabled = enabled,
        summary = summary?.let { { Text(it) } },
        valueText = {
            RollingNumber(
                number = display(sliderState.intValue),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                length = 5,
            )
        },
    )
}

@Composable
fun UrlPreference(title: String, url: String) = with(LocalContext.current) {
    Preference(title, url) { openBrowser(url) }
}

@Composable
fun HtmlPreference(title: String, summary: AnnotatedString? = null, onClick: () -> Unit = {}) {
    Preference(
        title = { Text(title) },
        summary = summary?.let { { Text(summary) } },
        onClick = onClick,
    )
}

@Composable
fun SimpleMenuPreferenceInt(title: String, summary: String? = null, @ArrayRes entry: Int, @ArrayRes entryValueRes: Int, state: MutableState<Int>) {
    val entryArray = stringArrayResource(id = entry)
    val valuesArray = integerArrayResource(id = entryValueRes)
    check(entryArray.size == valuesArray.size)
    val map = remember {
        val iter = entryArray.iterator()
        valuesArray.associateWith { iter.next() }
    }
    DropdownListPreference(
        state = state,
        items = map,
        title = { Text(title) },
        summary = { Text(summary ?: map[state.value].orEmpty()) },
    )
}

@Composable
fun WorkPreference(title: String, summary: String? = null, work: suspend CoroutineScope.() -> Unit) {
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    var completed by remember { mutableStateOf(true) }
    if (!completed) {
        ProgressDialog()
    }
    Preference(title = title, summary = summary) {
        completed = false
        coroutineScope.launch(block = work).invokeOnCompletion { completed = true }
    }
}

@Composable
fun <I, O> LauncherPreference(title: String, summary: String? = null, contract: ActivityResultContract<I, O>, key: I, work: suspend CoroutineScope.(O) -> Unit) {
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val callback = remember { AtomicReference<(O) -> Unit> {} }
    val launcher = rememberLauncherForActivityResult(contract = contract) { callback.exchange { }.invoke(it) }
    var completed by remember { mutableStateOf(true) }
    if (!completed) {
        ProgressDialog()
    }
    Preference(title = title, summary = summary) {
        coroutineScope.launch {
            val o = suspendCoroutine { cont ->
                callback.store { cont.resume(it) }
                launcher.launch(key)
            }
            completed = false
            work(o)
        }.invokeOnCompletion { completed = true }
    }
}
