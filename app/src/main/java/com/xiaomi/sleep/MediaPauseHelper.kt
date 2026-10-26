package com.xiaomi.sleep

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent

/**
 * 负责执行系统全局音频暂停的核心工具类
 * 具备三重兜底保障：
 * 1. 优先调用 MediaSession 精确暂停所有活动播放器 (网易云/QQ音乐/播客等)
 * 2. 模拟标准蓝牙线控按键 KEYCODE_MEDIA_PAUSE
 * 3. 瞬时抢占短时 AudioFocus（促使所有规范播放器自动暂停）
 * 4. 支持睡前优雅音量淡出 (Fade-out)，防止突然静音惊醒用户
 */
object MediaPauseHelper {

    private const val TAG = "XiaomiSleep_PauseHelper"

    /**
     * 执行带淡出效果的全局音频暂停
     * @param context 上下文
     * @param fadeOutDurationMs 淡出时长（毫秒），如 15000（15秒）
     * @param onComplete 完成回调
     */
    fun pauseAllAudioWithFadeOut(
        context: Context,
        fadeOutDurationMs: Long = 15000L,
        onComplete: (() -> Unit)? = null
    ) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager == null) {
            pauseAllAudioImmediately(context)
            onComplete?.invoke()
            return
        }

        val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (originalVolume <= 0) {
            pauseAllAudioImmediately(context)
            onComplete?.invoke()
            return
        }

        val steps = 15 // 15 级渐减
        val stepInterval = fadeOutDurationMs / steps
        val volumeStep = originalVolume.toFloat() / steps
        val handler = Handler(Looper.getMainLooper())

        for (i in 1..steps) {
            handler.postDelayed({
                val newVol = (originalVolume - volumeStep * i).toInt().coerceAtLeast(0)
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                } catch (e: Exception) {
                    Log.w(TAG, "递减音量失败", e)
                }

                if (i == steps) {
                    // 音量已降至 0，执行彻底暂停
                    pauseAllAudioImmediately(context)

                    // 延迟 1 秒悄悄恢复原本的媒体音量刻度（保证明早的闹钟与声音不受影响）
                    handler.postDelayed({
                        try {
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
                        } catch (e: Exception) {
                            Log.w(TAG, "复原音量失败", e)
                        }
                        onComplete?.invoke()
                    }, 1000L)
                }
            }, stepInterval * i)
        }
    }

    /**
     * 立即暂停当前所有音频播放
     */
    fun pauseAllAudioImmediately(context: Context) {
        Log.i(TAG, "正在触发全局媒体暂停指令...")

        // 第 1 步: 优先尝试通过 MediaSessionManager 精确暂停活动应用
        val pausedBySession = MediaSessionListenerService.pauseAllActiveSessions(context)
        Log.i(TAG, "通过 MediaSession 暂停的应用数: $pausedBySession")

        // 第 2 步: 发送标准 KEYCODE_MEDIA_PAUSE 键值（双重兜底）
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager != null) {
            val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE)
            val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE)
            audioManager.dispatchMediaKeyEvent(downEvent)
            audioManager.dispatchMediaKeyEvent(upEvent)

            // 第 3 步: 瞬态请求音频焦点，促使顽固播放器暂停
            requestTransientAudioFocus(audioManager)
        }
    }

    private fun requestTransientAudioFocus(audioManager: AudioManager) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .build()
                audioManager.requestAudioFocus(focusRequest)
                audioManager.abandonAudioFocusRequest(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "请求 AudioFocus 兼容暂停失败", e)
        }
    }
}
