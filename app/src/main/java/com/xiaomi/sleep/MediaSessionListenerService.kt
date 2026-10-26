package com.xiaomi.sleep

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log

/**
 * 基于 NotificationListenerService 的媒体会话监听服务
 * 在 Android 系统中，拥有通知使用权的 Service 可精准读取并直接调用当前活跃播放器的 transportControls.pause()
 */
class MediaSessionListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "XiaomiSleep_Session"

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
        Log.i(TAG, "XiaomiSleep 媒体精准接管服务已连接！")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        Log.i(TAG, "XiaomiSleep 媒体服务已断开")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
