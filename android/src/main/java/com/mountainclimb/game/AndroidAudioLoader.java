package com.mountainclimb.game;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;

/**
 * Android 平台音频加载器（用于处理 Android 音频路径差异）
 * 在 Android 上，外部更新目录的音频文件需要特殊处理
 */
public class AndroidAudioLoader {

    /**
     * 从外部存储加载音乐（热更新后使用）
     */
    public static Music loadExternalMusic(String path) {
        try {
            FileHandle fh = Gdx.files.external(path);
            if (fh.exists()) {
                return Gdx.audio.newMusic(fh);
            }
        } catch (Exception e) {
            Gdx.app.log("AndroidAudio", "Failed to load external music: " + path);
        }
        return null;
    }

    /**
     * 从外部存储加载音效（热更新后使用）
     */
    public static Sound loadExternalSound(String path) {
        try {
            FileHandle fh = Gdx.files.external(path);
            if (fh.exists()) {
                return Gdx.audio.newSound(fh);
            }
        } catch (Exception e) {
            Gdx.app.log("AndroidAudio", "Failed to load external sound: " + path);
        }
        return null;
    }

    /**
     * 检查是否为 Android 平台
     */
    public static boolean isAndroid() {
        return Gdx.app.getType() == Application.ApplicationType.Android;
    }
}
