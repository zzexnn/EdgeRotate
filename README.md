# 侧边旋转 EdgeRotate

为小米 Pad 7 Ultra / 澎湃 OS 4.0 设计的**极简 Android 悬浮按钮工具**。

## 功能
- 屏幕边缘半透明悬浮按钮（显示直径约 32dp，触控范围 52dp）
- 单击：锁定当前方向 / 恢复自动旋转
- 拖动：左、右侧自由切换，自动吸边，记住垂直位置
- 状态同步：外部通过控制中心修改自动旋转后，按钮图标实时更新
- 前台服务（Android 14+ `specialUse` 类型）、`START_STICKY`、开机重启尝试恢复
- 无联网权限、无广告、无分析 SDK、无 Root 需求

## 构建 APK

当前提供**完整 Android Studio 源码工程**，但在生成工程的环境中没有 Android SDK / Gradle / Android 真机，因此**未构建、未签名、未在澎湃 OS 4.0 进行真机测试**。不要把此 ZIP 当成 APK 安装。

1. 安装 Android Studio（JDK 17、Android SDK Platform 35、Android SDK Build-Tools）。
2. 在 Android Studio 选择 **Open**，打开本文件夹 `EdgeRotate`。
3. 首次同步时选择 Gradle 8.9（Android Gradle Plugin 8.7.3）。本工程仅包含 `gradle/wrapper/gradle-wrapper.properties`，**不包含 wrapper JAR**；如 Android Studio 要求生成完整 Wrapper，请在已安装 Gradle 的终端于工程根目录运行 `gradle wrapper --gradle-version 8.9`；或在 Android Studio 的 Gradle 设置中使用本地 Gradle 8.9。
4. 在菜单中点击 **Build > Build APK(s)**；输出通常位于 `app/build/outputs/apk/debug/app-debug.apk`。
5. 把 APK 复制到小米平板安装（可能需要允许安装未知应用）。

### 无需本地 Android Studio：用 GitHub Actions 构建

工程中附有 `.github/workflows/build-apk.yml`。将**工程根目录的内容**放进你自己的 GitHub 仓库（保留 `.github` 隐藏文件夹），在仓库的 **Actions > Build EdgeRotate APK > Run workflow** 手动运行。成功后，打开该次运行的 **Artifacts**，下载 `EdgeRotate-debug-APK`，解压即可得到 `app-debug.apk`。也可以通过推送 `main/master` 分支自动触发。

**注意：** 这是提供给用户自行执行的构建工作流；本环境未联网实际执行，不能保证在未调整环境配置时一次通过。

## 首次使用

1. 打开 App；点击“允许显示悬浮窗”，在系统设置中授予权限。
2. 点击“允许修改系统设置”，授予权限。
3. 点击“开启侧边按钮”。
4. 默认按钮在右边约屏幕高度 52% 的位置；点击切换，按住拖动。
5. 如果无法修改旋转，先用系统控制中心手动测试是否能解锁方向，随后查看 App 权限。

## 澎湃 OS 后台设置

- “设置 > 应用 > 权限 > 后台自启动”（部分版本为“设置 > 应用 > 后台自启动”）：允许侧边旋转。
- 在应用信息/电池使用设置中选择“不限制”或类似选项。
- 最近任务中尽可能锁定本 App（如该版本系统提供该入口）。
- 悬浮窗权限 + 修改系统设置权限必须保持开启。
- 遇到按钮因系统权限/省电政策消失，可重新打开本 App，点击关闭再开启。

不同澎湃 OS 版本的菜单文案可能不同。以上设置无法保证系统**绝对不会**杀后台。

## Android 兼容注意事项

- 项目最小 SDK 26（Android 8），目标 SDK 35（Android 15）；计划适配 HyperOS 4.0，但暂未实机验证。
- 通过 `Settings.System.ACCELEROMETER_ROTATION` 控制系统自动旋转；锁定前使用 `Settings.System.USER_ROTATION` 记录当前角度。不同 ROM 可能不允许更改该设置，或某些 App 强制自己的屏幕方向，此时功能受系统限制。
- 运行中的**前台服务会提供常驻通知**，可能出现在系统通知管理或任务管理界面，这是 Android 的正常机制；并非完全无痕运行。
- 系统可能限制在特殊安全界面、锁屏或部分全屏场景中显示悬浮窗。
- 若系统明确强制停止应用，开机广播/START_STICKY 不能绕过该限制。不要为追求保活引入滥用无障碍、重复闹钟或高耗电循环。

## 文件结构

- `app/src/main/java/zone/zexn/edgerotate/MainActivity.java`: 初次授权与开关
- `OverlayService.java`: 悬浮窗与前台服务
- `BubbleView.java`: 32dp 绘制图标
- `RotationController.java`: 系统自动旋转切换逻辑
- `BootReceiver.java`: 开机后尝试恢复
- `Prefs.java`: 保存开启状态与按钮位置

## 参考

- Android 修改系统设置: https://developer.android.com/reference/android/provider/Settings.System
- Android 前台服务类型: https://developer.android.com/develop/background-work/services/fgs/service-types
- Android 后台启动限制: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
