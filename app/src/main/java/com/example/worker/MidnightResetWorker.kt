package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppRepository

class MidnightResetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = AppRepository(applicationContext)
        // Midnight reset: delete all temporary break passes
        val passes = repository.getAllBreakPasses()
        for (pass in passes) {
            repository.deleteBreakPass(pass.packageName)
        }
        return Result.success()
    }
}
