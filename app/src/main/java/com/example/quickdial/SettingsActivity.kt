package com.example.quickdial

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var avatarSizeSeekBar: SeekBar
    private lateinit var textSizeSeekBar: SeekBar
    private lateinit var avatarSizeText: TextView
    private lateinit var textSizeText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 获取屏幕宽度（以dp为单位）
        val screenWidth = (resources.displayMetrics.widthPixels / resources.displayMetrics.density).toInt()

        avatarSizeSeekBar = findViewById(R.id.avatar_size_seekbar)
        textSizeSeekBar = findViewById(R.id.text_size_seekbar)
        avatarSizeText = findViewById(R.id.avatar_size_text)
        textSizeText = findViewById(R.id.text_size_text)

        // 设置头像大小的范围（50dp 到屏幕宽度）
        avatarSizeSeekBar.max = screenWidth - 50  // 调整最大值，使进度值加上最小值等于实际值
        
        // 设置文字大小的范围（12sp 到屏幕宽度的1/10）
        textSizeSeekBar.max = (screenWidth / 10) - 12  // 调整最大值，使进度值加上最小值等于实际值

        // 加载保存的设置
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedAvatarSize = prefs.getInt("avatar_size", 80)
        val savedTextSize = prefs.getInt("text_size", 16)

        // 设置初始值（需要减去最小值）
        avatarSizeSeekBar.progress = savedAvatarSize - 50
        textSizeSeekBar.progress = savedTextSize - 12
        updateSizeTexts(savedAvatarSize, savedTextSize)

        // 监听滑动条变化
        avatarSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val actualSize = progress + 50  // 加上最小值得到实际值
                updateSizeTexts(actualSize, textSizeSeekBar.progress + 12)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        textSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val actualSize = progress + 12  // 加上最小值得到实际值
                updateSizeTexts(avatarSizeSeekBar.progress + 50, actualSize)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 保存设置
        findViewById<Button>(R.id.save_settings_button).setOnClickListener {
            saveSettings()
        }
    }

    private fun updateSizeTexts(avatarSize: Int, textSize: Int) {
        avatarSizeText.text = "${avatarSize}dp"
        textSizeText.text = "${textSize}sp"
    }

    private fun saveSettings() {
        getSharedPreferences("settings", Context.MODE_PRIVATE).edit().apply {
            putInt("avatar_size", avatarSizeSeekBar.progress + 50)  // 保存时加上最小值
            putInt("text_size", textSizeSeekBar.progress + 12)  // 保存时加上最小值
            apply()
        }
        setResult(RESULT_OK)
        finish()
    }
} 