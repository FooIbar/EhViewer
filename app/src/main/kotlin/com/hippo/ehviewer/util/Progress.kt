package com.hippo.ehviewer.util

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.hippo.ehviewer.R.string
import com.hippo.ehviewer.ui.tools.DialogState

@Composable
fun ProgressDialog() {
    BasicAlertDialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            modifier = Modifier.width(280.dp),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(18.dp),
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.size(18.dp))
                Text(text = stringResource(id = string.please_wait))
            }
        }
    }
}

suspend fun <R> DialogState.bgWork(work: suspend () -> R) = dialog { cont ->
    ProgressDialog()
    LaunchedEffect(work) {
        val result = runCatching { work() }
        cont.resumeWith(result)
    }
}
