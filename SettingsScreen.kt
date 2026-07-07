package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SpideyBlue
import com.example.ui.theme.SpideyRed

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsScreen(viewModel: SpideyViewModel) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val context = LocalContext.current

    var selectedAppToLimit by remember { mutableStateOf<AppInfoItem?>(null) }
    var isAppTimerListExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // App Timers Expandable Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAppTimerListExpanded = !isAppTimerListExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = SpideyRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Manage App Timers",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Configure custom daily usage limits",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isAppTimerListExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand list",
                            tint = Color(0xFF64748B)
                        )
                    }

                    AnimatedVisibility(
                        visible = isAppTimerListExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(
                                "Tap an application below to set or modify its daily screen time limit.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Render all launchable apps
                            dashboardState.appUsageList.forEach { appItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedAppToLimit = appItem }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val imageBitmap = remember(appItem.icon) {
                                            val drawable = appItem.icon
                                            val bitmap = if (drawable is BitmapDrawable) {
                                                drawable.bitmap
                                            } else {
                                                val bmp = Bitmap.createBitmap(
                                                    drawable.intrinsicWidth.coerceAtLeast(1),
                                                    drawable.intrinsicHeight.coerceAtLeast(1),
                                                    Bitmap.Config.ARGB_8888
                                                )
                                                val canvas = Canvas(bmp)
                                                drawable.setBounds(0, 0, canvas.width, canvas.height)
                                                drawable.draw(canvas)
                                                bmp
                                            }
                                            bitmap.asImageBitmap()
                                        }

                                        // App Icon Container
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(8.dp))
                                                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(8.dp))
                                                .padding(3.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                bitmap = imageBitmap,
                                                contentDescription = null,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Text(
                                            text = appItem.appName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }

                                    if (appItem.limitMinutes != null) {
                                        Box(
                                            modifier = Modifier
                                                .background(SpideyRed.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${appItem.limitMinutes} min",
                                                color = SpideyRed,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Not limited",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Battery Optimization Exemptions Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BatteryAlert,
                            contentDescription = null,
                            tint = SpideyRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Battery Background Check",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Due to strict OS optimization guidelines, some manufacturers aggressively terminate background services, which can affect Focus Mode and App Timer alerts. Request an optimization exemption to ensure seamless coverage.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pm != null) {
                                val intent = Intent().apply {
                                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                    data = Uri.parse("package:${context.packageName}")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(fallbackIntent)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpideyBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Exempt Battery Optimization", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // About Card (Accent Block in SpideyBlue)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SpideyBlue),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(SpideyRed, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterCenterFocus,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Spidey Focus v1.1",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "CLAIM BACK YOUR TIME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFCDD2), // light red
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Spidey Focus is a completely native, offline-first digital wellbeing application designed for devices that do not have Google's Digital Wellbeing suite. Built with high performance Jetpack Compose, localized Room storage, and secure offline telemetry.",
                        fontSize = 12.sp,
                        color = Color(0xFFE2E8F0),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }

    // Timer Limit Selection Dialog
    if (selectedAppToLimit != null) {
        val app = selectedAppToLimit!!
        var limitMinutesInput by remember { mutableStateOf(app.limitMinutes ?: 30) }

        AlertDialog(
            onDismissRequest = { selectedAppToLimit = null },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Timer Limit for ${app.appName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Set daily screen time allowed for this app. Spidey Focus will block access automatically when reached.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 60, 120).forEach { mins ->
                            val isSelected = limitMinutesInput == mins
                            Button(
                                onClick = { limitMinutesInput = mins },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) SpideyRed else Color(0xFFF1F5F9),
                                    contentColor = if (isSelected) Color.White else Color(0xFF475569)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                val label = if (mins >= 60) "${mins / 60}h" else "${mins}m"
                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Custom slider
                    Text(
                        text = "Custom Limit: $limitMinutesInput minutes",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpideyRed
                    )

                    Slider(
                        value = limitMinutesInput.toFloat(),
                        onValueChange = { limitMinutesInput = it.toInt() },
                        valueRange = 5f..300f,
                        steps = 59,
                        colors = SliderDefaults.colors(
                            thumbColor = SpideyRed,
                            activeTrackColor = SpideyRed,
                            inactiveTrackColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setAppLimit(app.packageName, limitMinutesInput)
                        selectedAppToLimit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpideyRed)
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (app.limitMinutes != null) {
                        TextButton(
                            onClick = {
                                viewModel.removeAppLimit(app.packageName)
                                selectedAppToLimit = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = SpideyRed)
                        ) {
                            Text("Remove Limit", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    TextButton(
                        onClick = { selectedAppToLimit = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF64748B))
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }
                }
            }
        )
    }
}
