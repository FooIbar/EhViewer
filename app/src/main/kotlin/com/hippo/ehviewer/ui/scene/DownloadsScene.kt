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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachReversed
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
import arrow.core.partially1
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hippo.ehviewer.EhApplication.Companion.imageCache
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.Settings.detailSize
import com.hippo.ehviewer.Settings.listThumbSize
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.coil.read
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.databinding.DrawerListRvBinding
import com.hippo.ehviewer.databinding.ItemDownloadBinding
import com.hippo.ehviewer.databinding.ItemDrawerListBinding
import com.hippo.ehviewer.databinding.SceneDownloadBinding
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.DownloadService
import com.hippo.ehviewer.download.DownloadService.Companion.clear
import com.hippo.ehviewer.download.downloadDir
import com.hippo.ehviewer.ktbuilder.imageRequest
import com.hippo.ehviewer.ui.confirmRemoveDownload
import com.hippo.ehviewer.ui.legacy.AutoStaggeredGridLayoutManager
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.legacy.CheckBoxDialogBuilder
import com.hippo.ehviewer.ui.legacy.EditTextDialogBuilder
import com.hippo.ehviewer.ui.legacy.FabLayout
import com.hippo.ehviewer.ui.legacy.FabLayout.OnClickFabListener
import com.hippo.ehviewer.ui.legacy.HandlerDrawable
import com.hippo.ehviewer.ui.legacy.STRATEGY_MIN_SIZE
import com.hippo.ehviewer.ui.legacy.ViewTransition
import com.hippo.ehviewer.ui.main.DEFAULT_ASPECT
import com.hippo.ehviewer.ui.main.requestOf
import com.hippo.ehviewer.ui.navToReader
import com.hippo.ehviewer.ui.setMD3Content
import com.hippo.ehviewer.ui.tools.CropDefaults
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.LongList
import com.hippo.ehviewer.util.containsIgnoreCase
import com.hippo.ehviewer.util.sendTo
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withNonCancellableContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.pxToDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import rikka.core.res.resolveColor

