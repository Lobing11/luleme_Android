# 🦌 鹿了么

本地私密健康记录 App（Android / Kotlin + Jetpack Compose）。

对标「月经记录」类 App 的交互模式，用于**本地记录**个人自慰/性健康数据。
所有数据仅保存在设备本地，**应用不申请任何网络权限，不上传任何数据**。

## 功能

- **记录**：一键快速记录 + 详细记录（日期 / 时间 / 心情 5 档 / 备注），支持编辑与删除
- **日历热力图**：按日记录次数着色的月历（5 档深浅），点击日期查看当天详情
- **统计**：今日 / 本月 / 总次数、当前连续天数、最长与平均间隔、距上次记录、近 6 个月趋势柱状图
- **应用锁**：PIN（4-6 位，加盐 SHA-256 存储）+ 生物识别（指纹 / 人脸），退到后台自动重新锁定
- **提醒**：每日定时提醒 +「葛神模式」（记录后 8 小时）；不支持精确闹钟时降级为打开应用时提醒
- **备份**：JSON 导出 / 导入（导入替换现有数据）、清空全部记录
- **隐私**：防截屏保护（默认开启）
- **主题**：浅色 / 深色自动适配，粉色系 UI

## 技术要点

| 项目 | 方案 |
| --- | --- |
| 语言 / UI | Kotlin + Jetpack Compose（Material 3） |
| 本地存储 | Room（SQLite，`records` 表）+ DataStore Preferences |
| 路由 | Navigation Compose（底部 Tab + RecordEdit / Lock） |
| 安全 | SHA-256 加盐 PIN、BiometricPrompt、`FLAG_SECURE` |
| 提醒 | AlarmManager 精确闹钟 + 通知；开机重调度 |
| 文件 | SAF（CreateDocument / OpenDocument） |
| 测试 | JUnit（StatsCalculator / CalendarUtil / SecurityUtil） |

## 目录结构

```
app/src/main/java/com/luleme/app/
├── model/          数据模型（Record / 心情）
├── data/           Room + RecordRepository、SettingsStore
├── logic/          StatsCalculator、CalendarUtil、SecurityUtil、ReminderScheduler
├── presentation/   theme / components / screens
├── receiver/       AlarmReceiver、BootReceiver
├── MainActivity.kt
└── LulemeApp.kt
```

## 构建

Android Studio Ladybug+ 打开本仓库根目录，连接真机或模拟器运行。

命令行（需本机 Android SDK）：

```bat
set JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot
gradlew.bat :app:assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

最低系统版本：Android 8.0（API 26）。

## 隐私与合规

- 全程无网络权限（`AndroidManifest.xml` 未声明 `INTERNET`）
- 数据仅存本机 Room / DataStore

## 开源许可

本项目基于 [MIT License](LICENSE) 开源。

> 注意：本项目为个人健康记录工具，所有数据仅保存在用户设备本地。请在转载、宣传时保持内容健康化，并自行确认应用名称与第三方商标的兼容性。
