package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.content.res.Resources
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.hippo.ehviewer.image.Image
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import eu.kanade.tachiyomi.util.system.dpToPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Holder of the reader for a single page of a chapter.
 *
 * @param frame the root view for this holder.
 * @param viewer the page viewer.
 * @constructor creates a new page holder.
 */
class ReaderPageHolder(
    private val frame: ReaderPageImageView,
    private val viewer: BaseViewer,
) : RecyclerView.ViewHolder(frame) {

    /**
     * Context getter because it's used often.
     */
    val context: Context get() = frame.context

    /**
     * Loading progress bar to indicate the current progress.
     * Needed to keep a minimum height size of the holder, otherwise the adapter would create more
     * views to fill the screen, which is not wanted.
     */
    private val progressIndicator = ReaderProgressIndicator(context)

    /**
     * Error layout to show when the image fails to load.
     */
    private var errorLayout: ReaderErrorLayout? = null

    /**
     * Page of a chapter.
     */
    private lateinit var page: ReaderPage

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Subscription for status changes of the page.
     */
    private var statusJob: Job? = null

    init {
        if (viewer is WebtoonViewer) {
            val defaultHeight = (viewer.recycler.width * 1.4125).toInt()
            frame.addView(progressIndicator, MATCH_PARENT, defaultHeight)
            refreshLayoutParams()
        } else {
            frame.addView(progressIndicator)
            frame.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }

        frame.onImageLoaded = ::onImageLoaded
        frame.onScaleChanged = { viewer.activity.hideMenu() }
    }

    /**
     * Binds the given [page] with this view holder, subscribing to its state.
     */
    fun bind(page: ReaderPage) {
        this.page = page
        statusJob?.cancel()
        statusJob = scope.launch(Dispatchers.Main) {
            page.status.collectLatest {
                processStatus(it)
            }
        }
        refreshLayoutParams()
    }

    private fun refreshLayoutParams() {
        if (viewer is WebtoonViewer) {
            frame.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                if (!viewer.isContinuous) {
                    bottomMargin = 15.dpToPx
                }

                val margin = Resources.getSystem().displayMetrics.widthPixels * (viewer.config.sidePadding / 100f)
                marginEnd = margin.toInt()
                marginStart = margin.toInt()
            }
        }
    }

    fun onPageSelected(forward: Boolean) = frame.onPageSelected(forward)
    fun panEnd(reverse: Boolean) = if (reverse) frame.panLeft() else frame.panRight()

    /**
     * Called when the view is recycled and added to the view pool.
     */
    fun recycle() {
        statusJob?.cancel()
        statusJob = null

        removeErrorLayout()
        frame.recycle()
        progressIndicator.reset()
    }

    /**
     * Called when the status of the page changes.
     *
     * @param status the new status of the page.
     */
    private suspend fun processStatus(status: Page.State) {
        when (status) {
            Page.State.QUEUE -> setQueued()
            Page.State.LOAD_PAGE -> setLoading()
            Page.State.DOWNLOAD_IMAGE -> {
                setDownloading()
                page.progressFlow.collectLatest { value -> progressIndicator.setProgress(value) }
            }
            Page.State.READY -> page.image?.let { setImage(it) } ?: setError()
            Page.State.ERROR -> setError()
        }
    }

    /**
     * Called when the page is queued.
     */
    private fun setQueued() {
        progressIndicator.setProgress(0)
        progressIndicator.show()
        removeErrorLayout()
        frame.recycle()
    }

    /**
     * Called when the page is loading.
     */
    private fun setLoading() {
        progressIndicator.show()
        removeErrorLayout()
    }

    /**
     * Called when the page is downloading
     */
    private fun setDownloading() {
        progressIndicator.show()
        removeErrorLayout()
    }

    /**
     * Called when the page is ready.
     */
    private fun setImage(image: Image) {
        progressIndicator.setProgress(0)
        frame.setImage(image, viewer.readerConfig)
    }

    /**
     * Called when the page has an error.
     */
    private fun setError() {
        progressIndicator.hide()
        initErrorLayout()
    }

    /**
     * Called when the image is displayed.
     */
    private fun onImageLoaded() {
        progressIndicator.hide()
        removeErrorLayout()
    }

    /**
     * Initializes a button to retry pages.
     */
    private fun initErrorLayout() {
        if (errorLayout == null) {
            errorLayout = ReaderErrorLayout(context, page.errorMsg) {
                viewer.activity.retryPage(page.index)
            }.also {
                frame.addView(it)
            }
        }
    }

    /**
     * Removes the decode error layout from the holder, if found.
     */
    private fun removeErrorLayout() {
        errorLayout?.let {
            frame.removeView(it)
            errorLayout = null
        }
    }
}
