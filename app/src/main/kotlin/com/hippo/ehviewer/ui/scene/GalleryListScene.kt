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

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_IMAGE_SEARCH
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_NORMAL
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_SUBSCRIPTION
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_TAG
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_TOPLIST
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_WHATS_HOT
import com.hippo.ehviewer.client.exception.CloudflareBypassException
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.dao.QuickSearch
import com.hippo.ehviewer.databinding.DrawerListRvBinding
import com.hippo.ehviewer.databinding.ItemDrawerListBinding
import com.hippo.ehviewer.databinding.SceneGalleryListBinding
import com.hippo.ehviewer.ui.WebViewActivity
import com.hippo.ehviewer.ui.doGalleryInfoAction
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.legacy.BringOutTransition
import com.hippo.ehviewer.ui.legacy.EditTextDialogBuilder
import com.hippo.ehviewer.ui.legacy.FabLayout
import com.hippo.ehviewer.ui.legacy.HandlerDrawable
import com.hippo.ehviewer.ui.legacy.LayoutManagerUtils.firstVisibleItemPosition
import com.hippo.ehviewer.ui.legacy.ViewTransition
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.getParcelableCompat
import com.hippo.ehviewer.util.getValue
import com.hippo.ehviewer.util.lazyMut
import com.hippo.ehviewer.util.setValue
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import moe.tarsin.coroutines.runSuspendCatching
import rikka.core.res.resolveColor

class VMStorage1 : ViewModel() {
    var urlBuilder = ListUrlBuilder()
    val dataFlow = Pager(PagingConfig(25)) {
        object : PagingSource<String, BaseGalleryInfo>() {
            override fun getRefreshKey(state: PagingState<String, BaseGalleryInfo>): String? = null
            override suspend fun load(params: LoadParams<String>) = withIOContext {
                if (urlBuilder.mode == MODE_TOPLIST) {
                    // TODO: Since we know total pages, let pager support jump
                    val key = (params.key ?: urlBuilder.mJumpTo ?: "0").toInt()
                    val prev = (key - 1).takeIf { it > 0 }
                    val next = (key + 1).takeIf { it < TOPLIST_PAGES }
                    runSuspendCatching {
                        urlBuilder.setJumpTo(key)
                        EhEngine.getGalleryList(urlBuilder.build())
                    }.onFailure {
                        return@withIOContext LoadResult.Error(it)
                    }.onSuccess {
                        return@withIOContext LoadResult.Page(it.galleryInfoList, prev?.toString(), next?.toString())
                    }
                }
                when (params) {
                    is LoadParams.Prepend -> urlBuilder.setIndex(params.key, isNext = false)
                    is LoadParams.Append -> urlBuilder.setIndex(params.key, isNext = true)
                    is LoadParams.Refresh -> {
                        val key = params.key
                        if (key.isNullOrBlank()) {
                            if (urlBuilder.mJumpTo != null) {
                                urlBuilder.mNext ?: urlBuilder.setIndex("2", true)
                            }
                        } else {
                            urlBuilder.setIndex(key, false)
                        }
                    }
                }
                val r = runSuspendCatching {
                    if (MODE_IMAGE_SEARCH == urlBuilder.mode) {
                        EhEngine.imageSearch(
                            File(urlBuilder.imagePath!!),
                            urlBuilder.isUseSimilarityScan,
                            urlBuilder.isOnlySearchCovers,
                        )
                    } else {
                        val url = urlBuilder.build()
                        EhEngine.getGalleryList(url)
                    }
                }.onFailure {
                    return@withIOContext LoadResult.Error(it)
                }.getOrThrow()
                urlBuilder.mJumpTo = null
                LoadResult.Page(r.galleryInfoList, r.prev, r.next)
            }
        }
    }.flow.cachedIn(viewModelScope)
}

class GalleryListScene : SearchBarScene() {
    private val vm: VMStorage1 by viewModels()
    private var mUrlBuilder by lazyMut { vm::urlBuilder }
    private var mIsTopList = false

