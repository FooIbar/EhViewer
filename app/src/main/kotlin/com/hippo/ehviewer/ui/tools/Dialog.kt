package com.hippo.ehviewer.ui.tools

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NewLabel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.right
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.EhTagDatabase.suggestion
import com.jamal.composeprefs3.ui.ifNotNullThen
import com.jamal.composeprefs3.ui.ifTrueThen
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

fun interface ActionScope {
    fun onSelect(action: String, that: suspend () -> Unit)
}

interface DialogScope<R> {
    var expectedValue: R
}

typealias MutableComposable = MutableState<(@Composable BoxScope.() -> Unit)?>

class DialogState(val mutex: MutatorMutex = MutatorMutex()) : MutableComposable by mutableStateOf(null) {
    @Composable
    fun rememberLocal() = remember { DialogState(mutex) }

    context(scope: BoxScope)
    @Composable
    fun Place() = value?.let { it(scope) }

    fun dismiss() {
        value = null
    }
}

context(state: DialogState)
suspend inline fun <R> dialog(crossinline block: @Composable BoxScope.(CancellableContinuation<R>) -> Unit) = state.mutex.mutate {
    try {
        suspendCancellableCoroutine { cont -> state.value = { block(cont) } }
    } finally {
        state.dismiss()
    }
}

context(_: DialogState)
suspend fun <R> awaitResult(
    initial: R,
    @StringRes title: Int? = null,
    invalidator: (suspend Raise<String>.(R) -> Unit)? = null,
    block: @Composable DialogScope<R>.(String?) -> Unit,
): R = dialog { cont ->
    val state = remember(cont) { mutableStateOf(initial) }
    var errorMsg by remember(cont) { mutableStateOf<String?>(null) }
    val impl = remember(cont) {
        object : DialogScope<R> {
            override var expectedValue by state
        }
    }
    if (invalidator != null) {
        LaunchedEffect(state) {
            snapshotFlow { state.value }.collectLatest {
                errorMsg = either { invalidator(it) }.leftOrNull()
            }
        }
    }
    AlertDialog(
        onDismissRequest = { cont.cancel() },
        confirmButton = {
            TextButton(onClick = {
                if (invalidator == null || errorMsg == null) {
                    cont.resume(state.value)
                }
            }) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                cont.cancel()
            }) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
        title = title.ifNotNullThen { Text(text = stringResource(id = title!!)) },
        text = { block(impl, errorMsg) },
    )
}

context(ctx: Context, state: DialogState)
suspend fun awaitSelectTags(): List<String> = dialog { cont ->
    val selected = remember { mutableStateListOf<String>() }
    val state = rememberTextFieldState()
    var suggestionTranslate by rememberMutableStateInDataStore("SuggestionTranslate") { false }
    PausableAlertDialog(
        confirmButton = {
            TextButton(
                onClick = { cont.resume(selected.toList()) },
                content = { Text(text = stringResource(id = android.R.string.ok)) },
            )
        },
        dismissButton = {
            TextButton(
                onClick = { cont.cancel() },
                content = { Text(text = stringResource(id = android.R.string.cancel)) },
            )
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(id = R.string.action_add_tag))
                if (EhTagDatabase.translatable) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(id = R.string.translate_tag_for_tagger),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Checkbox(
                        checked = suggestionTranslate,
                        onCheckedChange = { suggestionTranslate = !suggestionTranslate },
                    )
                }
            }
        },
        text = {
            Column {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    selected.forEach { text ->
                        InputChip(
                            selected = true,
                            onClick = { },
                            label = { Text(text = text) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.clickable { selected -= text },
                                )
                            },
                        )
                    }
                }
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!it) expanded = false },
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                        state = state,
                        label = { Text(text = stringResource(id = R.string.action_add_tag_tip)) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val text = state.text.toString().trim()
                                    if (text.isNotEmpty()) {
                                        selected += text
                                        state.clearText()
                                    }
                                },
                                content = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                    )
                                },
                            )
                        },
                    )
                    val query = state.text.toString().trim().takeIf { s -> s.isNotEmpty() }
                    var items by remember { mutableStateOf(emptyList<Pair<String, String?>>()) }
                    LaunchedEffect(suggestionTranslate, query) {
                        items = query?.let { suggestion(query, suggestionTranslate).take(15).toList() }.orEmpty()
                        expanded = items.isNotEmpty()
                    }
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {},
                        modifier = Modifier.heightIn(max = 192.dp),
                    ) {
                        items.forEach { (tag, hint) ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = tag, maxLines = 1)
                                        ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                                            if (hint != null) {
                                                Text(
                                                    text = hint,
                                                    maxLines = 1,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    if (tag.endsWith(':')) {
                                        state.setTextAndPlaceCursorAtEnd(tag)
                                    } else {
                                        selected += tag
                                        state.clearText()
                                    }
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            }
        },
        idleIcon = Icons.Default.NewLabel,
    )
}

