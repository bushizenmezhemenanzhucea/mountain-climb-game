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

        Label title = new Label("设置", skin);
        title.setFontScale(1.5f);
        table.add(title).padBottom(40f).row();

        float sliderWidth = 400f;
        float labelWidth = 120f;

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
                float pct = sensitivitySlider.getValue();
                sensitivityValueLabel.setText((int)pct + "%");
                SaveManager.getInstance().setSensitivity(pct);
            }
        });

        TextButton btnBack = new TextButton("返回", skin);
        btnBack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                game.setScreen(new MainMenuScreen(game));
            }
        });
        table.add(btnBack).width(200f).height(60f).row();
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }
}
