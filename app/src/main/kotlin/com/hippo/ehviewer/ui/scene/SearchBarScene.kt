package com.hippo.ehviewer.ui.scene

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.CallSuper
import androidx.annotation.Keep
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.search.SearchView
import com.google.android.material.shape.MaterialShapeDrawable
import com.hippo.ehviewer.EhApplication.Companion.searchDatabase
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.dao.Search
import com.hippo.ehviewer.dao.SearchDao
import com.hippo.ehviewer.databinding.SceneSearchbarBinding
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.legacy.FabLayout
import com.hippo.ehviewer.ui.setMD3Content
import com.hippo.ehviewer.util.applyNavigationBarsPadding
import com.hippo.ehviewer.util.isAtLeastT
import com.jamal.composeprefs3.ui.ifNotNullThen
import com.jamal.composeprefs3.ui.ifTrueThen
import dev.chrisbanes.insetter.applyInsetter
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList

abstract class SearchBarScene : BaseScene(), ToolBarScene {
    private var _binding: SceneSearchbarBinding? = null

    @get:Keep
    private val binding get() = _binding!!
    private var mSuggestionList by mutableStateOf(emptyList<Suggestion>())
    private var mSuggestionProvider: SuggestionProvider? = null
    private var allowEmptySearch = true
    private val mSearchDatabase = searchDatabase.searchDao()
    private var onApplySearch: (String) -> Unit = {}

    protected abstract val fabLayout: FabLayout
    protected abstract val fastScroller: View
    protected abstract val recyclerView: View
    protected abstract val contentView: View

    override val enableDrawerGestures = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SceneSearchbarBinding.inflate(inflater, container, false)
        binding.appbar.statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(context)
        binding.searchview.editText.addTextChangedListener { updateSuggestions() }
        binding.searchview.editText.setOnEditorActionListener { _, _, _ ->
            onApplySearch()
            true
        }
        binding.searchBarList.consumeWindowInsets = false
        binding.searchBarList.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
        binding.searchBarList.setMD3Content {
            LazyColumn(modifier = Modifier.navigationBarsPadding().imePadding()) {
                items(mSuggestionList) {
                    ListItem(
                        headlineContent = { Text(text = it.keyword) },
                        supportingContent = it.hint.ifNotNullThen { Text(text = it.hint!!) },
                        leadingContent = it.canOpenDirectly.ifTrueThen {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.MenuBook,
                                contentDescription = null,
                            )
                        },
                        trailingContent = it.canDelete.ifTrueThen {
                            IconButton(onClick = { deleteKeyword(it.keyword) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { it.onClick() },
                    )
                }
            }
        }
        binding.searchview.addTransitionListener { _, _, newState ->
            if (newState == SearchView.TransitionState.SHOWING) {
                onSearchViewExpanded()
            } else if (newState == SearchView.TransitionState.HIDING) {
                onSearchViewHidden()
            }
        }
        onCreateViewWithToolbar(inflater, binding.root, savedInstanceState)
        if (!isAtLeastT) {
            // This has to be placed after onCreateViewWithToolbar() since
            // callbacks are invoked in the reverse order in which they are added
            binding.searchview.addTransitionListener(mSearchViewOnBackPressedCallback)
            requireActivity().onBackPressedDispatcher.addCallback(mSearchViewOnBackPressedCallback)
        }
        binding.appbar.bringToFront()
        fabLayout.applyNavigationBarsPadding()
        fastScroller.applyNavigationBarsPadding()
        recyclerView.applyNavigationBarsPadding()
        contentView.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.apply {
            val menuResId = getMenuResId()
            if (menuResId != 0) {
                inflateMenu(menuResId)
                setOnMenuItemClickListener { item: MenuItem -> onMenuItemClick(item) }
            }
            setNavigationOnClickListener { onNavigationClick() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!isAtLeastT) mSearchViewOnBackPressedCallback.remove()
        binding.root.removeAllViews()
        _binding = null
    }

    private var drawerLocked = false

    @CallSuper
    open fun onSearchViewExpanded() {
        if (!isDrawerLocked) {
            drawerLocked = true
            lockDrawer()
        }
        updateSuggestions()
    }

    @CallSuper
    open fun onSearchViewHidden() {
        binding.toolbar.setText(binding.searchview.text)
        if (drawerLocked) {
            unlockDrawer()
            drawerLocked = false
        }
    }

    fun setSearchBarHint(hint: String?) {
        binding.toolbar.hint = hint
    }

    fun setSearchBarText(text: String?) {
        binding.toolbar.setText(text)
        binding.searchview.setText(text)
    }

    fun setEditTextHint(hint: String?) {
        binding.searchview.editText.hint = hint
    }

    override fun onNavigationClick() {
        openDrawer()
    }

    override fun getMenuResId(): Int {
        return 0
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_open_side_sheet) {
            openSideSheet()
            true
        } else {
            false
        }
    }

    override fun setLiftOnScrollTargetView(view: View?) {
        binding.appbar.setLiftOnScrollTargetView(view)
    }

    fun setOnApplySearch(lambda: (String) -> Unit) {
        onApplySearch = lambda
    }

    private suspend fun addQuery(query: String) {
        mSearchDatabase.deleteQuery(query)
        if (query.isBlank()) return
        val search = Search(System.currentTimeMillis(), query)
        mSearchDatabase.insert(search)
    }

    fun onApplySearch() {
        binding.toolbar.setText(binding.searchview.text)
        binding.searchview.hide()
        val query = binding.toolbar.text.toString().trim()
        if (!allowEmptySearch && query.isEmpty()) return
        lifecycleScope.launchIO { addQuery(query) }
        onApplySearch(query)
    }

    fun interface SuggestionProvider {
        fun providerSuggestions(text: String): Suggestion?
    }

    private fun deleteKeyword(keyword: String) {
        BaseDialogBuilder(requireContext())
            .setMessage(requireContext().getString(R.string.delete_search_history, keyword))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launchIO {
                    mSearchDatabase.deleteQuery(keyword)
                    updateSuggestions()
                }
            }
            .show()
    }

