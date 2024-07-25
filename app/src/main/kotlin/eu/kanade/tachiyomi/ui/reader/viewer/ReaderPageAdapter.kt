package eu.kanade.tachiyomi.ui.reader.viewer

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.util.system.createReaderThemeContext

/**
 * RecyclerView Adapter used by this [viewer] to where [PageLoader] updates are posted.
 */
class ReaderPageAdapter(
    val viewer: BaseViewer,
    private val isWebtoon: Boolean = false,
) : RecyclerView.Adapter<ReaderPageHolder>() {

    /**
     * List of currently set items.
     */
    var items: List<ReaderPage> = emptyList()
        private set

    private var currentChapter: PageLoader? = null

    /**
     * Context that has been wrapped to use the correct theme values based on the
     * current app theme and reader background color
     */
    private var readerThemedContext = viewer.activity.createReaderThemeContext()

    /**
     * Updates this adapter with the given [chapter]. It handles setting a few pages of the
     * next/previous chapter to allow seamless transitions.
     */
    fun setChapters(chapter: PageLoader) {
        currentChapter = chapter
        items = chapter.pages
    }

    fun refresh() {
        readerThemedContext = viewer.activity.createReaderThemeContext()
    }

    /**
     * Returns the amount of items of the adapter.
     */
    override fun getItemCount(): Int = items.size

    /**
     * Creates a new view holder for an item with the given [viewType].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReaderPageHolder {
        val view = ReaderPageImageView(readerThemedContext, isWebtoon = isWebtoon)
        return ReaderPageHolder(view, viewer)
    }

    /**
     * Binds an existing view [holder] with the item at the given [position].
     */
    override fun onBindViewHolder(holder: ReaderPageHolder, position: Int) {
        val item = items[position]
        currentChapter!!.request(position)
        holder.bind(item)
    }

    /**
     * Recycles an existing view [holder] before adding it to the view pool.
     */
    override fun onViewRecycled(holder: ReaderPageHolder) {
        currentChapter?.cancelRequest(holder.bindingAdapterPosition)
        holder.recycle()
    }
}
