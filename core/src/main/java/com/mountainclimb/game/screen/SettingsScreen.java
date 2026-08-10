package com.mountainclimb.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mountainclimb.game.MountainClimbGame;
import com.mountainclimb.game.audio.AudioManager;
import com.mountainclimb.game.save.SaveManager;

/**
 * 设置界面
 * 音效音量、背景音乐音量、视角灵敏度（0% ~ 300%）
 */
public class SettingsScreen implements Screen {
    private MountainClimbGame game;
    private Stage stage;

    private Slider soundSlider;
    private Slider musicSlider;
    private Slider sensitivitySlider;
    private Label soundValueLabel;
    private Label musicValueLabel;
    private Label sensitivityValueLabel;

    public SettingsScreen(MountainClimbGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Skin skin = game.getSkin();
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // 标题
        Label title = new Label("设置", skin);
        title.setFontScale(1.5f);
        table.add(title).padBottom(40f).row();

        float sliderWidth = 400f;
        float labelWidth = 120f;

        // ===== 音效音量 =====
        Table soundRow = new Table();
        soundRow.add(new Label("音效音量", skin)).width(labelWidth).padRight(20f);
        soundSlider = new Slider(0f, 100f, 1f, false, skin);
        soundSlider.setValue(SaveManager.getInstance().getSoundVolume() * 100f);
        soundRow.add(soundSlider).width(sliderWidth).padRight(15f);
        soundValueLabel = new Label((int)soundSlider.getValue() + "%", skin);
        soundRow.add(soundValueLabel).width(60f);
        table.add(soundRow).padBottom(25f).row();

        soundSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float vol = soundSlider.getValue() / 100f;
                soundValueLabel.setText((int)soundSlider.getValue() + "%");
                AudioManager.getInstance().setSoundVolume(vol);
            }
        });

        // ===== 背景音乐音量 =====
        Table musicRow = new Table();
        musicRow.add(new Label("背景音乐", skin)).width(labelWidth).padRight(20f);
        musicSlider = new Slider(0f, 100f, 1f, false, skin);
        musicSlider.setValue(SaveManager.getInstance().getMusicVolume() * 100f);
        musicRow.add(musicSlider).width(sliderWidth).padRight(15f);
        musicValueLabel = new Label((int)musicSlider.getValue() + "%", skin);
        musicRow.add(musicValueLabel).width(60f);
        table.add(musicRow).padBottom(25f).row();

        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float vol = musicSlider.getValue() / 100f;
                musicValueLabel.setText((int)musicSlider.getValue() + "%");
                AudioManager.getInstance().setMusicVolume(vol);
            }
        });

        // ===== 视角灵敏度 (0% ~ 300%) =====
        Table sensRow = new Table();
        sensRow.add(new Label("视角灵敏度", skin)).width(labelWidth).padRight(20f);
        sensitivitySlider = new Slider(0f, 300f, 1f, false, skin);
        sensitivitySlider.setValue(SaveManager.getInstance().getSensitivityPercent());
        sensRow.add(sensitivitySlider).width(sliderWidth).padRight(15f);
        sensitivityValueLabel = new Label((int)sensitivitySlider.getValue() + "%", skin);
        sensRow.add(sensitivityValueLabel).width(60f);
        table.add(sensRow).padBottom(40f).row();

        sensitivitySlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float percent = sensitivitySlider.getValue();
                sensitivityValueLabel.setText((int)percent + "%");
                SaveManager.getInstance().setSensitivity(percent);
            }
        });

        // ===== 按钮 =====
        Table btnRow = new Table();
        TextButton btnBack = new TextButton("返回", skin);
        btnBack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                // 保存设置
                SaveManager.getInstance().setSoundVolume(soundSlider.getValue() / 100f);
                SaveManager.getInstance().setMusicVolume(musicSlider.getValue() / 100f);
                SaveManager.getInstance().setSensitivity(sensitivitySlider.getValue());
                game.setScreen(new MainMenuScreen(game));
            }
        });
        btnRow.add(btnBack).width(180f).height(55f).padRight(20f);

        TextButton btnReset = new TextButton("恢复默认", skin);
        btnReset.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                // 恢复默认值
                soundSlider.setValue(70f);
                musicSlider.setValue(50f);
                sensitivitySlider.setValue(100f);
                soundValueLabel.setText("70%");
                musicValueLabel.setText("50%");
                sensitivityValueLabel.setText("100%");
                AudioManager.getInstance().setSoundVolume(0.7f);
                AudioManager.getInstance().setMusicVolume(0.5f);
                SaveManager.getInstance().setSensitivity(100f);
            }
        });
        btnRow.add(btnReset).width(180f).height(55f);

        table.add(btnRow).padTop(20f);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.1f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}
    @Override
    public void resume() {}

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
