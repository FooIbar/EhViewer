package com.hippo.ehviewer.ui.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.ui.StartDestination
import com.hippo.ehviewer.ui.screen.popNavigate
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination
@Composable
fun SelectSiteScreen(navigator: DestinationsNavigator) {
    var siteEx by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.systemBarsPadding().padding(horizontal = dimensionResource(R.dimen.keyline_margin)).padding(top = dimensionResource(R.dimen.keyline_margin)).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.select_scene),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.weight(1f))
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(selected = !siteEx, onClick = { siteEx = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                Text(text = stringResource(id = R.string.site_e))
            }
            SegmentedButton(selected = siteEx, onClick = { siteEx = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                Text(text = stringResource(id = R.string.site_ex))
            }
        }
        Text(
            text = stringResource(id = R.string.select_scene_explain),
            modifier = Modifier.padding(top = 16.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.padding(horizontal = 12.dp).padding(top = 4.dp, bottom = 20.dp).fillMaxWidth()) {
            Button(
                onClick = {
                    // Gallery site was set to ex in sad panda check
                    if (!siteEx) Settings.gallerySite = EhUrl.SITE_E
                    Settings.needSignIn = false
                    navigator.popNavigate(StartDestination)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        }
    }
}
