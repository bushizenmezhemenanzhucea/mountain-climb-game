package com.mountainclimb.game.update;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 热更新管理器
 * 从 GitHub 检查并下载更新
 */
public class UpdateManager {
    private static final String TAG = "UpdateManager";
    
    // GitHub 配置
    private static final String GITHUB_OWNER = "bushizenmezhemenanzhucea";
    private static final String GITHUB_REPO = "mountain-climb-game";
    private static final String GITHUB_BRANCH = "main";
    private static final String GITHUB_RAW_BASE = "https://raw.githubusercontent.com/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/" + GITHUB_BRANCH + "/update/";

    private UpdateListener listener;
    private boolean checking = false;

    public void setListener(UpdateListener listener) {
        this.listener = listener;
    }

    /**
     * 检查是否有新版本
     */
    public void checkForUpdate() {
        if (checking) return;
        checking = true;

        String url = GITHUB_RAW_BASE + "version.json";
        
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        request.setUrl(url);
        request.setTimeOut(10000);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                try {
                    String jsonStr = httpResponse.getResultAsString();
                    Json json = new Json();
                    VersionInfo remoteVersion = json.fromJson(VersionInfo.class, jsonStr);
                    
                    // 比较版本
                    if (remoteVersion.versionCode > com.mountainclimb.game.GameConfig.VERSION_CODE) {
                        notifyUpdateFound(remoteVersion);
                    } else {
                        notifyNoUpdate("已是最新版本");
                    }
                } catch (Exception e) {
                    Gdx.app.error(TAG, "Parse version.json failed", e);
                    notifyError("解析版本信息失败");
                }
                checking = false;
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.error(TAG, "Check update failed", t);
                notifyError("检查更新失败: " + t.getMessage());
                checking = false;
            }

            @Override
            public void cancelled() {
                checking = false;
            }
        });
    }

    /**
     * 下载并应用更新
     */
    public void downloadAndApplyUpdate() {
        String url = GITHUB_RAW_BASE + "patch.zip";
        
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        request.setUrl(url);
        request.setTimeOut(30000);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                String contentLength = httpResponse.getHeader("Content-Length");
                long totalBytes = contentLength != null ? Long.parseLong(contentLength) : 0;
                long downloadedBytes = 0;
                byte[] buffer = new byte[4096];
                int read;
                try {
                    while ((read = httpResponse.getResultAsStream().read(buffer)) != -1) {
                        baos.write(buffer, 0, read);
                        downloadedBytes += read;
                        if (totalBytes > 0) {
                            int percent = (int) ((downloadedBytes * 100) / totalBytes);
                            if (listener != null) {
                                final int p = percent;
                                Gdx.app.postRunnable(() -> listener.onDownloadProgress(p));
                            }
                        }
                    }
                } catch (IOException e) {
                    Gdx.app.error(TAG, "Download read error", e);
                    notifyError("下载读取失败: " + e.getMessage());
                    return;
                }

                // 下载完成，应用更新
                Gdx.app.postRunnable(() -> {
                    if (listener != null) listener.onDownloadComplete();
                    applyPatch(baos.toByteArray());
                });
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.error(TAG, "Download failed", t);
                notifyError("下载失败: " + t.getMessage());
            }

            @Override
            public void cancelled() {
                notifyError("下载已取消");
            }
        });
    }

    /**
     * 应用补丁
     */
    private void applyPatch(byte[] patchData) {
        try {
            ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(patchData));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                
                String name = entry.getName();
                // 安全检查：防止 Zip Slip 漏洞
                if (name.contains("..") || name.startsWith("/")) {
                    Gdx.app.error(TAG, "Unsafe zip entry: " + name);
                    continue;
                }
                
                ByteArrayOutputStream entryBaos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = zis.read(buffer)) != -1) {
                    entryBaos.write(buffer, 0, read);
                }
                
                // 写入内部存储
                FileHandle file = Gdx.files.local("update/" + name);
                file.writeBytes(entryBaos.toByteArray(), false);
                zis.closeEntry();
            }
            zis.close();
            
            notifyUpdateComplete(true);
        } catch (IOException e) {
            Gdx.app.error(TAG, "Apply patch failed", e);
            notifyError("应用更新失败: " + e.getMessage());
        }
    }

    private void notifyUpdateFound(VersionInfo version) {
        Gdx.app.postRunnable(() -> {
            if (listener != null) listener.onUpdateFound(version);
        });
    }

    private void notifyNoUpdate(String reason) {
        Gdx.app.postRunnable(() -> {
            if (listener != null) listener.onNoUpdate(reason);
        });
    }

    private void notifyError(String error) {
        Gdx.app.postRunnable(() -> {
            if (listener != null) listener.onUpdateError(error);
        });
    }

    private void notifyUpdateComplete(boolean needRestart) {
        Gdx.app.postRunnable(() -> {
            if (listener != null) listener.onUpdateComplete(needRestart);
        });
    }
}
