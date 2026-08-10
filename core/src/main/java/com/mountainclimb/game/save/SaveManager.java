package com.mountainclimb.game.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.Vector3;
import com.mountainclimb.game.GameConfig;

/**
 * 游戏存档与设置管理器（使用 LibGDX Preferences）
 */
public class SaveManager {
    private static SaveManager instance;
    private Preferences prefs;

    private SaveManager() {
        prefs = Gdx.app.getPreferences(GameConfig.PREF_NAME);
    }

    public static SaveManager getInstance() {
        if (instance == null) {
            instance = new SaveManager();
        }
        return instance;
    }

    // ===== 游戏进度 =====

    public boolean hasProgress() {
        return prefs.getBoolean(GameConfig.KEY_HAS_PROGRESS, false);
    }

    public void saveProgress(Vector3 position) {
        prefs.putBoolean(GameConfig.KEY_HAS_PROGRESS, true);
        prefs.putFloat(GameConfig.KEY_PLAYER_X, position.x);
        prefs.putFloat(GameConfig.KEY_PLAYER_Y, position.y);
        prefs.putFloat(GameConfig.KEY_PLAYER_Z, position.z);
        prefs.flush();
    }

    public Vector3 loadProgress() {
        if (!hasProgress()) return null;
        float x = prefs.getFloat(GameConfig.KEY_PLAYER_X, 0);
        float y = prefs.getFloat(GameConfig.KEY_PLAYER_Y, 0);
        float z = prefs.getFloat(GameConfig.KEY_PLAYER_Z, 0);
        return new Vector3(x, y, z);
    }

    public void clearProgress() {
        prefs.putBoolean(GameConfig.KEY_HAS_PROGRESS, false);
        prefs.flush();
    }

    // ===== 设置 =====

    public void setSoundVolume(float volume) {
        prefs.putFloat(GameConfig.KEY_SOUND_VOL, Math.max(0f, Math.min(1f, volume)));
        prefs.flush();
    }

    public float getSoundVolume() {
        return prefs.getFloat(GameConfig.KEY_SOUND_VOL, 0.7f);
    }

    public void setMusicVolume(float volume) {
        prefs.putFloat(GameConfig.KEY_MUSIC_VOL, Math.max(0f, Math.min(1f, volume)));
        prefs.flush();
    }

    public float getMusicVolume() {
        return prefs.getFloat(GameConfig.KEY_MUSIC_VOL, 0.5f);
    }

    /**
     * 视角灵敏度 0% ~ 300%，保存为 0.0 ~ 3.0 的倍数
     */
    public void setSensitivity(float percent) {
        float multiplier = percent / 100f;
        prefs.putFloat(GameConfig.KEY_SENSITIVITY, Math.max(0f, Math.min(3f, multiplier)));
        prefs.flush();
    }

    public float getSensitivityPercent() {
        float multiplier = prefs.getFloat(GameConfig.KEY_SENSITIVITY, 1.0f);
        return multiplier * 100f;
    }

    public float getSensitivityMultiplier() {
        return prefs.getFloat(GameConfig.KEY_SENSITIVITY, 1.0f);
    }

    // ===== 工具方法 =====

    public void clearAll() {
        prefs.clear();
        prefs.flush();
    }
}
