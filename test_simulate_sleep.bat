@echo off
chcp 65001 >nul
echo [XiaomiSleep] 正在向手机发送“模拟手环入睡”系统广播...
adb shell am broadcast -a com.mc.xiaomi.tasker.fellAsleep
adb shell am broadcast -a com.xiaomi.sleep.ACTION_SLEEP_PAUSE
if %errorlevel% equ 0 (
    echo [成功] 入睡模拟广播已发送！XiaomiSleep 应当开始 15 秒音量平滑淡出并暂停。
) else (
    echo [提示] 发送失败，请确保手机已开启 USB 调试并已连接。
)
pause
