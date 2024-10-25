package androidx.compose.material3

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.SearchBarDefaults.InputFieldHeight
import androidx.compose.material3.SearchBarDefaults.inputFieldColors
import androidx.compose.material3.SearchBarDefaults.inputFieldShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A text field to input a query in a search bar.
 *
 * @param state [TextFieldState] that holds the internal editing state of the input field.
 * @param onSearch the callback to be invoked when the input service triggers the
 *   [ImeAction.Search] action. The current query in the [state] comes as a parameter of the
 *   callback.
 * @param expanded whether the search bar is expanded and showing search results.
 * @param onExpandedChange the callback to be invoked when the search bar's expanded state is
 *   changed.
 * @param modifier the [Modifier] to be applied to this input field.
 * @param enabled the enabled state of this input field. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param readOnly controls the editable state of the input field. When `true`, the field cannot
 *   be modified. However, a user can focus it and copy text from it.
 * @param textStyle the style to be applied to the input text. Defaults to [LocalTextStyle].
 * @param placeholder the placeholder to be displayed when the input text is empty.
 * @param leadingIcon the leading icon to be displayed at the start of the input field.
 * @param trailingIcon the trailing icon to be displayed at the end of the input field.
 * @param prefix the optional prefix to be displayed before the input text.
 * @param suffix the optional suffix to be displayed after the input text.
 * @param inputTransformation optional [InputTransformation] that will be used to transform
 *   changes to the [TextFieldState] made by the user. The transformation will be applied to
 *   changes made by hardware and software keyboard events, pasting or dropping text,
 *   accessibility services, and tests. The transformation will _not_ be applied when changing
 *   the [state] programmatically, or when the transformation is changed. If the transformation
 *   is changed on an existing text field, it will be applied to the next user edit. The
 *   transformation will not immediately affect the current [state].
 * @param outputTransformation optional [OutputTransformation] that transforms how the contents
 *   of the text field are presented.
 * @param scrollState scroll state that manages the horizontal scroll of the input field.
 * @param shape the shape of the input field.
 * @param colors [TextFieldColors] that will be used to resolve the colors used for this input
 *   field in different states. See [SearchBarDefaults.inputFieldColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this input field. You can use this to change the search bar's
 *   appearance or preview the search bar in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 */
@ExperimentalMaterial3Api
@Composable
fun SearchBarInputField(
    state: TextFieldState,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    scrollState: ScrollState = rememberScrollState(),
    shape: Shape = inputFieldShape,
    colors: TextFieldColors = inputFieldColors(),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    val focused = interactionSource.collectIsFocusedAsState().value
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val textColor = textStyle.color.takeOrElse {
        if (focused) colors.focusedTextColor else colors.unfocusedTextColor
    }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    BasicTextField(
        state = state,
        modifier = modifier
            .sizeIn(
                minWidth = SearchBarMinWidth,
                maxWidth = SearchBarMaxWidth,
                minHeight = InputFieldHeight,
            )
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onExpandedChange(true) }
            .fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = mergedTextStyle,
        cursorBrush = SolidColor(colors.cursorColor),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { onSearch(state.text.toString()) },
        interactionSource = interactionSource,
        inputTransformation = inputTransformation,
        outputTransformation = outputTransformation,
        scrollState = scrollState,
        decorator = TextFieldDefaults.decorator(
            state = state,
            enabled = enabled,
            lineLimits = TextFieldLineLimits.SingleLine,
            outputTransformation = outputTransformation,
            interactionSource = interactionSource,
            placeholder = placeholder,
            leadingIcon = leadingIcon?.let { leading ->
                { Box(Modifier.offset(x = SearchBarIconOffsetX)) { leading() } }
            },
            trailingIcon = trailingIcon?.let { trailing ->
                { Box(Modifier.offset(x = -SearchBarIconOffsetX)) { trailing() } }
            },
            prefix = prefix,
            suffix = suffix,
            colors = colors,
            contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(),
            container = {
                val containerColor =
                    animateColorAsState(
                        targetValue = if (focused) colors.focusedContainerColor else colors.unfocusedContainerColor,
                        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                        label = "ContainerColor",
                    )
                Box(Modifier.textFieldBackground(containerColor::value, shape))
            },
        ),
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

private fun Modifier.textFieldBackground(
    color: ColorProducer,
    shape: Shape,
) = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    onDrawBehind { drawOutline(outline, color = color()) }
}

private val SearchBarMinWidth = 360.dp
private val SearchBarMaxWidth = 720.dp
private val SearchBarIconOffsetX = 4.dp

@Suppress("ktlint:standard:property-naming")
private const val AnimationDelayMillis = 100
