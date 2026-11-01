package com.luleme.android.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.fragment.app.FragmentActivity
import com.luleme.android.SessionLockState
import com.luleme.android.data.SettingsStore
import com.luleme.android.logic.getBiometricStatus
import com.luleme.android.logic.startBiometricAuth
import com.luleme.android.logic.verifyPin
import com.luleme.android.presentation.theme.LocalExtraColors
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extras = LocalExtraColors.current
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var salt by remember { mutableStateOf("") }
    var hash by remember { mutableStateOf("") }
    var bioEnabled by remember { mutableStateOf(false) }
    var bioAvailable by remember { mutableStateOf(false) }
    var bioEnrolled by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }
    // Expected PIN length derived after first attempt range 4-6; verify when length in 4..6
    // and matches stored hash. Auto-verify at each length from 4 upwards.
    var targetLen by remember { mutableStateOf(6) }

    LaunchedEffect(Unit) {
        val store = SettingsStore.getInstance(context)
        salt = store.getPinSalt()
        hash = store.getPinHash()
        bioEnabled = store.isBiometricEnabled()
        if (bioEnabled) {
            val status = getBiometricStatus(context)
            bioAvailable = status.hardwareSupported
            bioEnrolled = status.enrolled
        }
        // Prefer stored length hint: try 4..6 by verifying progressively
        targetLen = 6
    }

    fun unlock() {
        SessionLockState.unlock()
        onUnlocked()
    }

    fun verifyCurrent() {
        if (verifying) return
        scope.launch {
            verifying = true
            try {
                val ok = verifyPin(pin, salt, hash)
                if (ok) {
                    unlock()
                } else if (pin.length >= 6) {
                    pin = ""
                    errorMsg = "PIN 不正确，请重试"
                    Toast.makeText(context, "PIN 不正确", Toast.LENGTH_SHORT).show()
                }
            } finally {
                verifying = false
            }
        }
    }

    fun onKey(key: String) {
        if (key == "del") {
            pin = pin.dropLast(1)
            errorMsg = ""
            return
        }
        if (pin.length >= 6) return
        pin += key
        if (pin.length in 4..6) {
            scope.launch {
                if (verifying) return@launch
                verifying = true
                try {
                    if (verifyPin(pin, salt, hash)) {
                        unlock()
                    } else if (pin.length == 6) {
                        pin = ""
                        errorMsg = "PIN 不正确，请重试"
                        Toast.makeText(context, "PIN 不正确", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    verifying = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🦌", fontSize = 48.sp)
        Text(
            "鹿了么",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(6) { i ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(
                            if (i < pin.length) MaterialTheme.colorScheme.primary else extras.border,
                            CircleShape
                        )
                )
            }
        }
        Text(
            errorMsg,
            fontSize = 12.sp,
            color = extras.danger,
            modifier = Modifier.height(18.dp)
        )
        Spacer(Modifier.height(12.dp))
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "del")
        )
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(Modifier.size(72.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(extras.card, CircleShape)
                                .clickable { onKey(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (key == "del") "⌫" else key,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        if (bioEnabled && bioAvailable) {
            Button(
                onClick = {
                    val activity = context as? FragmentActivity
                    if (activity == null) {
                        Toast.makeText(context, "无法启动生物识别", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        val ok = startBiometricAuth(activity, "解锁鹿了么")
                        if (ok) {
                            unlock()
                        } else if (!bioEnrolled) {
                            Toast.makeText(
                                context,
                                "未录入指纹/人脸，请先在系统设置中录入",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(context, "验证失败，请重试或输入 PIN", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = extras.primarySoft,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(22.dp)
            ) {
                Text("指纹 / 人脸解锁")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "忘记 PIN 时，只能通过清除应用数据恢复",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
