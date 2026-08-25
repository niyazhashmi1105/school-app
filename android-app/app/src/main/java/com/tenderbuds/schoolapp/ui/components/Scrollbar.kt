package com.tenderbuds.schoolapp.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Jetpack Compose has no built-in visible scrollbar for Android (unlike the
 * old View system's ListView/RecyclerView). This draws a simple thumb sized
 * and positioned from [LazyListState.layoutInfo] — visible only when the
 * list actually overflows its bounds.
 */
fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f)
): Modifier = composed {
    drawWithContent {
        drawContent()

        val layoutInfo = state.layoutInfo
        val totalItemsCount = layoutInfo.totalItemsCount
        val visibleItemsInfo = layoutInfo.visibleItemsInfo

        if (totalItemsCount > 0 && visibleItemsInfo.isNotEmpty() && visibleItemsInfo.size < totalItemsCount) {
            val firstVisibleIndex = visibleItemsInfo.first().index
            val proportionVisible = visibleItemsInfo.size.toFloat() / totalItemsCount.toFloat()
            val proportionScrolled = firstVisibleIndex.toFloat() / totalItemsCount.toFloat()

            val minThumbHeight = 24.dp.toPx()
            val thumbHeight = (size.height * proportionVisible).coerceAtLeast(minThumbHeight)
            val maxThumbOffset = size.height - thumbHeight
            val thumbOffsetY = (size.height * proportionScrolled).coerceIn(0f, maxThumbOffset)

            drawRoundRect(
                color = color,
                topLeft = Offset(size.width - width.toPx(), thumbOffsetY),
                size = Size(width.toPx(), thumbHeight),
                cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2)
            )
        }
    }
}
