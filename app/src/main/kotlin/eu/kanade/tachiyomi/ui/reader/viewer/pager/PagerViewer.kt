package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.PointF
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.core.view.get
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.hippo.ehviewer.R
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.BaseViewer
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageAdapter
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageHolder
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NavigationRegion
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * Implementation of a [BaseViewer] to display pages with a [ViewPager2].
 */
class PagerViewer(
    override val activity: ReaderActivity,
    override val isRtl: Boolean = false,
    val isVertical: Boolean = false,
) : BaseViewer {

    private val scope = MainScope()

    /**
     * View pager used by this viewer.
     */
    private val pager = ViewPager2(activity).apply {
        if (isRtl) {
            layoutDirection = ViewPager2.LAYOUT_DIRECTION_RTL
        } else if (isVertical) {
            orientation = ViewPager2.ORIENTATION_VERTICAL
        }
    }

    private val frame = PagerFrame(activity)

    /**
     * Configuration used by the pager, like allow taps, scale mode on images, page transitions...
     */
    private val config = PagerConfig(this, scope)

    /**
     * Adapter of the pager.
     */
    private val adapter = ReaderPageAdapter(this)

    /**
     * Currently active item. It can be a chapter page or a chapter transition.
     */
    private var currentPage: ReaderPage? = null

    /**
     * Viewer chapters to set when the pager enters idle mode. Otherwise, if the view was settling
     * or dragging, there'd be a noticeable and annoying jump.
     */
    private var awaitingIdleViewerChapters: PageLoader? = null

    /**
     * Whether the view pager is currently in idle mode. It sets the awaiting chapters if setting
     * this field to true.
     */
    private var isIdle = true
        set(value) {
            field = value
            if (value) {
                awaitingIdleViewerChapters?.let { viewerChapters ->
                    setChaptersInternal(viewerChapters)
                    awaitingIdleViewerChapters = null
                }
            }
        }

    private var longPressed = false

    init {
        pager.isVisible = false // Don't layout the pager yet
        pager.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        pager.clipToPadding = false
        pager.isFocusable = false
        pager.offscreenPageLimit = 1
        pager.id = R.id.reader_pager
        pager.adapter = adapter
        pager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    if (activity.isScrollingThroughPages.not()) {
                        activity.hideMenu()
                    }

                    onPageChange(position)
                }

                override fun onPageScrollStateChanged(state: Int) {
                    isIdle = state == ViewPager2.SCROLL_STATE_IDLE
                }
            },
        )
        frame.tapListener = { event ->
            val windowOffset = IntArray(2)
            activity.window.decorView.getLocationOnScreen(windowOffset)
            val pos = PointF(
                (event.rawX - windowOffset[0]) / pager.width,
                (event.rawY - windowOffset[1]) / pager.height,
            )
            val navigator = config.navigator

            when (navigator.getAction(pos)) {
                NavigationRegion.MENU -> activity.toggleMenu()
                NavigationRegion.NEXT -> moveToNext()
                NavigationRegion.PREV -> moveToPrevious()
                NavigationRegion.RIGHT -> moveRight()
                NavigationRegion.LEFT -> moveLeft()
            }
        }
        frame.longTapListener = f@{
            if (activity.menuVisible || config.longTapEnabled) {
                val item = adapter.items.getOrNull(pager.currentItem)
                if (item is ReaderPage) {
                    activity.onPageLongTap(item)
                    return@f true
                }
            }
            false
        }

        config.imagePropertyChangedListener = {
            refreshAdapter()
        }

        config.navigationModeChangedListener = {
            val showOnStart = config.navigationOverlayOnStart || config.forceNavigationOverlay
            activity.binding.navigationOverlay.setNavigation(config.navigator, showOnStart)
        }

        frame.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        frame.addView(pager)
    }

    override val readerConfig
        get() = ReaderPageImageView.Config(
            zoomDuration = config.doubleTapAnimDuration,
            minimumScaleType = config.imageScaleType,
            cropBorders = config.imageCropBorders,
            zoomStartPosition = config.imageZoomType,
            landscapeZoom = config.landscapeZoom,
        )

    override fun destroy() {
        scope.cancel()
    }

    /**
     * Returns the view this viewer uses.
     */
    override fun getView(): View = frame

    /**
     * Returns the [ReaderPageHolder] for the provided page
     */
    private fun getPageHolder(position: Int): ReaderPageHolder? =
        (pager[0] as RecyclerView).findViewHolderForAdapterPosition(position) as ReaderPageHolder?

    /**
     * Called when a new [ReaderPage] is marked as active
     */
    private fun onPageChange(position: Int) {
        val page = adapter.items.getOrNull(position)
        if (page != null && currentPage != page) {
            val forward = page.number > (currentPage?.number ?: 0)
            currentPage = page
            onReaderPageSelected(page, forward)
        }
    }

    /**
     * Called when a [ReaderPage] is marked as active. It notifies the
     * activity of the change and requests the preload of the next chapter if this is the last page.
     */
    private fun onReaderPageSelected(page: ReaderPage, forward: Boolean) {
        activity.onPageSelected(page)

        // Notify holder of page change
        getPageHolder(page.index)?.onPageSelected(forward)
    }

    /**
     * Tells this viewer to set the given [provider] as active. If the pager is currently idle,
     * it sets the chapters immediately, otherwise they are saved and set when it becomes idle.
     */
    override fun setGalleryProvider(provider: PageLoader) {
        if (isIdle) {
            setChaptersInternal(provider)
        } else {
            awaitingIdleViewerChapters = provider
        }
    }

    /**
     * Sets the active [chapters] on this pager.
     */
    private fun setChaptersInternal(chapters: PageLoader) {
        logcat { "setChaptersInternal" }
        adapter.setChapters(chapters)
        refreshAdapter(0)

        // Layout the pager once a chapter is being set
        if (pager.isGone) {
            logcat { "Pager first layout" }
            pager.isVisible = true
        }
    }

    /**
     * Tells this viewer to move to the given [page].
     */
    override fun moveToPage(page: ReaderPage) {
        logcat { "moveToPage ${page.number}" }
        val position = adapter.items.indexOf(page)
        if (position != -1) {
            val currentPosition = pager.currentItem
            pager.setCurrentItem(position, true)
            // manually call onPageChange since ViewPager listener is not triggered in this case
            if (currentPosition == position) {
                onPageChange(position)
            }
        } else {
            logcat { "Page $page not found in adapter" }
        }
    }

    /**
     * Moves to the next page.
     */
    private fun moveToNext() {
        val currentItem = pager.currentItem
        if (currentItem != adapter.itemCount - 1) {
            val holder = getPageHolder(currentItem)
            if (!config.navigateToPan || holder?.panEnd(isRtl) != true) {
                pager.setCurrentItem(currentItem + 1, config.usePageTransitions)
            }
        }
    }

    /**
     * Moves to the previous page.
     */
    private fun moveToPrevious() {
        val currentItem = pager.currentItem
        if (currentItem != 0) {
            val holder = getPageHolder(currentItem)
            if (!config.navigateToPan || holder?.panEnd(!isRtl) != true) {
                pager.setCurrentItem(currentItem - 1, config.usePageTransitions)
            }
        }
    }

    /**
     * Moves to the page at the right.
     */
    private fun moveRight() {
        if (isRtl) {
            moveToPrevious()
        } else {
            moveToNext()
        }
    }

    /**
     * Moves to the page at the left.
     */
    private fun moveLeft() {
        if (isRtl) {
            moveToNext()
        } else {
            moveToPrevious()
        }
    }

    /**
     * Moves to the page at the top (or previous).
     */
    private fun moveUp() {
        moveToPrevious()
    }

    /**
     * Moves to the page at the bottom (or next).
     */
    private fun moveDown() {
        moveToNext()
    }

    override fun refreshAdapter() {
        refreshAdapter(pager.currentItem)
    }

    /**
     * Resets the adapter in order to recreate all the views. Used when a image configuration is
     * changed.
     */
    private fun refreshAdapter(currentItem: Int) {
        adapter.refresh()
        pager.adapter = adapter
        pager.setCurrentItem(currentItem, false)
    }

    /**
     * Called from the containing activity when a key [event] is received. It should return true
     * if the event was handled, false otherwise.
     */
    override fun handleKeyEvent(event: KeyEvent): Boolean {
        val isUp = event.action == KeyEvent.ACTION_UP
        val ctrlPressed = event.metaState.and(KeyEvent.META_CTRL_ON) > 0
        val interval = config.volumeKeysInterval
        val movePage = longPressed && (interval == 0 || event.repeatCount % (interval + 1) == 0)
        longPressed = event.isLongPress || !isUp && longPressed

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (!config.volumeKeysEnabled || activity.menuVisible) {
                    return false
                } else if (isUp != movePage) {
                    if (!config.volumeKeysInverted) moveDown() else moveUp()
                }
            }

            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (!config.volumeKeysEnabled || activity.menuVisible) {
                    return false
                } else if (isUp != movePage) {
                    if (!config.volumeKeysInverted) moveUp() else moveDown()
                }
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isUp) {
                    if (ctrlPressed) moveToNext() else moveRight()
                }
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (isUp) {
                    if (ctrlPressed) moveToPrevious() else moveLeft()
                }
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> if (isUp) moveDown()
            KeyEvent.KEYCODE_DPAD_UP -> if (isUp) moveUp()
            KeyEvent.KEYCODE_PAGE_DOWN -> if (isUp) moveDown()
            KeyEvent.KEYCODE_PAGE_UP -> if (isUp) moveUp()
            KeyEvent.KEYCODE_MENU -> if (isUp) activity.toggleMenu()
            else -> return false
        }
        return true
    }

    /**
     * Called from the containing activity when a generic motion [event] is received. It should
     * return true if the event was handled, false otherwise.
     */
    override fun handleGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_CLASS_POINTER != 0) {
            when (event.action) {
                MotionEvent.ACTION_SCROLL -> {
                    if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) {
                        moveDown()
                    } else {
                        moveUp()
                    }
                    return true
                }
            }
        }
        return false
    }
}
