package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.annotation.SuppressLint
import android.content.Context
import coil3.BitmapImage
import com.hippo.ehviewer.image.Image
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderErrorLayout
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressIndicator
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * View of the ViewPager that contains a page of a chapter.
 */
@SuppressLint("ViewConstructor")
class PagerPageHolder(
    readerThemedContext: Context,
    val viewer: PagerViewer,
    val page: ReaderPage,
) : ReaderPageImageView(readerThemedContext),
    ViewPagerAdapter.PositionableView {

    /**
     * Item that identifies this view. Needed by the adapter to not recreate views.
     */
    override val item
        get() = page

    /**
     * Loading progress bar to indicate the current progress.
     */
    private val progressIndicator = ReaderProgressIndicator(readerThemedContext)

    /**
     * Error layout to show when the image fails to load.
     */
    private var errorLayout: ReaderErrorLayout? = null

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Subscription for status changes of the page.
     */
    private var statusJob: Job? = null

    init {
        addView(progressIndicator)
        statusJob = scope.launch(Dispatchers.Main) {
            page.status.collectLatest {
                processStatus(it)
            }
        }
    }

    /**
     * Called when this view is detached from the window. Unsubscribes any active subscription.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unsubscribeStatus()
        progressIndicator.hide()
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
     * Unsubscribes from the status subscription.
     */
    private fun unsubscribeStatus() {
        statusJob?.cancel()
        statusJob = null
    }

    /**
     * Called when the page is queued.
     */
    private fun setQueued() {
        progressIndicator.setProgress(0)
        progressIndicator.show()
        removeErrorLayout()
        recycle()
    }

    /**
     * Called when the page is loading.
     */
    private fun setLoading() {
        progressIndicator.show()
        removeErrorLayout()
    }

    /**
     * Called when the page is downloading.
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
        setImage(
            image,
            Config(
                zoomDuration = viewer.config.doubleTapAnimDuration,
                minimumScaleType = viewer.config.imageScaleType,
                cropBorders = viewer.config.imageCropBorders,
                zoomStartPosition = viewer.config.imageZoomType,
                landscapeZoom = viewer.config.landscapeZoom,
            ),
        )
        if (image.innerImage is BitmapImage) {
            pageBackground = background
        }
    }

    /**
     * Called when the page has an error.
     */
    private fun setError() {
        progressIndicator.hide()
        showErrorLayout()
    }

    override fun onImageLoaded() {
        super.onImageLoaded()
        progressIndicator.hide()
        removeErrorLayout()
    }

    /**
     * Called when an image is zoomed in/out.
     */
    override fun onScaleChanged(newScale: Float) {
        super.onScaleChanged(newScale)
        viewer.activity.hideMenu()
    }

    private fun showErrorLayout() {
        if (errorLayout == null) {
            errorLayout = ReaderErrorLayout(context, page.errorMsg) {
                viewer.activity.retryPage(page.index)
            }.also {
                addView(it)
            }
        }
    }

    private fun removeErrorLayout() {
        errorLayout?.let {
            removeView(it)
            errorLayout = null
        }
    }
}
