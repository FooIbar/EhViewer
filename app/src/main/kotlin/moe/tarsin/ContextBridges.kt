package moe.tarsin

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import com.hippo.ehviewer.ui.MainActivity
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.Direction
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

context(scope: CoroutineScope)
fun launch(context: CoroutineContext = EmptyCoroutineContext, start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) = scope.launch(context, start, block)

context(scope: CoroutineScope)
fun <T> async(context: CoroutineContext = EmptyCoroutineContext, start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> T) = scope.async(context, start, block)

context(_: CoroutineScope)
fun launchIO(block: suspend CoroutineScope.() -> Unit) = launch(Dispatchers.IO, block = block)

context(_: CoroutineScope)
fun launchUI(block: suspend CoroutineScope.() -> Unit) = launch(Dispatchers.Main, block = block)

context(nav: DestinationsNavigator)
fun navigate(direction: Direction) = nav.navigate(direction)

context(ctx: Context)
fun string(@StringRes id: Int) = ctx.getString(id)

context(ctx: Context)
fun string(@StringRes id: Int, vararg args: Any?) = ctx.getString(id, *args)

context(activity: MainActivity)
fun tip(message: String, useToast: Boolean = false) = activity.showTip(message, useToast)

context(activity: MainActivity)
fun tip(@StringRes id: Int, useToast: Boolean = false) = tip(string(id), useToast)

context(state: SnackbarHostState)
suspend fun snackbar(message: String, actionLabel: String? = null, withDismissAction: Boolean = false, duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite) = state.showSnackbar(message, actionLabel, withDismissAction, duration)
