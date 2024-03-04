package com.hippo.ehviewer.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.forEachTextValue
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.EhApplication.Companion.searchDatabase
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.data.TagNamespace
import com.hippo.ehviewer.dao.Search
import com.hippo.ehviewer.dao.SearchDao
import com.hippo.ehviewer.ui.LocalNavDrawerState
import com.hippo.ehviewer.ui.LockDrawer
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.jamal.composeprefs3.ui.ifNotNullThen
import com.jamal.composeprefs3.ui.ifTrueThen
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
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
    searchFieldState: TextFieldState = rememberTextFieldState(),
    searchFieldHint: String? = null,
    onApplySearch: (String) -> Unit,
    onSearchExpanded: () -> Unit,
    onSearchHidden: () -> Unit,
    refreshState: PullToRefreshState? = null,
    suggestionProvider: SuggestionProvider? = null,
    searchBarOffsetY: () -> Int,
    trailingIcon: @Composable () -> Unit,
    filter: @Composable (() -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    var mSuggestionList by remember { mutableStateOf(emptyList<Suggestion>()) }
    val mSearchDatabase = searchDatabase.searchDao()
    var active by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current
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

    var shouldLockDrawer by remember { mutableStateOf(false) }
    LockDrawer(shouldLockDrawer)

    fun onSearchViewExpanded() {
        onSearchExpanded()
        shouldLockDrawer = true
    }

    fun onSearchViewHidden() {
        shouldLockDrawer = false
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

    val scrollAwayModifier = if (!active) {
        Modifier.offset { IntOffset(0, searchBarOffsetY()) }
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
            floatingActionButton = floatingActionButton,
            content = content,
        )
        if (refreshState != null) {
            PullToRefreshContainer(
                state = refreshState,
                modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding()
                    .padding(top = 48.dp) then scrollAwayModifier,
            )
        }
        SearchBar(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter) then scrollAwayModifier
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
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
            title = title.ifNotNullThen { Text(title!!, overflow = TextOverflow.Ellipsis) },
            placeholder = searchFieldHint.ifNotNullThen { Text(searchFieldHint!!) },
            leadingIcon = {
                if (active) {
                    IconButton(onClick = { hideSearchView() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                } else {
                    val drawerState = LocalNavDrawerState.current
                    IconButton(onClick = { scope.launchUI { drawerState.open() } }) {
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
            filter?.invoke()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = WindowInsets.navigationBars.union(WindowInsets.ime)
                    .only(WindowInsetsSides.Bottom).asPaddingValues(),
            ) {
                // Workaround for prepending before the first item
                item {}
                items(mSuggestionList, key = { it.keyword.hashCode() * 31 + it.canDelete.hashCode() }) {
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
                        modifier = Modifier.clickable { it.onClick() }.animateItemPlacement(),
                    )
                }
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
            val namespacePrefix = TagNamespace(prefix).toPrefix()
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
