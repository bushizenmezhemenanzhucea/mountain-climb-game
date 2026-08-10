# 3D爬山游戏 V1

使用 **Java + LibGDX** 开发的 Android 3D 爬山游戏，支持 GitHub 热更新。

## 功能特性

- **真实3D渲染**：使用 LibGDX 3D API，地形和玩家均为真实3D模型
- **GitHub热更新**：无需重新安装APK即可更新游戏资源
- **主菜单系统**：开始游戏、继续游戏、设置、更新日志、检查更新
- **地图选择**：当前包含"后山"地图，配有文字介绍
- **进度保存**：自动保存玩家位置，支持继续游戏
- **完整设置**：音效音量、背景音乐音量、视角灵敏度（0%-300%）
- **操控系统**：左下角圆形虚拟摇杆 + 右半屏滑动切换视角
- **3D场景**：程序化生成的山脉地形、空气墙边界、登顶检测
- **音效系统**：按钮音效、爬山音效、登顶音效、背景音乐

## 技术栈

- Java 8
- LibGDX 1.12.1
- Gradle 7.x
- Android SDK 21+

## 项目结构

```
mountain-climb-game/
├── core/              # 核心游戏逻辑（跨平台）
│   └── src/main/java/com/mountainclimb/game/
│       ├── MountainClimbGame.java       # 游戏主类
│       ├── GameConfig.java              # 全局配置
│       ├── audio/AudioManager.java     # 音效管理
│       ├── input/Joystick.java          # 虚拟摇杆
│       ├── player/Player.java           # 玩家3D模型与控制
│       ├── save/SaveManager.java        # 存档管理
│       ├── screen/                      # 游戏界面
│       │   ├── MainMenuScreen.java      # 主菜单
│       │   ├── MapSelectScreen.java     # 地图选择
│       │   ├── SettingsScreen.java      # 设置
│       │   └── GameScreen.java          # 3D游戏场景
│       ├── update/                      # 热更新系统
│       │   ├── UpdateManager.java       # 更新管理器
│       │   ├── UpdateListener.java      # 更新回调
│       │   └── VersionInfo.java         # 版本信息
│       └── world/TerrainGenerator.java  # 3D地形生成
├── android/           # Android 平台适配
│   ├── src/main/java/com/mountainclimb/game/
│   │   └── AndroidLauncher.java        # Android入口
│   └── src/main/AndroidManifest.xml     # 清单文件
├── update/            # 热更新文件（GitHub仓库中）
│   ├── version.json    # 版本配置
│   └── patch.zip       # 资源补丁包
└── GITHUB_SETUP.md    # GitHub热更新配置指南
```

## 构建 APK

### 前提条件
- Android Studio 或 Gradle
- Android SDK（API 21+）
- JDK 8+

### 构建步骤

```bash
# 1. 进入项目目录
cd mountain-climb-game

# 2. 使用 Gradle 构建
./gradlew android:assembleDebug

# 3. APK 输出路径
android/build/outputs/apk/debug/android-debug.apk
```

### 使用 Android Studio

1. 打开 `mountain-climb-game` 文件夹
2. 等待 Gradle 同步完成
3. 连接 Android 手机或启动模拟器
4. 点击 **Run**（绿色三角按钮）

## GitHub 热更新配置

请查看 **GITHUB_SETUP.md** 获取详细配置说明。

快速步骤：
1. 创建 GitHub 仓库
2. 修改 `GameConfig.java` 中的 `GITHUB_OWNER` 和 `GITHUB_REPO`
3. 在仓库中创建 `update/version.json`
4. 发布更新时上传 `patch.zip`

## 操控说明

- **左下角摇杆**：控制玩家移动，方向跟随玩家视角
- **右半屏滑动**：切换视角方向
- **视角灵敏度**：在设置中调节（0% - 300%）
- **暂停按钮**：右上角 || 按钮

## 开发者

- 技术选型：Java + LibGDX
- 热更新：GitHub Raw + External Storage
- 横屏模式：Landscape

## 版本历史

### V1.0.0
- 初始版本
- 后山地图
- 热更新系统
- 完整菜单与设置
