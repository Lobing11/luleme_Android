package com.luleme.android.presentation.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luleme.android.common.RecordEditParam
import com.luleme.android.data.RecordRepository
import com.luleme.android.logic.ReminderScheduler
import com.luleme.android.model.DEFAULT_MOOD
import com.luleme.android.model.MOODS
import com.luleme.android.presentation.theme.DesignTokens
import com.luleme.android.presentation.theme.LocalExtraColors
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditScreen(
    param: RecordEditParam?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extras = LocalExtraColors.current
    var editing by remember { mutableStateOf(false) }
    var recordId by remember { mutableLongStateOf(0L) }
    var dateMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var mood by remember { mutableIntStateOf(DEFAULT_MOOD) }
    var note by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var formReady by remember { mutableStateOf(false) }

    LaunchedEffect(param?.id, param?.date) {
        formReady = false
        val repo = RecordRepository.getInstance(context)
        val id = param?.id
        if (id != null && id > 0) {
            val record = repo.getById(id)
            if (record != null) {
                editing = true
                recordId = record.id
                dateMs = record.timestamp
                mood = record.mood
                note = record.note
                formReady = true
                return@LaunchedEffect
            }
        }
        editing = false
        recordId = 0
        dateMs = if (param?.date != null && param.date > 0) {
            param.date + 12L * 60 * 60 * 1000
        } else {
            System.currentTimeMillis()
        }
        mood = DEFAULT_MOOD
        note = ""
        formReady = true
    }

    fun pickDate() {
        val cur = Calendar.getInstance().apply { timeInMillis = dateMs }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val merged = Calendar.getInstance().apply {
                    timeInMillis = dateMs
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                }
                dateMs = merged.timeInMillis
            },
            cur.get(Calendar.YEAR),
            cur.get(Calendar.MONTH),
            cur.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = Calendar.getInstance().apply { set(2015, 0, 1) }.timeInMillis
            datePicker.maxDate = Calendar.getInstance().apply { set(2035, 11, 31) }.timeInMillis
        }.show()
    }

    fun pickTime() {
        val cur = Calendar.getInstance().apply { timeInMillis = dateMs }
        TimePickerDialog(
            context,
            { _, h, min ->
                val merged = Calendar.getInstance().apply {
                    timeInMillis = dateMs
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, min)
                }
                dateMs = merged.timeInMillis
            },
            cur.get(Calendar.HOUR_OF_DAY),
            cur.get(Calendar.MINUTE),
            true
        ).show()
    }

    fun save() {
        if (saving || !formReady) return
        saving = true
        scope.launch {
            try {
                val repo = RecordRepository.getInstance(context)
                if (editing) {
                    repo.update(recordId, dateMs, mood, note)
                    Toast.makeText(context, "已保存修改", Toast.LENGTH_SHORT).show()
                } else {
                    repo.add(dateMs, mood, "")
                    ReminderScheduler.getInstance(context).onRecordCreated(System.currentTimeMillis())
                    Toast.makeText(context, "已记录", Toast.LENGTH_SHORT).show()
                }
                onBack()
            } catch (_: Exception) {
                saving = false
                Toast.makeText(context, "保存失败，请重试", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cal = Calendar.getInstance().apply { timeInMillis = dateMs }
    val dateLabel = String.format(
        Locale.CHINA,
        "%04d-%02d-%02d",
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
    val timeLabel = String.format(
        Locale.CHINA,
        "%02d:%02d",
        cal.get(Calendar.HOUR_OF_DAY),
        cal.get(Calendar.MINUTE)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing) "编辑记录" else "记一笔") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignTokens.PAGE_PADDING),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(extras.card, RoundedCornerShape(DesignTokens.CARD_RADIUS))
            ) {
                SettingClickRow("日期", dateLabel) { pickDate() }
                SettingClickRow("时间", timeLabel) { pickTime() }
            }
            Text("心情", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MOODS.forEach { m ->
                    val selected = mood == m.value
                    Column(
                        modifier = Modifier
                            .background(
                                if (selected) extras.primarySoft else extras.card,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                if (selected) 1.5.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else extras.border,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { mood = m.value }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(m.emoji, fontSize = 26.sp)
                        Text(
                            m.label,
                            fontSize = 10.sp,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Button(
                onClick = { save() },
                enabled = formReady && !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DesignTokens.BUTTON_MIN_HEIGHT),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (editing) "保存修改" else "保存记录")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingClickRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurface)
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
