package com.luleme.android.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.luleme.android.logic.MonthCount

@Composable
fun TrendChart(
    data: List<MonthCount>,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f || data.isEmpty()) return@Canvas
        var max = data.maxOfOrNull { it.count } ?: 0
        if (max == 0) max = 1
        val padL = 10f
        val padR = 10f
        val padT = 22f
        val padB = 28f
        val plotW = w - padL - padR
        val plotH = h - padT - padB
        val n = data.size
        val slot = plotW / n
        val barW = minOf(28f, slot * 0.55f)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 10.dp.toPx()
        }
        data.forEachIndexed { i, item ->
            val cx = padL + slot * i + slot / 2f
            val barH = maxOf(2f, (item.count.toFloat() / max) * plotH)
            val x = cx - barW / 2f
            val y = padT + plotH - barH
            drawRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barW, barH)
            )
            paint.color = labelColor.hashCode()
            // Use Compose color ARGB
            paint.color = android.graphics.Color.argb(
                (labelColor.alpha * 255).toInt(),
                (labelColor.red * 255).toInt(),
                (labelColor.green * 255).toInt(),
                (labelColor.blue * 255).toInt()
            )
            drawContext.canvas.nativeCanvas.drawText(
                item.count.toString(),
                cx,
                y - 4f,
                paint
            )
            drawContext.canvas.nativeCanvas.drawText(
                "${item.month + 1}月",
                cx,
                h - 8f,
                paint
            )
        }
    }
}
