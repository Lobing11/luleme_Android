package com.luleme.android.presentation.screens

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.luleme.android.data.RecordRepository
import com.luleme.android.data.SettingsStore
import com.luleme.android.logic.DEFAULT_REMINDER_TEXT
import com.luleme.android.logic.DailyReminderConfig
import com.luleme.android.logic.ReminderScheduler
import com.luleme.android.logic.getBiometricStatus
import com.luleme.android.logic.hashPin
import com.luleme.android.logic.randomSalt
import com.luleme.android.logic.verifyPin
import com.luleme.android.model.Record
import com.luleme.android.model.parseRecord
import com.luleme.android.presentation.theme.DesignTokens
import com.luleme.android.presentation.theme.LocalExtraColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Calendar
import java.util.Locale

@Composable
fun SettingsScreen(
    onPrivacyModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extras = LocalExtraColors.current
    val store = remember { SettingsStore.getInstance(context) }
    val snap by store.settingsFlow.collectAsState(initial = null)
    val bioStatus = remember { getBiometricStatus(context) }

    var showPinSetup by remember { mutableStateOf(false) }
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var showPinDisable by remember { mutableStateOf(false) }
    var disablePin by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }
    var reminderTextDraft by remember { mutableStateOf("") }

    LaunchedEffect(snap?.reminderText) {
        reminderTextDraft = snap?.reminderText ?: DEFAULT_REMINDER_TEXT
    }

    LaunchedEffect(reminderTextDraft, snap?.reminderEnabled) {
        val current = snap ?: return@LaunchedEffect
        if (reminderTextDraft == current.reminderText) return@LaunchedEffect
        delay(800)
        val text = reminderTextDraft.take(30)
        store.setReminderText(text)
        if (current.reminderEnabled) {
            ReminderScheduler.getInstance(context).applyDaily(
                DailyReminderConfig(current.reminderHour, current.reminderMinute, text.ifEmpty { DEFAULT_REMINDER_TEXT })
            )
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val records = RecordRepository.getInstance(context).getAll()
                val payload = mapOf(
                    "app" to "鹿了么",
                    "version" to 1,
                    "exportedAt" to System.currentTimeMillis(),
                    "records" to records
                )
                val json = Gson().toJson(payload)
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                } ?: return@launch
                val root = Gson().fromJson(text, JsonObject::class.java)
                if (!root.has("version") || !root.get("version").isJsonPrimitive) {
                    Toast.makeText(context, "备份格式无效", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                if (!root.has("records") || !root.get("records").isJsonArray) {
                    Toast.makeText(context, "备份格式无效", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val type = object : TypeToken<List<Map<String, Any?>>>() {}.type
                val rawList: List<Map<String, Any?>> = Gson().fromJson(root.get("records"), type)
                val parsed = rawList.mapNotNull { parseRecord(it) }
                val repo = RecordRepository.getInstance(context)
                repo.clearAll()
                for (r in parsed) {
                    repo.add(r.timestamp, r.mood, r.note)
                }
                Toast.makeText(context, "已导入 ${parsed.size} 条记录", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "导入失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled by caller re-check */ }

    suspend fun ensureNotificationAllowed(): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                return ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        return true
    }

    fun canExactAlarm(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    suspend fun persistReminder(enabled: Boolean, hour: Int, minute: Int, text: String, toast: Boolean) {
        store.setReminderEnabled(enabled)
        store.setReminderHour(hour)
        store.setReminderMinute(minute)
        store.setReminderText(text)
        val scheduler = ReminderScheduler.getInstance(context)
        if (!enabled) {
            scheduler.cancelDaily()
            return
        }
        if (!ensureNotificationAllowed()) {
            store.setReminderEnabled(false)
            Toast.makeText(context, "需要允许通知才能收到提醒", Toast.LENGTH_SHORT).show()
            return
        }
        if (!canExactAlarm()) {
            // Keep enabled with in-app fallback (align 1700002 degradation)
            if (toast) {
                Toast.makeText(context, "当前设备不支持精确闹钟，已改为打开应用时提醒", Toast.LENGTH_SHORT).show()
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                }
            } catch (_: Exception) { }
            return
        }
        val ok = scheduler.applyDaily(
            DailyReminderConfig(hour, minute, text.ifEmpty { DEFAULT_REMINDER_TEXT })
        )
        if (!ok) {
            // degrade: keep switch on for in-app banner
            if (toast) {
                Toast.makeText(context, "当前设备不支持后台提醒，已改为打开应用时提醒", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (toast) {
            Toast.makeText(
                context,
                "已设置每天 ${pad2(hour)}:${pad2(minute)} 提醒",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val current = snap
    if (current == null) {
        Column(modifier.fillMaxSize()) {}
        return
    }

    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (_: Exception) {
        ""
    }

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
        SectionCard(title = "隐私与安全") {
            SwitchRow(
                title = "应用锁",
                subtitle = if (current.hasPin) "已设置 PIN" else "用 PIN 保护应用",
                checked = current.lockEnabled && current.hasPin,
                onCheckedChange = { enabled ->
                    scope.launch {
                        if (enabled) {
                            if (!current.hasPin) {
                                showPinSetup = true
                            } else {
                                store.setLockEnabled(true)
                            }
                        } else {
                            if (current.hasPin) {
                                showPinDisable = true
                            } else {
                                store.setLockEnabled(false)
                            }
                        }
                    }
                }
            )
            if (current.hasPin) {
                ActionRow("重新设置 PIN") { showPinSetup = true; pin1 = ""; pin2 = "" }
            }
            if (bioStatus.hardwareSupported) {
                SwitchRow(
                    title = "生物识别解锁",
                    subtitle = if (bioStatus.enrolled) "指纹 / 人脸" else "未录入，请先在系统设置中录入",
                    checked = current.biometricEnabled,
                    enabled = current.lockEnabled && current.hasPin,
                    onCheckedChange = { v ->
                        scope.launch { store.setBiometricEnabled(v) }
                    }
                )
            }
            SwitchRow(
                title = "防截屏保护",
                subtitle = if (current.privacyModeEnabled) "截屏与最近任务缩略图已遮罩" else "已关闭，允许截屏与任务预览",
                checked = current.privacyModeEnabled,
                onCheckedChange = { v ->
                    scope.launch {
                        store.setPrivacyModeEnabled(v)
                        onPrivacyModeChanged(v)
                    }
                }
            )
        }

        SectionCard(title = "提醒") {
            SwitchRow(
                title = "每日定时提醒",
                subtitle = "默认 ${pad2(current.reminderHour)}:${pad2(current.reminderMinute)}",
                checked = current.reminderEnabled,
                onCheckedChange = { v ->
                    scope.launch {
                        persistReminder(
                            v,
                            current.reminderHour,
                            current.reminderMinute,
                            current.reminderText,
                            toast = v
                        )
                    }
                }
            )
            if (current.reminderEnabled) {
                ActionRow(
                    title = "提醒时间",
                    value = "${pad2(current.reminderHour)}:${pad2(current.reminderMinute)}"
                ) {
                    TimePickerDialog(
                        context,
                        { _, h, m ->
                            scope.launch {
                                persistReminder(true, h, m, current.reminderText, toast = true)
                            }
                        },
                        current.reminderHour,
                        current.reminderMinute,
                        true
                    ).show()
                }
                OutlinedTextField(
                    value = reminderTextDraft,
                    onValueChange = { reminderTextDraft = it.take(30) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    label = { Text("提醒文案") },
                    singleLine = true
                )
            }
            SwitchRow(
                title = "葛神模式",
                subtitle = "记录后 8 小时提醒一次",
                checked = current.geshenEnabled,
                onCheckedChange = { v ->
                    scope.launch {
                        store.setGeshenEnabled(v)
                        val scheduler = ReminderScheduler.getInstance(context)
                        if (!v) {
                            scheduler.cancelGeshen()
                            return@launch
                        }
                        if (!ensureNotificationAllowed()) {
                            store.setGeshenEnabled(false)
                            Toast.makeText(context, "需要允许通知才能收到提醒", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        Toast.makeText(
                            context,
                            "葛神模式：记录后 8 小时提醒你再来一次",
                            Toast.LENGTH_SHORT
                        ).show()
                        val last = RecordRepository.getInstance(context).getAll().firstOrNull()
                        if (last != null) {
                            scheduler.syncGeshenFromLastRecord(last.timestamp)
                        }
                    }
                }
            )
        }

        SectionCard(title = "数据") {
            ActionRow("导出备份") {
                exportLauncher.launch(backupFileName(System.currentTimeMillis()))
            }
            ActionRow("导入备份（替换现有）") {
                importLauncher.launch(arrayOf("application/json", "*/*"))
            }
            ActionRow("清空全部记录", danger = true) { showClearConfirm = true }
        }

        SectionCard(title = "关于") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("鹿了么", color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (versionName.isNotEmpty()) "v$versionName" else "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "本地私密健康记录，数据仅保存在本机。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    if (showPinSetup) {
        AlertDialog(
            onDismissRequest = { showPinSetup = false },
            title = { Text("设置 PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pin1,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin1 = it },
                        label = { Text("输入 4–6 位数字") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pin2,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin2 = it },
                        label = { Text("再次确认") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (pin1.length !in 4..6) {
                            Toast.makeText(context, "PIN 需为 4–6 位数字", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        if (pin1 != pin2) {
                            Toast.makeText(context, "两次输入不一致", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val salt = randomSalt()
                        val hash = hashPin(pin1, salt)
                        store.savePin(hash, salt)
                        showPinSetup = false
                        pin1 = ""
                        pin2 = ""
                        Toast.makeText(context, "PIN 已设置", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetup = false }) { Text("取消") }
            }
        )
    }

    if (showPinDisable) {
        AlertDialog(
            onDismissRequest = { showPinDisable = false },
            title = { Text("关闭应用锁") },
            text = {
                OutlinedTextField(
                    value = disablePin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) disablePin = it },
                    label = { Text("输入当前 PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val ok = verifyPin(disablePin, current.pinSalt, current.pinHash)
                        if (!ok) {
                            Toast.makeText(context, "PIN 不正确", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        store.resetLock()
                        showPinDisable = false
                        disablePin = ""
                        Toast.makeText(context, "应用锁已关闭", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showPinDisable = false }) { Text("取消") }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空全部记录？") },
            text = { Text("此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        RecordRepository.getInstance(context).clearAll()
                        ReminderScheduler.getInstance(context).cancelGeshen()
                        showClearConfirm = false
                        Toast.makeText(context, "已清空", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("清空", color = extras.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    val extras = LocalExtraColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(extras.card, RoundedCornerShape(DesignTokens.CARD_RADIUS))
        ) {
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun ActionRow(
    title: String,
    value: String = "",
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val extras = LocalExtraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            color = if (danger) extras.danger else MaterialTheme.colorScheme.onSurface
        )
        if (value.isNotEmpty()) {
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun pad2(n: Int): String = String.format(Locale.US, "%02d", n)

private fun backupFileName(ts: Long): String {
    val d = Calendar.getInstance().apply { timeInMillis = ts }
    val date = String.format(
        Locale.US,
        "%04d%02d%02d",
        d.get(Calendar.YEAR),
        d.get(Calendar.MONTH) + 1,
        d.get(Calendar.DAY_OF_MONTH)
    )
    val time = String.format(
        Locale.US,
        "%02d%02d",
        d.get(Calendar.HOUR_OF_DAY),
        d.get(Calendar.MINUTE)
    )
    return "luleme_backup_$date-$time.json"
}
