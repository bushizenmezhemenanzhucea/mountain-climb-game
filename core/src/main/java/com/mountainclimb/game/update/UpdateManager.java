package com.mountainclimb.game.update;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.mountainclimb.game.GameConfig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UpdateManager {
    private static final String TAG = "UpdateManager";
    private UpdateListener listener;
    private VersionInfo localVersion;
    private boolean checking = false;

    public UpdateManager() {
        this.localVersion = new VersionInfo(GameConfig.VERSION_CODE, GameConfig.VERSION);
    }

    public void setListener(UpdateListener listener) {
        this.listener = listener;
    }

    public void checkForUpdate() {
        if (checking) return;
        checking = true;

        String versionUrl = GameConfig.GITHUB_RAW_BASE + GameConfig.UPDATE_VERSION_FILE;
        Gdx.app.log(TAG, "Checking: " + versionUrl);

        try {
            Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
            request.setUrl(versionUrl);
            request.setTimeOut(15000);

            Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                    checking = false;
                    try {
                        int status = httpResponse.getStatus().getStatusCode();
                        Gdx.app.log(TAG, "HTTP status: " + status);
                        if (status != 200) {
                            notifyNoUpdate("服务器返回 " + status);
                            return;
                        }
                        String body = httpResponse.getResultAsString();
                        if (body == null || body.isEmpty()) {
                            notifyNoUpdate("服务器返回空数据");
                            return;
                        }
                        Json json = new Json();
                        VersionInfo remoteVersion = json.fromJson(VersionInfo.class, body);
                        if (remoteVersion == null) {
                            notifyNoUpdate("版本数据解析失败");
                            return;
                        }
                        if (remoteVersion.versionCode > localVersion.versionCode) {
                            Gdx.app.log(TAG, "New version: " + remoteVersion.versionName);
                            final VersionInfo finalRemote = remoteVersion;
                            Gdx.app.postRunnable(() -> {
                                if (listener != null) listener.onUpdateFound(finalRemote);
                            });
                        } else {
                            notifyNoUpdate("当前已是最新版本 (v" + localVersion.versionName + ")");
                        }
                    } catch (Exception e) {
                        Gdx.app.error(TAG, "Parse error: " + e.getMessage());
                        notifyNoUpdate("版本数据解析失败: " + e.getMessage());
                    }
                }
                @Override public void failed(Throwable t) {
                    checking = false;
                    Gdx.app.error(TAG, "Request failed: " + t.getMessage());
                    notifyNoUpdate("网络请求失败: " + t.getMessage());
                }
                @Override public void cancelled() {
                    checking = false;
                    notifyNoUpdate("请求已取消");
                }
            });
        } catch (Exception e) {
            checking = false;
            notifyNoUpdate("检查更新失败: " + e.getMessage());
        }
    }

    public void downloadUpdate(VersionInfo versionInfo) {
        String patchUrl = GameConfig.GITHUB_RAW_BASE + GameConfig.UPDATE_PATCH_FILE;
        Gdx.app.log(TAG, "Downloading: " + patchUrl);
        try {
            Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
            request.setUrl(patchUrl);
            request.setTimeOut(30000);
            Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                    try {
                        if (httpResponse.getStatus().getStatusCode() != 200) {
                            notifyDownloadFailed("下载失败: HTTP " + httpResponse.getStatus().getStatusCode());
                            return;
                        }
                        byte[] bytes = httpResponse.getResult();
                        if (bytes == null || bytes.length == 0) {
                            notifyDownloadFailed("下载的数据为空");
                            return;
                        }
                        Gdx.app.log(TAG, "Downloaded " + bytes.length + " bytes");
                        if (extractPatch(bytes)) {
                            notifyDownloadComplete(versionInfo);
                        } else {
                            notifyDownloadFailed("补丁解压失败");
                        }
                    } catch (Exception e) {
                        notifyDownloadFailed("下载处理失败: " + e.getMessage());
                    }
                }
                @Override public void failed(Throwable t) {
                    notifyDownloadFailed("下载失败: " + t.getMessage());
                }
                @Override public void cancelled() {
                    notifyDownloadFailed("下载已取消");
                }
            });
        } catch (Exception e) {
            notifyDownloadFailed("下载启动失败: " + e.getMessage());
        }
    }

    private boolean extractPatch(byte[] patchData) {
        try {
            FileHandle updateDir = Gdx.files.external(GameConfig.UPDATE_DIR);
            updateDir.mkdirs();
            ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(patchData));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    updateDir.child(entry.getName()).mkdirs();
                    continue;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                FileHandle outFile = updateDir.child(entry.getName());
                outFile.parent().mkdirs();
                outFile.writeBytes(baos.toByteArray(), false);
                Gdx.app.log(TAG, "Extracted: " + entry.getName());
            }
            zis.close();
            FileHandle versionFile = updateDir.child(GameConfig.UPDATE_VERSION_FILE);
            versionFile.writeString("{\"versionCode\":" + (localVersion.versionCode + 1) + ",\"versionName\":\"\"}", false);
            return true;
        } catch (Exception e) {
            Gdx.app.error(TAG, "Extract failed: " + e.getMessage());
            return false;
        }
    }

    private void notifyNoUpdate(String message) {
        Gdx.app.postRunnable(() -> {
            if (listener != null) listener.onNoUpdate(message);
        });
    }
    private void notifyDownloadFailed(String message) {
        Gdx.app.postRunnable(() -> {
            if (listener != null) listener.onDownloadFailed(message);
        });
    }
    private void notifyDownloadComplete(VersionInfo version) {
        Gdx.app.postRunnable(() -> {
            if (listener != null) listener.onDownloadComplete(version);
        });
    }
}
