package com.xiaomi.sleep

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * XiaomiSleep 极简主界面
 */
class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvMediaStatus: TextView
    private lateinit var btnGrantPermission: Button
    private lateinit var switchFadeOut: SwitchMaterial
    private lateinit var switchBedsideMonitor: SwitchMaterial
    private lateinit var btnTestImmediate: Button
    private lateinit var btnTestFadeOut: Button

    private var bedsideMonitor: BedsideMotionMonitor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvMediaStatus = findViewById(R.id.tvMediaStatus)
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        switchFadeOut = findViewById(R.id.switchFadeOut)
        switchBedsideMonitor = findViewById(R.id.switchBedsideMonitor)
        btnTestImmediate = findViewById(R.id.btnTestImmediate)
        btnTestFadeOut = findViewById(R.id.btnTestFadeOut)

        tvStatus.text = "● 手环入睡监听：已就绪 (广播接收模式)"
    }

    private fun setupListeners() {
        // 跳转授予通知使用权（用于精准读取并控制各播放器的 MediaSession）
        btnGrantPermission.setOnClickListener {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }

        // 立即暂停测试
        btnTestImmediate.setOnClickListener {
            MediaPauseHelper.pauseAllAudioImmediately(this)
            Toast.makeText(this, "已向系统发送全局暂停指令", Toast.LENGTH_SHORT).show()
        }

        // 淡出暂停测试
        btnTestFadeOut.setOnClickListener {
            Toast.makeText(this, "开始 10 秒音量淡出并暂停测试...", Toast.LENGTH_SHORT).show()
            MediaPauseHelper.pauseAllAudioWithFadeOut(this, 10000L) {
                Toast.makeText(this, "淡出暂停测试完成！", Toast.LENGTH_SHORT).show()
            }
        }

        // 床头静止守护模式开关
        switchBedsideMonitor.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (bedsideMonitor == null) {
                    bedsideMonitor = BedsideMotionMonitor(this, 15) {
                        Toast.makeText(this, "床头连续静止触发：已平滑暂停播放", Toast.LENGTH_LONG).show()
                        MediaPauseHelper.pauseAllAudioWithFadeOut(this, 15000L)
                    }
                }
                bedsideMonitor?.start()
                Toast.makeText(this, "床头 15 分钟静止守护已开启", Toast.LENGTH_SHORT).show()
            } else {
                bedsideMonitor?.stop()
                Toast.makeText(this, "床头静止守护已关闭", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePermissionStatus() {
        val hasNotificationAccess = isNotificationServiceEnabled()
        if (hasNotificationAccess) {
            tvMediaStatus.text = "● 媒体高级控制：已授权 (可精确控制所有 App 播放器)"
            tvMediaStatus.setTextColor(ContextCompat.getColor(this, R.color.green_status))
            btnGrantPermission.visibility = View.GONE
        } else {
            tvMediaStatus.text = "● 媒体控制模式：基础线控模式 (建议授权增强兼容性)"
            tvMediaStatus.setTextColor(ContextCompat.getColor(this, R.color.text_sub))
            btnGrantPermission.visibility = View.VISIBLE
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        val cn = ComponentName(this, MediaSessionListenerService::class.java).flattenToString()
        return flat.contains(cn)
    }

    override fun onDestroy() {
        super.onDestroy()
        bedsideMonitor?.stop()
    }
}