@SuppressLint("RtlHardcoded")
class DownloadsScene :
    SearchBarScene(),
    OnClickFabListener {
    /*---------------
     Whole life cycle
     ---------------*/
    private var mKeyword: String? = null
    private var mLabel: String? = null
    private val mList get() = mAdapter?.currentList

    /*---------------
     View life cycle
     ---------------*/
    private var _binding: SceneDownloadBinding? = null
    private val binding get() = _binding!!
    private var mViewTransition: ViewTransition? = null
    private var mAdapter: DownloadAdapter? = null
    private var mItemTouchHelper: ItemTouchHelper? = null
    private val tracker get() = mAdapter!!.tracker!!
    private var mGid = -1L
    private var mLabelAdapter: DownloadLabelAdapter? = null
    private lateinit var mLabels: MutableList<String>
    private var mType = -1

    private val dialogState = DialogState()

    override val fabLayout get() = binding.fabLayout
    override val fastScroller get() = binding.fastScroller
    override val recyclerView get() = binding.recyclerView

    private fun initLabels() {
        context ?: return
        val listLabel = DownloadManager.labelList
        // Add "All" and "Default" label names
        mLabels = arrayListOf(
            getString(R.string.download_all),
            getString(R.string.default_download_label_name),
        )
        listLabel.forEach {
            mLabels.add(it.label)
        }
    }

    private fun handleArguments(args: Bundle?): Boolean {
        if (null == args) {
            return false
        }
        if (ACTION_CLEAR_DOWNLOAD_SERVICE == args.getString(KEY_ACTION)) {
            clear()
        }
        val gid = args.getLong(KEY_GID, -1L)
        if (-1L != gid) {
            val info = DownloadManager.getDownloadInfo(gid)
            if (null != info) {
                mLabel = info.label
                mGid = gid
                return true
            }
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            onInit()
        } else {
            onRestore(savedInstanceState)
        }
    }

    private fun filter(info: DownloadInfo) = info.run {
        (mType == -1 || state == mType) &&
            mKeyword?.let {
                title.containsIgnoreCase(it) || titleJpn.containsIgnoreCase(it) ||
                    uploader.containsIgnoreCase(it)
            } ?: true
    }

    private fun updateInfoList() {
        mAdapter?.run {
            val list = when (mLabel) {
                null -> DownloadManager.allInfoList
                getString(R.string.default_download_label_name) -> DownloadManager.defaultInfoList
                else -> DownloadManager.getLabelDownloadInfoList(mLabel)
                    ?: DownloadManager.allInfoList.also { mLabel = null }
            }.mapNotNull { it.takeIf(::filter)?.copy(downloadInfo = it.downloadInfo.copy()) }
            submitList(list) {
                // This may be called after view destroyed
                _binding ?: return@submitList
                if (mGid != -1L) {
                    val position = list.indexOfFirst { it.gid == mGid }
                    if (position != -1) {
                        binding.recyclerView.scrollToPosition(position)
                    }
                    mGid = -1L
                }
                updateView()
            }
        }
    }

    private fun updateForLabel() {
        updateInfoList()
        Settings.recentDownloadLabel = mLabel
    }

    private fun updateTitle() {
        val title = getString(R.string.scene_download_title, mLabel ?: getString(R.string.download_all))
        setTitle(title)
        setSearchBarHint(getString(R.string.search_bar_hint, title))
    }

    private fun onInit() {
        if (!handleArguments(arguments)) {
            mLabel = Settings.recentDownloadLabel
        }
    }

    private fun onRestore(savedInstanceState: Bundle) {
        mLabel = savedInstanceState.getString(KEY_LABEL)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_LABEL, mLabel)
        mAdapter?.let { tracker.saveSelection(outState) }
    }

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?,
    ): ViewBinding {
        _binding = SceneDownloadBinding.inflate(inflater, container)
        binding.run {
            mViewTransition = ViewTransition(content, tip)
            val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.big_download)
            drawable!!.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            tip.setCompoundDrawables(null, drawable, null, null)
            mAdapter = DownloadAdapter()
            mAdapter!!.setHasStableIds(true)
            recyclerView.adapter = mAdapter
            mAdapter!!.tracker = GallerySelectionTracker(
                "download-selection",
                recyclerView,
                { mList!! },
                { (this as DownloadHolder).itemId },
            ).apply {
                addCustomChoiceListener({
                    binding.fabLayout.isExpanded = true
                    lockDrawer()
                }) {
                    binding.fabLayout.isExpanded = false
                    unlockDrawer()
                }
                restoreSelection(savedInstanceState)
            }
            val layoutManager = AutoStaggeredGridLayoutManager(0, StaggeredGridLayoutManager.VERTICAL)
            layoutManager.setColumnSize(
                resources.getDimensionPixelOffset(
                    when (detailSize) {
                        0 -> R.dimen.gallery_list_column_width_long
                        1 -> R.dimen.gallery_list_column_width_short
                        else -> throw IllegalStateException("Unexpected value: $detailSize")
                    },
                ),
            )
            layoutManager.setStrategy(STRATEGY_MIN_SIZE)
            layoutManager.supportsPredictiveItemAnimations = false
            recyclerView.layoutManager = layoutManager
            val interval = resources.getDimensionPixelOffset(R.dimen.gallery_list_interval)
            val decoration = object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    outRect.set(0, interval / 2, 0, interval / 2)
                }
            }
            recyclerView.addItemDecoration(decoration)
            mItemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
                override fun isLongPressDragEnabled() = false
                override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                    return if (tracker.isInCustomChoice) {
                        0
                    } else {
                        makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT)
                    }
                }
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
                    val fromItem = mList!![fromPosition]
                    val toItem = mList!![toPosition]
                    val list = DownloadManager.moveDownload(fromItem, toItem)
                    updateInfoList()
                    lifecycleScope.launchIO {
                        EhDB.updateDownloadInfo(list)
                    }
                    return true
                }
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.bindingAdapterPosition
                    val info = mList!![position]
                    lifecycleScope.launchUI {
                        dialogState.confirmRemoveDownload(info) {
                            mAdapter!!.notifyItemChanged(position)
                        }
                    }
                }
            })
            mItemTouchHelper!!.attachToRecyclerView(recyclerView)
            fastScroller.attachToRecyclerView(recyclerView)
            val handlerDrawable = HandlerDrawable()
            handlerDrawable.setColor(theme.resolveColor(com.google.android.material.R.attr.colorPrimary))
            fastScroller.setHandlerDrawable(handlerDrawable)
            fabLayout.setExpanded(expanded = false, animation = false)
            fabLayout.addOnExpandListener { if (!it && tracker.isInCustomChoice) tracker.clearSelection() }
            fabLayout.setHidePrimaryFab(true)
            fabLayout.setAutoCancel(false)
            fabLayout.setOnClickFabListener(this@DownloadsScene)
            updateForLabel()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            DownloadManager.stateFlow().collectLatest {
                updateInfoList()
            }
        }
        setOnApplySearch {
            mKeyword = it.takeUnless { it.isEmpty() }
            updateInfoList()
        }
        return binding
    }

    override fun onSearchViewExpanded() {
        binding.fabLayout.hide()
        super.onSearchViewExpanded()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.stopScroll()
        mViewTransition = null
        mAdapter = null
        mLabelAdapter = null
        mItemTouchHelper = null
        _binding = null
    }

    @Composable
    override fun TrailingIcon() {
        dialogState.Intercept()
        var expanded by remember { mutableStateOf(false) }
        IconButton(onClick = { openSideSheet() }) {
            Icon(imageVector = Icons.AutoMirrored.Outlined.Label, contentDescription = stringResource(id = R.string.download_labels))
        }
        IconButton(onClick = { expanded = !expanded }) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
        }
        val states = stringArrayResource(id = R.array.download_state)
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.download_filter)) },
                onClick = {
                    expanded = false
                    lifecycleScope.launch {
                        val type = dialogState.showSingleChoice(states, mType)
                        if (type != mType) {
                            updateInfoList()
                        }
                    }
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.download_start_all)) },
                onClick = {
                    expanded = false
                    val activity = requireActivity()
                    val intent = Intent(activity, DownloadService::class.java)
                    intent.action = DownloadService.ACTION_START_ALL
                    ContextCompat.startForegroundService(activity, intent)
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.download_stop_all)) },
                onClick = {
                    expanded = false
                    lifecycleScope.launchIO {
                        DownloadManager.stopAllDownload()
                    }
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.download_reset_reading_progress)) },
                onClick = {
                    expanded = false
                    lifecycleScope.launchIO {
                        dialogState.awaitPermissionOrCancel(
                            confirmText = android.R.string.ok,
                            dismissText = android.R.string.cancel,
                        ) {
                            Text(text = stringResource(id = R.string.reset_reading_progress_message))
                        }
                        withNonCancellableContext {
                            DownloadManager.resetAllReadingProgress()
                        }
                    }
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.download_start_all_reversed)) },
                onClick = {
                    expanded = false
                    val list = mList ?: return@DropdownMenuItem
                    val activity = requireActivity()
                    val gidList = LongList()
                    list.fastForEachReversed { info ->
                        if (info.state != DownloadInfo.STATE_FINISH) {
                            gidList.add(info.gid)
                        }
                    }
                    val intent = Intent(activity, DownloadService::class.java)
                    intent.action = DownloadService.ACTION_START_RANGE
                    intent.putExtra(DownloadService.KEY_GID_LIST, gidList)
                    ContextCompat.startForegroundService(activity, intent)
                },
            )
        }
    }

    private fun updateView() {
        if (mViewTransition != null) {
            if (mList.isNullOrEmpty()) {
                mViewTransition!!.showView(1)
            } else {
                mViewTransition!!.showView(0)
            }
        }
        updateTitle()
    }

    override fun onCreateDrawerView(inflater: LayoutInflater): View {
        val drawerBinding = DrawerListRvBinding.inflate(inflater)
        drawerBinding.toolbar.setTitle(R.string.download_labels)
        drawerBinding.toolbar.inflateMenu(R.menu.drawer_download)
        drawerBinding.toolbar.setOnMenuItemClickListener { item: MenuItem ->
            val id = item.itemId
            if (id == R.id.action_add) {
                val builder = EditTextDialogBuilder(
                    requireContext(),
                    null,
                    getString(R.string.download_labels),
                )
                builder.setTitle(R.string.new_label_title)
                builder.setPositiveButton(android.R.string.ok, null)
                val dialog = builder.show()
                NewLabelDialogHelper(builder, dialog)
                return@setOnMenuItemClickListener true
            } else if (id == R.id.action_default_download_label) {
                val list = DownloadManager.labelList.map { it.label }.toTypedArray()
                val items = arrayOf(
                    getString(R.string.let_me_select),
                    getString(R.string.default_download_label_name),
                    *list,
                )
                BaseDialogBuilder(requireContext())
                    .setTitle(R.string.default_download_label)
                    .setItems(items) { _: DialogInterface?, which: Int ->
                        if (which == 0) {
                            Settings.hasDefaultDownloadLabel = false
                        } else {
                            Settings.hasDefaultDownloadLabel = true
                            val label: String? = if (which == 1) {
                                null
                            } else {
                                items[which]
                            }
                            Settings.defaultDownloadLabel = label
                        }
                    }.show()
                return@setOnMenuItemClickListener true
            }
            false
        }
        initLabels()
        mLabelAdapter = DownloadLabelAdapter(inflater)
        drawerBinding.recyclerViewDrawer.layoutManager = LinearLayoutManager(context)
        mLabelAdapter!!.setHasStableIds(true)
        val itemTouchHelper = ItemTouchHelper(DownloadLabelItemTouchHelperCallback())
        itemTouchHelper.attachToRecyclerView(drawerBinding.recyclerViewDrawer)
        drawerBinding.recyclerViewDrawer.adapter = mLabelAdapter
        return drawerBinding.root
    }

    fun onItemClick(position: Int): Boolean {
        val context = context ?: return false
        val list = mList ?: return false
        if (position < 0 || position >= list.size) {
            return false
        }
        context.navToReader(list[position].galleryInfo)
        return true
    }

    override fun onClickPrimaryFab(view: FabLayout, fab: FloatingActionButton) {
        if (tracker.isInCustomChoice) {
            tracker.clearSelection()
        }
    }

    override fun onClickSecondaryFab(view: FabLayout, fab: FloatingActionButton, position: Int) {
        val context = context
        val activity: Activity? = mainActivity
        if (null == context || null == activity) {
            return
        }
        if (0 == position) {
            tracker.selectAll()
        } else {
            val downloadInfoList = tracker.getAndClearSelection()
            val gidList = if (position in 1..3) {
                LongList(downloadInfoList.map(DownloadInfo::gid).toMutableList())
            } else {
                null
            }
            when (position) {
                1 -> {
                    // Start
                    val intent = Intent(activity, DownloadService::class.java)
                    intent.action = DownloadService.ACTION_START_RANGE
                    intent.putExtra(DownloadService.KEY_GID_LIST, gidList)
                    ContextCompat.startForegroundService(activity, intent)
                }

                2 -> {
                    // Stop
                    lifecycleScope.launchIO {
                        DownloadManager.stopRangeDownload(gidList!!)
                    }
                }

                3 -> {
                    // Delete
                    val builder = CheckBoxDialogBuilder(
                        context,
                        getString(R.string.download_remove_dialog_message_2, gidList!!.size),
                        getString(R.string.download_remove_dialog_check_text),
                        Settings.removeImageFiles,
                    )
                    val helper = DeleteRangeDialogHelper(
                        downloadInfoList,
                        gidList,
                        builder,
                    )
                    builder.setTitle(R.string.download_remove_dialog_title)
                        .setPositiveButton(android.R.string.ok, helper)
                        .show()
                }

                4 -> {
                    // Move
                    val labelRawList = DownloadManager.labelList
                    val labelList: MutableList<String> = ArrayList(labelRawList.size + 1)
                    labelList.add(getString(R.string.default_download_label_name))
                    labelRawList.forEach {
                        labelList.add(it.label)
                    }
                    val labels = labelList.toTypedArray()
                    val helper = MoveDialogHelper(labels, downloadInfoList)
                    BaseDialogBuilder(context)
                        .setTitle(R.string.download_move_dialog_title)
                        .setItems(labels, helper)
                        .show()
                }
            }
        }
    }

    private class DownloadLabelHolder(val binding: ItemDrawerListBinding) :
        RecyclerView.ViewHolder(binding.root)

    private inner class DownloadLabelAdapter(private val mInflater: LayoutInflater) :
        RecyclerView.Adapter<DownloadLabelHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadLabelHolder {
            val holder =
                DownloadLabelHolder(ItemDrawerListBinding.inflate(mInflater, parent, false))
            holder.itemView.setOnClickListener {
                val position = holder.bindingAdapterPosition
                val label = mLabels[position].takeUnless { position == 0 }
                if (mLabel != label) {
                    mKeyword = null
                    clearSearchBarText()
                    mLabel = label
                    updateForLabel()
                }
                closeSideSheet()
            }
            holder.binding.edit.setOnClickListener {
                val context = context
                val label = mLabels[holder.bindingAdapterPosition]
                if (context != null) {
                    val builder = EditTextDialogBuilder(
                        context,
                        label,
                        getString(R.string.download_labels),
                    )
                    builder.setTitle(R.string.rename_label_title)
                    builder.setPositiveButton(android.R.string.ok, null)
                    val dialog = builder.show()
                    RenameLabelDialogHelper(builder, dialog, label)
                }
            }
            return holder
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: DownloadLabelHolder, position: Int) {
            val label = mLabels[position]
            val list: List<DownloadInfo?>? = when (position) {
                0 -> {
                    DownloadManager.allInfoList
                }

                1 -> {
                    DownloadManager.defaultInfoList
                }

                else -> {
                    DownloadManager.getLabelDownloadInfoList(label)
                }
            }
            holder.binding.run {
                if (list != null) {
                    text.text = "$label [${list.size}]"
                } else {
                    text.text = label
                }
                if (position < LABEL_OFFSET) {
                    edit.visibility = View.GONE
                    option.visibility = View.GONE
                } else {
                    edit.visibility = View.VISIBLE
                    option.visibility = View.VISIBLE
                }
            }
        }

        override fun getItemId(position: Int): Long {
            return (if (position < LABEL_OFFSET) position else mLabels[position].hashCode()).toLong()
        }

        override fun getItemCount(): Int {
            return mLabels.size
        }
    }

    private inner class DeleteRangeDialogHelper(
        private val mDownloadInfoList: List<DownloadInfo>,
        private val mGidList: LongList,
        private val mBuilder: CheckBoxDialogBuilder,
    ) : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            if (which != DialogInterface.BUTTON_POSITIVE) {
                return
            }

            lifecycleScope.launchIO {
                // Delete
                DownloadManager.deleteRangeDownload(mGidList)
                // Delete image files
                val checked = mBuilder.isChecked
                Settings.removeImageFiles = checked
                if (checked) {
                    mDownloadInfoList.forEach { info ->
                        // Delete file
                        info.downloadDir?.delete()
                        // Remove download path
                        EhDB.removeDownloadDirname(info.gid)
                    }
                }
            }
        }
    }

    private inner class MoveDialogHelper(
        private val mLabels: Array<String>,
        private val mDownloadInfoList: List<DownloadInfo>,
    ) : DialogInterface.OnClickListener {
        @SuppressLint("NotifyDataSetChanged")
        override fun onClick(dialog: DialogInterface, which: Int) {
            context ?: return
            val label: String? = if (which == 0) {
                null
            } else {
                mLabels[which]
            }
            lifecycleScope.launchIO {
                DownloadManager.changeLabel(mDownloadInfoList, label)
                withUIContext {
                    if (mLabelAdapter != null) {
                        mLabelAdapter!!.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private inner class DownloadHolder(
        private val binding: ItemDownloadBinding,
    ) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            // TODO cancel on click listener when select items
            binding.start.setOnClickListener(this)
            binding.stop.setOnClickListener(this)
            binding.thumb.setMD3Content {
                Spacer(modifier = Modifier.height(height).fillMaxWidth())
            }
            binding.handle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    mItemTouchHelper!!.startDrag(this)
                }
                false
            }
        }

        override fun onClick(v: View) {
            val context = context
            val activity: Activity? = mainActivity
            val recyclerView = this@DownloadsScene.binding.recyclerView
            if (null == context || null == activity || tracker.isInCustomChoice) {
                return
            }
            val list = mList ?: return
            val size = list.size
            val index = recyclerView.getChildAdapterPosition(itemView)
            if (index < 0 || index >= size) {
                return
            }
            when (v) {
                binding.thumb -> {
                    val args = Bundle()
                    args.putString(
                        GalleryDetailScene.KEY_ACTION,
                        GalleryDetailScene.ACTION_GALLERY_INFO,
                    )
                    args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, list[index].galleryInfo)
                    navAnimated(R.id.galleryDetailScene, args)
                }

                binding.start -> {
                    val intent = Intent(activity, DownloadService::class.java)
                    intent.action = DownloadService.ACTION_START
                    intent.putExtra(DownloadService.KEY_GALLERY_INFO, list[index].galleryInfo)
                    ContextCompat.startForegroundService(activity, intent)
                }

                binding.stop -> {
                    lifecycleScope.launchIO {
                        DownloadManager.stopDownload(list[index].gid)
                    }
                }
            }
        }

        private val height = (3 * listThumbSize * 3).pxToDp.dp

        fun bind(info: DownloadInfo, isChecked: Boolean) {
            binding.root.isChecked = isChecked
            binding.thumb.setMD3Content {
                Card(onClick = ::onClick.partially1(binding.thumb)) {
                    CompanionAsyncThumb(
                        info = info,
                        path = info.downloadDir?.subFile(".thumb"),
                        modifier = Modifier.height(height).aspectRatio(DEFAULT_ASPECT),
                    )
                }
            }
            binding.title.text = EhUtils.getSuitableTitle(info)
            binding.uploader.text = info.uploader
            binding.rating.rating = info.rating
            binding.category.setMD3Content {
                val categoryColor = EhUtils.getCategoryColor(info.category)
                val categoryText = EhUtils.getCategory(info.category).uppercase()
                Text(
                    text = categoryText,
                    modifier = Modifier.clip(ShapeDefaults.Small).background(categoryColor).padding(vertical = 2.dp, horizontal = 8.dp),
                    color = if (Settings.harmonizeCategoryColor) Color.Unspecified else EhUtils.categoryTextColor,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            bindForState(info)
        }

        fun bindForState(info: DownloadInfo) {
            val context = context ?: return
            when (info.state) {
                DownloadInfo.STATE_NONE -> bindState(
                    info,
                    context.getString(R.string.download_state_none),
                )

                DownloadInfo.STATE_WAIT -> bindState(
                    info,
                    context.getString(R.string.download_state_wait),
                )

                DownloadInfo.STATE_DOWNLOAD -> bindProgress(info)
                DownloadInfo.STATE_FAILED -> {
                    val text: String = if (info.legacy <= 0) {
                        context.getString(R.string.download_state_failed)
                    } else {
                        context.getString(R.string.download_state_failed_2, info.legacy)
                    }
                    bindState(info, text)
                }

                DownloadInfo.STATE_FINISH -> bindState(
                    info,
                    context.getString(R.string.download_state_finish),
                )
            }
        }

        private fun bindState(info: DownloadInfo, newState: String) {
            binding.run {
                uploader.visibility = View.VISIBLE
                rating.visibility = View.VISIBLE
                category.visibility = View.VISIBLE
                state.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                percent.visibility = View.GONE
                speed.visibility = View.GONE
                if (info.state == DownloadInfo.STATE_WAIT || info.state == DownloadInfo.STATE_DOWNLOAD) {
                    start.visibility = View.GONE
                    stop.visibility = View.VISIBLE
                } else {
                    start.visibility = View.VISIBLE
                    stop.visibility = View.GONE
                }
                state.text = newState
            }
        }

        @SuppressLint("SetTextI18n")
        private fun bindProgress(info: DownloadInfo) {
            binding.run {
                uploader.visibility = View.GONE
                rating.visibility = View.GONE
                category.visibility = View.GONE
                state.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
                percent.visibility = View.VISIBLE
                speed.visibility = View.VISIBLE
                if (info.state == DownloadInfo.STATE_WAIT || info.state == DownloadInfo.STATE_DOWNLOAD) {
                    start.visibility = View.GONE
                    stop.visibility = View.VISIBLE
                } else {
                    start.visibility = View.VISIBLE
                    stop.visibility = View.GONE
                }
                if (info.total <= 0 || info.finished < 0) {
                    percent.text = null
                    progressBar.isIndeterminate = true
                } else {
                    percent.text = info.finished.toString() + "/" + info.total
                    progressBar.isIndeterminate = false
                    progressBar.max = info.total
                    progressBar.progress = info.finished
                }
                speed.text =
                    FileUtils.humanReadableByteCount(info.speed.coerceAtLeast(0), false) + "/S"
            }
        }
    }

    private val diffCallback = object : DiffUtil.ItemCallback<DownloadInfo>() {
        override fun areItemsTheSame(oldItem: DownloadInfo, newItem: DownloadInfo) = oldItem.gid == newItem.gid
        override fun areContentsTheSame(oldItem: DownloadInfo, newItem: DownloadInfo) = oldItem == newItem
        override fun getChangePayload(oldItem: DownloadInfo, newItem: DownloadInfo): Any? {
            return if (oldItem.downloadInfo != newItem.downloadInfo) {
                PAYLOAD_DOWNLOAD_INFO
            } else {
                super.getChangePayload(oldItem, newItem)
            }
        }
    }

    private inner class DownloadAdapter : ListAdapter<DownloadInfo, DownloadHolder>(diffCallback) {
        private val mInflater: LayoutInflater = layoutInflater
        var tracker: GallerySelectionTracker<DownloadInfo>? = null

        override fun getItemId(position: Int) = getItem(position).gid

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadHolder {
            val holder = DownloadHolder(ItemDownloadBinding.inflate(mInflater, parent, false))
            holder.itemView.setOnClickListener { onItemClick(holder.bindingAdapterPosition) }
            return holder
        }

        override fun onBindViewHolder(holder: DownloadHolder, position: Int) {
            val info = getItem(position)
            holder.bind(info, tracker?.isSelected(info.gid) ?: false)
        }

        override fun onBindViewHolder(
            holder: DownloadHolder,
            position: Int,
            payloads: MutableList<Any>,
        ) {
            payloads.forEach { payload ->
                when (payload) {
                    PAYLOAD_DOWNLOAD_INFO -> {
                        val info = getItem(position)
                        holder.bindForState(info)
                        return
                    }
                }
            }
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private inner class RenameLabelDialogHelper(
        private val mBuilder: EditTextDialogBuilder,
        private val mDialog: AlertDialog,
        private val mOriginalLabel: String?,
    ) : View.OnClickListener {
        init {
            val button: Button = mDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            button.setOnClickListener(this)
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onClick(v: View) {
            context ?: return
            val text = mBuilder.text
            if (text.isEmpty()) {
                mBuilder.setError(getString(R.string.label_text_is_empty))
            } else if (getString(R.string.default_download_label_name) == text) {
                mBuilder.setError(getString(R.string.label_text_is_invalid))
            } else if (DownloadManager.containLabel(text)) {
                mBuilder.setError(getString(R.string.label_text_exist))
            } else {
                mBuilder.setError(null)
                mDialog.dismiss()
                lifecycleScope.launchIO {
                    DownloadManager.renameLabel(mOriginalLabel!!, text)
                    if (mLabelAdapter != null) {
                        withUIContext {
                            initLabels()
                            mLabelAdapter!!.notifyDataSetChanged()
                            if (mLabel == mOriginalLabel) {
                                mLabel = text
                                updateForLabel()
                            }
                        }
                    }
                }
            }
        }
    }

    private inner class NewLabelDialogHelper(
        private val mBuilder: EditTextDialogBuilder,
        private val mDialog: AlertDialog,
    ) : View.OnClickListener {
        init {
            val button: Button = mDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            button.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            context ?: return
            val text = mBuilder.text
            if (text.isEmpty()) {
                mBuilder.setError(getString(R.string.label_text_is_empty))
            } else if (getString(R.string.default_download_label_name) == text) {
                mBuilder.setError(getString(R.string.label_text_is_invalid))
            } else if (DownloadManager.containLabel(text)) {
                mBuilder.setError(getString(R.string.label_text_exist))
            } else {
                mBuilder.setError(null)
                mDialog.dismiss()
                lifecycleScope.launchIO {
                    DownloadManager.addLabel(text)
                    initLabels()
                    withUIContext {
                        mLabelAdapter?.notifyItemInserted(mLabels.size)
                    }
                }
            }
        }
    }

    private inner class DownloadLabelItemTouchHelperCallback : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
        ): Int {
            val position = viewHolder.bindingAdapterPosition
            return if (position < LABEL_OFFSET) {
                makeMovementFlags(0, 0)
            } else {
                makeMovementFlags(
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                    ItemTouchHelper.LEFT,
                )
            }
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            val fromPosition = viewHolder.bindingAdapterPosition
            val toPosition = target.bindingAdapterPosition
            val context = context
            if (null == context || fromPosition == toPosition || toPosition < LABEL_OFFSET) {
                return false
            }
            lifecycleScope.launchIO {
                DownloadManager.moveLabel(fromPosition - LABEL_OFFSET, toPosition - LABEL_OFFSET)
            }
            val item = mLabels.removeAt(fromPosition)
            mLabels.add(toPosition, item)
            mLabelAdapter!!.notifyItemMoved(fromPosition, toPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.bindingAdapterPosition
            val label = mLabels[position]
            BaseDialogBuilder(context!!)
                .setMessage(getString(R.string.delete_label, label))
                .setPositiveButton(R.string.delete) { _, _ ->
                    lifecycleScope.launchIO {
                        DownloadManager.deleteLabel(label)
                        mLabels.removeAt(position)
                        withUIContext {
                            mLabel = null
                            updateForLabel()
                            mLabelAdapter!!.notifyItemRemoved(position)
                        }
                    }
                }.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }.setOnCancelListener {
                    mLabelAdapter!!.notifyItemChanged(position)
                }.show()
        }
    }

    companion object {
        const val KEY_GID = "gid"
        const val KEY_ACTION = "action"
        const val ACTION_CLEAR_DOWNLOAD_SERVICE = "clear_download_service"
        private const val KEY_LABEL = "label"
        private const val LABEL_OFFSET = 2
        private const val PAYLOAD_DOWNLOAD_INFO = 0
    }
}

@Composable
private fun CompanionAsyncThumb(
    info: GalleryInfo,
    path: UniFile?,
    modifier: Modifier = Modifier,
) {
    var contentScale by remember(info.gid) { mutableStateOf(ContentScale.Fit) }
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current
    var localReq by remember(info.gid) {
        path?.takeIf { it.isFile }?.uri?.let {
            context.imageRequest {
                data(it.toString())
                memoryCacheKey(info.thumbKey)
            }
        }.let { mutableStateOf(it) }
    }
    AsyncImage(
        model = localReq ?: requestOf(info),
        contentDescription = null,
        modifier = modifier,
        onState = { state ->
            if (state is AsyncImagePainter.State.Success) {
                state.result.drawable.run {
                    if (CropDefaults.shouldCrop(intrinsicWidth, intrinsicHeight)) {
                        contentScale = ContentScale.Crop
                    }
                }
                path?.let {
                    coroutineScope.launch {
                        runCatching {
                            if (!path.exists() && path.ensureFile()) {
                                val key = info.thumbKey!!
                                imageCache.read(key) {
                                    UniFile.fromFile(data.toFile())!!.openFileDescriptor("r").use { src ->
                                        path.openFileDescriptor("w").use { dst ->
                                            src sendTo dst
                                        }
                                    }
                                }
                            }
                        }.onFailure {
                            it.printStackTrace()
                        }
                    }
                }
            }
            if (state is AsyncImagePainter.State.Error) {
                path?.let {
                    coroutineScope.launch {
                        if (path.exists()) {
                            path.delete()
                            localReq = null
                        }
                    }
                }
            }
        },
        contentScale = contentScale,
    )
}
