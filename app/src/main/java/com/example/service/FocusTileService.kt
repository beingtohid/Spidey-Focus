package com.example.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.data.AppRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class FocusTileService : TileService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var repository: AppRepository
    private var collectJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = AppRepository(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        // Collect focus mode state flow to update tile in real time
        collectJob = serviceScope.launch {
            repository.focusModeEnabled.collect { isEnabled ->
                withContext(Dispatchers.Main) {
                    updateTileState(isEnabled)
                }
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        collectJob?.cancel()
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            val currentState = repository.focusModeEnabled.first()
            val newState = !currentState
            repository.setFocusModeEnabled(newState)

            // Trigger Foreground Service starting/stopping based on toggle
            withContext(Dispatchers.Main) {
                updateTileState(newState)
                val intent = FocusService.ACTION_START
                val serviceIntent = android.content.Intent(applicationContext, FocusService::class.java)
                if (newState) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                } else {
                    stopService(serviceIntent)
                }
            }
        }
    }

    private fun updateTileState(isEnabled: Boolean) {
        val activeTile = qsTile ?: return
        activeTile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activeTile.subtitle = if (isEnabled) "On" else "Off"
        }
        
        activeTile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
