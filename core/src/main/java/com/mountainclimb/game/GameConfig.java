package com.mountainclimb.game;

/**
 * 游戏全局配置与常量
 */
public class GameConfig {
    // 版本信息
    public static final String VERSION = "1.0.0";
    public static final int VERSION_CODE = 1;
    public static final String GDX_VERSION = "1.12.1";

    // GitHub 热更新配置（请替换为你的仓库）
    public static final String GITHUB_OWNER = "YOUR_GITHUB_USERNAME";
    public static final String GITHUB_REPO = "mountain-climb-game";
    public static final String GITHUB_BRANCH = "main";
    // 用于获取最新 release/tag 的 API 地址
    public static final String GITHUB_API_LATEST =
        "https://api.github.com/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases/latest";
    // 用于获取 raw 文件的 CDN 地址（更新文件所在路径）
    public static final String GITHUB_RAW_BASE =
        "https://raw.githubusercontent.com/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/" + GITHUB_BRANCH + "/update/";

    // 本地更新文件存放路径（Android 外部缓存目录下）
    public static final String UPDATE_DIR = "game_update";
    public static final String UPDATE_VERSION_FILE = "version.json";
    public static final String UPDATE_PATCH_FILE = "patch.zip";

    // 3D 世界配置
    public static final float WORLD_SIZE = 200f;          // 世界边界（正方体边长）
    public static final float PLAYER_SPEED = 10f;       // 玩家移动速度
    public static final float PLAYER_HEIGHT = 1.8f;     // 玩家高度
    public static final float PLAYER_RADIUS = 0.4f;     // 玩家碰撞半径
    public static final float CLIMB_ANGLE = 50f;        // 最大爬坡角度（度）
    public static final float GRAVITY = -9.8f;          // 重力加速度
    public static final float JUMP_FORCE = 5f;          // 跳跃力（预留）

    // 相机/视角配置
    public static final float DEFAULT_SENSITIVITY = 100f; // 默认视角灵敏度 (对应100%)
    public static final float MAX_SENSITIVITY = 300f;   // 最大灵敏度
    public static final float MIN_SENSITIVITY = 0f;     // 最小灵敏度
    public static final float CAMERA_DISTANCE = 5f;     // 第三人称相机距离
    public static final float CAMERA_HEIGHT = 2f;       // 相机高度偏移

    // UI 配置
    public static final float JOYSTICK_SIZE = 120f;       // 摇杆底座大小（dp 转换后像素）
    public static final float JOYSTICK_KNOB_SIZE = 50f;   // 摇杆按钮大小
    public static final float JOYSTICK_DEADZONE = 0.15f;  // 摇杆死区

    // 保存键
    public static final String PREF_NAME = "mountain_climb_save";
    public static final String KEY_HAS_PROGRESS = "has_progress";
    public static final String KEY_PLAYER_X = "player_x";
    public static final String KEY_PLAYER_Y = "player_y";
    public static final String KEY_PLAYER_Z = "player_z";
    public static final String KEY_SOUND_VOL = "sound_volume";
    public static final String KEY_MUSIC_VOL = "music_volume";
    public static final String KEY_SENSITIVITY = "sensitivity";
}
