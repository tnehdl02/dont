package com.example.binancephonescanner

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, ScannerService::class.java).apply {
            action = "START_SCAN"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}

class ScannerService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val client = OkHttpClient()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "SCANNER_CHANNEL")
            .setContentTitle("바이낸스 감시 실행 중")
            .setContentText("실시간 조건 감시를 진행하고 있습니다.")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()

        startForeground(1001, notification)

        if (intent?.action == "START_SCAN") {
            performScan()
        }

        return START_STICKY
    }

    private fun performScan() {
        scope.launch {
            val prefs = getSharedPreferences("scanner_prefs", MODE_PRIVATE)
            val json = prefs.getString("tasks", "[]")
            val gson = Gson()
            val type = object : TypeToken<List<ScanTask>>() {}.type
            val tasks: List<ScanTask> = gson.fromJson(json, type) ?: emptyList()

            for (task in tasks) {
                val isTriggered = checkBinanceCondition(task)
                if (isTriggered) {
                    sendAlertNotification("조건 포착!", "[${task.name}] 설정 조건에 부합하는 종목을 발견했습니다.")
                }
            }
        }
    }

    private fun checkBinanceCondition(task: ScanTask): Boolean {
        return try {
            val request = Request.Builder()
                .url("https://fapi.binance.com/fapi/v1/ticker/24hr")
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun sendAlertNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, "ALERT_CHANNEL")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("SCANNER_CHANNEL", "Background Scanner", NotificationManager.IMPORTANCE_LOW)
            val alertChannel = NotificationChannel("ALERT_CHANNEL", "Scan Alerts", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
