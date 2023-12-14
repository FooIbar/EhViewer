package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.annotation.IntRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import com.hippo.ehviewer.ui.theme.EhTheme

class ReaderProgressIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    init {
        layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
    }

    private var progress by mutableFloatStateOf(0f)
    private var visible by mutableStateOf(false)

    @Composable
    override fun Content() {
        EhTheme {
            if (visible) CombinedCircularProgressIndicator(progress = progress)
        }
    }

    fun show() {
        visible = true
        isVisible = true
    }

    fun hide() {
        visible = false
        isVisible = false
    }

    fun setProgress(@IntRange(from = 0, to = 100) progress: Int) {
        this.progress = progress / 100f
    }
}
