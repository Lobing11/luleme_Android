package com.luleme.android.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luleme.android.data.RecordRepository
import com.luleme.android.logic.countInMonth
import com.luleme.android.logic.countToday
import com.luleme.android.logic.currentStreak
import com.luleme.android.logic.daysSinceLast
import com.luleme.android.logic.intervalStats
import com.luleme.android.logic.monthCounts
import com.luleme.android.logic.totalCount
import com.luleme.android.presentation.components.EmptyState
import com.luleme.android.presentation.components.StatTile
import com.luleme.android.presentation.components.TrendChart
import com.luleme.android.presentation.theme.DesignTokens
import com.luleme.android.presentation.theme.LocalExtraColors
import java.util.Calendar

@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember { RecordRepository.getInstance(context) }
    val records by repo.observeAll().collectAsState(initial = emptyList())
    val extras = LocalExtraColors.current
    val now = System.currentTimeMillis()
    val cur = Calendar.getInstance().apply { timeInMillis = now }
    val monthTotal = countInMonth(records, cur.get(Calendar.YEAR), cur.get(Calendar.MONTH))
    val streak = currentStreak(records, now)
    val grandTotal = totalCount(records)
    val gaps = intervalStats(records, now)
    val since = daysSinceLast(records, now)
    val sinceLast = since ?: -1
    val trend = monthCounts(records, now, 6)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(
                start = DesignTokens.PAGE_PADDING,
                end = DesignTokens.PAGE_PADDING,
                top = DesignTokens.PAGE_PADDING,
                bottom = DesignTokens.PAGE_BOTTOM_PADDING
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile("本月次数", monthTotal.toString(), "次", Modifier.weight(1f))
            StatTile("当前连续", streak.toString(), "天", Modifier.weight(1f))
            StatTile("总次数", grandTotal.toString(), "次", Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile("最长间隔", gaps.max.toString(), "天", Modifier.weight(1f))
            StatTile("平均间隔", gaps.avg.toString(), "天", Modifier.weight(1f))
            StatTile(
                title = "距上次",
                value = if (sinceLast < 0) "—" else sinceLast.toString(),
                unit = if (sinceLast < 0) "" else "天",
                modifier = Modifier.weight(1f)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(extras.card, RoundedCornerShape(DesignTokens.CARD_RADIUS))
                .padding(DesignTokens.CARD_PADDING),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "近 6 个月趋势",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (grandTotal == 0) {
                EmptyState(title = "暂无数据", desc = "还没有记录，先去记一笔吧")
            } else {
                TrendChart(data = trend)
            }
        }
    }
}
