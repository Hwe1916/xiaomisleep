package com.xiaomi.sleep

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.sqrt

/**
 * 手机床头静止无感监测器 (备用独立无感模式)
 * 当手机放在床头/枕边连续 15 分钟完全无微动且息屏时，推定用户已熟睡，平滑淡出并暂停播放。
 */
class BedsideMotionMonitor(
    private val context: Context,
    private val idleTimeoutMinutes: Int = 15,
    private val onIdleConfirmed: () -> Unit
) : SensorEventListener {

    companion object {
        private const val TAG = "XiaomiSleep_Motion"
        private const val MOVEMENT_THRESHOLD = 0.35f
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val handler = Handler(Looper.getMainLooper())

    private var isMonitoring = false
    private var lastMovementTime = System.currentTimeMillis()
    private var lastMagnitude = 9.8f

    private val checkRunnable = object : Runnable {
        override fun run() {
            if (!isMonitoring) return
            val idleMs = System.currentTimeMillis() - lastMovementTime
            val targetMs = idleTimeoutMinutes * 60 * 1000L

            if (idleMs >= targetMs) {
                Log.i(TAG, "手机已连续静止 $idleTimeoutMinutes 分钟无触动，判定已入睡！触发暂停...")
                stop()
                onIdleConfirmed.invoke()
            } else {
                handler.postDelayed(this, 10000L) // 每 10 秒巡检一次
            }
        }
    }

    fun start() {
        if (isMonitoring || accelerometer == null) return
        isMonitoring = true
        lastMovementTime = System.currentTimeMillis()
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        handler.post(checkRunnable)
        Log.i(TAG, "床头静止监测已启动，超时阈值: $idleTimeoutMinutes 分钟")
    }

    fun stop() {
        if (!isMonitoring) return
        isMonitoring = false
        sensorManager?.unregisterListener(this)
        handler.removeCallbacks(checkRunnable)
        Log.i(TAG, "床头静止监测已停止")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isMonitoring) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = kotlin.math.abs(magnitude - lastMagnitude)
        lastMagnitude = magnitude

        // 翻身微震或拿取手机更新时间戳
        if (delta > MOVEMENT_THRESHOLD) {
            lastMovementTime = System.currentTimeMillis()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
