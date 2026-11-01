package com.luleme.android.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luleme.android.common.heatLevel
import com.luleme.android.logic.buildMonthGrid
import com.luleme.android.logic.weekdayHeaders
import com.luleme.android.presentation.theme.LocalExtraColors

@Composable
fun CalendarHeatmap(
    year: Int,
    month: Int,
    dayCounts: IntArray,
    todayDay: Int,
    onDayTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val extras = LocalExtraColors.current
    val cells = buildMonthGrid(year, month)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayHeaders().forEach { wd ->
                Text(
                    text = wd,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        cells.chunked(7).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { day ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (day != null) {
                            val count = dayCounts.getOrElse(day) { 0 }
                            val level = heatLevel(count)
                            val bg = extras.heat[level]
                            val textColor = if (level >= 3) Color.White else MaterialTheme.colorScheme.onSurface
                            val isToday = todayDay > 0 && day == todayDay
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(bg, RoundedCornerShape(10.dp))
                                    .then(
                                        if (isToday) Modifier.border(
                                            1.2.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            RoundedCornerShape(10.dp)
                                        ) else Modifier
                                    )
                                    .clickable { onDayTap(day) }
                                    .padding(vertical = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )
                                Text(
                                    text = when {
                                        count <= 0 -> " "
                                        count > 9 -> "9+"
                                        else -> count.toString()
                                    },
                                    fontSize = 10.sp,
                                    color = textColor
                                )
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(40.dp))
                        }
                    }
                }
            }
        }
    }
}
