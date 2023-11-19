/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.ui.scene

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CalendarConstraints.DateValidator
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.databinding.SceneFavoritesBinding
import com.hippo.ehviewer.ui.legacy.HandlerDrawable
import com.hippo.ehviewer.ui.legacy.SecondaryFab
import com.hippo.ehviewer.ui.legacy.ViewTransition
import com.hippo.ehviewer.ui.startDownload
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.getValue
import com.hippo.ehviewer.util.lazyMut
import com.hippo.ehviewer.util.mapToLongArray
import com.hippo.ehviewer.util.setValue
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withIOContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching
import rikka.core.res.resolveColor

// Note that we do not really follow mvvm structure, just use it as ... storage
class VMStorage : ViewModel() {
    var urlBuilder = FavListUrlBuilder(favCat = Settings.recentFavCat)
    private val cloudDataFlow = Pager(PagingConfig(25)) {
        object : PagingSource<String, BaseGalleryInfo>() {
            override fun getRefreshKey(state: PagingState<String, BaseGalleryInfo>): String? = null
            override suspend fun load(params: LoadParams<String>) = withIOContext {
                when (params) {
                    is LoadParams.Prepend -> urlBuilder.setIndex(params.key, isNext = false)
                    is LoadParams.Append -> urlBuilder.setIndex(params.key, isNext = true)
                    is LoadParams.Refresh -> {
                        val key = params.key
                        if (key.isNullOrBlank()) {
                            if (urlBuilder.jumpTo != null) {
                                urlBuilder.mNext ?: urlBuilder.setIndex("2", true)
                            }
                        } else {
                            urlBuilder.setIndex(key, false)
                        }
                    }
                }
                val r = runSuspendCatching {
                    EhEngine.getFavorites(urlBuilder.build())
                }.onFailure {
                    return@withIOContext LoadResult.Error(it)
                }.getOrThrow()
                Settings.favCat = r.catArray
                Settings.favCount = r.countArray
                Settings.favCloudCount = r.countArray.sum()
                urlBuilder.jumpTo = null
                LoadResult.Page(r.galleryInfoList, r.prev, r.next)
            }
        }
    }.flow.cachedIn(viewModelScope)

    private val localFavDataFlow = Pager(PagingConfig(20, jumpThreshold = 40)) {
        val keyword = urlBuilder.keyword
        if (keyword.isNullOrBlank()) {
            EhDB.localFavLazyList
        } else {
            EhDB.searchLocalFav(keyword)
        }
    }.flow.cachedIn(viewModelScope)
    fun dataflow() = if (urlBuilder.favCat == FavListUrlBuilder.FAV_CAT_LOCAL) localFavDataFlow else cloudDataFlow
    val localFavCount = EhDB.localFavCount
}

class FavoritesScene : SearchBarScene() {
    private val vm: VMStorage by viewModels()
    private var urlBuilder by lazyMut { vm::urlBuilder }
    private var _binding: SceneFavoritesBinding? = null
    private val binding get() = _binding!!
    private var mAdapter: GalleryAdapter? = null
    private val tracker get() = mAdapter!!.tracker!!
    private var showNormalFabsJob: Job? = null

    private val dialogState = DialogState()

    override val fabLayout get() = binding.fabLayout
    override val fastScroller get() = binding.fastScroller
    override val recyclerView get() = binding.recyclerView

    private var onDrawerItemClick: ((Int) -> Unit)? = null

    private var collectJob: Job? = null

