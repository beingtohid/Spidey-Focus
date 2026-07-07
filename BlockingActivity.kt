package com.example.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.AppRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SpideyBlue
import com.example.ui.theme.SpideyRed
import com.example.worker.RevokePassWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BlockingActivity : ComponentActivity() {

    private lateinit var repository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = AppRepository(applicationContext)

        val targetPackage = intent.getStringExtra("target_package") ?: ""
        val blockType = intent.getStringExtra("block_type") ?: "focus_mode"
        val blockMessage = intent.getStringExtra("block_message") ?: "This app is paused."

        val appLabel = try {
            val pm = packageManager
            val info = pm.getApplicationInfo(targetPackage, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            targetPackage.substringAfterLast('.')
        }

        setContent {
            MyApplicationTheme(darkTheme = true) { // Always dark theme for premium/cyber look
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BlockingScreen(
                        appName = appLabel,
                        packageName = targetPackage,
                        blockType = blockType,
                        message = blockMessage,
                        onGoHome = { goHome() },
                        onGrantBreak = { durationMinutes ->
                            grantBreakPass(targetPackage, durationMinutes)
                        }
                    )
                }
            }
        }
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    private fun grantBreakPass(targetPackage: String, minutes: Int) {
        lifecycleScope.launch {
            val expiryTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes.toLong())
            repository.insertBreakPass(targetPackage, expiryTime)

            // Schedule WorkManager job to revoke the pass
            val workManager = WorkManager.getInstance(applicationContext)
            val inputData = Data.Builder()
                .putString("package_name", targetPackage)
                .build()
            
            val workRequest = OneTimeWorkRequestBuilder<RevokePassWorker>()
                .setInputData(inputData)
                .setInitialDelay(minutes.toLong(), TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniqueWork(
                "revoke_$targetPackage",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            // Relaunch the blocked app
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
            finish()
        }
    }
}

@Composable
fun BlockingScreen(
    appName: String,
    packageName: String,
    blockType: String,
    message: String,
    onGoHome: () -> Unit,
    onGrantBreak: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF060814),
                        Color(0xFF131735),
                        Color(0xFF03050C)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181D3D)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.linearGradient(listOf(SpideyRed, SpideyBlue)),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (blockType) {
                            "timer_exceeded" -> Icons.Default.HourglassEmpty
                            "bedtime_mode" -> Icons.Default.Lock
                            else -> Icons.Default.Warning
                        },
                        contentDescription = "Warning icon",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // App Name
                Text(
                    text = appName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Block Message
                Text(
                    text = message,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFB4B9D2),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (blockType == "focus_mode") {
                    // Show break buttons (Feature 5)
                    Text(
                        text = "Need a quick break?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpideyRed,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onGrantBreak(5) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B3361)),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text("5m", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onGrantBreak(10) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B3361)),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text("10m", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onGrantBreak(30) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B3361)),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text("30m", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onGrantBreak(45) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B3361)),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text("45m", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = onGoHome,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                    ) {
                        Text("Stay Focused", fontWeight = FontWeight.Bold)
                    }

                } else {
                    // Timer Exceeded or Bedtime Mode
                    Button(
                        onClick = onGoHome,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SpideyRed)
                    ) {
                        Text("Back to Home", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
