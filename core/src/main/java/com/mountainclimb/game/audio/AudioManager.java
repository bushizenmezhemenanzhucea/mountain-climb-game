package com.mountainclimb.game.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.mountainclimb.game.GameConfig;
import com.mountainclimb.game.save.SaveManager;

/**
 * 音效管理器：统一管理按钮音、爬山音、BGM，支持音量调节
 */
public class AudioManager {
    private static AudioManager instance;

    private Music bgm;
    private Sound btnSound;
    private Sound climbSound;
    private Sound summitSound;

    private float soundVolume = 0.7f;
    private float musicVolume = 0.5f;
    private boolean bgmPlaying = false;

    private AudioManager() {
        loadFromSave();
    }

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    public void loadFromSave() {
        soundVolume = SaveManager.getInstance().getSoundVolume();
        musicVolume = SaveManager.getInstance().getMusicVolume();
    }

    /**
     * 优先从外部更新目录加载，回退到内部 assets
     */
    private FileHandle getAudioFile(String path) {
        // 检查外部更新目录
        FileHandle external = Gdx.files.external(GameConfig.UPDATE_DIR + "/" + path);
        if (external.exists()) {
            return external;
        }
        return Gdx.files.internal(path);
    }

    public void loadSounds() {
        try {
            btnSound = Gdx.audio.newSound(getAudioFile("sounds/button_click.mp3"));
        } catch (Exception e) {
            Gdx.app.log("AudioManager", "Button sound not found");
        }
        try {
            climbSound = Gdx.audio.newSound(getAudioFile("sounds/climb.mp3"));
        } catch (Exception e) {
            Gdx.app.log("AudioManager", "Climb sound not found");
        }
        try {
            summitSound = Gdx.audio.newSound(getAudioFile("sounds/summit.mp3"));
        } catch (Exception e) {
            Gdx.app.log("AudioManager", "Summit sound not found");
        }
    }

    public void loadBGM() {
        try {
            if (bgm != null) {
                bgm.stop();
                bgm.dispose();
            }
            bgm = Gdx.audio.newMusic(getAudioFile("music/bgm.mp3"));
            bgm.setLooping(true);
            bgm.setVolume(musicVolume);
        } catch (Exception e) {
            Gdx.app.log("AudioManager", "BGM not found");
        }
    }

    public void playBGM() {
        if (bgm != null && !bgmPlaying) {
            bgm.play();
            bgmPlaying = true;
        }
    }

    public void pauseBGM() {
        if (bgm != null && bgmPlaying) {
            bgm.pause();
            bgmPlaying = false;
        }
    }

    public void stopBGM() {
        if (bgm != null) {
            bgm.stop();
            bgmPlaying = false;
        }
    }

    public void playButtonSound() {
        if (btnSound != null) {
            btnSound.play(soundVolume);
        }
    }

    public void playClimbSound() {
        if (climbSound != null) {
            climbSound.play(soundVolume);
        }
    }

    public void playSummitSound() {
        if (summitSound != null) {
            summitSound.play(soundVolume);
        }
    }

    public void setSoundVolume(float volume) {
        soundVolume = Math.max(0f, Math.min(1f, volume));
        SaveManager.getInstance().setSoundVolume(soundVolume);
    }

    public void setMusicVolume(float volume) {
        musicVolume = Math.max(0f, Math.min(1f, volume));
        SaveManager.getInstance().setMusicVolume(musicVolume);
        if (bgm != null) {
            bgm.setVolume(musicVolume);
        }
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public void dispose() {
        if (btnSound != null) btnSound.dispose();
        if (climbSound != null) climbSound.dispose();
        if (summitSound != null) summitSound.dispose();
        if (bgm != null) bgm.dispose();
    }
}