    private fun switchFav(newCat: Int, keyword: String? = null) {
        _binding ?: return
        urlBuilder.keyword = keyword
        urlBuilder.favCat = newCat
        urlBuilder.jumpTo = null
        urlBuilder.setIndex(null, true)
        collectJob?.cancel()
        collectJob = viewLifecycleOwner.lifecycleScope.launchIO {
            vm.dataflow().collectLatest {
                mAdapter?.submitData(it)
            }
        }
        mAdapter?.refresh()
        val favCatName: String = when (val favCat = urlBuilder.favCat) {
            in 0..9 -> Settings.favCat[favCat]
            FavListUrlBuilder.FAV_CAT_LOCAL -> getString(R.string.local_favorites)
            else -> getString(R.string.cloud_favorites)
        }
        if (keyword.isNullOrEmpty()) {
            setTitle(getString(R.string.favorites_title, favCatName))
            setSearchBarText(null)
        } else {
            setTitle(getString(R.string.favorites_title_2, favCatName, keyword))
        }
        setSearchBarHint(getString(R.string.search_bar_hint, favCatName))
        Settings.recentFavCat = urlBuilder.favCat
    }

    @Composable
    override fun TrailingIcon() {
        dialogState.Intercept()
        IconButton(onClick = { openSideSheet() }) {
            Icon(imageVector = Icons.Outlined.FolderSpecial, contentDescription = stringResource(id = R.string.collections))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mAdapter?.let { tracker.saveSelection(outState) }
    }

    override fun onResume() {
        super.onResume()
        mAdapter?.type = Settings.listMode
    }

    private val SceneFavoritesBinding.fastScroller get() = contentLayout.fastScroller
    private val SceneFavoritesBinding.refreshLayout get() = contentLayout.refreshLayout
    private val SceneFavoritesBinding.recyclerView get() = contentLayout.recyclerView
    private val SceneFavoritesBinding.progress get() = contentLayout.progress
    private val SceneFavoritesBinding.tip get() = contentLayout.tip

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?,
    ): ViewBinding {
        _binding = SceneFavoritesBinding.inflate(inflater, container)
        val fabActions = listOf<SecondaryFab>(
            // Normal
            Icons.AutoMirrored.Filled.Redo to {
                showGoToDialog()
            },
            Icons.Default.Refresh to {
                switchFav(urlBuilder.favCat)
            },
            Icons.Default.ArrowDownward to {
                urlBuilder.setIndex("1-0", false)
                mAdapter?.refresh()
            },

            // Select
            Icons.Default.DoneAll to {
                tracker.selectAll()
                throw CancellationException()
            },
            Icons.Default.Download to {
                dialogState.startDownload(requireContext(), false, *takeCheckedInfo().toTypedArray())
            },
            Icons.Default.Delete to {
                dialogState.awaitPermissionOrCancel(title = R.string.delete_favorites_dialog_title) {
                    Text(text = stringResource(R.string.delete_favorites_dialog_message, checkedSize()))
                }
                val info = takeCheckedInfo()
                val srcCat = urlBuilder.favCat
                if (srcCat == FavListUrlBuilder.FAV_CAT_LOCAL) { // Delete local fav
                    EhDB.removeLocalFavorites(info)
                } else {
                    val delList = info.mapToLongArray(BaseGalleryInfo::gid)
                    EhEngine.modifyFavorites(delList, srcCat, -1)
                }
                mAdapter?.refresh()
            },
            Icons.AutoMirrored.Default.DriveFileMove to {
                // First is local favorite, the other 10 is cloud favorite
                val localFav = getString(R.string.local_favorites)
                val array = if (EhCookieStore.hasSignedIn()) {
                    arrayOf(localFav, *Settings.favCat)
                } else {
                    arrayOf(localFav)
                }
                val index = dialogState.showSelectItem(*array, title = R.string.move_favorites_dialog_title)
                val srcCat = urlBuilder.favCat
                val dstCat = if (index == 0) FavListUrlBuilder.FAV_CAT_LOCAL else index - 1
                val info = takeCheckedInfo()
                if (srcCat != dstCat) {
                    if (srcCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
                        // Move from local to cloud
                        EhDB.removeLocalFavorites(info)
                        val galleryList = info.map { it.gid to it.token!! }
                        runSuspendCatching {
                            EhEngine.addFavorites(galleryList, dstCat)
                        }
                    } else if (dstCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
                        // Move from cloud to local
                        EhDB.putLocalFavorites(info)
                    } else {
                        // Move from cloud to cloud
                        val gidArray = info.mapToLongArray(BaseGalleryInfo::gid)
                        runSuspendCatching {
                            EhEngine.modifyFavorites(gidArray, srcCat, dstCat)
                        }
                    }
                    mAdapter?.refresh()
                }
            },
        )
        fun updateJumpFab() {
            val isLocalFav = urlBuilder.favCat == FavListUrlBuilder.FAV_CAT_LOCAL
            binding.fabLayout.secondaryFab = if (isLocalFav) listOf(fabActions[1]) else fabActions.take(3)
        }
        onDrawerItemClick = {
            // Skip if in search mode
            if (!tracker.isInCustomChoice) {
                switchFav(it - 2)
                updateJumpFab()
                closeSideSheet()
            }
        }
        setOnApplySearch {
            if (!tracker.isInCustomChoice) {
                switchFav(urlBuilder.favCat, it)
            }
        }
        binding.fastScroller.attachToRecyclerView(binding.recyclerView)
        binding.fastScroller.setHandlerDrawable(HandlerDrawable().apply { setColor(inflater.context.theme.resolveColor(androidx.appcompat.R.attr.colorPrimary)) })
        binding.fabLayout.run {
            updateJumpFab()
            addOnExpandStateListener {
                if (tracker.isInCustomChoice && !it) tracker.clearSelection()
            }
        }
        binding.recyclerView.run {
            mAdapter = GalleryAdapter(
                this@run,
                false,
                { info ->
                    navAnimated(
                        R.id.galleryDetailScene,
                        bundleOf(GalleryDetailScene.KEY_ARGS to GalleryInfoArgs(info)),
                    )
                },
                {},
            ).also { adapter ->
                val drawable = ContextCompat.getDrawable(context, R.drawable.big_sad_pandroid)!!
                drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                binding.tip.setCompoundDrawables(null, drawable, null, null)
                binding.tip.setOnClickListener { mAdapter?.retry() }
                binding.refreshLayout.setOnRefreshListener { switchFav(urlBuilder.favCat) }
                val transition = ViewTransition(binding.refreshLayout, binding.progress, binding.tip)
                val empty = getString(R.string.gallery_list_empty_hit)
                viewLifecycleOwner.lifecycleScope.launch {
                    adapter.loadStateFlow.collectLatest {
                        when (val state = it.refresh) {
                            is LoadState.Loading -> {
                                if (!binding.refreshLayout.isRefreshing) {
                                    transition.showView(1)
                                }
                            }
                            is LoadState.Error -> {
                                binding.refreshLayout.isRefreshing = false
                                binding.tip.text = ExceptionUtils.getReadableString(state.error)
                                transition.showView(2)
                            }
                            is LoadState.NotLoading -> {
                                binding.refreshLayout.isRefreshing = false
                                if (mAdapter?.itemCount == 0) {
                                    binding.tip.text = empty
                                    transition.showView(2)
                                } else {
                                    transition.showView(0)
                                }
                            }
                        }
                    }
                }
                adapter.tracker = GallerySelectionTracker(
                    "favorite-selection",
                    this,
                    { adapter.snapshot().items },
                    { (this as? GalleryHolder)?.galleryId },
                ).apply {
                    addCustomChoiceListener(
                        onIntoCustomChoiceListener = {
                            showNormalFabsJob?.cancel()
                            binding.fabLayout.secondaryFab = fabActions.drop(3)
                            binding.fabLayout.autoCancel = false
                            binding.fabLayout.expanded = true
                            binding.refreshLayout.isEnabled = false
                            lockDrawer()
                        },
                        onOutOfCustomChoiceListener = {
                            // Delay showing normal fab to avoid mutation
                            showNormalFabsJob = lifecycleScope.launch {
                                delay(300)
                                updateJumpFab()
                            }
                            binding.fabLayout.autoCancel = true
                            binding.fabLayout.expanded = false
                            binding.refreshLayout.isEnabled = true
                            unlockDrawer()
                        },
                    )
                    restoreSelection(savedInstanceState)
                }
            }
            switchFav(Settings.recentFavCat)
        }
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                return if (tracker.isInCustomChoice) {
                    0
                } else {
                    makeMovementFlags(0, ItemTouchHelper.LEFT)
                }
            }
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val info = mAdapter!!.peek(position)!!
                lifecycleScope.launchIO {
                    dialogState.awaitPermissionOrCancel(
                        confirmText = R.string.delete,
                        title = R.string.delete_favorites_dialog_title,
                        onDismiss = { mAdapter!!.notifyItemChanged(position) },
                    ) {
                        Text(text = stringResource(id = R.string.delete_favorites_dialog_message, 1))
                    }
                    if (urlBuilder.favCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
                        EhDB.removeLocalFavorites(info)
                    } else {
                        EhEngine.modifyFavorites(info.gid, info.token)
                    }
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
        return binding
    }

