package com.hippo.ehviewer.ui.legacy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.AttributeSet
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.ui.main.AdvancedSearchOption
import com.hippo.ehviewer.ui.main.ImageSearch
import com.hippo.ehviewer.ui.main.NormalSearch
import com.hippo.ehviewer.ui.main.SearchAdvanced
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.pickVisualMedia
import com.hippo.unifile.UniFile
import kotlinx.coroutines.launch

class SearchLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AbstractComposeView(context, attrs, defStyle) {
    private var isNormalMode by mutableStateOf(true) // else ImageSearch mode
    private var isAdvancedMode by mutableStateOf(false)
    private var mCategory by mutableIntStateOf(Settings.searchCategory)
    private var mSearchMode by mutableIntStateOf(1)
    private var advancedState by mutableStateOf(AdvancedSearchOption())
    private var uss by mutableStateOf(false)
    private var osc by mutableStateOf(false)
    private var path by mutableStateOf("")

    @Composable
    override fun Content() {
        val coroutineScope = rememberCoroutineScope()
        fun selectImage() = coroutineScope.launch {
            context.pickVisualMedia(ActivityResultContracts.PickVisualMedia.ImageOnly)?.let {
                path = it.toString()
            }
        }
        Mdc3Theme {
            Column(modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.search_layout_margin_h)).imePadding().verticalScroll(rememberScrollState())) {
                AnimatedVisibility(visible = isNormalMode) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = dimensionResource(id = R.dimen.search_layout_margin_v))) {
                        Column(modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.search_category_padding_h), vertical = dimensionResource(id = R.dimen.search_category_padding_v))) {
                            Text(text = stringResource(id = R.string.search_normal), modifier = Modifier.height(dimensionResource(id = R.dimen.search_category_title_height)), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            NormalSearch(
                                category = mCategory,
                                onCategoryChanged = {
                                    Settings.searchCategory = it
                                    mCategory = it
                                },
                                searchMode = mSearchMode,
                                onSearchModeChanged = { mSearchMode = it },
                                isAdvanced = isAdvancedMode,
                                onAdvancedChanged = { isAdvancedMode = it },
                                showInfo = { BaseDialogBuilder(context).setMessage(R.string.search_tip).show() },
                            )
                        }
                    }
                }
                AnimatedVisibility(visible = isNormalMode && isAdvancedMode) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = dimensionResource(id = R.dimen.search_layout_margin_v))) {
                        Column(modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.search_category_padding_h), vertical = dimensionResource(id = R.dimen.search_category_padding_v))) {
                            Text(text = stringResource(id = R.string.search_advance), modifier = Modifier.height(dimensionResource(id = R.dimen.search_category_title_height)), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            SearchAdvanced(
                                state = advancedState,
                                onStateChanged = { advancedState = it },
                            )
                        }
                    }
                }
                AnimatedVisibility(visible = !isNormalMode) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = dimensionResource(id = R.dimen.search_layout_margin_v))) {
                        Column(modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.search_category_padding_h), vertical = dimensionResource(id = R.dimen.search_category_padding_v))) {
                            Text(text = stringResource(id = R.string.search_image), modifier = Modifier.height(dimensionResource(id = R.dimen.search_category_title_height)), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            ImageSearch(
                                imagePath = path,
                                onSelectImage = ::selectImage,
                                uss = uss,
                                onUssChecked = { uss = it },
                                osc = osc,
                                onOscChecked = { osc = it },
                            )
                        }
                    }
                }
                TabRow(
                    selectedTabIndex = if (isNormalMode) 0 else 1,
                    divider = {},
                ) {
                    Tab(
                        selected = isNormalMode,
                        onClick = { isNormalMode = true },
                        text = { Text(text = stringResource(id = R.string.keyword_search)) },
                    )
                    Tab(
                        selected = !isNormalMode,
                        onClick = { isNormalMode = false },
                        text = { Text(text = stringResource(id = R.string.search_image)) },
                    )
                }
                Spacer(modifier = Modifier.height(WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()))
            }
        }
    }

    fun setSearchMyTags(isMyTags: Boolean) {
        if (isMyTags) mSearchMode = 2
    }

    fun setCategory(category: Int) {
        mCategory = category
    }

    fun formatListUrlBuilder(urlBuilder: ListUrlBuilder, query: String?) {
        urlBuilder.reset()
        when (isNormalMode) {
            true -> {
                when (mSearchMode) {
                    1 -> urlBuilder.mode = ListUrlBuilder.MODE_NORMAL
                    2 -> urlBuilder.mode = ListUrlBuilder.MODE_SUBSCRIPTION
                    3 -> urlBuilder.mode = ListUrlBuilder.MODE_UPLOADER
                    4 -> urlBuilder.mode = ListUrlBuilder.MODE_TAG
                }
                urlBuilder.keyword = query
                urlBuilder.category = mCategory
                if (isAdvancedMode) {
                    urlBuilder.advanceSearch = advancedState.advanceSearch
                    urlBuilder.minRating = advancedState.minRating
                    val pageFrom = advancedState.fromPage
                    val pageTo = advancedState.toPage
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
                if (path.isBlank()) throw EhException(context.getString(R.string.select_image_first))
                val uri = Uri.parse(path)
                val src = UniFile.fromUri(context, uri)?.imageSource ?: return
                val temp = AppConfig.createTempFile() ?: return
                val bitmap = ImageDecoder.decodeBitmap(src, Image.imageSearchDecoderSampleListener)
                temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                urlBuilder.imagePath = temp.path
                urlBuilder.isUseSimilarityScan = uss
                urlBuilder.isOnlySearchCovers = osc
            }
        }
    }
}
