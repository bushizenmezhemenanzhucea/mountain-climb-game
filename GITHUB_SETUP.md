# 3D爬山游戏 - GitHub 热更新配置指南

## 1. 创建 GitHub 仓库

1. 登录你的 GitHub 账号
2. 创建新仓库，命名为 `mountain-climb-game`（或自定义）
3. 仓库设为 **Public**（热更新需要公开访问 raw 文件）
4. 初始化仓库（添加 README）

## 2. 本地项目准备

在你完成游戏开发并构建 APK 后，需要将项目推送到 GitHub：

```bash
# 进入项目根目录
cd mountain-climb-game

# 初始化 Git 仓库
git init

# 添加所有文件
git add .

# 提交
git commit -m "Initial commit - 3D爬山游戏V1.0.0"

# 关联远程仓库（替换 YOUR_USERNAME）
git remote add origin https://github.com/YOUR_USERNAME/mountain-climb-game.git

# 推送
git push -u origin main
```

## 3. 配置热更新

### 3.1 修改 GameConfig.java

编辑 `core/src/main/java/com/mountainclimb/game/GameConfig.java`，替换以下常量：

```java
// 替换为你的 GitHub 用户名和仓库名
public static final String GITHUB_OWNER = "YOUR_GITHUB_USERNAME";
public static final String GITHUB_REPO = "mountain-climb-game";
public static final String GITHUB_BRANCH = "main";
```

### 3.2 创建更新目录结构

在 GitHub 仓库根目录下创建 `update/` 文件夹：

```
update/
├── version.json      # 版本信息文件
├── patch.zip         # 更新补丁包（包含新资源/配置）
└── assets/           # 可选：单独更新的资源文件
    ├── textures/
    ├── sounds/
    └── music/
```

### 3.3 创建 version.json

在 `update/` 目录下创建 `version.json` 文件：

```json
{
    "versionCode": 2,
    "versionName": "1.0.1",
    "updateUrl": "https://github.com/YOUR_USERNAME/mountain-climb-game/releases/download/v1.0.1/patch.zip",
    "patchList": "textures/menu_bg.png,sounds/new_sound.mp3",
    "updateLog": "修复了爬山音效；优化了地形渲染",
    "forceUpdate": false
}
```

**字段说明：**
- `versionCode`: 版本号，必须大于当前 APK 中的 `GameConfig.VERSION_CODE`
- `versionName`: 版本名称，用于显示
- `updateUrl`: 补丁包下载地址（可以是 GitHub Release asset，也可以是 raw 文件地址）
- `patchList`: 需要更新的文件列表（可选）
- `updateLog`: 更新日志
- `forceUpdate`: 是否强制更新

### 3.4 打包更新补丁

当你需要发布更新时：

1. 准备好需要更新的文件（纹理、音效、配置等）
2. 将这些文件按目录结构打包成 ZIP：

```bash
cd update/assets
zip -r ../patch.zip .
cd ../..
```

3. 确保 ZIP 内的目录结构与 `android/assets/` 一致，例如：
   ```
   patch.zip
   ├── textures/menu_bg.png
   ├── sounds/button_click.mp3
   └── music/bgm.mp3
   ```

4. 将 `patch.zip` 和 `version.json` 上传到 GitHub：
   - 方法一：直接 push 到仓库的 `update/` 目录
   - 方法二：上传到 GitHub Release 页面，将下载链接填入 `version.json` 的 `updateUrl`

### 3.5 推送更新文件

```bash
# 添加更新文件
git add update/
git commit -m "Release v1.0.1 - 热更新补丁"
git push origin main
```

## 4. 发布 GitHub Release（推荐方式）

使用 GitHub Release 管理更新更稳定：

1. 在 GitHub 仓库页面点击 **Releases** -> **Create a new release**
2. 选择或创建 tag，如 `v1.0.1`
3. 填写 Release 标题和说明
4. 上传 `patch.zip` 文件到 Release Assets
5. 发布后，将 Asset 的下载链接填入 `update/version.json` 的 `updateUrl`
6. 同时更新仓库中的 `update/version.json`，确保用户能检测到新版本

## 5. 测试热更新流程

1. 安装 V1.0.0 的 APK 到手机
2. 修改 `update/version.json` 中的 `versionCode` 为 2
3. 准备新的补丁包，上传
4. 在游戏中点击 **检查更新**
5. 应弹出发现新版本的提示
6. 点击更新，下载完成后重启应用
7. 验证新资源是否生效

## 6. 注意事项

- **网络权限**：确保 AndroidManifest.xml 已声明 `INTERNET` 权限
- **存储权限**：Android 10+ 需要 `MANAGE_EXTERNAL_STORAGE` 或使用分区存储
- **版本号递增**：每次更新 `versionCode` 必须严格递增
- **ZIP 路径**：补丁包内的路径必须与游戏代码中 `assets/` 的路径一致
- **回滚**：如果更新失败，游戏会自动回退到内置资源（不会崩溃）
- **大文件**：如果补丁包很大，建议拆分成多个小补丁或使用 GitHub Release

## 7. 常见问题

**Q: 更新后资源没有变化？**
A: 检查 `patch.zip` 内的路径是否正确。LibGDX 会优先读取外部存储的更新文件，确保文件已正确解压到 `/sdcard/Android/data/com.mountainclimb.game/files/game_update/` 下。

**Q: 下载失败？**
A: 检查网络连接、GitHub raw 地址是否可访问。在中国大陆可能需要配置代理或使用镜像。

**Q: 如何只更新代码逻辑？**
A: LibGDX 热更新主要更新资源文件。如需更新 Java 代码逻辑，需要重新打包 APK。建议将可变逻辑放在脚本（如 Lua/JS）中执行。

## 8. 自动化脚本（可选）

创建 `scripts/release.sh` 用于自动化发布：

```bash
#!/bin/bash
VERSION_CODE=$1
VERSION_NAME=$2

# 更新版本号
sed -i "s/VERSION_CODE = [0-9]*/VERSION_CODE = $VERSION_CODE/" core/src/main/java/com/mountainclimb/game/GameConfig.java
sed -i "s/VERSION = \"[0-9.]*\"/VERSION = \"$VERSION_NAME\"/" core/src/main/java/com/mountainclimb/game/GameConfig.java

# 打包补丁
cd update/assets && zip -r ../patch.zip . && cd ../..

# 更新 version.json
cat > update/version.json << EOF
{
    "versionCode": $VERSION_CODE,
    "versionName": "$VERSION_NAME",
    "updateUrl": "https://github.com/YOUR_USERNAME/mountain-climb-game/releases/download/v$VERSION_NAME/patch.zip",
    "updateLog": "",
    "forceUpdate": false
}
EOF

# 提交并推送
git add .
git commit -m "Release v$VERSION_NAME"
git push origin main

echo "发布完成！请在 GitHub 创建 Release 并上传 patch.zip"
```

使用方式：
```bash
bash scripts/release.sh 2 "1.0.1"
```
