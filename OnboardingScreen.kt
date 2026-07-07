package com.example.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SpideyBlue
import com.example.ui.theme.SpideyRed

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: SpideyViewModel,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) }

    // Recheck permissions on resume to auto-advance if possible
    val hasUsage = viewModel.hasUsageAccess()
    val hasAccessibility = viewModel.isAccessibilityEnabled()
    val hasNotificationListener = viewModel.isNotificationListenerEnabled()
    val hasDnd = viewModel.hasDndAccess()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Proceed regardless of result (skip/allow are both handled)
        currentStep = 6
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF080B1C),
                        Color(0xFF131735),
                        Color(0xFF04060E)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(Color(0xFF1A1F45), shape = RoundedCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Steps indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                for (i in 1..6) {
                    Box(
                        modifier = Modifier
                            .size(if (currentStep == i) 12.dp else 8.dp)
                            .background(
                                color = if (currentStep == i) SpideyRed else Color(0xFF484E7B),
                                shape = CircleShape
                            )
                    )
                }
            }

            // Step Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn() + slideInHorizontally { it } with fadeOut() + slideOutHorizontally { -it }
                },
                label = "onboarding_step"
            ) { step ->
                when (step) {
                    1 -> OnboardingStepWelcome(onNext = { currentStep = 2 })
                    2 -> OnboardingStepPermission(
                        title = "Usage Access",
                        description = "Required to measure how much time you spend on each application and build screen time charts.",
                        icon = Icons.Default.TrendingUp,
                        buttonText = "Grant Usage Access",
                        isGranted = hasUsage,
                        onAction = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        onNext = { currentStep = 3 }
                    )
                    3 -> OnboardingStepPermission(
                        title = "Accessibility Service",
                        description = "Required to detect which application is currently active so we can block distracting apps and app limiters immediately.",
                        icon = Icons.Default.Visibility,
                        buttonText = "Enable Accessibility",
                        isGranted = hasAccessibility,
                        onAction = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        onNext = { currentStep = 4 }
                    )
                    4 -> OnboardingStepPermission(
                        title = "Notification Access",
                        description = "Required to count incoming notifications for each app and help you monitor alert triggers.",
                        icon = Icons.Default.Notifications,
                        buttonText = "Grant Notification Access",
                        isGranted = hasNotificationListener,
                        onAction = {
                            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        onNext = { currentStep = 5 }
                    )
                    5 -> OnboardingStepPermission(
                        title = "Do Not Disturb Access",
                        description = "Required to automatically silence notification alerts during Bedtime Mode.",
                        icon = Icons.Default.DoNotDisturbOn,
                        buttonText = "Grant DND Permission",
                        isGranted = hasDnd,
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } else {
                                currentStep = 6
                            }
                        },
                        onNext = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                currentStep = 6
                            } else {
                                viewModel.completeOnboarding()
                                onFinish()
                            }
                        }
                    )
                    6 -> OnboardingStepPermission(
                        title = "Post Notifications",
                        description = "Allow Spidey Focus to display status updates and a permanent toggle in the notification drawer.",
                        icon = Icons.Default.Announcement,
                        buttonText = "Allow Notifications",
                        isGranted = viewModel.hasPostNotificationsPermission(),
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.completeOnboarding()
                                onFinish()
                            }
                        },
                        onNext = {
                            viewModel.completeOnboarding()
                            onFinish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingStepWelcome(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    Brush.radialGradient(listOf(SpideyRed, Color.Transparent)),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Eco,
                contentDescription = "Spidey emblem",
                tint = Color.White,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to Spidey Focus",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Take complete control of your screen time. Block distracting apps, set healthy limits, and claim back your productivity with a subtle spider theme.",
            fontSize = 14.sp,
            color = Color(0xFFC5C9E0),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SpideyRed),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text("Let's Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OnboardingStepPermission(
    title: String,
    description: String,
    icon: ImageVector,
    buttonText: String,
    isGranted: Boolean,
    onAction: () -> Unit,
    onNext: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFF2A3163), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF4CAF50) else SpideyRed,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            if (isGranted) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.CheckCircle, "Granted", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = description,
            fontSize = 14.sp,
            color = Color(0xFFC5C9E0),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAction,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = if (isGranted) Color(0xFF3B447A) else SpideyRed),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text(if (isGranted) "Permission Enabled" else buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
        ) {
            Text(if (isGranted) "Continue" else "Skip / Proceed", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}
