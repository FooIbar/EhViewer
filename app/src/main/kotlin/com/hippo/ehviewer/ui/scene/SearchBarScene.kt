package com.hippo.ehviewer.ui.scene

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
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
import com.hippo.ehviewer.ui.legacy.FAB_ANIMATE_TIME
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
    title: String? = null,
    searchFieldState: TextFieldState,
    searchFieldHint: String? = null,
    showSearchFab: Boolean = false,
    onApplySearch: suspend (String) -> Unit,
    onSearchExpanded: () -> Unit,
    onSearchHidden: () -> Unit,
    refreshState: PullToRefreshState? = null,
    suggestionProvider: SuggestionProvider? = null,
    searchBarOffsetY: Int,
    trailingIcon: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
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
        scope.launchIO {
            onApplySearch(query)
        }
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

    val searchbarOfs by rememberUpdatedState(searchBarOffsetY)
    val scrollAwayModifier = if (!active) {
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.placeRelative(0, searchbarOfs)
            }
        }
    } else {
        Modifier
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                // Placeholder, fill immutable SearchBar padding
                Spacer(
                    modifier = Modifier.statusBarsPadding().displayCutoutPadding()
                        .height(SearchBarDefaults.InputFieldHeight + 16.dp),
                )
            },
            snackbarHost = snackbarHost,
            floatingActionButton = {
                val hiddenState by animateFloatAsState(
                    targetValue = if (showSearchFab) 1f else 0f,
                    animationSpec = tween(FAB_ANIMATE_TIME, if (showSearchFab) FAB_ANIMATE_TIME else 0),
                    label = "hiddenState",
                )
                FloatingActionButton(
                    onClick = { onApplySearch() },
                    modifier = Modifier.rotate(lerp(90f, 0f, hiddenState)).scale(hiddenState),
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                }
            },
            content = content,
        )
        if (refreshState != null) {
            @SuppressLint("PrivateResource")
            val overlay = colorResource(com.google.android.material.R.color.m3_popupmenu_overlay_color)
            val surface = MaterialTheme.colorScheme.surface
            val background = overlay.compositeOver(surface)
            PullToRefreshContainer(
                state = refreshState,
                modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding()
                    .padding(top = 48.dp) then scrollAwayModifier,
                containerColor = background,
            )
        }
        // Workaround for BTF2 cursor not showing
        // https://issuetracker.google.com/issues/307323842
        CompositionLocalProvider(LocalWindowInfo provides FocusedWindowInfo) {
            SearchBar(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter) then scrollAwayModifier,
                state = searchFieldState,
                onSearch = {
                    hideSearchView()
                    onApplySearch()
                },
                active = active,
                onActiveChange = {
                    if (it) {
                        onSearchViewExpanded()
                    } else {
                        onSearchViewHidden()
                    }
                    active = it
                },
                title = title?.let { { Text(it) } },
                placeholder = searchFieldHint?.let { { Text(it) } },
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
                LazyColumn(contentPadding = WindowInsets.navigationBars.union(WindowInsets.ime).asPaddingValues()) {
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
    private var searchFieldHint by mutableStateOf<String?>(null)
    private var searchBarTitle by mutableStateOf<String?>(null)
    var showSearchFab by mutableStateOf(false)

    protected abstract val fabLayout: View
    protected abstract val fastScroller: View
    protected abstract val recyclerView: RecyclerView

    override val enableDrawerGestures = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        var ofs by mutableIntStateOf(0)
        val onScrollListener = object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                ofs = (ofs - dy).coerceIn(-300, 0)
            }
        }
        return ComposeWithMD3 {
            val compositionContext = rememberCompositionContext()
            val density = LocalDensity.current
            val margin = with(density) { 8.dp.roundToPx() }
            SearchBarScreen(
                title = searchBarTitle,
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
                    fabLayout.updatePadding(bottom = bottomPadding)
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
        searchBarTitle = title
    }

    fun setSearchBarText(text: String?) {
        if (text != null) {
            searchFieldState.setTextAndPlaceCursorAtEnd(text)
        } else {
            searchFieldState.clearText()
        }
    }

    fun setSearchBarHint(hint: String?) {
        searchFieldHint = hint
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

private val FocusedWindowInfo = object : WindowInfo {
    override val isWindowFocused: Boolean
        get() = true
}

const val SEARCH_VIEW_ANIMATE_TIME = 300L