    override fun onRelease() {
        super.onRelease()
        binding.recyclerView.stopScroll()
        mAdapter = null
        _binding = null
        showNormalFabsJob = null
        onDrawerItemClick = null
    }

    override fun onCreateDrawerView(inflater: LayoutInflater) = ComposeWithMD3 {
        val localFavCount by vm.localFavCount.collectAsState(0)
        ElevatedCard {
            TopAppBar(title = { Text(text = stringResource(id = R.string.collections)) })
            val scope = currentRecomposeScope
            LaunchedEffect(Unit) {
                Settings.favChangesFlow.collect {
                    scope.invalidate()
                }
            }
            val localFav = stringResource(id = R.string.local_favorites) to localFavCount
            val faves = if (EhCookieStore.hasSignedIn()) {
                arrayOf(
                    localFav,
                    stringResource(id = R.string.cloud_favorites) to Settings.favCloudCount,
                    *Settings.favCat.zip(Settings.favCount.toTypedArray()).toTypedArray(),
                )
            } else {
                arrayOf(localFav)
            }
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                faves.forEachIndexed { index, (name, count) ->
                    ListItem(
                        headlineContent = { Text(text = name) },
                        trailingContent = { Text(text = count.toString(), style = MaterialTheme.typography.bodyLarge) },
                        modifier = Modifier.clickable { onDrawerItemClick?.invoke(index) },
                    )
                }
            }
        }
    }

    override fun onSearchViewExpanded() {
        binding.fabLayout.hide()
        super.onSearchViewExpanded()
    }

    override fun onSearchViewHidden() {
        super.onSearchViewHidden()
        lifecycleScope.launch {
            delay(300)
            _binding ?: return@launch
            binding.fabLayout.show()
        }
    }

    private fun showGoToDialog() {
        context ?: return
        val local = LocalDateTime.of(2007, 3, 21, 0, 0)
        val fromDate = local.atZone(ZoneId.ofOffset("UTC", ZoneOffset.UTC)).toInstant().toEpochMilli()
        val toDate = MaterialDatePicker.todayInUtcMilliseconds()
        val listValidators = ArrayList<DateValidator>()
        listValidators.add(DateValidatorPointForward.from(fromDate))
        listValidators.add(DateValidatorPointBackward.before(toDate))
        val constraintsBuilder = CalendarConstraints.Builder()
            .setStart(fromDate)
            .setEnd(toDate)
            .setValidator(CompositeDateValidator.allOf(listValidators))
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setCalendarConstraints(constraintsBuilder.build())
            .setTitleText(R.string.go_to)
            .setSelection(toDate)
            .build()
        datePicker.show(requireActivity().supportFragmentManager, "date-picker")
        datePicker.addOnPositiveButtonClickListener { time: Long ->
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US).withZone(ZoneOffset.UTC)
            val jumpTo = formatter.format(Instant.ofEpochMilli(time))
            urlBuilder.jumpTo = jumpTo
            mAdapter?.refresh()
        }
    }

    private fun takeCheckedInfo() = tracker.getAndClearSelection()

    private fun checkedSize() = tracker.selectionSize
}
