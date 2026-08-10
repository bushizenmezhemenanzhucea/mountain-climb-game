package com.mountainclimb.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mountainclimb.game.GameConfig;
import com.mountainclimb.game.MountainClimbGame;
import com.mountainclimb.game.audio.AudioManager;
import com.mountainclimb.game.input.Joystick;
import com.mountainclimb.game.player.Player;
import com.mountainclimb.game.save.SaveManager;
import com.mountainclimb.game.world.TerrainGenerator;

/**
 * 3D游戏场景
 * 后山地图：包含山脉、空气墙、碰撞检测、登顶检测
 */
public class GameScreen implements Screen {
    private MountainClimbGame game;
    private boolean continueGame;

    // 3D
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private TerrainGenerator terrain;
    private Player player;

    // 输入
    private Stage uiStage;
    private Joystick joystick;
    private InputMultiplexer inputMultiplexer;
    private GestureDetector gestureDetector;

    // 状态
    private boolean paused = false;
    private boolean summitShown = false;
    private float summitTimer = 0f;
    private boolean wasMoving = false;

    // UI
    private Label summitLabel;
    private Dialog pauseDialog;
    private Skin skin;

    // 触摸跟踪
    private float lastTouchX, lastTouchY;
    private boolean touchInRightHalf = false;

    public GameScreen(MountainClimbGame game, boolean continueGame) {
        this.game = game;
        this.continueGame = continueGame;
    }

