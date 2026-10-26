# XiaomiSleep (小米手环 9 入睡自动暂停伴侣)

> **极简·纯净·无广告·睡熟后平滑淡出暂停手机所有声音**  
> 专为小米手环 9 (NFC) 用户定制。

---

## 🌙 为什么需要它？

市面上的手环/睡眠 App 充斥着繁杂的图表、社交、心率报警和收费会员，而许多用户的真实需求只有一个：  
**晚上听歌/播客/有声书助眠，睡着后帮我把声音关掉，别吵我，别费电。**

---

## ✨ 核心特性

1. **手环入睡广播自动联动**：
   - 监听手环确认入睡后发出的广播（`com.mc.xiaomi.tasker.fellAsleep`）。
2. **15 秒平滑淡出 (Fade-Out)**：
   - 绝不突兀骤停（骤停容易把浅睡者惊醒）。
   - 音量在 15 秒内平缓递减至静音后暂停，随后**自动复原系统音量刻度**，保证第二天早上的系统闹钟依然响亮。
3. **精准媒体接管 (`MediaSession`)**：
   - 优先通过系统 MediaSession 精确暂停网易云音乐、QQ音乐、喜马拉雅、微信读书、B站、播客等所有主流应用。
   - 具备蓝牙线控 `KEYCODE_MEDIA_PAUSE` 与瞬时音频焦点双重兜底。
4. **床头静止无感守护 (备选辅助)**：
   - 即使手环忘戴或没电，手机放在床头连续 15 分钟完全无微动且息屏，也会推定入睡并自动平滑暂停。

---

## 🚀 获取与构建 APK (2 分钟快速指南)

### 推荐方法：GitHub Actions 云端 1 键打包（免在本地安装任何 Android 环境）
本项目已内置好自动化编译脚本 [`.github/workflows/build_apk.yml`](file:///.github/workflows/build_apk.yml)。

1. 打开 GitHub，新建一个空仓库（名称可填 `xiaomisleep`）。
2. 在本地 `D:\Desktop\xiaomisleep` 运行以下两行命令推送到你的仓库：
   ```bash
   git remote add origin https://github.com/<你的用户名>/xiaomisleep.git
   git branch -M main
   git push -u origin main
   ```
3. 打开你的 GitHub 仓库，点击顶部的 **Actions** 标签。
4. 约 1 分钟后构建完成，在 **Artifacts** 处直接点击下载 `XiaomiSleep-Debug-APK.zip`。
5. 解压后将 APK 传到手机上直接安装！

### 本地编译（若已安装 Android Studio）
1. 用 Android Studio 打开 `D:\Desktop\xiaomisleep` 目录。
2. 菜单点击 **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**。
3. 产物生成在 `app/build/outputs/apk/debug/app-debug.apk`。

---

## 📱 手机端快速设置

1. **安装打开**：在手机上打开 `XiaomiSleep`。
2. **授权通知使用权（推荐）**：点击主界面的按钮，在系统设置中允许本应用。此权限仅用于获取系统 `MediaSession` 精确控制各 App 暂停，无任何数据上传。
3. **配合手环驱动**：
   - 手机后台的 Notify for Xiaomi（仅作为底层蓝牙驱动），只需在设置里打开 **Tasker Integration** 开关即可，不用理会它的任何复杂功能。
4. **验证测试**：
   - 放一首歌曲，点击 App 里的“模拟 10 秒平滑淡出暂停”，听听音量如何平滑降低并自动暂停。
   - 或用电脑插上数据线双击运行 [`test_simulate_sleep.bat`](file:///d:/Desktop/xiaomisleep/test_simulate_sleep.bat) 立即模拟触发。
