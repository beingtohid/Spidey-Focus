package com.example.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BedtimeSchedule
import com.example.ui.theme.SpideyBlue
import com.example.ui.theme.SpideyRed

@Composable
fun BedtimeScreen(viewModel: SpideyViewModel) {
    val bedtimeOn by viewModel.bedtimeModeEnabled.collectAsState()
    val schedule by viewModel.bedtimeSchedule.collectAsState()
    val context = LocalContext.current
    val hasDnd = viewModel.hasDndAccess()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Redundant large title removed. We display a small, elegant description item
        item {
            Text(
                text = "Wind down and sleep soundly. Silence notifications and block distractions automatically.",
                fontSize = 13.sp,
                color = Color(0xFF64748B), // slate-500
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                lineHeight = 20.sp
            )
        }

        // DND Access Permission Card
        if (!hasDnd) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)), // rose-50
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFE4E6)) // rose-100
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DoNotDisturbOn, "DND needed", tint = SpideyRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Do Not Disturb Required",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF9F1239) // rose-800
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To automatically silence alerts and trigger Do Not Disturb during bedtime hours, Spidey Focus needs DND policy access.",
                            fontSize = 13.sp,
                            color = Color(0xFF9F1239).copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SpideyRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Grant DND Access", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Master Toggle Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if (bedtimeOn) SpideyBlue else Color(0xFFF1F5F9)),
                elevation = CardDefaults.cardElevation(defaultElevation = if (bedtimeOn) 2.dp else 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (bedtimeOn) "BEDTIME PROTECTION ACTIVE" else "BEDTIME MODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (bedtimeOn) SpideyBlue else Color(0xFF475569),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (bedtimeOn) "Do Not Disturb is silencing notifications" else "Mute all notifications during bedtime hours",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Switch(
                        checked = bedtimeOn,
                        onCheckedChange = { viewModel.toggleBedtimeMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SpideyBlue,
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }
        }

        // Automatic Bedtime Schedule Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NightsStay,
                                contentDescription = null,
                                tint = SpideyBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Schedule Settings",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Switch(
                            checked = schedule.enabled,
                            onCheckedChange = {
                                viewModel.updateBedtimeSchedule(schedule.copy(enabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SpideyBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bedtime Start", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(8.dp))
                            BedtimeTimeSelector(
                                hour = schedule.startHour,
                                minute = schedule.startMin,
                                onTimeChanged = { h, m ->
                                    viewModel.updateBedtimeSchedule(schedule.copy(startHour = h, startMin = m))
                                }
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Wake Up End", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(8.dp))
                            BedtimeTimeSelector(
                                hour = schedule.endHour,
                                minute = schedule.endMin,
                                onTimeChanged = { h, m ->
                                    viewModel.updateBedtimeSchedule(schedule.copy(endHour = h, endMin = m))
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Bedtime Days", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(8.dp))
                    BedtimeDayOfWeekPicker(
                        selectedDaysString = schedule.days,
                        onDaysChanged = { newDays ->
                            viewModel.updateBedtimeSchedule(schedule.copy(days = newDays))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BedtimeTimeSelector(
    hour: Int,
    minute: Int,
    onTimeChanged: (Int, Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Increment hour",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onTimeChanged((hour + 1) % 24, minute) },
                tint = SpideyBlue
            )
            Text(
                text = String.format("%02d", hour),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Decrement hour",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onTimeChanged((hour + 23) % 24, minute) },
                tint = SpideyBlue
            )
        }

        Text(":", fontSize = 18.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Increment minute",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onTimeChanged(hour, (minute + 5) % 60) },
                tint = SpideyBlue
            )
            Text(
                text = String.format("%02d", minute),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Decrement minute",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onTimeChanged(hour, (minute + 55) % 60) },
                tint = SpideyBlue
            )
        }
    }
}

@Composable
fun BedtimeDayOfWeekPicker(
    selectedDaysString: String,
    onDaysChanged: (String) -> Unit
) {
    val dayMap = mapOf(
        2 to "M",
        3 to "T",
        4 to "W",
        5 to "T",
        6 to "F",
        7 to "S",
        1 to "S"
    )

    val selectedDays = remember(selectedDaysString) {
        selectedDaysString.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        dayMap.forEach { (dayInt, label) ->
            val isSelected = selectedDays.contains(dayInt)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (isSelected) SpideyBlue else Color(0xFFF1F5F9),
                        shape = CircleShape
                    )
                    .clickable {
                        val newSet = if (isSelected) {
                            selectedDays - dayInt
                        } else {
                            selectedDays + dayInt
                        }
                        onDaysChanged(newSet.sorted().joinToString(","))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else Color(0xFF475569),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