    private val stateBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            when (mState) {
                State.NORMAL -> error("SearchStateOnBackPressedCallback should not be enabled on $mState")
                State.SIMPLE_SEARCH, State.SEARCH -> setState(State.NORMAL)
                State.SEARCH_SHOW_LIST -> setState(State.SEARCH)
            }
        }
    }
    private var _binding: SceneGalleryListBinding? = null
    private val binding get() = _binding!!
    private var mViewTransition: ViewTransition? = null
    private var mAdapter: GalleryAdapter? = null
    lateinit var mQuickSearchList: MutableList<QuickSearch>
    private var mHideActionFabSlop = 0
    private var mState = State.NORMAL

    override val fabLayout get() = binding.fabLayout
    override val fastScroller get() = binding.fastScroller
    override val recyclerView get() = binding.recyclerView

    private fun handleArgs(args: Bundle?) {
        val action = args?.getString(KEY_ACTION) ?: ACTION_HOMEPAGE
        mUrlBuilder = when (action) {
            ACTION_HOMEPAGE -> ListUrlBuilder()
            ACTION_SUBSCRIPTION -> ListUrlBuilder(MODE_SUBSCRIPTION)
            ACTION_WHATS_HOT -> ListUrlBuilder(MODE_WHATS_HOT)
            ACTION_TOP_LIST -> ListUrlBuilder(MODE_TOPLIST, mKeyword = Settings.recentToplist)
            ACTION_LIST_URL_BUILDER -> args?.getParcelableCompat<ListUrlBuilder>(KEY_LIST_URL_BUILDER)?.copy() ?: ListUrlBuilder()
            else -> throw IllegalStateException("Wrong KEY_ACTION:${args?.getString(KEY_ACTION)} when handle args!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            onInit()
        } else {
            onRestore(savedInstanceState)
        }
    }

    override fun onResume() {
        super.onResume()
        mAdapter?.type = Settings.listMode
    }

    private fun onInit() {
        handleArgs(arguments)
    }

    private fun onRestore(savedInstanceState: Bundle) {
        mUrlBuilder = savedInstanceState.getParcelableCompat(KEY_LIST_URL_BUILDER)!!
        mState = savedInstanceState.getParcelableCompat(KEY_STATE)!!
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_LIST_URL_BUILDER, mUrlBuilder)
        outState.putParcelable(KEY_STATE, mState)
    }

    private fun setSearchBarHint() {
        setSearchBarHint(getString(if (EhUtils.isExHentai) R.string.gallery_list_search_bar_hint_exhentai else R.string.gallery_list_search_bar_hint_e_hentai))
    }

    // Update search bar title, drawer checked item
    private fun onUpdateUrlBuilder() {
        _binding ?: return
        var keyword = mUrlBuilder.keyword
        val category = mUrlBuilder.category
        val mode = mUrlBuilder.mode
        mIsTopList = mode == MODE_TOPLIST

        // Update normal search mode and category
        binding.searchLayout.setSearchMyTags(mode == MODE_SUBSCRIPTION)
        if (category != EhUtils.NONE) {
            binding.searchLayout.setCategory(category)
        }

        // Update search edit text
        if (!mIsTopList) {
            if (mode == MODE_TAG) {
                keyword = wrapTagKeyword(keyword!!)
            }
            setSearchBarText(keyword)
        }

        // Update title
        var title = requireContext().getSuitableTitleForUrlBuilder(mUrlBuilder, true)
        if (null == title) {
            title = resources.getString(R.string.search)
        }
        setTitle(title)
    }

    private val dialogState = DialogState()

    private val SceneGalleryListBinding.fastScroller get() = contentLayout.fastScroller
    private val SceneGalleryListBinding.refreshLayout get() = contentLayout.refreshLayout
    private val SceneGalleryListBinding.recyclerView get() = contentLayout.recyclerView
    private val SceneGalleryListBinding.progress get() = contentLayout.progress
    private val SceneGalleryListBinding.tip get() = contentLayout.tip

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?,
    ): ViewBinding {
        _binding = SceneGalleryListBinding.inflate(inflater, container)
        requireActivity().onBackPressedDispatcher.addCallback(stateBackPressedCallback)
        mHideActionFabSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop
        mViewTransition = BringOutTransition(binding.contentLayout.contentView, binding.searchLayout)
        binding.searchLayout.consumeWindowInsets = false
        binding.searchLayout.setViewTreeViewModelStoreOwner(this)
        mAdapter = GalleryAdapter(
            binding.recyclerView,
            true,
            { info ->
                navAnimated(
                    R.id.galleryDetailScene,
                    bundleOf(GalleryDetailScene.KEY_ARGS to GalleryInfoArgs(info)),
                )
            },
            { info ->
                lifecycleScope.launchIO {
                    dialogState.doGalleryInfoAction(info, requireContext())
                }
            },
        ).also { adapter ->
            viewLifecycleOwner.lifecycleScope.launch {
                val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.big_sad_pandroid)!!
                drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                binding.tip.setCompoundDrawables(null, drawable, null, null)
                binding.tip.setOnClickListener { mAdapter?.retry() }
                binding.refreshLayout.setOnRefreshListener {
                    mUrlBuilder.setIndex(null, true)
                    mUrlBuilder.mJumpTo = null
                    mAdapter?.refresh()
                }
                val transition = ViewTransition(binding.refreshLayout, binding.progress, binding.tip)
                val empty = getString(R.string.gallery_list_empty_hit)
                launch {
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
                                if (state.error.cause is CloudflareBypassException) {
                                    dialogState.awaitPermissionOrCancel(title = R.string.cloudflare_bypass_failed) {
                                        Text(text = stringResource(id = R.string.open_in_webview))
                                    }
                                    navAnimated(R.id.webView, bundleOf(WebViewActivity.KEY_URL to EhUrl.host))
                                }
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
                Settings.needSignInFlow.first { !it }
                vm.dataFlow.collectLatest {
                    adapter.submitData(it)
                }
            }
        }
        binding.recyclerView.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {}
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy >= mHideActionFabSlop) {
                        hideActionFab()
                    } else if (dy <= -mHideActionFabSlop / 2) {
                        showActionFab()
                    }
                }
            },
        )
        binding.fastScroller.attachToRecyclerView(binding.recyclerView)
        binding.fastScroller.setHandlerDrawable(HandlerDrawable().apply { setColor(inflater.context.theme.resolveColor(androidx.appcompat.R.attr.colorPrimary)) })
        setOnApplySearch { query: String? ->
            onApplySearch(query)
        }
        setSearchBarHint()
        setSuggestionProvider { text ->
            GalleryDetailUrlParser.parse(text, false)?.run {
                GalleryDetailUrlSuggestion(gid, token)
            } ?: GalleryPageUrlParser.parse(text, false)?.run {
                GalleryPageUrlSuggestion(gid, pToken, page)
            }
        }
        val isPop = mUrlBuilder.mode == MODE_WHATS_HOT
        binding.fabLayout.apply {
            val fab = listOf<Pair<ImageVector, () -> Unit>>(
                Icons.Default.Refresh to {
                    mUrlBuilder.setIndex(null, true)
                    mUrlBuilder.mJumpTo = null
                    mAdapter?.refresh()
                },
                Icons.AutoMirrored.Filled.Redo to {
                    showGoToDialog()
                },
                Icons.Default.ArrowDownward to {
                    if (mIsTopList) {
                        mUrlBuilder.mJumpTo = "${TOPLIST_PAGES - 1}"
                        mAdapter?.refresh()
                    } else {
                        mUrlBuilder.setIndex("1", false)
                        mAdapter?.refresh()
                    }
                },
            )
            secondFabs = if (!isPop) {
                fab
            } else {
                fab.take(1)
            }
        }
        binding.fabLayout.addOnExpandStateListener {
            if (it) {
                lockDrawer()
            } else {
                unlockDrawer()
            }
        }

        // Update list url builder
        onUpdateUrlBuilder()

        // Restore state
        val newState = mState
        mState = State.NORMAL
        setState(newState, false)
        return binding
    }

    override fun onRelease() {
        super.onRelease()
        stateBackPressedCallback.remove()
        binding.recyclerView.stopScroll()
        _binding = null
        mAdapter = null
        mViewTransition = null
    }

    private fun showQuickSearchTipDialog() {
        val context = context ?: return
        val builder = BaseDialogBuilder(context)
        builder.setMessage(R.string.add_quick_search_tip)
        builder.setTitle(R.string.readme)
        builder.show()
    }

    private fun showAddQuickSearchDialog(
        adapter: QsDrawerAdapter,
        recyclerView: RecyclerView,
        tip: TextView,
    ) {
        val context = context ?: return
        if (mAdapter!!.itemCount == 0) return

        // Can't add image search as quick search
        if (MODE_IMAGE_SEARCH == mUrlBuilder.mode) {
            showTip(R.string.image_search_not_quick_search, LENGTH_LONG)
            return
        }
        val gi = mAdapter!!.peek(binding.recyclerView.layoutManager!!.firstVisibleItemPosition)!!
        val next = gi.gid + 1

        // Check duplicate
        for (q in mQuickSearchList) {
            if (mUrlBuilder.equalsQuickSearch(q)) {
                val nextStr = q.name.substringAfterLast('@', "").takeIf { it.isNotEmpty() }
                if (nextStr?.toLongOrNull() == next) {
                    showTip(getString(R.string.duplicate_quick_search, q.name), LENGTH_LONG)
                    return
                }
            }
        }
        val builder = EditTextDialogBuilder(
            context,
            context.getSuitableTitleForUrlBuilder(mUrlBuilder, false),
            getString(R.string.quick_search),
        )
        builder.setTitle(R.string.add_quick_search_dialog_title)
        builder.setPositiveButton(android.R.string.ok, null)
        // TODO: It's ugly
        val checked = booleanArrayOf(Settings.qSSaveProgress)
        val hint = arrayOf(getString(R.string.save_progress))
        builder.setMultiChoiceItems(hint, checked) { _, which, isChecked -> checked[which] = isChecked }
        val dialog = builder.show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            lifecycleScope.launchIO {
                var text = builder.text.trim { it <= ' ' }

                // Check name empty
                if (text.isEmpty()) {
                    withUIContext {
                        builder.setError(getString(R.string.name_is_empty))
                    }
                    return@launchIO
                }
                if (checked[0]) {
                    text += "@$next"
                    Settings.qSSaveProgress = true
                } else {
                    Settings.qSSaveProgress = false
                }

                // Check name duplicate
                for ((_, name) in mQuickSearchList) {
                    if (text == name) {
                        withUIContext {
                            builder.setError(getString(R.string.duplicate_name))
                        }
                        return@launchIO
                    }
                }
                builder.setError(null)
                dialog.dismiss()
                val quickSearch = mUrlBuilder.toQuickSearch(text)
                quickSearch.position = mQuickSearchList.size
                EhDB.insertQuickSearch(quickSearch)
                mQuickSearchList.add(quickSearch)
                withUIContext {
                    adapter.notifyItemInserted(mQuickSearchList.size - 1)
                    if (0 == mQuickSearchList.size) {
                        tip.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        tip.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onCreateDrawerView(inflater: LayoutInflater): View {
        val drawerBinding = DrawerListRvBinding.inflate(inflater)
        drawerBinding.recyclerViewDrawer.layoutManager = LinearLayoutManager(context)
        val qsDrawerAdapter = QsDrawerAdapter(inflater)
        qsDrawerAdapter.setHasStableIds(true)
        drawerBinding.recyclerViewDrawer.adapter = qsDrawerAdapter
        if (!mIsTopList) {
            val itemTouchHelper =
                ItemTouchHelper(GalleryListQSItemTouchHelperCallback(qsDrawerAdapter))
            itemTouchHelper.attachToRecyclerView(drawerBinding.recyclerViewDrawer)
            drawerBinding.tip.visibility = View.VISIBLE
            drawerBinding.recyclerViewDrawer.visibility = View.GONE
        }
        lifecycleScope.launchIO {
            mQuickSearchList = EhDB.getAllQuickSearch() as MutableList<QuickSearch>
            if (mQuickSearchList.isNotEmpty()) {
                withUIContext {
                    drawerBinding.tip.visibility = View.GONE
                    drawerBinding.recyclerViewDrawer.visibility = View.VISIBLE
                }
            }
        }
        drawerBinding.tip.setText(R.string.quick_search_tip)
        if (mIsTopList) {
            drawerBinding.toolbar.setTitle(R.string.toplist)
        } else {
            drawerBinding.toolbar.setTitle(R.string.quick_search)
            if (mUrlBuilder.mode != MODE_WHATS_HOT) {
                drawerBinding.toolbar.inflateMenu(R.menu.drawer_gallery_list)
                drawerBinding.toolbar.setOnMenuItemClickListener { item: MenuItem ->
                    val id = item.itemId
                    if (id == R.id.action_add) {
                        showAddQuickSearchDialog(
                            qsDrawerAdapter,
                            drawerBinding.recyclerViewDrawer,
                            drawerBinding.tip,
                        )
                    } else if (id == R.id.action_help) {
                        showQuickSearchTipDialog()
                    }
                    true
                }
            }
        }
        return drawerBinding.root
    }

    private fun showGoToDialog() {
        mAdapter ?: return
        if (mIsTopList) {
            val page = mUrlBuilder.mJumpTo?.toIntOrNull() ?: 0
            val title = getString(R.string.go_to)
            val hint = getString(R.string.go_to_hint, page + 1, TOPLIST_PAGES)
            lifecycleScope.launch {
                val text = dialogState.awaitInputText(
                    title = title,
                    hint = hint,
                    isNumber = true,
                ) { oriText ->
                    val text = oriText.trim()
                    val goTo = runCatching {
                        text.toInt() - 1
                    }.onFailure {
                        return@awaitInputText getString(R.string.error_invalid_number)
                    }.getOrThrow()
                    if (goTo !in 0..<TOPLIST_PAGES) {
                        getString(R.string.error_out_of_range)
                    } else {
                        null
                    }
                }.trim().toInt() - 1
                mUrlBuilder.setJumpTo(text)
                mAdapter?.refresh()
            }
        } else {
            val local = LocalDateTime.of(2007, 3, 21, 0, 0)
            val fromDate =
                local.atZone(ZoneId.ofOffset("UTC", ZoneOffset.UTC)).toInstant().toEpochMilli()
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
            datePicker.addOnPositiveButtonClickListener { time ->
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US).withZone(ZoneOffset.UTC)
                val jumpTo = formatter.format(Instant.ofEpochMilli(time))
                mUrlBuilder.mJumpTo = jumpTo
                mAdapter?.refresh()
            }
        }
    }

    private fun showSearchFab(delay: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            delay(delay)
            showSearchFab = true
        }
    }

    private fun selectSearchFab(animation: Boolean) {
        binding.fabLayout.hide()
        showSearchFab(FabLayout.ANIMATE_TIME)
    }

    private fun selectActionFab(animation: Boolean) {
        showSearchFab = false
        binding.fabLayout.show()
    }

    override fun onSearchViewExpanded() {
        if (mState == State.NORMAL) {
            binding.fabLayout.hide()
        } else {
            showSearchFab = false
            stateBackPressedCallback.isEnabled = false
        }
        super.onSearchViewExpanded()
    }

    override fun onSearchViewHidden() {
        super.onSearchViewHidden()
        if (mState == State.NORMAL) {
            lifecycleScope.launchUI {
                delay(SEARCH_VIEW_ANIMATE_TIME)
                _binding ?: return@launchUI
                binding.fabLayout.show()
            }
        } else {
            showSearchFab(SEARCH_VIEW_ANIMATE_TIME)
            stateBackPressedCallback.isEnabled = true
        }
    }

    private fun showActionFab() {
        _binding ?: return
        if (State.NORMAL == mState) {
            binding.fabLayout.show()
        }
    }

    private fun hideActionFab() {
        _binding ?: return
        if (State.NORMAL == mState) {
            binding.fabLayout.hide()
        }
    }

    private fun setState(state: State, animation: Boolean = true) {
        _binding ?: return
        if (null == mViewTransition) {
            return
        }
        if (mState != state) {
            val oldState = mState
            mState = state
            onStateChange(state)
            when (oldState) {
                State.NORMAL -> when (state) {
                    State.SIMPLE_SEARCH -> {
                        selectSearchFab(animation)
                    }
                    State.SEARCH -> {
                        mViewTransition!!.showView(1, animation)
                        selectSearchFab(animation)
                    }
                    State.SEARCH_SHOW_LIST -> {
                        mViewTransition!!.showView(1, animation)
                        selectSearchFab(animation)
                    }
                    else -> error("Unreachable!!!")
                }
                State.SIMPLE_SEARCH -> when (state) {
                    State.NORMAL -> selectActionFab(animation)
                    State.SEARCH -> mViewTransition!!.showView(1, animation)
                    State.SEARCH_SHOW_LIST -> mViewTransition!!.showView(1, animation)
                    else -> error("Unreachable!!!")
                }
                State.SEARCH, State.SEARCH_SHOW_LIST -> if (state == State.NORMAL) {
                    mViewTransition!!.showView(0, animation)
                    selectActionFab(animation)
                } else if (state == State.SIMPLE_SEARCH) {
                    mViewTransition!!.showView(0, animation)
                }
            }
        }
    }

    @Composable
    override fun TrailingIcon() {
        dialogState.Intercept()
        IconButton(onClick = { openSideSheet() }) {
            Icon(imageVector = Icons.Outlined.Bookmarks, contentDescription = stringResource(id = R.string.quick_search))
        }
        IconButton(onClick = {
            if (mState == State.NORMAL) {
                setState(State.SEARCH)
            } else {
                setState(State.NORMAL)
            }
        }) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
        }
    }

    private fun onApplySearch(query: String?) {
        _binding ?: return
        lifecycleScope.launchIO {
            val builder = ListUrlBuilder()
            val oldMode = mUrlBuilder.mode
            if (mState == State.SEARCH || mState == State.SEARCH_SHOW_LIST) {
                try {
                    binding.searchLayout.formatListUrlBuilder(builder, query)
                } catch (e: EhException) {
                    showTip(e.message, LENGTH_LONG)
                    return@launchIO
                }
            } else {
                // If it's MODE_SUBSCRIPTION, keep it
                val newMode = if (oldMode == MODE_SUBSCRIPTION) MODE_SUBSCRIPTION else MODE_NORMAL
                builder.mode = newMode
                builder.keyword = query
            }
            withUIContext {
                when (oldMode) {
                    MODE_TOPLIST, MODE_WHATS_HOT -> {
                        // Wait for search view to hide
                        delay(300)
                        navAnimated(R.id.galleryListScene, builder.toStartArgs())
                    }

                    else -> {
                        mUrlBuilder = builder
                        onUpdateUrlBuilder()
                        mAdapter?.refresh()
                    }
                }
                setState(State.NORMAL)
            }
        }
    }

    private fun onStateChange(newState: State) {
        stateBackPressedCallback.isEnabled = newState != State.NORMAL
        if (newState == State.NORMAL || newState == State.SIMPLE_SEARCH) {
            unlockDrawer()
        } else {
            lockDrawer()
        }
    }

    private class QsDrawerHolder(val binding: ItemDrawerListBinding) : RecyclerView.ViewHolder(binding.root)

    private inner class QsDrawerAdapter(private val mInflater: LayoutInflater) :
        RecyclerView.Adapter<QsDrawerHolder>() {
        private val toplists = resources.getStringArray(R.array.toplist_entries)
        private val keywords = resources.getStringArray(R.array.toplist_values)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QsDrawerHolder {
            val holder = QsDrawerHolder(ItemDrawerListBinding.inflate(mInflater, parent, false))
            holder.itemView.setOnClickListener {
                if (mUrlBuilder.mode == MODE_WHATS_HOT) {
                    val q = mQuickSearchList[holder.bindingAdapterPosition]
                    navAnimated(R.id.galleryListScene, ListUrlBuilder().apply { set(q) }.toStartArgs())
                } else {
                    if (mIsTopList) {
                        val keyword = keywords[holder.bindingAdapterPosition]
                        Settings.recentToplist = keyword
                        mUrlBuilder.keyword = keyword
                        mUrlBuilder.mJumpTo = null
                    } else {
                        val q = mQuickSearchList[holder.bindingAdapterPosition]
                        mUrlBuilder.set(q)
                    }
                    onUpdateUrlBuilder()
                    mAdapter?.refresh()
                }
                setState(State.NORMAL)
                closeSideSheet()
            }
            return holder
        }

        override fun onBindViewHolder(holder: QsDrawerHolder, position: Int) {
            holder.binding.run {
                if (!mIsTopList) {
                    text.text = mQuickSearchList[position].name
                } else {
                    text.text = toplists[position]
                    option.visibility = View.GONE
                }
            }
        }

        override fun getItemId(position: Int): Long {
            if (mIsTopList) {
                return position.toLong()
            }
            return mQuickSearchList[position].id!!
        }

        override fun getItemCount(): Int {
            return if (!mIsTopList) mQuickSearchList.size else 4
        }
    }

    private abstract inner class UrlSuggestion : Suggestion() {
        override val keyword = getString(R.string.gallery_list_search_bar_open_gallery)
        override val canOpenDirectly = true
        override fun onClick() {
            navAnimated(destination, args)
            if (mState == State.SIMPLE_SEARCH) {
                setState(State.NORMAL)
            } else if (mState == State.SEARCH_SHOW_LIST) {
                setState(State.SEARCH)
            }
        }
        abstract val destination: Int
        abstract val args: Bundle
    }

    private inner class GalleryDetailUrlSuggestion(
        gid: Long,
        token: String,
    ) : UrlSuggestion() {
        override val destination = R.id.galleryDetailScene
        override val args = bundleOf(GalleryDetailScene.KEY_ARGS to TokenArgs(gid, token))
    }

    private inner class GalleryPageUrlSuggestion(
        gid: Long,
        pToken: String,
        page: Int,
    ) : UrlSuggestion() {
        override val destination = R.id.progressScene
        override val args = bundleOf(
            ProgressFragment.KEY_GID to gid,
            ProgressFragment.KEY_PTOKEN to pToken,
            ProgressFragment.KEY_PAGE to page,
        )
    }

    private inner class GalleryListQSItemTouchHelperCallback(private val mAdapter: QsDrawerAdapter) :
        ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
        ) = makeMovementFlags(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT,
        )

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            val fromPosition = viewHolder.bindingAdapterPosition
            val toPosition = target.bindingAdapterPosition
            if (fromPosition == toPosition) {
                return false
            }
            val item = mQuickSearchList.removeAt(fromPosition)
            mQuickSearchList.add(toPosition, item)
            mAdapter.notifyItemMoved(fromPosition, toPosition)
            lifecycleScope.launchIO {
                val range = if (fromPosition < toPosition) fromPosition..toPosition else toPosition..fromPosition
                val list = mQuickSearchList.slice(range)
                list.zip(range).forEach { it.first.position = it.second }
                EhDB.updateQuickSearch(list)
            }
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.bindingAdapterPosition
            val quickSearch = mQuickSearchList[position]
            BaseDialogBuilder(context!!)
                .setMessage(getString(R.string.delete_quick_search, quickSearch.name))
                .setPositiveButton(R.string.delete) { _, _ ->
                    lifecycleScope.launchIO {
                        EhDB.deleteQuickSearch(quickSearch)
                        mQuickSearchList.removeAt(position)
                        withUIContext {
                            mAdapter.notifyItemRemoved(position)
                        }
                    }
                }.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }.setOnCancelListener {
                    mAdapter.notifyItemChanged(position)
                }.show()
        }
    }

    companion object {
        const val KEY_ACTION = "action"
        const val ACTION_HOMEPAGE = "action_homepage"
        const val ACTION_SUBSCRIPTION = "action_subscription"
        const val ACTION_WHATS_HOT = "action_whats_hot"
        const val ACTION_TOP_LIST = "action_top_list"
        const val ACTION_LIST_URL_BUILDER = "action_list_url_builder"
        const val KEY_LIST_URL_BUILDER = "list_url_builder"
        const val KEY_STATE = "state"
        fun ListUrlBuilder.toStartArgs() = bundleOf(
            KEY_ACTION to ACTION_LIST_URL_BUILDER,
            KEY_LIST_URL_BUILDER to this,
        )
    }
}

