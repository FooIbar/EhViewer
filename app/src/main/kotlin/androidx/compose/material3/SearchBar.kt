@file:Suppress("ktlint:standard:property-naming")

package androidx.compose.material3

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.SearchBarDefaults.InputFieldHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SearchBarInputField(
    state: TextFieldState,
    onSearch: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: TextFieldColors = SearchBarDefaults.inputFieldColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val focused by interactionSource.collectIsFocusedAsState()
    val textColor = LocalTextStyle.current.color.takeOrElse {
        if (focused) colors.focusedTextColor else colors.unfocusedTextColor
    }

    BasicTextField(
        state = state,
        modifier = modifier.sizeIn(
            minWidth = SearchBarMinWidth,
            maxWidth = SearchBarMaxWidth,
            minHeight = InputFieldHeight,
        ).focusRequester(focusRequester).onFocusChanged {
            if (it.isFocused) onExpandedChange(true)
        }.fillMaxWidth(),
        enabled = enabled,
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = LocalTextStyle.current.merge(TextStyle(color = textColor)),
        cursorBrush = SolidColor(colors.cursorColor),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { onSearch() },
        interactionSource = interactionSource,
        decorator = @Composable { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = state.text.toString(),
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = placeholder,
                leadingIcon = leadingIcon?.let { leading -> { Box(Modifier.offset(x = SearchBarIconOffsetX)) { leading() } } },
                trailingIcon = trailingIcon?.let { trailing -> { Box(Modifier.offset(x = -SearchBarIconOffsetX)) { trailing() } } },
                shape = SearchBarDefaults.inputFieldShape,
                colors = colors,
                container = {},
            )
        },
    )

    val shouldClearFocus = !expanded && focused
    LaunchedEffect(expanded) {
        if (shouldClearFocus) {
            // Not strictly needed according to the motion spec, but since the animation
            // already has a delay, this works around b/261632544.
            delay(AnimationDelayMillis.toLong())
            focusManager.clearFocus()
        }
    }
}

val SearchBarIconOffsetX = 4.dp
val SearchBarMinWidth = 360.dp
val SearchBarMaxWidth = 720.dp
const val AnimationDelayMillis = 100
