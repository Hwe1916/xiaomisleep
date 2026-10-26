package com.xiaomi.sleep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 监听手环入睡广播的核心接收器
 * 拦截手环/驱动工具发出的入睡事件，并自动触发声音淡出与暂停
 */
class SleepBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "XiaomiSleep_Receiver"

        // 小米手环后台驱动广播 Action
        const val ACTION_NOTIFY_XIAOMI_FELL_ASLEEP = "com.mc.xiaomi.tasker.fellAsleep"
        const val ACTION_NOTIFY_MIBAND_FELL_ASLEEP = "com.mc.miband.tasker.fellAsleep"
        
        // 外部测试或脚本触发广播
        const val ACTION_CUSTOM_SLEEP_PAUSE = "com.xiaomi.sleep.ACTION_SLEEP_PAUSE"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i(TAG, "收到系统入睡广播: $action")

        when (action) {
            ACTION_NOTIFY_XIAOMI_FELL_ASLEEP,
            ACTION_NOTIFY_MIBAND_FELL_ASLEEP,
            ACTION_CUSTOM_SLEEP_PAUSE -> {
                Log.i(TAG, "检测到入睡事件！准备在 15 秒内淡出并暂停所有媒体播放...")
                
                val pendingResult = goAsync()
                MediaPauseHelper.pauseAllAudioWithFadeOut(
                    context = context,
                    fadeOutDurationMs = 15000L,
                    onComplete = {
                        Log.i(TAG, "所有媒体已成功淡出暂停！祝好梦。")
                        pendingResult.finish()
                    }
                )
            }
            else -> {
                Log.d(TAG, "忽略其他未关联广播: $action")
            }
        }
    }
}
