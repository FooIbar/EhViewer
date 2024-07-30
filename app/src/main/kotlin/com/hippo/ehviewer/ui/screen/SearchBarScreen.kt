package com.hippo.ehviewer.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
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
import androidx.compose.material3.SearchBarInputField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.dao.Search
import com.hippo.ehviewer.dao.SearchDao
import com.hippo.ehviewer.ui.LocalNavDrawerState
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.ui.tools.rememberCompositionActiveState
import com.hippo.ehviewer.ui.tools.thenIf
import com.jamal.composeprefs3.ui.ifNotNullThen
import com.jamal.composeprefs3.ui.ifTrueThen
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
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
    (if (prefix.isBlank()) list(limit) else rawSuggestions(prefix, limit))

@Composable
fun SearchBarScreen(
    onApplySearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    title: String?,
    searchFieldHint: String,
    searchFieldState: TextFieldState = rememberTextFieldState(),
    suggestionProvider: SuggestionProvider? = null,
    tagNamespace: Boolean = false,
    searchBarOffsetY: () -> Int = { 0 },
    trailingIcon: @Composable () -> Unit = {},
    filter: @Composable (() -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    var mSuggestionList by remember { mutableStateOf(emptyList<Suggestion>()) }
    val mSearchDatabase = searchDatabase.searchDao()
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current
    val dialogState = LocalDialogState.current
    val animateItems by Settings.animateItems.collectAsState()

    class TagSuggestion(
        override val hint: String?,
        override val keyword: String,
    ) : Suggestion() {
        override fun onClick() {
            val query = searchFieldState.text.toString()
            var keywords = query.substringBeforeLast(' ', "")
            if (keywords.isNotEmpty()) keywords += ' '
            keywords += if (tagNamespace) wrapTagKeyword(keyword) else keyword.substringAfter(':')
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

    if (expanded) {
        LaunchedEffect(Unit) {
            snapshotFlow { searchFieldState.text }.collectLatest {
                updateSuggestions()
            }
        }
    }

    fun hideSearchView() {
        onExpandedChange(false)
    }

    fun onApplySearch() {
        // May have invalid whitespaces if pasted from clipboard, replace them with spaces
        val query = searchFieldState.text.trim().replace(WhitespaceRegex, " ")
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                // Placeholder, fill immutable SearchBar padding
                Spacer(modifier = Modifier.statusBarsPadding().height(SearchBarDefaults.InputFieldHeight + 16.dp))
            },
            floatingActionButton = floatingActionButton,
            content = content,
        )
        // https://issuetracker.google.com/337191298
        // Workaround for can't exit SearchBar due to refocus in non-touch mode
        Box(Modifier.size(1.dp).focusable())
        val activeState = rememberCompositionActiveState()
        SearchBar(
            modifier = Modifier.align(Alignment.TopCenter).thenIf(!expanded) { offset { IntOffset(0, searchBarOffsetY()) } }
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
            inputField = {
                SearchBarInputField(
                    state = searchFieldState,
                    onSearch = {
                        hideSearchView()
                        onApplySearch()
                    },
                    expanded = expanded,
                    onExpandedChange = onExpandedChange,
                    modifier = Modifier.widthIn(max = maxWidth - SearchBarHorizontalPadding * 2),
                    placeholder = {
                        val contentActive by activeState.state
                        val text = title.takeUnless { expanded || contentActive } ?: searchFieldHint
                        Text(text, overflow = TextOverflow.Ellipsis, maxLines = 1)
                    },
                    leadingIcon = {
                        if (expanded) {
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
                        if (expanded) {
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
                )
            },
            expanded = expanded,
            onExpandedChange = onExpandedChange,
        ) {
            activeState.Anchor()
            filter?.invoke()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
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
                        modifier = Modifier.clickable { it.onClick() }.thenIf(animateItems) { animateItem() },
                    )
                }
            }
        }
    }
}

fun wrapTagKeyword(keyword: String, translate: Boolean = false): String = if (keyword.endsWith(':')) {
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

private val WhitespaceRegex = Regex("\\s+")
private val SearchBarHorizontalPadding = 16.dp