    @Override
    public void show() {
        skin = game.getSkin();
        modelBatch = new ModelBatch();

        // 相机设置
        float aspect = (float) Gdx.graphics.getWidth() / Gdx.graphics.getHeight();
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 300f;

        // 环境光照
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.7f, -0.5f, -1f, -0.5f));
        environment.add(new DirectionalLight().set(0.3f, 0.3f, 0.4f, 0.5f, 0.5f, -0.5f));

        // 生成地形
        terrain = new TerrainGenerator();

        // 创建玩家
        Vector3 startPos = new Vector3(0, 5, 0);
        if (continueGame) {
            Vector3 saved = SaveManager.getInstance().loadProgress();
            if (saved != null) startPos = saved;
        }
        player = new Player(startPos.x, startPos.y, startPos.z);

        // UI
        uiStage = new Stage(new ScreenViewport());
        setupUI();

        // 输入处理
        setupInput();

        // 播放BGM
        AudioManager.getInstance().playBGM();
    }

    private void setupUI() {
        // 摇杆
        float joySize = Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()) * 0.25f;
        float knobSize = joySize * 0.4f;
        joystick = new Joystick(joySize, knobSize);
        joystick.setPosition(30f, 30f);
        uiStage.addActor(joystick);

        // 登顶提示（默认隐藏）
        summitLabel = new Label("成功登顶!", skin);
        summitLabel.setFontScale(2f);
        summitLabel.setColor(Color.GOLD);
        summitLabel.setPosition(
            Gdx.graphics.getWidth() / 2f - summitLabel.getWidth(),
            Gdx.graphics.getHeight() / 2f
        );
        summitLabel.setVisible(false);
        uiStage.addActor(summitLabel);

        // 暂停按钮（右上角）
        TextButton btnPause = new TextButton("||", skin);
        float btnSize = 60f;
        btnPause.setSize(btnSize, btnSize);
        btnPause.setPosition(Gdx.graphics.getWidth() - btnSize - 20f, Gdx.graphics.getHeight() - btnSize - 20f);
        btnPause.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                togglePause();
            }
        });
        uiStage.addActor(btnPause);
    }

    private void setupInput() {
        // 手势检测器（右半屏幕滑屏转视角）
        gestureDetector = new GestureDetector(new GestureDetector.GestureAdapter() {
            @Override
            public boolean touchDown(float x, float y, int pointer, int button) {
                touchInRightHalf = x > Gdx.graphics.getWidth() / 2f;
                lastTouchX = x;
                lastTouchY = y;
                return touchInRightHalf;
            }

            @Override
            public boolean pan(float x, float y, float deltaX, float deltaY) {
                if (paused) return false;
                if (x > Gdx.graphics.getWidth() / 2f) {
                    float sensitivity = SaveManager.getInstance().getSensitivityMultiplier();
                    float rotSpeed = GameConfig.DEFAULT_SENSITIVITY * sensitivity * 0.1f;
                    player.rotate(-deltaX * rotSpeed, deltaY * rotSpeed, 1f);
                    return true;
                }
                return false;
            }
        });

        InputProcessor gameInput = new InputProcessor() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE || keycode == com.badlogic.gdx.Input.Keys.BACK) {
                    togglePause();
                    return true;
                }
                return false;
            }

            @Override public boolean keyUp(int keycode) { return false; }
            @Override public boolean keyTyped(char character) { return false; }
            @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
            @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
            @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
            @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
            @Override public boolean scrolled(float amountX, float amountY) { return false; }
        };

        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(uiStage);
        inputMultiplexer.addProcessor(gestureDetector);
        inputMultiplexer.addProcessor(gameInput);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    private void togglePause() {
        paused = !paused;
        if (paused) {
            showPauseDialog();
            AudioManager.getInstance().pauseBGM();
        } else {
            if (pauseDialog != null) pauseDialog.hide();
            AudioManager.getInstance().playBGM();
        }
    }

    private void showPauseDialog() {
        pauseDialog = new Dialog("暂停", skin) {
            @Override
            protected void result(Object object) {
                String action = (String) object;
                if ("continue".equals(action)) {
                    togglePause();
                } else if ("save".equals(action)) {
                    SaveManager.getInstance().saveProgress(player.getPosition());
                    AudioManager.getInstance().playButtonSound();
                    // 显示保存成功提示
                } else if ("menu".equals(action)) {
                    AudioManager.getInstance().playButtonSound();
                    game.setScreen(new MainMenuScreen(game));
                }
            }
        };
        pauseDialog.getContentTable().pad(20f);
        pauseDialog.button("继续游戏", "continue").pad(10f);
        pauseDialog.button("保存进度", "save").pad(10f);
        pauseDialog.button("返回主菜单", "menu").pad(10f);
        pauseDialog.show(uiStage);
    }

    @Override
    public void render(float delta) {
        if (!paused) {
            updateGame(delta);
        }

        // 渲染3D
        Gdx.gl.glClearColor(0.5f, 0.7f, 0.9f, 1f); // 天蓝色背景
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // 更新相机
        camera.position.set(player.getCameraPosition());
        camera.lookAt(player.getLookAt());
        camera.up.set(Vector3.Y);
        camera.update();

        // 渲染模型
        modelBatch.begin(camera);
        modelBatch.render(terrain.getTerrainInstance(), environment);
        for (ModelInstance wall : terrain.getWallInstances()) {
            modelBatch.render(wall, environment);
        }
        modelBatch.render(player.getModelInstance(), environment);
        modelBatch.end();

        // 渲染UI
        uiStage.act(delta);
        uiStage.draw();

        // 登顶提示淡出
        if (summitShown) {
            summitTimer += delta;
            if (summitTimer > 3f) {
                summitLabel.setVisible(false);
                summitShown = false;
            } else {
                float alpha = 1f - (summitTimer / 3f);
                summitLabel.setColor(Color.GOLD.r, Color.GOLD.g, Color.GOLD.b, alpha);
            }
        }
    }

    private void updateGame(float delta) {
        // 1. 获取摇杆方向
        Vector2 moveDir = joystick.getDirection();
        boolean moving = moveDir.len2() > 0.01f;

        // 2. 播放/停止爬山音效
        if (moving && player.isGrounded() && !wasMoving) {
            AudioManager.getInstance().playClimbSound();
        } else if (!moving) {
            // 音效较短，无需手动停止
        }
        wasMoving = moving;

        // 3. 移动玩家
        player.move(moveDir, delta, 0);

        // 4. 获取当前地形高度
        float terrainH = terrain.getTerrainHeight(player.getPosition().x, player.getPosition().z);

        // 5. 应用地形和重力
        player.applyGravity(delta, terrainH);

        // 6. 限制在世界边界内（空气墙碰撞）
        Vector3 pos = player.getPosition();
        pos = terrain.clampToWorld(pos);
        player.setPosition(pos);

        // 7. 更新玩家变换
        player.updateTransform();

        // 8. 自动保存进度（每30秒）
        // 可以添加计时器定期保存

        // 9. 登顶检测
        checkSummit();
    }

    private void checkSummit() {
        Vector3 pos = player.getPosition();
        for (TerrainGenerator.PeakInfo peak : terrain.getPeaks()) {
            if (!peak.summitReached && peak.isOnSummit(pos, 2f)) {
                peak.summitReached = true;
                summitShown = true;
                summitTimer = 0f;
                summitLabel.setVisible(true);
                summitLabel.setPosition(
                    Gdx.graphics.getWidth() / 2f - summitLabel.getPrefWidth() / 2f,
                    Gdx.graphics.getHeight() / 2f
                );
                AudioManager.getInstance().playSummitSound();

                // 保存登顶进度
                SaveManager.getInstance().saveProgress(pos);
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        uiStage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
        // 自动保存
        SaveManager.getInstance().saveProgress(player.getPosition());
    }

    @Override
    public void resume() {}

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        terrain.dispose();
        player.dispose();
        joystick.dispose();
        uiStage.dispose();
    }
}