context(_: DialogState)
suspend fun awaitInputText(
    initial: String = "",
    title: String? = null,
    hint: String? = null,
    isNumber: Boolean = false,
    @StringRes confirmText: Int = android.R.string.ok,
    onUserDismiss: (() -> Unit)? = null,
    invalidator: (suspend Raise<String>.(String) -> Unit)? = null,
) = dialog { cont ->
    val coroutineScope = rememberCoroutineScope()
    val state = rememberTextFieldState(initial)
    var error by remember(cont) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = {
            cont.cancel()
            onUserDismiss?.invoke()
        },
        confirmButton = {
            TextButton(onClick = {
                val text = state.text.toString()
                if (invalidator == null) {
                    cont.resume(text)
                } else {
                    coroutineScope.launch {
                        error = either { invalidator(text) }.leftOrNull()
                        error ?: cont.resume(text)
                    }
                }
            }) {
                Text(text = stringResource(id = confirmText))
            }
        },
        title = title.ifNotNullThen { Text(text = title!!) },
        text = {
            OutlinedTextField(
                state = state,
                label = hint?.let { { Text(text = it) } },
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

context(_: DialogState)
suspend fun awaitInputTextWithCheckBox(
    initial: String = "",
    @StringRes title: Int? = null,
    @StringRes hint: Int? = null,
    checked: Boolean,
    @StringRes checkBoxText: Int,
    isNumber: Boolean = false,
    invalidator: (suspend Raise<String>.(String, Boolean) -> Unit)? = null,
): Pair<String, Boolean> = dialog { cont ->
    val coroutineScope = rememberCoroutineScope()
    val state = rememberTextFieldState(initial)
    var error by remember(cont) { mutableStateOf<String?>(null) }
    var checkedState by remember { mutableStateOf(checked) }
    AlertDialog(
        onDismissRequest = { cont.cancel() },
        confirmButton = {
            TextButton(onClick = {
                val text = state.text.toString()
                if (invalidator == null) {
                    cont.resume(text to checkedState)
                } else {
                    coroutineScope.launch {
                        error = either { invalidator(text, checkedState) }.leftOrNull()
                        error ?: cont.resume(text to checkedState)
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
                    state = state,
                    label = hint?.let {
                        { Text(text = stringResource(id = it)) }
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
                LabeledCheckbox(
                    modifier = Modifier.fillMaxWidth(),
                    checked = checkedState,
                    onCheckedChange = { checkedState = !checkedState },
                    label = stringResource(checkBoxText),
                )
            }
        },
    )
}

context(_: DialogState)
suspend fun awaitConfirmationOrCancel(
    @StringRes confirmText: Int = android.R.string.ok,
    @StringRes dismissText: Int = android.R.string.cancel,
    @StringRes title: Int? = null,
    showConfirmButton: Boolean = true,
    showCancelButton: Boolean = true,
    onCancelButtonClick: () -> Unit = {},
    secure: Boolean = false,
    text: @Composable (() -> Unit)? = null,
) = dialog { cont ->
    AlertDialog(
        onDismissRequest = { cont.cancel() },
        confirmButton = {
            if (showConfirmButton) {
                TextButton(onClick = { cont.resume(Unit) }) {
                    Text(text = stringResource(id = confirmText))
                }
            }
        },
        dismissButton = showCancelButton.ifTrueThen {
            TextButton(onClick = {
                onCancelButtonClick()
                cont.cancel()
            }) {
                Text(text = stringResource(id = dismissText))
            }
        },
        title = title.ifNotNullThen { Text(text = stringResource(id = title!!)) },
        text = text,
        properties = if (secure) {
            DialogProperties(securePolicy = SecureFlagPolicy.SecureOn)
        } else {
            DialogProperties()
        },
    )
}

context(_: DialogState)
suspend fun awaitSelectDate(
    @StringRes title: Int,
    initialSelectedDateMillis: Long? = null,
    initialDisplayedMonthMillis: Long? = initialSelectedDateMillis,
    yearRange: IntRange = DatePickerDefaults.YearRange,
    initialDisplayMode: DisplayMode = DisplayMode.Picker,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
    showModeToggle: Boolean = true,
): Long? = dialog { cont ->
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

context(_: DialogState)
suspend fun <R> showNoButton(respectDefaultWidth: Boolean = true, block: @Composable Continuation<R>.() -> Unit): R = dialog { cont ->
    BasicAlertDialog(
        onDismissRequest = { cont.cancel() },
        properties = DialogProperties(usePlatformDefaultWidth = respectDefaultWidth),
        content = {
            Surface(
                modifier = with(Modifier) { if (!respectDefaultWidth) defaultMinSize(280.dp) else width(280.dp) },
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
                content = { block(cont) },
            )
        },
    )
}

context(_: DialogState)
suspend fun awaitSelectTime(
    title: String,
    initialHour: Int,
    initialMinute: Int,
) = dialog { cont ->
    val state = rememberTimePickerState(initialHour, initialMinute)
    TimePickerDialog(
        onDismissRequest = { cont.cancel() },
        confirmButton = {
            TextButton(onClick = { cont.resume(state.hour to state.minute) }) {
                Text(stringResource(id = android.R.string.ok))
            }
        },
        title = {
            Text(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                text = title,
                style = MaterialTheme.typography.labelMedium,
            )
        },
        dismissButton = {
            TextButton(onClick = { cont.cancel() }) {
                Text(stringResource(id = android.R.string.cancel))
            }
        },
        content = { TimePicker(state = state) },
    )
}

context(_: DialogState)
suspend fun awaitSingleChoice(
    items: List<String>,
    selected: Int,
    @StringRes title: Int? = null,
): Int = showNoButton {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        title?.let {
            Text(
                text = stringResource(id = it),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        items.forEachIndexed { index, text ->
            Row(
                modifier = Modifier.clickable { resume(index) }.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = index == selected, onClick = { resume(index) })
                Text(text = text)
            }
        }
    }
}

context(_: DialogState)
suspend fun awaitSelectItem(
    items: List<String>,
    @StringRes title: Int? = null,
    selected: Int = -1,
    respectDefaultWidth: Boolean = true,
) = awaitSelectItem(items, title?.right(), selected, respectDefaultWidth)

context(_: DialogState)
suspend fun awaitSelectItem(
    items: List<String>,
    title: Either<String, Int>?,
    selected: Int = -1,
    respectDefaultWidth: Boolean = true,
): Int = showNoButton(respectDefaultWidth) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        if (title != null) {
            Text(
                text = title.fold({ it }, { stringResource(id = it) }),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        FastScrollLazyColumn {
            itemsIndexed(items) { index, text ->
                CheckableItem(
                    text = text,
                    checked = index == selected,
                    modifier = Modifier.fillMaxWidth().clickable { resume(index) }.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

context(_: DialogState)
suspend inline fun awaitSelectAction(
    @StringRes title: Int? = null,
    selected: Int = -1,
    builder: ActionScope.() -> Unit,
): suspend () -> Unit {
    val (items, actions) = buildList { builder { action, that -> add(action to that) } }.unzip()
    val index = awaitSelectItem(items, title, selected)
    return actions[index]
}

context(_: DialogState)
suspend fun awaitSelectItemWithCheckBox(
    items: List<String>,
    @StringRes title: Int,
    @StringRes checkBoxText: Int,
    selected: Int = -1,
    initialChecked: Boolean = false,
): Pair<Int, Boolean> = showNoButton {
    var checked by remember { mutableStateOf(initialChecked) }
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = stringResource(id = title),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
        FastScrollLazyColumn {
            itemsIndexed(items) { index, text ->
                CheckableItem(
                    text = text,
                    checked = index == selected,
                    modifier = Modifier.fillMaxWidth().clickable { resume(index to checked) }.padding(horizontal = 8.dp),
                )
            }
        }
        LabeledCheckbox(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            checked = checked,
            onCheckedChange = { checked = !checked },
            label = stringResource(checkBoxText),
        )
    }
}

context(_: DialogState)
suspend fun awaitSelectItemWithIcon(
    items: List<Pair<ImageVector, Int>>,
    title: String,
): Int = showNoButton {
    LazyColumn {
        stickyHeader {
            Text(text = title, modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
        }
        itemsIndexed(items) { index, (icon, text) ->
            ListItem(
                headlineContent = { Text(text = stringResource(id = text), style = MaterialTheme.typography.titleMedium) },
                modifier = Modifier.clickable { resume(index) }.padding(horizontal = 8.dp),
                leadingContent = { Icon(imageVector = icon, contentDescription = null, tint = AlertDialogDefaults.iconContentColor) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}

context(_: DialogState)
suspend fun awaitSelectItemWithIconAndTextField(
    items: List<Pair<ImageVector, String>>,
    @StringRes title: Int,
    @StringRes hint: Int,
    initialNote: String,
    maxChar: Int,
): Pair<Int, String> = showNoButton(false) {
    Column {
        Text(text = stringResource(id = title), modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
        CircularLayout(
            modifier = Modifier.fillMaxWidth().aspectRatio(1F),
            placeFirstItemInCenter = true,
        ) {
            val note = rememberTextFieldState(initialNote)
            TextField(
                state = note,
                modifier = Modifier.fillMaxWidth(0.45F).aspectRatio(1F),
                label = { Text(text = stringResource(id = hint)) },
                trailingIcon = {
                    if (note.text.isNotEmpty()) {
                        IconButton(onClick = { note.clearText() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                supportingText = {
                    Text(
                        text = "${note.text.toString().toByteArray().size} / $maxChar",
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
                    modifier = Modifier.clip(IconWithTextCorner).clickable { resume(index to note.text.toString()) }.fillMaxWidth(0.2F),
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

@Composable
private fun CheckableItem(text: String, checked: Boolean, modifier: Modifier = Modifier) {
    val textStyle = MaterialTheme.typography.titleMedium
    val checkedColor = MaterialTheme.colorScheme.primary
    ListItem(
        headlineContent = {
            Text(
                text = text,
                style = if (checked) textStyle.copy(color = checkedColor) else textStyle,
            )
        },
        modifier = modifier,
        trailingContent = checked.ifTrueThen {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = checkedColor)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

private val IconWithTextCorner = RoundedCornerShape(8.dp)
private val DatePickerTitlePadding = PaddingValues(start = 24.dp, end = 12.dp, top = 16.dp)

val LocalGlobalDialogState = compositionLocalOf<DialogState> { error("CompositionLocal LocalDialogState not present!") }
