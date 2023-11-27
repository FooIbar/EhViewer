package com.hippo.ehviewer.ui.tools

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jamal.composeprefs3.ui.ifNotNullThen
import com.jamal.composeprefs3.ui.ifTrueThen
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

fun interface ActionScope {
    fun onSelect(action: String, that: suspend () -> Unit)
}

interface DialogScope<R> {
    var expectedValue: R
}

fun interface DismissDialogScope<R> {
    fun dismissWith(value: R)
}

class DialogState {
    var content: (@Composable () -> Unit)? by mutableStateOf(null)

    @Composable
    fun Intercept() = content?.invoke()

    fun dismiss() {
        content = null
    }

    suspend inline fun <R> dialog(crossinline block: @Composable (CancellableContinuation<R>) -> Unit) = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { dismiss() }
        val realContinuation = object : CancellableContinuation<R> by cont {
            override fun resumeWith(result: Result<R>) {
                dismiss()
                cont.resumeWith(result)
            }
        }
        content = { block(realContinuation) }
    }

    suspend fun <R> awaitResult(initial: R, @StringRes title: Int? = null, block: @Composable DialogScope<R>.() -> Unit): R {
        return dialog { cont ->
            val state = remember(cont) { mutableStateOf(initial) }
            val impl = remember(cont) {
                object : DialogScope<R> {
                    override var expectedValue by state
                }
            }
            AlertDialog(
                onDismissRequest = { cont.cancel() },
                confirmButton = {
                    TextButton(onClick = { cont.resume(state.value) }) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                },
                title = title.ifNotNullThen { Text(text = stringResource(id = title!!)) },
                text = { block(impl) },
            )
        }
    }

    suspend fun awaitInputText(
        initial: String = "",
        title: String? = null,
        hint: String? = null,
        isNumber: Boolean = false,
        @StringRes confirmText: Int = android.R.string.ok,
        invalidator: (suspend (String) -> String?)? = null,
    ): String {
        return dialog { cont ->
            val coroutineScope = rememberCoroutineScope()
            var state by remember(cont) { mutableStateOf(initial) }
            var error by remember(cont) { mutableStateOf<String?>(null) }
            AlertDialog(
                onDismissRequest = { cont.cancel() },
                confirmButton = {
                    TextButton(onClick = {
                        if (invalidator == null) {
                            cont.resume(state)
                        } else {
                            coroutineScope.launch {
                                error = invalidator.invoke(state)
                                error ?: cont.resume(state)
                            }
                        }
                    }) {
                        Text(text = stringResource(id = confirmText))
                    }
                },
                title = title.ifNotNullThen { Text(text = title!!) },
                text = {
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it },
                        label = hint.ifNotNullThen {
                            Text(text = hint!!)
                        },
                        trailingIcon = error.ifNotNullThen {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                            )
                        },
                        supportingText = error.ifNotNullThen {
                            Text(text = error!!)
                        },
                        isError = error != null,
                        keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                    )
                },
            )
        }
    }

    suspend fun awaitInputTextWithCheckBox(
        initial: String = "",
        @StringRes title: Int? = null,
        @StringRes hint: Int? = null,
        checked: Boolean,
        @StringRes checkBoxText: Int,
        isNumber: Boolean = false,
        invalidator: (suspend (String, Boolean) -> String?)? = null,
    ): Pair<String, Boolean> {
        return dialog { cont ->
            val coroutineScope = rememberCoroutineScope()
            var state by remember(cont) { mutableStateOf(initial) }
            var error by remember(cont) { mutableStateOf<String?>(null) }
            var checkedState by remember { mutableStateOf(checked) }
            AlertDialog(
                onDismissRequest = { cont.cancel() },
                confirmButton = {
                    TextButton(onClick = {
                        if (invalidator == null) {
                            cont.resume(state to checkedState)
                        } else {
                            coroutineScope.launch {
                                error = invalidator.invoke(state, checkedState)
                                error ?: cont.resume(state to checkedState)
                            }
                        }
                    }) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                },
                title = title.ifNotNullThen { Text(text = stringResource(id = title!!)) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = hint.ifNotNullThen {
                                Text(text = stringResource(id = hint!!))
                            },
                            trailingIcon = error.ifNotNullThen {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                )
                            },
                            supportingText = error.ifNotNullThen {
                                Text(text = error!!)
                            },
                            isError = error != null,
                            keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                        )
                        Row(
                            modifier = Modifier.clickable { checkedState = !checkedState }.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checkedState,
                                onCheckedChange = { checkedState = !checkedState },
                            )
                            Text(text = stringResource(checkBoxText))
                        }
                    }
                },
            )
        }
    }

    suspend fun awaitPermissionOrCancel(
        @StringRes confirmText: Int = android.R.string.ok,
        @StringRes dismissText: Int = android.R.string.cancel,
        showCancelButton: Boolean = true,
        @StringRes title: Int? = null,
        onDismiss: () -> Unit = {},
        text: (@Composable () -> Unit)? = null,
    ) {
        return dialog { cont ->
            AlertDialog(
                onDismissRequest = {
                    onDismiss()
                    cont.cancel()
                },
                confirmButton = {
                    TextButton(onClick = { cont.resume(Unit) }) {
                        Text(text = stringResource(id = confirmText))
                    }
                },
                dismissButton = showCancelButton.ifTrueThen {
                    TextButton(onClick = {
                        onDismiss()
                        cont.cancel()
                    }) {
                        Text(text = stringResource(id = dismissText))
                    }
                },
                title = title.ifNotNullThen { Text(text = stringResource(id = title!!)) },
                text = text,
            )
        }
    }

    suspend fun showDatePicker(
        @StringRes title: Int,
        initialSelectedDateMillis: Long? = null,
        initialDisplayedMonthMillis: Long? = initialSelectedDateMillis,
        yearRange: IntRange = DatePickerDefaults.YearRange,
        initialDisplayMode: DisplayMode = DisplayMode.Picker,
        selectableDates: SelectableDates = DatePickerDefaults.AllDates,
        showModeToggle: Boolean = true,
    ): Long? {
        return dialog { cont ->
            val state = rememberDatePickerState(
                initialSelectedDateMillis,
                initialDisplayedMonthMillis,
                yearRange,
                initialDisplayMode,
                selectableDates,
            )
            DatePickerDialog(
                onDismissRequest = { cont.cancel() },
                confirmButton = {
                    TextButton(onClick = { cont.resume(state.selectedDateMillis) }) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { cont.cancel() }) {
                        Text(text = stringResource(id = android.R.string.cancel))
                    }
                },
            ) {
                DatePicker(
                    state = state,
                    title = {
                        Text(
                            text = stringResource(id = title),
                            modifier = Modifier.padding(DatePickerTitlePadding),
                        )
                    },
                    showModeToggle = showModeToggle,
                )
            }
        }
    }

    suspend fun <R> showNoButton(respectDefaultWidth: Boolean = true, block: @Composable DismissDialogScope<R>.() -> Unit): R {
        return dialog { cont ->
            val impl = remember(cont) {
                DismissDialogScope<R> {
                    cont.resume(it)
                }
            }
            BasicAlertDialog(
                onDismissRequest = { cont.cancel() },
                properties = DialogProperties(usePlatformDefaultWidth = respectDefaultWidth),
                content = {
                    Surface(
                        modifier = with(Modifier) { if (!respectDefaultWidth) defaultMinSize(280.dp) else width(280.dp) },
                        shape = AlertDialogDefaults.shape,
                        color = AlertDialogDefaults.containerColor,
                        tonalElevation = AlertDialogDefaults.TonalElevation,
                        content = { block(impl) },
                    )
                },
            )
        }
    }

    suspend fun showTimePicker(
        title: String,
        initialHour: Int,
        initialMinute: Int,
    ) = dialog { cont ->
        val state = rememberTimePickerState(initialHour, initialMinute)
        Dialog(
            onDismissRequest = { cont.cancel() },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                modifier = Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min).background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    TimePicker(state = state)
                    Row(modifier = Modifier.height(40.dp).fillMaxWidth()) {
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { cont.cancel() }) {
                            Text(stringResource(id = android.R.string.cancel))
                        }
                        TextButton(onClick = { cont.resume(state.hour to state.minute) }) {
                            Text(stringResource(id = android.R.string.ok))
                        }
                    }
                }
            }
        }
    }

    suspend fun showSingleChoice(
        items: Array<String>,
        selected: Int,
    ): Int = showNoButton {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            items.forEachIndexed { index, text ->
                Row(
                    modifier = Modifier.clickable { dismissWith(index) }.fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = index == selected, onClick = { dismissWith(index) })
                    Text(text = text)
                }
            }
        }
    }

    suspend fun showSelectItem(
        vararg items: String?,
        @StringRes title: Int,
    ) = showSelectItem(
        *items.filterNotNull().mapIndexed { a, b -> b to a }.toTypedArray(),
        title = title,
    )

    suspend fun <R> showSelectItem(
        vararg items: Pair<String, R>,
        @StringRes title: Int? = null,
    ): R = showNoButton {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            if (title != null) {
                Text(
                    text = stringResource(id = title),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            LazyColumn {
                items(items) { (text, item) ->
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.tertiary) {
                        Text(
                            text = text,
                            modifier = Modifier.clickable { dismissWith(item) }.fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }

    suspend inline fun showSelectActions(
        @StringRes title: Int? = null,
        builder: ActionScope.() -> Unit,
    ) = showSelectItem(
        *buildList { builder(ActionScope { action, that -> add(action to that) }) }.toTypedArray(),
        title = title,
    ).invoke()

    suspend fun showSelectItemWithCheckBox(
        vararg items: String?,
        @StringRes title: Int,
        @StringRes checkBoxText: Int,
    ) = showSelectItemWithCheckBox(
        *items.filterNotNull().mapIndexed { a, b -> b to a }.toTypedArray(),
        title = title,
        checkBoxText = checkBoxText,
    )

    private suspend fun <R> showSelectItemWithCheckBox(
        vararg items: Pair<String, R>,
        @StringRes title: Int,
        @StringRes checkBoxText: Int,
    ): Pair<R, Boolean> = showNoButton {
        var checked by remember { mutableStateOf(false) }
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = stringResource(id = title),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
            LazyColumn {
                items(items) { (text, item) ->
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.tertiary) {
                        Text(
                            text = text,
                            modifier = Modifier.clickable { dismissWith(item to checked) }.fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.clickable { checked = !checked }.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { checked = !checked },
                )
                Text(text = stringResource(checkBoxText))
            }
        }
    }

    suspend fun showSelectItemWithIcon(
        vararg items: Pair<ImageVector, Int>,
        title: String,
    ): Int = showNoButton {
        LazyColumn {
            stickyHeader {
                Text(text = title, modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
            }
            itemsIndexed(items) { index, (icon, text) ->
                ListItem(
                    headlineContent = {
                        Text(text = stringResource(id = text), style = MaterialTheme.typography.titleMedium)
                    },
                    modifier = Modifier.clickable { dismissWith(index) },
                    leadingContent = {
                        Icon(imageVector = icon, contentDescription = null, tint = AlertDialogDefaults.iconContentColor)
                    },
                )
            }
        }
    }

    suspend fun showSelectItemWithIconAndTextField(
        vararg items: Pair<ImageVector, String>,
        @StringRes title: Int,
        @StringRes hint: Int,
        maxChar: Int,
    ): Pair<Int, String> = showNoButton(false) {
        Column {
            Text(text = stringResource(id = title), modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
            CircularLayout(
                modifier = Modifier.fillMaxWidth().aspectRatio(1F),
                placeFirstItemInCenter = true,
            ) {
                var note by remember { mutableStateOf("") }
                TextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(0.45F).aspectRatio(1F),
                    label = { Text(text = stringResource(id = hint)) },
                    trailingIcon = {
                        if (note.isNotEmpty()) {
                            IconButton(onClick = { note = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    supportingText = {
                        Text(
                            text = "${note.toByteArray().size} / $maxChar",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                        )
                    },
                    shape = ShapeDefaults.ExtraSmall,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                items.forEachIndexed { index, (icon, text) ->
                    Column(
                        modifier = Modifier.clip(IconWithTextCorner).clickable { dismissWith(index to note) }
                            .fillMaxWidth(0.2F),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = AlertDialogDefaults.iconContentColor)
                        Text(
                            text = text,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 2,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

private val IconWithTextCorner = RoundedCornerShape(8.dp)
private val DatePickerTitlePadding = PaddingValues(start = 24.dp, end = 12.dp, top = 16.dp)

val LocalDialogState = compositionLocalOf<DialogState> { error("CompositionLocal LocalDialogState not present!") }
