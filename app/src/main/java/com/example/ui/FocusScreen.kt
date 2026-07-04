package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FocusSchedule
import com.example.ui.theme.SpideyBlue
import com.example.ui.theme.SpideyRed

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FocusScreen(viewModel: SpideyViewModel) {
    val focusOn by viewModel.focusModeEnabled.collectAsState()
    val schedule by viewModel.focusSchedule.collectAsState()
    val dashboardState by viewModel.dashboardState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isScheduleExpanded by remember { mutableStateOf(false) }

    val filteredApps = remember(dashboardState.appUsageList, searchQuery) {
        dashboardState.appUsageList.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Focus Mode Master Toggle Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if (focusOn) SpideyRed else Color(0xFFF1F5F9)),
                elevation = CardDefaults.cardElevation(defaultElevation = if (focusOn) 2.dp else 0.dp)
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
                            text = if (focusOn) "FOCUS MODE ACTIVE" else "FOCUS MODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (focusOn) SpideyRed else SpideyBlue,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (focusOn) "Distracting apps are paused" else "Block notifications and custom apps",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Switch(
                        checked = focusOn,
                        onCheckedChange = { viewModel.toggleFocusMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SpideyRed,
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }
        }

        // Automatic Schedule Card
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
                            .clickable { isScheduleExpanded = !isScheduleExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = SpideyBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Focus Schedule",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (schedule.enabled) "Active (${formatTime(schedule.startHour, schedule.startMin)} - ${formatTime(schedule.endHour, schedule.endMin)})" else "Disabled",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isScheduleExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand schedule settings",
                            tint = Color(0xFF64748B)
                        )
                    }

                    AnimatedVisibility(
                        visible = isScheduleExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Enable Schedule", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                                Switch(
                                    checked = schedule.enabled,
                                    onCheckedChange = {
                                        viewModel.updateFocusSchedule(schedule.copy(enabled = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = SpideyBlue
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Time Picker increments
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Start Time", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TimeSelector(
                                        hour = schedule.startHour,
                                        minute = schedule.startMin,
                                        onTimeChanged = { h, m ->
                                            viewModel.updateFocusSchedule(schedule.copy(startHour = h, startMin = m))
                                        }
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("End Time", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TimeSelector(
                                        hour = schedule.endHour,
                                        minute = schedule.endMin,
                                        onTimeChanged = { h, m ->
                                            viewModel.updateFocusSchedule(schedule.copy(endHour = h, endMin = m))
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Day list selection
                            Text("Active Days", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(8.dp))
                            DayOfWeekPicker(
                                selectedDaysString = schedule.days,
                                onDaysChanged = { newDays ->
                                    viewModel.updateFocusSchedule(schedule.copy(days = newDays))
                                }
                            )
                        }
                    }
                }
            }
        }

        // Search Distracting Apps Header
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "Distracting Applications",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569) // slate-600
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed applications...") },
                    leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color(0xFF64748B)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = SpideyBlue,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color(0xFFF1F5F9)), shape = RoundedCornerShape(16.dp))
                )
            }
        }

        // List of Apps
        items(filteredApps, key = { it.packageName }) { item ->
            DistractingAppRow(
                item = item,
                focusOn = focusOn,
                onCheckedChange = { isChecked ->
                    viewModel.toggleDistractingApp(item.packageName, isChecked)
                }
            )
        }
    }
}

@Composable
fun DistractingAppRow(
    item: AppInfoItem,
    focusOn: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val isPaused = focusOn && item.isDistracting

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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!item.isDistracting) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            val grayscaleFilter = if (isPaused) {
                ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            } else null

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(12.dp))
                    .padding(4.dp)
                    .alpha(if (isPaused) 0.5f else 1.0f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = item.appName,
                    colorFilter = grayscaleFilter,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.appName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPaused) Color(0xFF0F172A).copy(alpha = 0.5f) else Color(0xFF0F172A)
                )
                Text(
                    text = if (isPaused) "Paused • Distracting app" else if (item.isDistracting) "Marked as distracting" else "Tap to toggle distraction",
                    fontSize = 12.sp,
                    color = if (isPaused) SpideyRed.copy(alpha = 0.7f) else Color(0xFF64748B)
                )
            }

            Checkbox(
                checked = item.isDistracting,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = SpideyRed,
                    uncheckedColor = Color(0xFF94A3B8)
                )
            )
        }
    }
}

@Composable
fun TimeSelector(
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
fun DayOfWeekPicker(
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
                        color = if (isSelected) SpideyRed else Color(0xFFF1F5F9),
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

fun formatTime(hour: Int, minute: Int): String {
    return String.format("%02d:%02d", hour, minute)
}
