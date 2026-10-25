package com.example.binancephonescanner

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.binancephonescanner.databinding.ActivityConditionBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ConditionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConditionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConditionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()

        binding.btnSaveCondition.setOnClickListener {
            val name = binding.etTaskName.text.toString().ifEmpty { "감시 규칙" }
            val timeFrame = TimeFrame.values()[binding.spTimeframe.selectedItemPosition]
            val condType = ConditionType.values()[binding.spConditionType.selectedItemPosition]
            val value = binding.etValue.text.toString().toDoubleOrNull() ?: 0.0

            val newTask = ScanTask(
                id = System.currentTimeMillis().toString(),
                name = name,
                timeFrame = timeFrame,
                conditionType = condType,
                targetValue = value
            )

            saveTask(newTask)
            Toast.makeText(this, "조건이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupSpinners() {
        val timeframes = TimeFrame.values().map { it.code }
        val tfAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, timeframes)
        binding.spTimeframe.adapter = tfAdapter

        val conditions = ConditionType.values().map { it.title }
        val condAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, conditions)
        binding.spConditionType.adapter = condAdapter
    }

    private fun saveTask(task: ScanTask) {
        val prefs = getSharedPreferences("scanner_prefs", MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString("tasks", "[]")
        val type = object : TypeToken<MutableList<ScanTask>>() {}.type
        val taskList: MutableList<ScanTask> = gson.fromJson(json, type) ?: mutableListOf()
        
        taskList.add(task)
        prefs.edit().putString("tasks", gson.toJson(taskList)).apply()
    }
}
