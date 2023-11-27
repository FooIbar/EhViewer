package com.hippo.ehviewer.ui.scene

import android.annotation.SuppressLint
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.hippo.ehviewer.EhApplication.Companion.searchDatabase
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.dao.Search
import com.hippo.ehviewer.dao.SearchDao
import com.hippo.ehviewer.ui.LocalNavDrawerState
import com.hippo.ehviewer.ui.LockDrawer
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.main.FAB_ANIMATE_TIME
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.util.findActivity
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
    content: @Composable (PaddingValues) -> Unit,
) {
    var mSuggestionList by remember { mutableStateOf(emptyList<Suggestion>()) }
    val mSearchDatabase = searchDatabase.searchDao()
    var active by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current
    val activity = context.findActivity<MainActivity>()
    val dialogState = LocalDialogState.current

    // Workaround for BTF2 cursor not showing
    // We can't use an always focused WindowInfo because callbacks won't be called once
    // the window regained focus after losing it (e.g. showing a dialog on top of it)
    // https://issuetracker.google.com/issues/307323842
    val windowInfo = LocalWindowInfo.current
    remember {
        val clazz = Class.forName("androidx.compose.ui.platform.WindowInfoImpl")
        clazz.cast(windowInfo).let {
            clazz.getDeclaredMethod("setWindowFocused", Boolean::class.javaPrimitiveType).apply {
                invoke(it, true)
            }
        }
    }

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
            title = title?.let { { Text(it, overflow = TextOverflow.Ellipsis) } },
            placeholder = searchFieldHint?.let { { Text(it) } },
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
