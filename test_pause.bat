@echo off
chcp 65001 >nul
echo [XiaomiSleep] 正在向连接的手机发送全局媒体暂停指令...
adb shell input keyevent 127
if %errorlevel% equ 0 (
    echo [成功] 暂停指令已成功送达手机！
) else (
    echo [提示] 手机未通过 USB 调试连接。若已连接，请在手机上点击“允许 USB 调试”。
)
pause
