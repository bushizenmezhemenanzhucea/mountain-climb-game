package com.mountainclimb.game.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.mountainclimb.game.GameConfig;
import com.mountainclimb.game.save.SaveManager;

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
        if (instance == null) instance = new AudioManager();
        return instance;
    }

    public void loadFromSave() {
        soundVolume = SaveManager.getInstance().getSoundVolume();
        musicVolume = SaveManager.getInstance().getMusicVolume();
    }

    private FileHandle getAudioFile(String path) {
        FileHandle external = Gdx.files.external(GameConfig.UPDATE_DIR + "/" + path);
        if (external.exists()) return external;
        return Gdx.files.internal(path);
    }

    public void loadSounds() {
        try { btnSound = Gdx.audio.newSound(getAudioFile("sounds/button_click.wav")); } catch (Exception e) {}
        try { climbSound = Gdx.audio.newSound(getAudioFile("sounds/climb.wav")); } catch (Exception e) {}
        try { summitSound = Gdx.audio.newSound(getAudioFile("sounds/summit.wav")); } catch (Exception e) {}
    }

    public void loadBGM() {
        try {
            if (bgm != null) { bgm.stop(); bgm.dispose(); }
            bgm = Gdx.audio.newMusic(getAudioFile("music/bgm.wav"));
            bgm.setLooping(true);
            bgm.setVolume(musicVolume);
        } catch (Exception e) {}
    }

    public void playBGM() {
        if (bgm != null && !bgmPlaying) { bgm.play(); bgmPlaying = true; }
    }
    public void pauseBGM() {
        if (bgm != null && bgmPlaying) { bgm.pause(); bgmPlaying = false; }
    }
    public void stopBGM() {
        if (bgm != null) { bgm.stop(); bgmPlaying = false; }
    }

    public void playButtonSound() {
        if (btnSound != null) btnSound.play(soundVolume);
    }
    public void playClimbSound() {
        if (climbSound != null) climbSound.loop(soundVolume);
    }
    public void stopClimbSound() {
        if (climbSound != null) climbSound.stop();
    }
    public void playSummitSound() {
        if (summitSound != null) summitSound.play(soundVolume);
    }

    public void setSoundVolume(float volume) {
        soundVolume = Math.max(0f, Math.min(1f, volume));
        SaveManager.getInstance().setSoundVolume(soundVolume);
    }
    public void setMusicVolume(float volume) {
        musicVolume = Math.max(0f, Math.min(1f, volume));
        SaveManager.getInstance().setMusicVolume(musicVolume);
        if (bgm != null) bgm.setVolume(musicVolume);
    }

    public void dispose() {
        if (btnSound != null) btnSound.dispose();
        if (climbSound != null) climbSound.dispose();
        if (summitSound != null) summitSound.dispose();
        if (bgm != null) bgm.dispose();
    }
}
