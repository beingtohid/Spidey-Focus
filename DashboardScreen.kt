package com.example.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(viewModel: SpideyViewModel) {
    val state by viewModel.dashboardState.collectAsState()
    val focusOn by viewModel.focusModeEnabled.collectAsState()
    val context = LocalContext.current

    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = { viewModel.refreshDashboard() }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (state.isPermissionMissingOnboard) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(Color(0xFFFFEBEE), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Permission needed",
                        tint = SpideyRed,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Usage Access Required",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "To track and show your daily screen time, Spidey Focus needs Usage Stats permission. Please enable it in system settings.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpideyRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                // Unified Summary & Bar Chart Card (Professional Polish Style)
                item {
                    UnifiedSummaryCard(
                        totalMinutes = state.totalScreenTimeMinutes,
                        unlockCount = state.unlockCountToday,
                        weeklyStats = state.weeklyScreenTime
                    )
                }

                // Interactive Focus Banner (Matches Tailwind)
                item {
                    FocusBanner(
                        focusOn = focusOn,
                        onToggle = { viewModel.toggleFocusMode(it) }
                    )
                }

                // Header for App List
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily App Usage",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569) // slate-600
                        )
                        Text(
                            text = "View All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SpideyBlue,
                            modifier = Modifier.clickable { /* No action needed */ }
                        )
                    }
                }

                if (state.appUsageList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.HourglassEmpty, "Empty stats", tint = SpideyRed, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "No usage data recorded yet. Open other apps on your device to start tracking!",
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                } else {
                    items(state.appUsageList, key = { it.packageName }) { appItem ->
                        AppUsageRow(item = appItem)
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = state.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = SpideyRed
        )
    }
}

@Composable
fun UnifiedSummaryCard(
    totalMinutes: Int,
    unlockCount: Int,
    weeklyStats: List<Pair<String, Float>>
) {
    val hrs = totalMinutes / 60
    val mins = totalMinutes % 60
    val timeString = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Stats Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Total Screen Time",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B) // slate-500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeString,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A), // slate-900
                        letterSpacing = (-1).sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "-12% today",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32) // green-700
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$unlockCount UNLOCKS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8), // slate-400
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bar Chart matching exact Tailwind spec
            val maxVal = weeklyStats.maxOfOrNull { it.second } ?: 100f
            val baseMax = if (maxVal < 10f) 60f else maxVal

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyStats.forEach { stat ->
                    val ratio = stat.second / baseMax
                    val barHeightFraction = ratio.coerceIn(0.02f, 1f)

                    // Peak day gets SpideyRed, others get SpideyBlue with alpha matching Tailwind bg-[#1A237E]/30
                    val isPeakDay = stat.first.equals("Sat", ignoreCase = true) || (stat.second == maxVal && stat.second > 0f)
                    val barColor = if (isPeakDay) SpideyRed else SpideyBlue.copy(alpha = 0.3f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(barHeightFraction)
                                    .background(barColor, shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stat.first,
                            fontSize = 9.sp,
                            color = if (isPeakDay) SpideyRed else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FocusBanner(focusOn: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SpideyBlue),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Glow Dot Container
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(if (focusOn) SpideyRed else Color.White.copy(alpha = 0.4f), shape = CircleShape)
                            .border(3.dp, Color.White.copy(alpha = 0.15f), shape = CircleShape)
                    )
                }

                Column {
                    Text(
                        text = "Focus Mode",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (focusOn) "Distracting apps are paused" else "Tap ON to mute distractions",
                        fontSize = 10.sp,
                        color = Color(0xFFBFDBFE) // blue-200
                    )
                }
            }

            // ON/OFF Pill Button
            Button(
                onClick = { onToggle(!focusOn) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (focusOn) Color.White else Color.White.copy(alpha = 0.15f),
                    contentColor = if (focusOn) SpideyBlue else Color.White
                ),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = if (focusOn) "ON" else "OFF",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun AppUsageRow(item: AppInfoItem) {
    val hrs = item.usageMinutes / 60
    val mins = item.usageMinutes % 60
    val durationText = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"

    val imageBitmap = remember(item.icon) {
        val drawable = item.icon
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF8FAFC))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon Container
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(12.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    bitmap = imageBitmap,
                    contentDescription = item.appName,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // App details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.appName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A) // slate-900
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${item.notificationCount} Notifications",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8), // slate-400
                    fontWeight = FontWeight.Medium
                )
            }

            // Duration and Limits
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = durationText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E293B) // slate-800
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (item.limitMinutes != null) "Limit: ${item.limitMinutes}m" else "No Limit",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.limitMinutes != null && item.usageMinutes >= item.limitMinutes) SpideyRed else Color(0xFF94A3B8)
                )
            }
        }
    }
}
