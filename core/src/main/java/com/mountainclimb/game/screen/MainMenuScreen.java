package com.mountainclimb.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mountainclimb.game.GameConfig;
import com.mountainclimb.game.MountainClimbGame;
import com.mountainclimb.game.audio.AudioManager;
import com.mountainclimb.game.save.SaveManager;
import com.mountainclimb.game.update.UpdateListener;
import com.mountainclimb.game.update.UpdateManager;
import com.mountainclimb.game.update.VersionInfo;

/**
 * 主菜单界面（横屏）
 * 包含：开始游戏、继续游戏、设置、更新日志、检查更新
 * 支持滚动，底部显示版本号，背景为群山图片
 */
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

        // 加载背景（群山环绕）
        try {
            bgTexture = new Texture(Gdx.files.internal("textures/menu_bg.png"));
            bgTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            Gdx.app.log("MainMenu", "Background not found, using color");
        }

        // 创建根布局
        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        // 创建菜单按钮表格（用于滚动）
        menuTable = new Table();
        menuTable.top();

        Skin skin = game.getSkin();
        float btnWidth = Gdx.graphics.getWidth() * 0.5f;
        float btnHeight = 70f;
        float padTop = 20f;

        // 开始游戏
        TextButton btnStart = new TextButton("开始游戏", skin);
        btnStart.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                game.setScreen(new MapSelectScreen(game));
            }
        });
        menuTable.add(btnStart).width(btnWidth).height(btnHeight).padTop(padTop).row();

        // 继续游戏（根据存档状态启用/禁用）
        btnContinue = new TextButton("继续游戏", skin);
        btnContinue.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                if (SaveManager.getInstance().hasProgress()) {
                    game.setScreen(new GameScreen(game, true));
                }
            }
        });
        menuTable.add(btnContinue).width(btnWidth).height(btnHeight).padTop(padTop).row();

        // 设置
        TextButton btnSettings = new TextButton("设置", skin);
        btnSettings.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                game.setScreen(new SettingsScreen(game));
            }
        });
        menuTable.add(btnSettings).width(btnWidth).height(btnHeight).padTop(padTop).row();

        // 更新日志
        TextButton btnChangelog = new TextButton("更新日志", skin);
        btnChangelog.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                showChangelogDialog();
            }
        });
        menuTable.add(btnChangelog).width(btnWidth).height(btnHeight).padTop(padTop).row();

        // 检查更新
        TextButton btnCheckUpdate = new TextButton("检查更新", skin);
        btnCheckUpdate.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                checkUpdate();
            }
        });
        menuTable.add(btnCheckUpdate).width(btnWidth).height(btnHeight).padTop(padTop).row();

        // 滚动面板包裹菜单
        scrollPane = new ScrollPane(menuTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false); // 只允许垂直滚动

        // 将滚动面板放入根表格
        rootTable.add(scrollPane).expand().fill().pad(50f).row();

        // 底部灰色版本号
        versionLabel = new Label("v" + GameConfig.VERSION, skin);
        versionLabel.setColor(Color.GRAY);
        rootTable.add(versionLabel).padBottom(10f);

        // 刷新继续按钮状态
        refreshContinueButton();

        // 初始化热更新
        updateManager = new UpdateManager();
        updateManager.setListener(this);

        // 播放BGM
        AudioManager.getInstance().playBGM();
    }

    private void refreshContinueButton() {
        boolean hasProgress = SaveManager.getInstance().hasProgress();
        btnContinue.setDisabled(!hasProgress);
        // 灰色禁用样式
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
        Dialog dialog = new Dialog("更新日志", game.getSkin()) {
            @Override
            protected void result(Object object) {
                // 关闭
            }
        };
        dialog.getContentTable().add(new Label("V1.0.0 初始版本\n- 3D爬山游戏\n- 后山地图\n- 热更新系统", game.getSkin()))
            .pad(20f);
        dialog.button("关闭", true).padBottom(10f);
        dialog.show(stage);
    }

    private void showInfoDialog(String message) {
        if (updateDialog != null) {
            updateDialog.hide();
        }
        updateDialog = new Dialog("提示", game.getSkin());
        updateDialog.text(message);
        updateDialog.button("确定", true);
        updateDialog.show(stage);
    }

    // ===== UpdateListener 回调 =====

    @Override
    public void onUpdateFound(VersionInfo newVersion) {
        showInfoDialog("发现新版本: " + newVersion.versionName + "\n是否立即更新?");
        // 可以扩展为带"更新"按钮的对话框
    }

    @Override
    public void onNoUpdate(String reason) {
        showInfoDialog(reason);
    }

    @Override
    public void onDownloadProgress(int percent) {
        // 显示下载进度
    }

    @Override
    public void onDownloadComplete() {
        showInfoDialog("下载完成，正在应用更新...");
    }

    @Override
    public void onUpdateComplete(boolean needRestart) {
        showInfoDialog("更新完成! 请重启应用。");
    }

    @Override
    public void onUpdateError(String error) {
        showInfoDialog("更新出错: " + error);
    }

    // ===== Screen 接口 =====

    @Override
    public void render(float delta) {
        // 清屏
        Gdx.gl.glClearColor(0.1f, 0.12f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 绘制背景
        if (bgTexture != null) {
            stage.getBatch().begin();
            stage.getBatch().draw(bgTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            stage.getBatch().end();
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
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
        if (bgTexture != null) bgTexture.dispose();
    }
}
