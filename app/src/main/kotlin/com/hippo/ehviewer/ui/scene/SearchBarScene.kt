package com.hippo.ehviewer.ui.scene

import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.clearText
import androidx.compose.foundation.text2.input.forEachTextValue
import androidx.compose.foundation.text2.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.viewbinding.ViewBinding
import com.google.android.material.sidesheet.SideSheetDialog
import com.hippo.ehviewer.EhApplication.Companion.searchDatabase
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.dao.Search
import com.hippo.ehviewer.dao.SearchDao
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.util.findActivity
import com.hippo.ehviewer.util.getSparseParcelableArrayCompat
import com.jamal.composeprefs3.ui.ifNotNullThen
import com.jamal.composeprefs3.ui.ifTrueThen
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch

fun interface SuggestionProvider {
    fun providerSuggestions(text: String): Suggestion?
}

abstract class Suggestion {
    abstract val keyword: String
    open val hint: String? = null
    abstract fun onClick()
    open val canDelete: Boolean = false
    open val canOpenDirectly: Boolean = false
}

suspend fun SearchDao.suggestions(prefix: String, limit: Int) =
    (if (prefix.isBlank()) list(limit) else rawSuggestions(prefix, limit)).map { it.query }

@Composable
fun SearchBarScreen(
    searchFieldState: TextFieldState,
    searchFieldHint: String = "",
    showSearchFab: Boolean = false,
    onApplySearch: (String) -> Unit,
    onSearchExpanded: () -> Unit,
    onSearchHidden: () -> Unit,
    suggestionProvider: SuggestionProvider? = null,
    searchBarOffsetY: MutableState<Int>,
    trailingIcon: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    var mSuggestionList by remember { mutableStateOf(emptyList<Suggestion>()) }
    val mSearchDatabase = searchDatabase.searchDao()
    var active by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current
    val activity = context.findActivity<MainActivity>()
    val dialogState = LocalDialogState.current

    class TagSuggestion(
        override val hint: String?,
        override val keyword: String,
    ) : Suggestion() {
        override fun onClick() {
            val query = searchFieldState.text.toString()
            var keywords = query.substringBeforeLast(' ', "")
            if (keywords.isNotEmpty()) keywords += ' '
            keywords += wrapTagKeyword(keyword)
            if (!keywords.endsWith(':')) keywords += ' '
            searchFieldState.setTextAndPlaceCursorAtEnd(keywords)
        }
    }

    class KeywordSuggestion(
        override val keyword: String,
    ) : Suggestion() {
        override val canDelete = true
        override fun onClick() {
            searchFieldState.setTextAndPlaceCursorAtEnd(keyword)
        }
    }

    fun mergedSuggestionFlow(): Flow<Suggestion> = flow {
        val query = searchFieldState.text.toString()
        suggestionProvider?.run { providerSuggestions(query)?.let { emit(it) } }
        mSearchDatabase.suggestions(query, 128).forEach { emit(KeywordSuggestion(it)) }
        EhTagDatabase.takeIf { it.initialized }?.run {
            if (query.isNotEmpty() && !query.endsWith(' ')) {
                val keyword = query.substringAfterLast(' ')
                val translate = Settings.showTagTranslations && isTranslatable(context)
                suggestFlow(keyword, translate, true).collect {
                    emit(TagSuggestion(it.first, it.second))
                }
                suggestFlow(keyword, translate).collect {
                    emit(TagSuggestion(it.first, it.second))
                }
            }
        }
    }

    suspend fun updateSuggestions() {
        mSuggestionList = mergedSuggestionFlow().toList()
    }

    var drawerLocked by remember { mutableStateOf(false) }

    fun onSearchViewExpanded() {
        onSearchExpanded()
        if (!activity.drawerLocked) {
            drawerLocked = true
            activity.drawerLocked = true
        }
    }

    fun onSearchViewHidden() {
        if (drawerLocked) {
            activity.drawerLocked = false
            drawerLocked = false
        }
        onSearchHidden()
    }

    if (active) {
        LaunchedEffect(Unit) {
            searchFieldState.forEachTextValue {
                updateSuggestions()
            }
        }
    }

    fun hideSearchView() {
        onSearchViewHidden()
        active = false
    }

    fun onApplySearch() {
        val query = searchFieldState.text.trim().toString()
        if (query.isNotEmpty()) {
            scope.launchIO {
                mSearchDatabase.deleteQuery(query)
                val search = Search(System.currentTimeMillis(), query)
                mSearchDatabase.insert(search)
            }
        }
        onApplySearch(query)
    }

    fun deleteKeyword(keyword: String) {
        scope.launch {
            dialogState.awaitPermissionOrCancel(
                confirmText = R.string.delete,
            ) {
                Text(text = stringResource(id = R.string.delete_search_history, keyword))
            }
            mSearchDatabase.deleteQuery(keyword)
            updateSuggestions()
        }
    }

    var searchbarOfs by searchBarOffsetY
    Box(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            modifier = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(0, searchbarOfs)
                }
            }.align(Alignment.TopCenter),
            state = searchFieldState,
            onSearch = {
                hideSearchView()
                onApplySearch()
            },
            active = active,
            onActiveChange = {
                if (it) {
                    searchbarOfs = 0
                    onSearchViewExpanded()
                } else {
                    onSearchViewHidden()
                }
                active = it
            },
            placeholder = { Text(searchFieldHint) },
            leadingIcon = {
                if (active) {
                    IconButton(onClick = { hideSearchView() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                } else {
                    IconButton(onClick = { activity.openDrawer() }) {
                        Icon(Icons.Default.Menu, contentDescription = null)
                    }
                }
            },
            trailingIcon = {
                if (active) {
                    if (searchFieldState.text.isNotEmpty()) {
                        IconButton(onClick = { searchFieldState.clearText() }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                } else {
                    Row {
                        trailingIcon()
                    }
                }
            },
        ) {
            LazyColumn(
                contentPadding = WindowInsets.navigationBars.union(WindowInsets.ime).asPaddingValues(),
            ) {
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
        Scaffold(
            floatingActionButton = {
                AnimatedVisibility(
                    visible = showSearchFab,
                    enter = scaleIn(),
                    exit = scaleOut(),
                ) {
                    FloatingActionButton(onClick = ::onApplySearch) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    }
                }
            },
        ) {
            content(
                PaddingValues(
                    top = it.calculateTopPadding() + 64.dp,
                    bottom = it.calculateBottomPadding(),
                ),
            )
        }
    }
}

abstract class SearchBarScene : BaseScene() {
    private var drawerView: View? = null
    private var drawerViewState: SparseArray<Parcelable>? = null
    private var sideSheetDialog: SideSheetDialog? = null

    private val searchFieldState = TextFieldState()
    private var mSuggestionProvider: SuggestionProvider? = null
    private var onApplySearch: (String) -> Unit = {}
    private var searchFieldHint by mutableStateOf("")
    var showSearchFab by mutableStateOf(false)

    protected abstract val fabLayout: View
    protected abstract val fastScroller: View
    protected abstract val recyclerView: RecyclerView

    override val enableDrawerGestures = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ofs = mutableIntStateOf(0)
        val onScrollListener = object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                var toChange by ofs
                toChange = (toChange - dy).coerceIn(-300, 0)
            }
        }
        return ComposeWithMD3 {
            val compositionContext = rememberCompositionContext()
            val density = LocalDensity.current
            val fabPadding = with(density) { 16.dp.roundToPx() }
            val margin = with(density) { 8.dp.roundToPx() }
            SearchBarScreen(
                searchFieldState = searchFieldState,
                searchFieldHint = searchFieldHint,
                showSearchFab = showSearchFab,
                onApplySearch = onApplySearch,
                onSearchExpanded = ::onSearchViewExpanded,
                onSearchHidden = ::onSearchViewHidden,
                suggestionProvider = mSuggestionProvider,
                searchBarOffsetY = ofs,
                trailingIcon = {
                    TrailingIcon()
                },
            ) { contentPadding ->
                val topPadding = with(density) { contentPadding.calculateTopPadding().roundToPx() }
                val bottomPadding = with(density) { contentPadding.calculateBottomPadding().roundToPx() }
                AndroidViewBinding(
                    modifier = Modifier.fillMaxSize(),
                    factory = { inflater, parent, _ ->
                        onCreateViewWithToolbar(inflater, parent, savedInstanceState).also {
                            recyclerView.addOnScrollListener(onScrollListener)
                            createDrawerView(savedInstanceState)?.apply {
                                if (this is ComposeView) {
                                    val owner = viewLifecycleOwner
                                    setViewTreeLifecycleOwner(owner)
                                    setViewTreeViewModelStoreOwner(owner as ViewModelStoreOwner)
                                    setViewTreeSavedStateRegistryOwner(owner as SavedStateRegistryOwner)
                                    setParentCompositionContext(compositionContext)
                                }
                                sideSheetDialog = SideSheetDialog(parent.context).also {
                                    it.setContentView(this)
                                }
                            }
                        }
                    },
                    onRelease = {
                        onRelease()
                    },
                ) {
                    fabLayout.updatePadding(bottom = fabPadding + bottomPadding)
                    fastScroller.updatePadding(top = topPadding + margin, bottom = bottomPadding)
                    recyclerView.updatePadding(top = topPadding + margin, bottom = bottomPadding)
                }
            }
        }
    }

    abstract fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?,
    ): ViewBinding

    fun openSideSheet() = sideSheetDialog!!.show()
    fun closeSideSheet() = sideSheetDialog!!.dismiss()

    private fun createDrawerView(savedInstanceState: Bundle?): View? {
        drawerView = onCreateDrawerView(layoutInflater)?.apply {
            val saved = drawerViewState ?: savedInstanceState?.getSparseParcelableArrayCompat(KEY_DRAWER_VIEW_STATE)
            saved?.let {
                restoreHierarchyState(it)
            }
        }
        return drawerView
    }

    open fun onCreateDrawerView(inflater: LayoutInflater): View? = null

    private fun destroyDrawerView() {
        drawerView?.let {
            drawerViewState = SparseArray()
            it.saveHierarchyState(drawerViewState)
        }
        onDestroyDrawerView()
        drawerView = null
    }

    open fun onDestroyDrawerView() {
        sideSheetDialog?.dismiss()
        sideSheetDialog = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        drawerView?.let {
            drawerViewState = SparseArray()
            it.saveHierarchyState(drawerViewState)
            outState.putSparseParcelableArray(KEY_DRAWER_VIEW_STATE, drawerViewState)
        }
    }

    @CallSuper
    open fun onRelease() {
        destroyDrawerView()
    }

    @Composable
    abstract fun TrailingIcon()

    open fun onSearchViewExpanded() {}

    open fun onSearchViewHidden() {}

    fun setTitle(title: String?) {
        // TODO
    }

    fun setSearchBarText(text: String?) {
        if (text != null) {
            searchFieldState.setTextAndPlaceCursorAtEnd(text)
        } else {
            searchFieldState.clearText()
        }
    }

    fun setSearchBarHint(hint: String?) {
        searchFieldHint = hint.orEmpty()
    }

    fun setOnApplySearch(lambda: (String) -> Unit) {
        onApplySearch = lambda
    }

    fun setSuggestionProvider(suggestionProvider: SuggestionProvider) {
        mSuggestionProvider = suggestionProvider
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
