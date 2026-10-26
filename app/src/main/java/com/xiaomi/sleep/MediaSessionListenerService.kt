package com.xiaomi.sleep

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log

/**
 * 基于 NotificationListenerService 的媒体会话与系统状态监听服务
 * 特性：
 * 1. 精准读取并直接调用当前活跃播放器的 transportControls.pause()
 * 2. 监听系统勿扰模式/睡眠模式的改变（无需第三方App，手环入睡后同步勿扰即可自动触发暂停）
 */
class MediaSessionListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "XiaomiSleep_Session"
        private const val PREFS_NAME = "xiaomi_sleep_prefs"
        const val KEY_DND_PAUSE_ENABLED = "dnd_pause_enabled"

        private var instance: MediaSessionListenerService? = null

        fun isServiceConnected(): Boolean = instance != null

        /**
         * 精准暂停系统中所有处于播放/缓冲状态的媒体会话
         * @return 成功暂停的会话个数
         */
        fun pauseAllActiveSessions(context: Context): Int {
            var pausedCount = 0
            try {
                val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                val componentName = ComponentName(context, MediaSessionListenerService::class.java)
                
                val controllers: List<MediaController> = sessionManager?.getActiveSessions(componentName) ?: emptyList()
                Log.i(TAG, "检测到活跃的媒体会话数量: ${controllers.size}")

                for (controller in controllers) {
                    val state = controller.playbackState?.state
                    val isPlaying = state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
                    val pkg = controller.packageName
                    val title = controller.metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "未知"

                    Log.i(TAG, "会话应用: $pkg, 曲目: $title, 状态: $state")

                    if (isPlaying) {
                        try {
                            controller.transportControls.pause()
                            pausedCount++
                            Log.i(TAG, "已成功暂停应用: $pkg")
                        } catch (e: Exception) {
                            Log.w(TAG, "调用 $pkg pause 失败，稍后由全局按键兜底", e)
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "未授予通知使用权，将自动降级使用标准线控键值暂停", e)
            } catch (e: Exception) {
                Log.e(TAG, "读取媒体会话失败", e)
            }
            return pausedCount
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.i(TAG, "XiaomiSleep 媒体与系统监听服务已连接！")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        Log.i(TAG, "XiaomiSleep 媒体服务已断开")
    }

    /**
     * 核心突破：当手环入睡后同步开启勿扰模式/睡眠模式时，此回调由 Android 系统直接触发
     * 完全无需安装 Notify for Xiaomi，100% 兼容小米运动健康与手环官方功能！
     */
    override fun onInterruptionFilterChanged(interruptionFilter: Int) {
        super.onInterruptionFilterChanged(interruptionFilter)
        Log.i(TAG, "系统勿扰/睡眠状态改变，新过滤级别: $interruptionFilter")

        // 当非全通模式时（即进入了勿扰/仅重要/仅闹钟），判定为开启了勿扰或睡眠模式
        if (interruptionFilter != INTERRUPTION_FILTER_ALL) {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val dndPauseEnabled = prefs.getBoolean(KEY_DND_PAUSE_ENABLED, true)

            if (dndPauseEnabled) {
                Log.i(TAG, "手环/系统已进入勿扰或睡眠模式！触发 15 秒平滑淡出暂停...")
                MediaPauseHelper.pauseAllAudioWithFadeOut(this, 15000L) {
                    Log.i(TAG, "已随勿扰同步完成音频淡出暂停！")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