    abstract class Suggestion {
        abstract val keyword: String
        open val hint: String? = null
        abstract fun onClick()
        open val canDelete: Boolean = false
        open val canOpenDirectly: Boolean = false
    }

    inner class TagSuggestion(
        override val hint: String?,
        override val keyword: String,
    ) : Suggestion() {
        override fun onClick() {
            binding.searchview.editText.let {
                var keywords = it.text.toString().substringBeforeLast(' ', "")
                if (keywords.isNotEmpty()) keywords += ' '
                keywords += wrapTagKeyword(keyword)
                if (!keywords.endsWith(':')) keywords += ' '
                it.setText(keywords)
                it.setSelection(keywords.length)
            }
        }
    }

    inner class KeywordSuggestion(
        override val keyword: String,
    ) : Suggestion() {
        override val canDelete = true
        override fun onClick() {
            binding.searchview.editText.run {
                setText(keyword)
                setSelection(length())
            }
        }
    }

    private fun updateSuggestions() {
        _binding ?: return
        viewLifecycleOwner.lifecycleScope.launchIO {
            mSuggestionList = mergedSuggestionFlow().toList()
        }
    }

    private suspend fun SearchDao.suggestions(prefix: String, limit: Int) = (if (prefix.isBlank()) list(limit) else rawSuggestions(prefix, limit)).map { it.query }

    private fun mergedSuggestionFlow(): Flow<Suggestion> = flow {
        binding.searchview.editText.text?.toString()?.let { text ->
            mSuggestionProvider?.run { providerSuggestions(text)?.let { emit(it) } }
            mSearchDatabase.suggestions(text, 128).forEach { emit(KeywordSuggestion(it)) }
            EhTagDatabase.takeIf { it.initialized }?.run {
                if (text.isNotEmpty() && !text.endsWith(' ')) {
                    val keyword = text.substringAfterLast(' ')
                    val translate = Settings.showTagTranslations && isTranslatable(requireContext())
                    suggestFlow(keyword, translate, true).collect {
                        emit(TagSuggestion(it.first, it.second))
                    }
                    suggestFlow(keyword, translate).collect {
                        emit(TagSuggestion(it.first, it.second))
                    }
                }
            }
        }
    }

    fun setSuggestionProvider(suggestionProvider: SuggestionProvider) {
        mSuggestionProvider = suggestionProvider
    }

    fun showSearchBar() {
        _binding ?: return
        binding.appbar.setExpanded(true)
    }

    private val mSearchViewOnBackPressedCallback =
        object : OnBackPressedCallback(false), SearchView.TransitionListener {
            override fun handleOnBackPressed() {
                binding.searchview.hide()
            }

            override fun onStateChanged(
                searchView: SearchView,
                previousState: SearchView.TransitionState,
                newState: SearchView.TransitionState,
            ) {
                if (newState == SearchView.TransitionState.SHOWING) {
                    isEnabled = true
                } else if (newState == SearchView.TransitionState.HIDING) {
                    isEnabled = false
                }
            }
        }
}

fun wrapTagKeyword(keyword: String, translate: Boolean = false): String {
    return if (keyword.endsWith(':')) {
        keyword
    } else {
        val tag = keyword.substringAfter(':')
        val prefix = keyword.dropLast(tag.length + 1)
        if (translate) {
            val namespacePrefix = EhTagDatabase.namespaceToPrefix(prefix)
            val newPrefix = EhTagDatabase.getTranslation(tag = prefix) ?: prefix
            val newTag = EhTagDatabase.getTranslation(namespacePrefix, tag) ?: tag
            "$newPrefix：$newTag"
        } else if (keyword.contains(' ')) {
            "$prefix:\"$tag$\""
        } else {
            "$keyword$"
        }
    }
}

const val SEARCH_VIEW_ANIMATE_TIME = 300L
