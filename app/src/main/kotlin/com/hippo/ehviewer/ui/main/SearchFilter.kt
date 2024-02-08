package com.hippo.ehviewer.ui.main

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.util.toIntOrDefault
import kotlinx.coroutines.launch

private val categoryTable = arrayOf(
    EhUtils.DOUJINSHI to R.string.doujinshi,
    EhUtils.MANGA to R.string.manga,
    EhUtils.ARTIST_CG to R.string.artist_cg,
    EhUtils.GAME_CG to R.string.game_cg,
    EhUtils.WESTERN to R.string.western,
    EhUtils.NON_H to R.string.non_h,
    EhUtils.IMAGE_SET to R.string.image_set,
    EhUtils.COSPLAY to R.string.cosplay,
    EhUtils.ASIAN_PORN to R.string.asian_porn,
    EhUtils.MISC to R.string.misc,
)

@Composable
fun SearchFilter(
    modifier: Modifier = Modifier,
    category: Int,
    onCategoryChanged: (Int) -> Unit,
    advancedOption: AdvancedSearchOption,
    onAdvancedOptionChanged: (AdvancedSearchOption) -> Unit,
) = Column(modifier) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dialogState = LocalDialogState.current
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categoryTable.forEach {
            val selected = category and it.first != 0
            FilterChip(
                selected = selected,
                onClick = { onCategoryChanged(category xor it.first) },
                label = { Text(text = stringResource(id = it.second)) },
            )
        }
    }
    var languageFilter by Settings.languageFilter.asMutableState()
    val languages = remember {
        GalleryInfo.S_LANG_TAGS.map { tag ->
            tag.substringAfter(':').let {
                if (EhTagDatabase.initialized && EhTagDatabase.isTranslatable(context)) {
                    EhTagDatabase.getTranslation("l", it) ?: it
                } else {
                    it
                }
            }
        }
    }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val any = stringResource(id = R.string.any)
        val languageStr = stringResource(id = R.string.key_language)
        FilterChip(
            selected = languageFilter != -1,
            onClick = {
                scope.launch {
                    val items = buildList {
                        add(any)
                        addAll(languages)
                    }
                    languageFilter = dialogState.showSingleChoice(items, languageFilter + 1) - 1
                }
            },
            label = {
                Text(text = if (languageFilter == -1) languageStr else languages[languageFilter])
            },
            trailingIcon = {
                Icon(
                    imageVector = if (languageFilter == -1) Icons.Outlined.FilterAlt else Icons.Default.FilterAlt,
                    contentDescription = null,
                )
            },
        )
        val pageErr1 = stringResource(R.string.search_sp_err1)
        val pageErr2 = stringResource(R.string.search_sp_err2)
        val pages = stringResource(id = R.string.key_pages)
        val pagesText = remember(advancedOption.fromPage, advancedOption.toPage) {
            advancedOption.run {
                val hasFrom = fromPage > 0
                val hasTo = toPage > 0
                if (hasFrom && hasTo) {
                    "$fromPage - $toPage P"
                } else if (hasFrom) {
                    "$fromPage+ P"
                } else if (hasTo) {
                    "$toPage- P"
                } else {
                    pages
                }
            }
        }
        FilterChip(
            selected = advancedOption.fromPage != 0 || advancedOption.toPage != 0,
            onClick = {
                scope.launch {
                    val (from, to) = dialogState.awaitResult(
                        initial = advancedOption.fromPage to advancedOption.toPage,
                        title = R.string.key_pages,
                        invalidator = { (from, to) ->
                            if (to != 0 && to < 10) {
                                pageErr1
                            } else if (from != 0 && to != 0 && to - from < 20) {
                                pageErr2
                            } else {
                                null
                            }
                        },
                    ) { error ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    value = expectedValue.first.takeIf { it > 0 }?.toString().orEmpty(),
                                    onValueChange = {
                                        expectedValue =
                                            expectedValue.copy(first = it.toIntOrDefault(0).coerceAtLeast(0))
                                    },
                                    modifier = Modifier.width(112.dp).padding(16.dp),
                                    singleLine = true,
                                    isError = error != null,
                                )
                                Text(text = stringResource(id = R.string.search_sp_to))
                                OutlinedTextField(
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    value = expectedValue.second.takeIf { it > 0 }?.toString().orEmpty(),
                                    onValueChange = {
                                        expectedValue =
                                            expectedValue.copy(second = it.toIntOrDefault(0).coerceAtLeast(0))
                                    },
                                    modifier = Modifier.width(112.dp).padding(16.dp),
                                    singleLine = true,
                                    isError = error != null,
                                )
                                Text(text = stringResource(id = R.string.search_sp_suffix))
                            }
                            if (error != null) {
                                ListItem(
                                    headlineContent = { Text(text = error) },
                                    leadingContent = {
                                        Icon(imageVector = Icons.Default.Info, contentDescription = null)
                                    },
                                    colors = ListItemDefaults.colors(
                                        headlineColor = MaterialTheme.colorScheme.error,
                                        leadingIconColor = MaterialTheme.colorScheme.error,
                                    ),
                                )
                            }
                        }
                    }
                    onAdvancedOptionChanged(advancedOption.copy(fromPage = from, toPage = to))
                }
            },
            label = { Text(text = pagesText) },
        )
        val minRatingItems = stringArrayResource(id = R.array.search_min_rating)
        val minRatingStr = stringResource(id = R.string.search_sr)
        val minRatingIndex = (advancedOption.minRating - 1).coerceAtLeast(0)
        FilterChip(
            selected = advancedOption.minRating != 0,
            onClick = {
                scope.launch {
                    val selected = dialogState.showSingleChoice(minRatingItems.toList(), minRatingIndex)
                    onAdvancedOptionChanged(advancedOption.copy(minRating = if (selected == 0) 0 else selected + 1))
                }
            },
            label = { Text(text = minRatingStr + minRatingItems[minRatingIndex]) },
        )
        fun checked(bit: Int) = advancedOption.advanceSearch and bit != 0
        fun AdvancedSearchOption.inv(bit: Int) = onAdvancedOptionChanged(copy(advanceSearch = advanceSearch xor bit))
        FilterChip(
            selected = checked(AdvanceTable.SH),
            onClick = { advancedOption.inv(AdvanceTable.SH) },
            label = { Text(text = stringResource(id = R.string.search_sh)) },
        )
        FilterChip(
            selected = checked(AdvanceTable.STO),
            onClick = { advancedOption.inv(AdvanceTable.STO) },
            label = { Text(text = stringResource(id = R.string.search_sto)) },
        )
        val disableFilter = stringResource(id = R.string.search_sf)
        FilterChip(
            selected = checked(AdvanceTable.SFL),
            onClick = { advancedOption.inv(AdvanceTable.SFL) },
            label = { Text(text = disableFilter + stringResource(id = R.string.search_sfl)) },
        )
        FilterChip(
            selected = checked(AdvanceTable.SFU),
            onClick = { advancedOption.inv(AdvanceTable.SFU) },
            label = { Text(text = disableFilter + stringResource(id = R.string.search_sfu)) },
        )
        FilterChip(
            selected = checked(AdvanceTable.SFT),
            onClick = { advancedOption.inv(AdvanceTable.SFT) },
            label = { Text(text = disableFilter + stringResource(id = R.string.search_sft)) },
        )
    }
}
