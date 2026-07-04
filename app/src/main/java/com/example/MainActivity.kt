package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SpideyBlue
import com.example.ui.theme.SpideyRed

enum class AppTab {
    Dashboard, Focus, Bedtime, Settings
}

class MainActivity : ComponentActivity() {

    private val viewModel: SpideyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val isOnboardingDone by viewModel.onboardingCompleted.collectAsState()

                if (!isOnboardingDone) {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onFinish = { viewModel.completeOnboarding() }
                    )
                } else {
                    MainAppLayout(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(viewModel: SpideyViewModel) {
    var activeTab by remember { mutableStateOf(AppTab.Dashboard) }

    // Periodically refresh data when tab is changed
    LaunchedEffect(activeTab) {
        viewModel.refreshDashboard()
    }

    Scaffold(
        topBar = {
            CustomHeader(activeTab = activeTab)
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.height(72.dp)
            ) {
                NavigationBarItem(
                    selected = activeTab == AppTab.Dashboard,
                    onClick = { activeTab = AppTab.Dashboard },
                    label = { Text("Stats", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Stats") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = SpideyRed,
                        indicatorColor = SpideyRed,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )

                NavigationBarItem(
                    selected = activeTab == AppTab.Focus,
                    onClick = { activeTab = AppTab.Focus },
                    label = { Text("Focus", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    icon = { Icon(Icons.Default.FilterCenterFocus, contentDescription = "Focus") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = SpideyBlue,
                        indicatorColor = SpideyBlue,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )

                NavigationBarItem(
                    selected = activeTab == AppTab.Bedtime,
                    onClick = { activeTab = AppTab.Bedtime },
                    label = { Text("Bedtime", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    icon = { Icon(Icons.Default.NightsStay, contentDescription = "Bedtime") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = SpideyBlue,
                        indicatorColor = SpideyBlue,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )

                NavigationBarItem(
                    selected = activeTab == AppTab.Settings,
                    onClick = { activeTab = AppTab.Settings },
                    label = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = SpideyBlue,
                        indicatorColor = SpideyBlue,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (activeTab) {
                AppTab.Dashboard -> DashboardScreen(viewModel = viewModel)
                AppTab.Focus -> FocusScreen(viewModel = viewModel)
                AppTab.Bedtime -> BedtimeScreen(viewModel = viewModel)
                AppTab.Settings -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CustomHeader(activeTab: AppTab) {
    val subtitle = when (activeTab) {
        AppTab.Dashboard -> "Digital Wellbeing"
        AppTab.Focus -> "Attention Controller"
        AppTab.Bedtime -> "Sleep Protection"
        AppTab.Settings -> "App Settings"
    }

    val title = when (activeTab) {
        AppTab.Dashboard -> "Spidey Focus"
        AppTab.Focus -> "Focus Mode"
        AppTab.Bedtime -> "Bedtime"
        AppTab.Settings -> "Settings"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = subtitle.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SpideyBlue.copy(alpha = 0.6f),
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SpideyBlue,
                letterSpacing = (-0.5).sp
            )
        }

        // Brand Badge (Target logo matching Tailwind bg-[#D32F2F])
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(SpideyRed, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(2.dp, Color.White, shape = RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White, shape = CircleShape)
                )
            }
        }
    }
}
