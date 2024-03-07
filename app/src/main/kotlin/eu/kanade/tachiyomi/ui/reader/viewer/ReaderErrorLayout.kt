package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.ui.theme.EhTheme

class ReaderErrorLayout @JvmOverloads constructor(
    context: Context,
    private val errorMsg: String? = null,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val onRetry: () -> Unit = {},
) : AbstractComposeView(context, attrs, defStyleAttr) {
    init {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
    }

    @Composable
    override fun Content() = EhTheme {
        val defaultError = stringResource(id = R.string.decode_image_error)
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(DEFAULT_ASPECT)) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = errorMsg ?: defaultError,
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onRetry, modifier = Modifier.padding(8.dp)) {
                    Text(text = stringResource(id = R.string.action_retry))
                }
            }
        }
    }
}

private const val DEFAULT_ASPECT = 1 / 1.4125f
