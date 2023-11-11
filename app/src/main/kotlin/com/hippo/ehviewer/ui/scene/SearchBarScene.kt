package com.hippo.ehviewer.ui.scene

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.view.updatePadding
import androidx.viewbinding.ViewBinding
import com.hippo.ehviewer.EhApplication.Companion.searchDatabase
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.dao.Search
import com.hippo.ehviewer.dao.SearchDao
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.util.findActivity
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
    initialQuery: String = "",
    hint: String = "",
    showSearchFab: Boolean = false,
    onApplySearch: (String) -> Unit,
    onSearchExpanded: () -> Unit,
    onSearchHidden: () -> Unit,
    suggestionProvider: SuggestionProvider? = null,
    trailingIcon: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    var mSuggestionList by remember { mutableStateOf(emptyList<Suggestion>()) }
    val mSearchDatabase = searchDatabase.searchDao()
    var query by remember { mutableStateOf(initialQuery) }
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
            var keywords = query.substringBeforeLast(' ', "")
            if (keywords.isNotEmpty()) keywords += ' '
            keywords += wrapTagKeyword(keyword)
            if (!keywords.endsWith(':')) keywords += ' '
            query = keywords
        }
    }

    class KeywordSuggestion(
        override val keyword: String,
    ) : Suggestion() {
        override val canDelete = true
        override fun onClick() {
            query = keyword
        }
    }

    fun mergedSuggestionFlow(): Flow<Suggestion> = flow {
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
        LaunchedEffect(query) {
            updateSuggestions()
        }
    }

    fun hideSearchView() {
        onSearchViewHidden()
        active = false
    }

    fun onApplySearch() {
        query = query.trim()
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

    Box(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            modifier = Modifier.align(Alignment.TopCenter),
            query = query,
            onQueryChange = {
                query = it
            },
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
            placeholder = { Text(hint) },
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
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
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
            content = content,
        )
    }
}

abstract class SearchBarScene : BaseScene() {
    private var mSuggestionProvider: SuggestionProvider? = null
    private var onApplySearch: (String) -> Unit = {}
    private var query by mutableStateOf("")
    private var hint by mutableStateOf("")
    var showSearchFab by mutableStateOf(false)

    protected abstract val fabLayout: View
    protected abstract val fastScroller: View
    protected abstract val recyclerView: View

    override val enableDrawerGestures = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeWithMD3 {
            val density = LocalDensity.current
            val fabPadding = with(density) { 16.dp.roundToPx() }
            SearchBarScreen(
                initialQuery = query,
                hint = hint,
                showSearchFab = showSearchFab,
                onApplySearch = onApplySearch,
                onSearchExpanded = ::onSearchViewExpanded,
                onSearchHidden = ::onSearchViewHidden,
                suggestionProvider = mSuggestionProvider,
                trailingIcon = {
                    TrailingIcon()
                },
            ) { contentPadding ->
                val bottomPadding = with(density) { contentPadding.calculateBottomPadding().roundToPx() }
                AndroidViewBinding(
                    modifier = Modifier.padding(top = contentPadding.calculateTopPadding()).fillMaxSize(),
                    factory = { inflater, parent, _ ->
                        onCreateViewWithToolbar(inflater, parent, savedInstanceState)
                    },
                ) {
                    fabLayout.updatePadding(bottom = fabPadding + bottomPadding)
                    fastScroller.updatePadding(bottom = bottomPadding)
                    recyclerView.updatePadding(bottom = bottomPadding)
                }
            }
        }
    }

    abstract fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?,
    ): ViewBinding

    @Composable
    abstract fun TrailingIcon()

    open fun onSearchViewExpanded() {}

    open fun onSearchViewHidden() {}

    fun setSearchBarHint(hint: String?) {
        this.hint = hint.orEmpty()
    }

    fun setSearchBarText(text: String?) {
        query = text.orEmpty()
    }

    fun setEditTextHint(hint: String?) {
        this.hint = hint.orEmpty()
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
