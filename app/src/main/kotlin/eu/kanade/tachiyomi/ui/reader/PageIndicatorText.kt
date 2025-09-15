package eu.kanade.tachiyomi.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ehviewer.core.ui.component.RollingNumber
import com.ehviewer.core.ui.component.RollingNumberPlaceholder

@Composable
fun PageIndicatorText(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier,
) {
    if (currentPage <= 0 || totalPages <= 0) return

    val style = TextStyle(
        color = Color(235, 235, 235),
        fontSize = MaterialTheme.typography.bodySmall.fontSize,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
    )
    val strokeStyle = style.copy(
        color = Color(45, 45, 45),
        drawStyle = Stroke(width = 4f),
    )

    val total = "$totalPages"

    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            RollingNumber(number = currentPage, style = strokeStyle, length = total.length)
            RollingNumber(number = currentPage, style = style, length = total.length)
        }
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(text = " / ", style = strokeStyle)
            Text(text = " / ", style = style)
        }
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            RollingNumberPlaceholder(number = totalPages, style = strokeStyle)
            RollingNumberPlaceholder(number = totalPages, style = style)
        }
    }
}
