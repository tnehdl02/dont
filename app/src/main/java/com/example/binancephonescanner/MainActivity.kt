package com.example.binancephonescanner

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.binancephonescanner.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupIntervalSpinner()

        binding.btnOpenSettings.setOnClickListener {
            startActivity(Intent(this, ConditionActivity::class.java))
        }

        binding.btnStart.setOnClickListener {
            startScanning()
        }

        binding.btnStop.setOnClickListener {
            stopScanning()
        }
    }

    override fun onResume() {
        super.onResume()
        loadTaskList()
    }

    private fun setupIntervalSpinner() {
        val intervals = listOf("정각마다 (매시 0분)", "매 50분마다", "매 59분마다")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, intervals)
        binding.spInterval.adapter = adapter
    }

    private fun loadTaskList() {
        binding.containerTasks.removeAllViews()
        val prefs = getSharedPreferences("scanner_prefs", MODE_PRIVATE)
        val json = prefs.getString("tasks", "[]")
        val gson = Gson()
        val type = object : TypeToken<List<ScanTask>>() {}.type
        val tasks: List<ScanTask> = gson.fromJson(json, type) ?: emptyList()

        for (task in tasks) {
            val textView = TextView(this).apply {
                text = "• ${task.name} | ${task.timeFrame.code} | ${task.conditionType.title} (${task.targetValue})"
                setTextColor(resources.getColor(R.color.text_white, null))
                setPadding(0, 8, 0, 8)
            }
            binding.containerTasks.addView(textView)
        }
    }

    private fun startScanning() {
        val intent = Intent(this, ScannerService::class.java).apply { action = "START_SCAN" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        scheduleAlarm()
        binding.tvStatus.text = "상태: 감시 작동 중"
        binding.progressBar.progress = 100
        Toast.makeText(this, "첫 감시를 시작했으며 백그라운드 예약을 완료했습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun stopScanning() {
        val intent = Intent(this, ScannerService::class.java)
        stopService(intent)

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)

        binding.tvStatus.text = "상태: 중지됨"
        binding.progressBar.progress = 0
        Toast.makeText(this, "감시가 중지되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun scheduleAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 50)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }
}
