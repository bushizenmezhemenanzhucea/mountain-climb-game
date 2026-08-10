package com.mountainclimb.game.update;

/**
 * 热更新事件监听器
 */
public interface UpdateListener {
    /** 发现新版本 */
    void onUpdateFound(VersionInfo newVersion);

    /** 无更新或检查失败 */
    void onNoUpdate(String reason);

    /** 下载进度 */
    void onDownloadProgress(int percent);

    /** 下载完成 */
    void onDownloadComplete();

    /** 更新完成（资源已替换，需重启） */
    void onUpdateComplete(boolean needRestart);

    /** 更新出错 */
    void onUpdateError(String error);
}
