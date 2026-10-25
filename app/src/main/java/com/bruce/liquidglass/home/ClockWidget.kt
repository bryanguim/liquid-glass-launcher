package com.bruce.liquidglass.home

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.AlarmClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bruce.liquidglass.data.ClockStyle
import com.bruce.liquidglass.data.LauncherSettings
import com.bruce.liquidglass.ui.Glass
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Battery(val level: Int, val charging: Boolean)

private fun readBattery(ctx: Context): Battery {
    val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val pct = if (level >= 0 && scale > 0) level * 100 / scale else 0
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    return Battery(pct, charging)
}

@Composable
fun ClockWidget(settings: LauncherSettings, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var battery by remember { mutableStateOf(readBattery(ctx)) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            battery = readBattery(ctx)
            delay(1000L * (60 - (now / 1000 % 60)).coerceAtLeast(1))
        }
    }
    val time = remember(now / 60000) { SimpleDateFormat("HH:mm", Locale.FRANCE).format(Date(now)) }
    val date = remember(now / 60000) {
        SimpleDateFormat("EEEE d MMMM", Locale.FRANCE).format(Date(now)).replaceFirstChar { it.uppercase() }
    }

    Glass(
        modifier.pointerInput(Unit) {
            detectTapGestures {
                runCatching {
                    ctx.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Text(date, color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    time,
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = when (settings.clockStyle) {
                        ClockStyle.THIN -> FontWeight.Light
                        ClockStyle.REGULAR -> FontWeight.Normal
                        ClockStyle.BOLD -> FontWeight.Bold
                    },
                    letterSpacing = (-1.5).sp,
                    lineHeight = 66.sp,
                )
            }
            if (settings.showBattery) BatteryRing(battery)
        }
    }
}

@Composable
private fun BatteryRing(b: Battery) {
    Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(Color.White.copy(alpha = 0.15f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            val color = when {
                b.charging -> Color(0xFF30D158)
                b.level <= 20 -> Color(0xFFFF453A)
                else -> Color.White
            }
            drawArc(color, -90f, 360f * b.level / 100f, false, Offset(inset, inset), arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (b.charging) Icon(Icons.Rounded.Bolt, null, tint = Color(0xFF30D158), modifier = Modifier.size(14.dp))
            Text("${b.level}%", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(0.dp))
        }
    }
}
