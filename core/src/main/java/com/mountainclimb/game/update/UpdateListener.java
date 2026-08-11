package com.mountainclimb.game.update;

public interface UpdateListener {
    void onUpdateFound(VersionInfo newVersion);
    void onNoUpdate(String message);
    void onDownloadComplete(VersionInfo version);
    void onDownloadFailed(String message);
}
