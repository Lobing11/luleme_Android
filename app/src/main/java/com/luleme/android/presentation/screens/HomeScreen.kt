package com.luleme.android.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luleme.android.common.RecordEditParam
import com.luleme.android.common.heatLevel
import com.luleme.android.data.RecordRepository
import com.luleme.android.data.SettingsStore
import com.luleme.android.logic.GESHEN_TEXT
import com.luleme.android.logic.countByDayOfMonth
import com.luleme.android.logic.countInMonth
import com.luleme.android.logic.countToday
import com.luleme.android.logic.currentStreak
import com.luleme.android.logic.monthStart
import com.luleme.android.logic.recordsOnDay
import com.luleme.android.logic.shiftMonth
import com.luleme.android.logic.startOfDay
import com.luleme.android.logic.totalCount
import com.luleme.android.model.Record
import com.luleme.android.model.formatDateTime
import com.luleme.android.model.moodEmoji
import com.luleme.android.presentation.components.CalendarHeatmap
import com.luleme.android.presentation.components.EmptyState
import com.luleme.android.presentation.components.StatTile
import com.luleme.android.presentation.theme.DesignTokens
import com.luleme.android.presentation.theme.LocalExtraColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddRecord: (RecordEditParam?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extras = LocalExtraColors.current
    val repo = remember { RecordRepository.getInstance(context) }
    val records by repo.observeAll().collectAsState(initial = emptyList())
    val settings by SettingsStore.getInstance(context).settingsFlow.collectAsState(
        initial = null
    )

    val nowCal = Calendar.getInstance()
    var viewYear by remember { mutableIntStateOf(nowCal.get(Calendar.YEAR)) }
    var viewMonth by remember { mutableIntStateOf(nowCal.get(Calendar.MONTH)) }

    var showDaySheet by remember { mutableStateOf(false) }
    var sheetDateMs by remember { mutableLongStateOf(0L) }
    var sheetTitle by remember { mutableStateOf("") }
    var reminderBanner by remember { mutableStateOf("") }
    var undoRecord by remember { mutableStateOf<Record?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val now = System.currentTimeMillis()
    val monthTotal = countInMonth(records, viewYear, viewMonth)
    val todayTotal = countToday(records, now)
    val streak = currentStreak(records, now)
    val grandTotal = totalCount(records)
    val dayMap = countByDayOfMonth(records, viewYear, viewMonth)
    val dayCounts = IntArray(32) { d -> dayMap[d] ?: 0 }
    val todayDay = run {
        val n = Calendar.getInstance()
        if (n.get(Calendar.YEAR) == viewYear && n.get(Calendar.MONTH) == viewMonth) {
            n.get(Calendar.DAY_OF_MONTH)
        } else 0
    }

    LaunchedEffect(records, settings, todayTotal) {
        var message = ""
        val store = settings
        if (store != null) {
            if (store.reminderEnabled) {
                val cal = Calendar.getInstance()
                val passed = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE) >=
                    store.reminderHour * 60 + store.reminderMinute
                if (passed && todayTotal == 0) {
                    message = store.reminderText
                }
            }
            if (message.isEmpty() && store.geshenEnabled && records.isNotEmpty()) {
                val elapsed = System.currentTimeMillis() - records[0].timestamp
                if (elapsed >= 8L * 60 * 60 * 1000) {
                    message = GESHEN_TEXT
                }
            }
        }
        reminderBanner = message
    }

    fun openDay(day: Int) {
        val ms = monthStart(viewYear, viewMonth) + (day - 1) * 24L * 60 * 60 * 1000
        // Use calendar for exact day start
        val dayStart = Calendar.getInstance().apply {
            set(Calendar.YEAR, viewYear)
            set(Calendar.MONTH, viewMonth)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        sheetDateMs = dayStart
        sheetTitle = "${viewMonth + 1}月${day}日"
        showDaySheet = true
    }

    fun deleteWithUndo(record: Record) {
        scope.launch {
            repo.remove(record.id)
            undoRecord = record
            val result = snackbarHostState.showSnackbar(
                message = "已删除",
                actionLabel = "撤销",
                withDismissAction = true
            )
            if (result == SnackbarResult.ActionPerformed) {
                val r = undoRecord
                if (r != null) {
                    repo.add(r.timestamp, r.mood, r.note)
                }
            }
            undoRecord = null
        }
    }

    val canGoNext = run {
        val cur = Calendar.getInstance()
        viewYear < cur.get(Calendar.YEAR) ||
            (viewYear == cur.get(Calendar.YEAR) && viewMonth < cur.get(Calendar.MONTH))
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = DesignTokens.PAGE_PADDING,
                    end = DesignTokens.PAGE_PADDING,
                    top = DesignTokens.PAGE_PADDING,
                    bottom = 8.dp
                ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.CARD_GAP)
        ) {
            if (reminderBanner.isNotEmpty()) {
                Text(
                    text = reminderBanner,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(extras.primarySoft, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(extras.card, RoundedCornerShape(DesignTokens.CARD_RADIUS))
                    .padding(DesignTokens.CARD_PADDING)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        val m = shiftMonth(viewYear, viewMonth, -1)
                        viewYear = m.year
                        viewMonth = m.month
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上月")
                    }
                    Text(
                        "${viewYear}年${viewMonth + 1}月",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = {
                            if (!canGoNext) return@IconButton
                            val m = shiftMonth(viewYear, viewMonth, 1)
                            viewYear = m.year
                            viewMonth = m.month
                        },
                        enabled = canGoNext
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下月")
                    }
                }
                CalendarHeatmap(
                    year = viewYear,
                    month = viewMonth,
                    dayCounts = dayCounts,
                    todayDay = todayDay,
                    onDayTap = { openDay(it) }
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("少", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    extras.heat.drop(1).forEach { color ->
                        Spacer(
                            modifier = Modifier
                                .height(12.dp)
                                .weight(1f)
                                .background(color, RoundedCornerShape(2.dp))
                        )
                    }
                    Text("多", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile("今日", todayTotal.toString(), "次", Modifier.weight(1f))
                StatTile("本月", monthTotal.toString(), "次", Modifier.weight(1f))
                StatTile("连续", streak.toString(), "天", Modifier.weight(1f))
            }

            if (grandTotal == 0) {
                EmptyState(title = "还没有记录", desc = "点下方「记一笔」开始吧")
            }
        }

        Button(
            onClick = { onAddRecord(null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.PAGE_PADDING)
                .padding(bottom = 8.dp)
                .height(DesignTokens.BUTTON_MIN_HEIGHT),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("记一笔")
        }

        SnackbarHost(hostState = snackbarHostState) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = extras.card,
                contentColor = MaterialTheme.colorScheme.onSurface,
                actionColor = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showDaySheet) {
        val sheetRecords = recordsOnDay(records, sheetDateMs)
        val sheetMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.72f).dp
        ModalBottomSheet(
            onDismissRequest = { showDaySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetMaxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(sheetTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (sheetRecords.isEmpty()) {
                    EmptyState(title = "这一天还没有记录", desc = "可以补记一笔")
                } else {
                    sheetRecords.forEach { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(extras.card, RoundedCornerShape(12.dp))
                                .clickable {
                                    showDaySheet = false
                                    onAddRecord(RecordEditParam(id = r.id))
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${moodEmoji(r.mood)}  ${formatDateTime(r.timestamp)}")
                            TextButton(onClick = {
                                showDaySheet = false
                                deleteWithUndo(r)
                            }) {
                                Text("删除", color = extras.danger)
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        showDaySheet = false
                        onAddRecord(RecordEditParam(date = sheetDateMs))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("添加记录")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
