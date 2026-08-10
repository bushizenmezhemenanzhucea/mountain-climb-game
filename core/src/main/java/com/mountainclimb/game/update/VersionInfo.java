package com.mountainclimb.game.update;

/**
 * 版本信息数据结构（对应 GitHub 仓库中 update/version.json）
 */
public class VersionInfo {
    public int versionCode;      // 版本号，用于比较
    public String versionName;     // 版本名称，如 "1.0.1"
    public String updateUrl;       // 补丁包 ZIP 下载地址（GitHub Release asset 或 raw 地址）
    public String patchList;       // 需要更新的文件列表（可选，用于增量更新）
    public String updateLog;       // 更新日志（可选）
    public boolean forceUpdate;    // 是否强制更新

    public VersionInfo() {}

    public VersionInfo(int code, String name) {
        this.versionCode = code;
        this.versionName = name;
    }
}
