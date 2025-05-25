package com.hippo.ehviewer.ui.screen

import android.content.Context
import android.view.ViewConfiguration
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.filled.GoTo
import com.hippo.ehviewer.ui.DrawerHandle
import com.hippo.ehviewer.ui.LocalSideSheetState
import com.hippo.ehviewer.ui.ProvideSideSheetContent
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.awaitSelectDate
import com.hippo.ehviewer.ui.main.AvatarIcon
import com.hippo.ehviewer.ui.main.FAB_ANIMATE_TIME
import com.hippo.ehviewer.ui.main.FabLayout
import com.hippo.ehviewer.ui.main.GalleryInfoGridItem
import com.hippo.ehviewer.ui.main.GalleryInfoListItem
import com.hippo.ehviewer.ui.main.GalleryList
import com.hippo.ehviewer.ui.startDownload
import com.hippo.ehviewer.ui.tools.asyncState
import com.hippo.ehviewer.ui.tools.awaitConfirmationOrCancel
import com.hippo.ehviewer.ui.tools.awaitSelectItem
import com.hippo.ehviewer.ui.tools.thenIf
import com.hippo.ehviewer.util.mapToLongArray
import com.hippo.ehviewer.util.takeAndClear
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import moe.tarsin.coroutines.onEachLatest
import moe.tarsin.coroutines.runSwallowingWithUI
import moe.tarsin.launch
import moe.tarsin.navigate

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.FavouritesScreen(navigator: DestinationsNavigator, viewModel: FavoritesViewModel = viewModel()) = Screen(navigator) {
    // Immutables
    val localFavName = stringResource(R.string.local_favorites)
    val cloudFavName = stringResource(R.string.cloud_favorites)
    val animateItems by Settings.animateItems.collectAsState()
    val hasSignedIn by Settings.hasSignedIn.collectAsState()

    // Meta State
    var urlBuilder by viewModel.urlBuilder
    var searchBarExpanded by rememberSaveable { mutableStateOf(false) }
    var searchBarOffsetY by remember { mutableIntStateOf(0) }

    // Derived State
    val keyword = urlBuilder.keyword
    val favCatName = remember(urlBuilder) {
        when (val favCat = urlBuilder.favCat) {
            in 0..9 -> Settings.favCat[favCat]
            FavListUrlBuilder.FAV_CAT_LOCAL -> localFavName.also { searchBarOffsetY = 0 }
            else -> cloudFavName
        }
    }
    val title = if (keyword.isNullOrBlank()) {
        stringResource(R.string.favorites_title, favCatName)
    } else {
        stringResource(R.string.favorites_title_2, favCatName, keyword)
    }
    val density = LocalDensity.current
    val searchBarHint = stringResource(R.string.search_bar_hint, favCatName)
    val data = viewModel.data.collectAsLazyPagingItems()

    fun refresh(newUrlBuilder: FavListUrlBuilder = urlBuilder.copy(jumpTo = null, prev = null, next = null)) {
        urlBuilder = newUrlBuilder
        data.refresh()
    }

    ProvideSideSheetContent { sheetState ->
        val localFavCount by viewModel.localFavCount.collectAsState(0)
        TopAppBar(
            title = { Text(text = stringResource(id = R.string.collections)) },
            windowInsets = WindowInsets(),
            colors = topBarOnDrawerColor(),
        )
        val scope = currentRecomposeScope
        LaunchedEffect(Unit) {
            Settings.favChangesFlow.collect {
                scope.invalidate()
            }
        }
        val localFav = stringResource(id = R.string.local_favorites) to localFavCount
        val faves = if (hasSignedIn) {
            arrayOf(
                localFav,
                stringResource(id = R.string.cloud_favorites) to Settings.favCloudCount,
                *Settings.favCat.zip(Settings.favCount.toTypedArray()).toTypedArray(),
            )
        } else {
            arrayOf(localFav)
        }
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 4.dp)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom)),
        ) {
            faves.forEachIndexed { index, (name, count) ->
                ListItem(
                    headlineContent = { Text(text = name) },
                    trailingContent = { Text(text = count.toString(), style = MaterialTheme.typography.bodyLarge) },
                    modifier = Modifier.clip(CardDefaults.shape).clickable {
                        val newCat = index - 2
                        refresh(FavListUrlBuilder(newCat))
                        Settings.recentFavCat = newCat
                        launch { sheetState.close() }
                    },
                    colors = listItemOnDrawerColor(urlBuilder.favCat == index - 2),
                )
            }
        }
    }

    var fabExpanded by remember { mutableStateOf(false) }
    var fabHidden by remember { mutableStateOf(false) }
    val checkedInfoMap = remember { mutableStateMapOf<Long, BaseGalleryInfo>() }
    val selectMode = checkedInfoMap.isNotEmpty()
    DrawerHandle(!selectMode && !searchBarExpanded)

    SearchBarScreen(
        onApplySearch = { refresh(FavListUrlBuilder(urlBuilder.favCat, it)) },
        expanded = searchBarExpanded,
        onExpandedChange = {
            searchBarExpanded = it
            fabHidden = it
            if (it) checkedInfoMap.clear()
        },
        title = title,
        searchFieldHint = searchBarHint,
        tagNamespace = !urlBuilder.isLocal,
        searchBarOffsetY = { searchBarOffsetY },
        trailingIcon = {
            val sheetState = LocalSideSheetState.current
            IconButton(onClick = { launch { sheetState.open() } }) {
                Icon(imageVector = Icons.Outlined.FolderSpecial, contentDescription = null)
            }
            AvatarIcon()
        },
    ) { contentPadding ->
        val listMode by Settings.listMode.collectAsState()
        val height by collectListThumbSizeAsState()
        val showPages by Settings.showGalleryPages.collectAsState()
        val searchBarConnection = remember {
            val slop = ViewConfiguration.get(contextOf<Context>()).scaledTouchSlop
            val topPaddingPx = with(density) { contentPadding.calculateTopPadding().roundToPx() }
            object : NestedScrollConnection {
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    val dy = -consumed.y
                    if (dy >= slop) {
                        fabHidden = true
                    } else if (dy <= -slop / 2) {
                        fabHidden = false
                    }
                    searchBarOffsetY = (searchBarOffsetY - dy).roundToInt().coerceIn(-topPaddingPx, 0)
                    return Offset.Zero // We never consume it
                }
            }
        }
        GalleryList(
            data = data,
            contentModifier = Modifier.nestedScroll(searchBarConnection),
            contentPadding = contentPadding,
            listMode = listMode,
            detailItemContent = { info ->
                val checked = info.gid in checkedInfoMap
                CheckableItem(
                    checked = checked,
                    modifier = Modifier.thenIf(animateItems) { animateItem() },
                ) { interactionSource ->
                    GalleryInfoListItem(
                        onClick = {
                            if (selectMode) {
                                if (checked) {
                                    checkedInfoMap.remove(info.gid)
                                } else {
                                    checkedInfoMap[info.gid] = info
                                }
                            } else {
                                navigate(info.asDst())
                            }
                        },
                        onLongClick = {
                            checkedInfoMap[info.gid] = info
                        },
                        info = info,
                        showPages = showPages,
                        modifier = Modifier.height(height),
                        isInFavScene = true,
                        interactionSource = interactionSource,
                    )
                }
            },
            thumbItemContent = { info ->
                val checked = info.gid in checkedInfoMap
                CheckableItem(
                    checked = checked,
                    modifier = Modifier.thenIf(animateItems) { animateItem() },
                ) { interactionSource ->
                    GalleryInfoGridItem(
                        onClick = {
                            if (selectMode) {
                                if (checked) {
                                    checkedInfoMap.remove(info.gid)
                                } else {
                                    checkedInfoMap[info.gid] = info
                                }
                            } else {
                                navigate(info.asDst())
                            }
                        },
                        onLongClick = {
                            checkedInfoMap[info.gid] = info
                        },
                        info = info,
                        showPages = showPages,
                        showFavoriteStatus = false,
                        interactionSource = interactionSource,
                    )
                }
            },
            searchBarOffsetY = { searchBarOffsetY },
            scrollToTopOnRefresh = urlBuilder.favCat != FavListUrlBuilder.FAV_CAT_LOCAL,
            onRefresh = { refresh() },
            onLoading = { searchBarOffsetY = 0 },
        )
    }

    val hideFab by asyncState(
        produce = { fabHidden },
        transform = {
            onEachLatest { hide ->
                if (!hide) delay(FAB_ANIMATE_TIME.toLong())
            }
        },
    )

    FabLayout(
        hidden = hideFab && !selectMode,
        expanded = fabExpanded || selectMode,
        onExpandChanged = {
            fabExpanded = it
            checkedInfoMap.clear()
        },
        autoCancel = !selectMode,
    ) {
        if (!selectMode) {
            if (urlBuilder.isLocal) {
                onClick(Icons.Default.Shuffle) {
                    EhDB.randomLocalFav()?.let { info ->
                        withUIContext { navigate(info.asDst()) }
                    }
                }
            }
            onClick(EhIcons.Default.GoTo) {
                val date = awaitSelectDate()
                refresh(urlBuilder.copy(jumpTo = date))
            }
            onClick(Icons.Default.Refresh) {
                refresh()
            }
            onClick(Icons.AutoMirrored.Default.LastPage) {
                refresh(urlBuilder.copy(jumpTo = null, prev = "1-0", next = null))
            }
        } else {
            onClick(Icons.Default.DoneAll, autoClose = false) {
                val info = data.itemSnapshotList.items.associateBy { it.gid }
                checkedInfoMap.putAll(info)
            }
            onClick(Icons.Default.Download) {
                val info = checkedInfoMap.takeAndClear()
                runSwallowingWithUI {
                    startDownload(false, *info.toTypedArray())
                }
            }
            onClick(Icons.Default.Delete) {
                val info = checkedInfoMap.takeAndClear()
                awaitConfirmationOrCancel(title = R.string.delete_favorites_dialog_title) {
                    Text(text = stringResource(R.string.delete_favorites_dialog_message, info.size))
                }
                val srcCat = urlBuilder.favCat
                runSwallowingWithUI {
                    if (srcCat == FavListUrlBuilder.FAV_CAT_LOCAL) { // Delete local fav
                        EhDB.removeLocalFavorites(info)
                    } else {
                        val delList = info.mapToLongArray(BaseGalleryInfo::gid)
                        EhEngine.modifyFavorites(delList, srcCat, -1)
                    }
                }
                // We refresh anyway as cloud data maybe partially modified
                data.refresh()
            }
            onClick(Icons.AutoMirrored.Default.DriveFileMove) {
                // First is local favorite, the other 10 is cloud favorite
                val items = buildList {
                    add(localFavName)
                    if (hasSignedIn) {
                        addAll(Settings.favCat)
                    }
                }
                val index = awaitSelectItem(items, R.string.move_favorites_dialog_title)
                val srcCat = urlBuilder.favCat
                val dstCat = if (index == 0) FavListUrlBuilder.FAV_CAT_LOCAL else index - 1
                val info = checkedInfoMap.takeAndClear()
                if (srcCat != dstCat) {
                    runSwallowingWithUI {
                        if (srcCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
                            // Move from local to cloud
                            val galleryList = info.map { it.gid to it.token }
                            EhEngine.addFavorites(galleryList, dstCat)
                            EhDB.removeLocalFavorites(info)
                        } else if (dstCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
                            // Move from cloud to local
                            EhDB.putLocalFavorites(info)
                        } else {
                            // Move from cloud to cloud
                            val gidArray = info.mapToLongArray(BaseGalleryInfo::gid)
                            EhEngine.modifyFavorites(gidArray, srcCat, dstCat)
                        }
                    }
                    // We refresh anyway as cloud data maybe partially modified
                    data.refresh()
                }
            }
        }
    }
}
