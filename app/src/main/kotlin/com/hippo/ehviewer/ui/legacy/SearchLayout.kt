package com.hippo.ehviewer.ui.legacy

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.AttributeSet
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.get
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.image.Image.Companion.decodeBitmap
import com.hippo.ehviewer.ui.main.AdvancedSearchOption
import com.hippo.ehviewer.ui.main.ImageSearch
import com.hippo.ehviewer.ui.main.NormalSearch
import com.hippo.ehviewer.ui.main.SearchAdvanced
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.findActivity
import com.hippo.ehviewer.util.pickVisualMedia
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    var isNormalMode by mutableStateOf(true) // else ImageSearch mode
    var isAdvancedMode by mutableStateOf(false)
    var mCategory by mutableIntStateOf(Settings.searchCategory)
    var mSearchMode by mutableIntStateOf(1)
    var advancedState by mutableStateOf(AdvancedSearchOption())
    var uss by mutableStateOf(false)
    var osc by mutableStateOf(false)
    var path by mutableStateOf("")
}

class SearchLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AbstractComposeView(context, attrs, defStyle) {
    private val vm by lazy {
        ViewModelProvider(findViewTreeViewModelStoreOwner()!!).get<SearchViewModel>()
    }

    @Composable
    override fun Content() {
        val coroutineScope = rememberCoroutineScope()
        val windowSizeClass = calculateWindowSizeClass(activity = context.findActivity())
        fun selectImage() = coroutineScope.launch {
            context.pickVisualMedia(ActivityResultContracts.PickVisualMedia.ImageOnly)?.let {
                vm.path = it.toString()
            }
        }
        Mdc3Theme {
            Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = dimensionResource(id = R.dimen.search_layout_margin_h))) {
                AnimatedVisibility(visible = vm.isNormalMode) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = dimensionResource(id = R.dimen.search_layout_margin_v))) {
                        Column(modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.search_category_padding_h), vertical = dimensionResource(id = R.dimen.search_category_padding_v))) {
                            Text(text = stringResource(id = R.string.search_normal), modifier = Modifier.height(dimensionResource(id = R.dimen.search_category_title_height)), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            NormalSearch(
                                category = vm.mCategory,
                                onCategoryChanged = {
                                    Settings.searchCategory = it
                                    vm.mCategory = it
                                },
                                searchMode = vm.mSearchMode,
                                onSearchModeChanged = { vm.mSearchMode = it },
                                isAdvanced = vm.isAdvancedMode,
                                onAdvancedChanged = { vm.isAdvancedMode = it },
                                showInfo = { BaseDialogBuilder(context).setMessage(R.string.search_tip).show() },
                                maxItemsInEachRow = when (windowSizeClass.widthSizeClass) {
                                    WindowWidthSizeClass.Compact -> 2
                                    WindowWidthSizeClass.Medium -> 3
                                    else -> 5
                                },
                            )
                        }
                    }
                }
                AnimatedVisibility(visible = vm.isNormalMode && vm.isAdvancedMode) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = dimensionResource(id = R.dimen.search_layout_margin_v))) {
                        Column(modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.search_category_padding_h), vertical = dimensionResource(id = R.dimen.search_category_padding_v))) {
                            Text(text = stringResource(id = R.string.search_advance), modifier = Modifier.height(dimensionResource(id = R.dimen.search_category_title_height)), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            SearchAdvanced(
                                state = vm.advancedState,
                                onStateChanged = { vm.advancedState = it },
                            )
                        }
                    }
                }
                AnimatedVisibility(visible = !vm.isNormalMode) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = dimensionResource(id = R.dimen.search_layout_margin_v))) {
                        Column(modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.search_category_padding_h), vertical = dimensionResource(id = R.dimen.search_category_padding_v))) {
                            Text(text = stringResource(id = R.string.search_image), modifier = Modifier.height(dimensionResource(id = R.dimen.search_category_title_height)), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            ImageSearch(
                                imagePath = vm.path,
                                onSelectImage = ::selectImage,
                                uss = vm.uss,
                                onUssChecked = { vm.uss = it },
                                osc = vm.osc,
                                onOscChecked = { vm.osc = it },
                            )
                        }
                    }
                }
                SecondaryTabRow(
                    selectedTabIndex = if (vm.isNormalMode) 0 else 1,
                    divider = {},
                ) {
                    Tab(
                        selected = vm.isNormalMode,
                        onClick = { vm.isNormalMode = true },
                        text = { Text(text = stringResource(id = R.string.keyword_search)) },
                    )
                    Tab(
                        selected = !vm.isNormalMode,
                        onClick = { vm.isNormalMode = false },
                        text = { Text(text = stringResource(id = R.string.search_image)) },
                    )
                }
            }
        }
    }

    fun setSearchMyTags(isMyTags: Boolean) {
        if (isMyTags) vm.mSearchMode = 2
    }

    fun setCategory(category: Int) {
        vm.mCategory = category
    }

    fun formatListUrlBuilder(urlBuilder: ListUrlBuilder, query: String?) {
        urlBuilder.reset()
        when (vm.isNormalMode) {
            true -> {
                when (vm.mSearchMode) {
                    1 -> urlBuilder.mode = ListUrlBuilder.MODE_NORMAL
                    2 -> urlBuilder.mode = ListUrlBuilder.MODE_SUBSCRIPTION
                    3 -> urlBuilder.mode = ListUrlBuilder.MODE_UPLOADER
                    4 -> urlBuilder.mode = ListUrlBuilder.MODE_TAG
                }
                urlBuilder.keyword = query
                urlBuilder.category = vm.mCategory
                if (vm.isAdvancedMode) {
                    urlBuilder.advanceSearch = vm.advancedState.advanceSearch
                    urlBuilder.minRating = vm.advancedState.minRating
                    val pageFrom = vm.advancedState.fromPage
                    val pageTo = vm.advancedState.toPage
                    if (pageTo != -1 && pageTo < 10) {
                        throw EhException(context.getString(R.string.search_sp_err1))
                    } else if (pageFrom != -1 && pageTo != -1 && pageTo - pageFrom < 20) {
                        throw EhException(context.getString(R.string.search_sp_err2))
                    }
                    urlBuilder.pageFrom = pageFrom
                    urlBuilder.pageTo = pageTo
                }
            }

            false -> {
                urlBuilder.mode = ListUrlBuilder.MODE_IMAGE_SEARCH
                if (vm.path.isBlank()) throw EhException(context.getString(R.string.select_image_first))
                val uri = Uri.parse(vm.path)
                val temp = AppConfig.createTempFile() ?: return
                val bitmap = context.decodeBitmap(uri) ?: return
                temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                urlBuilder.imagePath = temp.path
                urlBuilder.isUseSimilarityScan = vm.uss
                urlBuilder.isOnlySearchCovers = vm.osc
            }
        }
    }
}