private const val TOPLIST_PAGES = 200

@Parcelize
enum class State : Parcelable {
    NORMAL,
    SIMPLE_SEARCH,
    SEARCH,
    SEARCH_SHOW_LIST,
}

private fun Context.getSuitableTitleForUrlBuilder(
    urlBuilder: ListUrlBuilder,
    appName: Boolean,
): String? {
    val keyword = urlBuilder.keyword
    val category = urlBuilder.category
    val mode = urlBuilder.mode
    return if (mode == MODE_WHATS_HOT) {
        getString(R.string.whats_hot)
    } else if (!keyword.isNullOrEmpty()) {
        when (mode) {
            MODE_TOPLIST -> {
                when (keyword) {
                    "11" -> getString(R.string.toplist_alltime)
                    "12" -> getString(R.string.toplist_pastyear)
                    "13" -> getString(R.string.toplist_pastmonth)
                    "15" -> getString(R.string.toplist_yesterday)
                    else -> null
                }
            }

            MODE_TAG -> {
                val canTranslate = Settings.showTagTranslations && EhTagDatabase.isTranslatable(this) && EhTagDatabase.initialized
                wrapTagKeyword(keyword, canTranslate)
            }
            else -> keyword
        }
    } else if (category == EhUtils.NONE && urlBuilder.advanceSearch == -1) {
        when (mode) {
            MODE_NORMAL -> getString(if (appName) R.string.app_name else R.string.homepage)
            MODE_SUBSCRIPTION -> getString(R.string.subscription)
            else -> null
        }
    } else if (category.countOneBits() == 1) {
        EhUtils.getCategory(category)
    } else {
        null
    }
}
