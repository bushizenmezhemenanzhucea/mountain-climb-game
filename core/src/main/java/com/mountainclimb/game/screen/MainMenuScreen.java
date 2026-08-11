package com.mountainclimb.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mountainclimb.game.GameConfig;
import com.mountainclimb.game.MountainClimbGame;
import com.mountainclimb.game.audio.AudioManager;
import com.mountainclimb.game.save.SaveManager;
import com.mountainclimb.game.update.UpdateListener;
import com.mountainclimb.game.update.UpdateManager;
import com.mountainclimb.game.update.VersionInfo;

public class MainMenuScreen implements Screen, UpdateListener {
    private MountainClimbGame game;
    private Stage stage;
    private Viewport viewport;
    private Table rootTable;
    private ScrollPane scrollPane;
    private Table menuTable;
    private Label versionLabel;
    private TextButton btnContinue;
    private UpdateManager updateManager;
    private Dialog updateDialog;
    private Texture bgTexture;

    public MainMenuScreen(MountainClimbGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        viewport = new ScreenViewport();
        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);

        try {
            bgTexture = new Texture(Gdx.files.internal("textures/menu_bg.png"));
            bgTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            Gdx.app.log("MainMenu", "Background not found");
        }

        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        menuTable = new Table();
        menuTable.top();

        Skin skin = game.getSkin();
        float btnWidth = Gdx.graphics.getWidth() * 0.5f;
        float btnHeight = 70f;

        TextButton btnStart = new TextButton("开始游戏", skin);
        btnStart.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                game.setScreen(new MapSelectScreen(game));
            }
        });
        menuTable.add(btnStart).width(btnWidth).height(btnHeight).padTop(20f).row();

        btnContinue = new TextButton("继续游戏", skin);
        btnContinue.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                game.setScreen(new GameScreen(game, true));
            }
        });
        menuTable.add(btnContinue).width(btnWidth).height(btnHeight).padTop(20f).row();

        TextButton btnSettings = new TextButton("设置", skin);
        btnSettings.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                game.setScreen(new SettingsScreen(game));
            }
        });
        menuTable.add(btnSettings).width(btnWidth).height(btnHeight).padTop(20f).row();

        TextButton btnChangelog = new TextButton("更新日志", skin);
        btnChangelog.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                showChangelogDialog();
            }
        });
        menuTable.add(btnChangelog).width(btnWidth).height(btnHeight).padTop(20f).row();

        TextButton btnUpdate = new TextButton("检查更新", skin);
        btnUpdate.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                checkUpdate();
            }
        });
        menuTable.add(btnUpdate).width(btnWidth).height(btnHeight).padTop(20f).row();

        scrollPane = new ScrollPane(menuTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        rootTable.add(scrollPane).expand().fill().pad(50f).row();

        versionLabel = new Label("v" + GameConfig.VERSION, skin);
        versionLabel.setColor(Color.GRAY);
        rootTable.add(versionLabel).padBottom(10f);

        refreshContinueButton();

        updateManager = new UpdateManager();
        updateManager.setListener(this);

        AudioManager.getInstance().playBGM();
    }

    private void refreshContinueButton() {
        boolean hasProgress = SaveManager.getInstance().hasProgress();
        btnContinue.setDisabled(!hasProgress);
        if (!hasProgress) {
            btnContinue.getLabel().setColor(Color.GRAY);
        } else {
            btnContinue.getLabel().setColor(Color.WHITE);
        }
    }

    private void checkUpdate() {
        showInfoDialog("正在检查更新...");
        updateManager.checkForUpdate();
    }

    private void showChangelogDialog() {
        try {
            Dialog dialog = new Dialog("更新日志", game.getSkin(), "dialog") {
                @Override
                protected void result(Object object) {}
            };
            dialog.getContentTable().add(new Label(
                "V1.0.0 初始版本\n- 3D爬山游戏\n- 后山地图\n- 热更新系统",
                game.getSkin()
            )).pad(20f);
            dialog.button("关闭", true).padBottom(10f);
            dialog.show(stage);
        } catch (Exception e) {
            Gdx.app.error("MainMenu", "Changelog dialog error: " + e.getMessage());
            showInfoDialog("更新日志: V1.0.0 初始版本");
        }
    }

    private void showInfoDialog(String message) {
        try {
            if (updateDialog != null) updateDialog.hide();
            updateDialog = new Dialog("提示", game.getSkin(), "dialog") {
                @Override
                protected void result(Object object) {}
            };
            updateDialog.text(message);
            updateDialog.button("确定", true);
            updateDialog.show(stage);
        } catch (Exception e) {
            Gdx.app.error("MainMenu", "Dialog error: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateFound(VersionInfo newVersion) {
        try {
            if (updateDialog != null) updateDialog.hide();
            updateDialog = new Dialog("发现新版本", game.getSkin(), "dialog") {
                @Override
                protected void result(Object object) {
                    if (Boolean.TRUE.equals(object)) {
                        showInfoDialog("正在下载更新...");
                        updateManager.downloadUpdate(newVersion);
                    }
                }
            };
            updateDialog.text("发现新版本: v" + newVersion.versionName + "\n是否下载更新?");
            updateDialog.button("下载", true).pad(10f);
            updateDialog.button("取消", false).pad(10f);
            updateDialog.show(stage);
        } catch (Exception e) {
            Gdx.app.error("MainMenu", "Update dialog error: " + e.getMessage());
        }
    }

    @Override
    public void onNoUpdate(String message) {
        showInfoDialog(message);
    }

    @Override
    public void onDownloadComplete(VersionInfo version) {
        showInfoDialog("更新已下载完成!\n请重启游戏以应用更新。");
    }

    @Override
    public void onDownloadFailed(String message) {
        showInfoDialog("下载失败: " + message);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.3f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (bgTexture != null) {
            stage.getBatch().begin();
            stage.getBatch().draw(bgTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            stage.getBatch().end();
        }
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { Gdx.input.setInputProcessor(null); }
    @Override public void dispose() {
        stage.dispose();
        if (bgTexture != null) bgTexture.dispose();
    }
}
