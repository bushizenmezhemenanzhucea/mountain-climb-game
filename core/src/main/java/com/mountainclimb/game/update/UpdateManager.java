package com.mountainclimb.game.update;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.Json;
import com.mountainclimb.game.GameConfig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * GitHub 热更新管理器
 * 流程：检查版本 -> 下载补丁 -> 解压覆盖 -> 重启生效
 */
public class UpdateManager {
    private static final String TAG = "UpdateManager";

    private UpdateListener listener;
    private VersionInfo localVersion;
    private VersionInfo remoteVersion;
    private boolean checking = false;
    private boolean downloading = false;

    public UpdateManager() {
        this.localVersion = new VersionInfo(GameConfig.VERSION_CODE, GameConfig.VERSION);
    }

    public void setListener(UpdateListener listener) {
        this.listener = listener;
    }

    /**
     * 检查 GitHub 仓库是否有新版本
     */
    public void checkForUpdate() {
        if (checking) return;
        checking = true;

        String versionUrl = GameConfig.GITHUB_RAW_BASE + GameConfig.UPDATE_VERSION_FILE;
        Gdx.app.log(TAG, "Checking update from: " + versionUrl);

        HttpRequestBuilder builder = new HttpRequestBuilder();
        Net.HttpRequest request = builder.newRequest()
            .method(Net.HttpMethods.GET)
            .url(versionUrl)
            .timeout(10000)
            .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                checking = false;
                String body = httpResponse.getResultAsString();
                if (body == null || body.isEmpty()) {
                    notifyNoUpdate("服务器返回空数据");
                    return;
                }

                try {
                    Json json = new Json();
                    remoteVersion = json.fromJson(VersionInfo.class, body);

                    if (remoteVersion.versionCode > localVersion.versionCode) {
                        Gdx.app.log(TAG, "New version found: " + remoteVersion.versionName);
                        if (listener != null) {
                            Gdx.app.postRunnable(() -> listener.onUpdateFound(remoteVersion));
                        }
                    } else {
                        notifyNoUpdate("当前已是最新版本");
                    }
                } catch (Exception e) {
                    Gdx.app.error(TAG, "Parse version json failed", e);
                    notifyNoUpdate("版本数据解析失败: " + e.getMessage());
                }
            }

            @Override
            public void failed(Throwable t) {
                checking = false;
                Gdx.app.error(TAG, "Check update failed", t);
                notifyNoUpdate("网络请求失败: " + t.getMessage());
            }

            @Override
            public void cancelled() {
                checking = false;
                notifyNoUpdate("用户取消");
            }
        });
    }

    /**
     * 下载并应用更新
     */
    public void downloadAndApplyUpdate() {
        if (downloading || remoteVersion == null) return;
        downloading = true;

        String patchUrl = (remoteVersion.updateUrl != null && !remoteVersion.updateUrl.isEmpty())
            ? remoteVersion.updateUrl
            : GameConfig.GITHUB_RAW_BASE + GameConfig.UPDATE_PATCH_FILE;

        Gdx.app.log(TAG, "Downloading patch from: " + patchUrl);

        HttpRequestBuilder builder = new HttpRequestBuilder();
        Net.HttpRequest request = builder.newRequest()
            .method(Net.HttpMethods.GET)
            .url(patchUrl)
            .timeout(30000)
            .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            private ByteArrayOutputStream baos = new ByteArrayOutputStream();
            private long totalBytes = 0;
            private long downloadedBytes = 0;

            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                totalBytes = Long.parseLong(httpResponse.getHeader("Content-Length", "0"));
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
                downloading = false;
                Gdx.app.error(TAG, "Download failed", t);
                notifyError("下载失败: " + t.getMessage());
            }

            @Override
            public void cancelled() {
                downloading = false;
                notifyNoUpdate("下载取消");
            }
        });
    }

    /**
     * 解压补丁包并覆盖本地资源
     */
    private void applyPatch(byte[] patchData) {
        try {
            FileHandle updateDir = Gdx.files.external(GameConfig.UPDATE_DIR);
            if (!updateDir.exists()) {
                updateDir.mkdirs();
            }

            // 1. 保存补丁文件到本地（可选，用于回滚或日志）
            FileHandle patchFile = updateDir.child(GameConfig.UPDATE_PATCH_FILE);
            patchFile.writeBytes(patchData, false);

            // 2. 解压 ZIP 到 updateDir
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(patchData))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;
                    FileHandle outFile = updateDir.child(entry.getName());
                    outFile.parent().mkdirs();

                    ByteArrayOutputStream entryOut = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        entryOut.write(buffer, 0, len);
                    }
                    outFile.writeBytes(entryOut.toByteArray(), false);
                    zis.closeEntry();
                }
            }

            // 3. 将解压后的文件覆盖到 assets（外部存储优先，LibGDX 的 FileType.External 会优先读取）
            // 在 Android 中，External 文件会放在 /sdcard/Android/data/.../files/ 下
            // 游戏加载资源时应优先从此处读取，回退到 Internal assets

            // 4. 保存新的版本信息到本地，下次启动时读取
            FileHandle versionFile = updateDir.child(GameConfig.UPDATE_VERSION_FILE);
            Json json = new Json();
            versionFile.writeString(json.toJson(remoteVersion), false);

            downloading = false;
            Gdx.app.log(TAG, "Update applied successfully. Restart needed.");

            if (listener != null) {
                listener.onUpdateComplete(true);
            }
        } catch (Exception e) {
            downloading = false;
            Gdx.app.error(TAG, "Apply patch failed", e);
            notifyError("应用更新失败: " + e.getMessage());
        }
    }

    /**
     * 读取本地已更新的版本信息（启动时调用）
     */
    public VersionInfo getLocalUpdatedVersion() {
        FileHandle vf = Gdx.files.external(GameConfig.UPDATE_DIR + "/" + GameConfig.UPDATE_VERSION_FILE);
        if (vf.exists()) {
            try {
                Json json = new Json();
                return json.fromJson(VersionInfo.class, vf.readString());
            } catch (Exception e) {
                Gdx.app.error(TAG, "Read local version file failed", e);
            }
        }
        return null;
    }

    /**
     * 获取更新目录下的文件（用于优先加载更新资源）
     */
    public FileHandle getUpdatedFile(String path) {
        FileHandle external = Gdx.files.external(GameConfig.UPDATE_DIR + "/" + path);
        if (external.exists()) {
            return external;
        }
        return null;
    }

    private void notifyNoUpdate(String reason) {
        if (listener != null) {
            Gdx.app.postRunnable(() -> listener.onNoUpdate(reason));
        }
    }

    private void notifyError(String error) {
        if (listener != null) {
            Gdx.app.postRunnable(() -> listener.onUpdateError(error));
        }
    }
}
