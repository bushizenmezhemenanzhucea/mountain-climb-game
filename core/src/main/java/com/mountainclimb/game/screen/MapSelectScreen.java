package com.mountainclimb.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mountainclimb.game.MountainClimbGame;
import com.mountainclimb.game.audio.AudioManager;

/**
 * 地图选择界面（目前只有后山一个地图）
 */
public class MapSelectScreen implements Screen {
    private MountainClimbGame game;
    private Stage stage;

    private Texture mapPreviewTexture;

    public MapSelectScreen(MountainClimbGame game) {
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
        Label title = new Label("选择地图", skin);
        title.setFontScale(1.5f);
        table.add(title).padBottom(30f).row();

        // 地图卡片（后山）
        Table mapCard = new Table();
        mapCard.setBackground(skin.newDrawable("default", 0.15f, 0.18f, 0.22f, 0.9f));

        // 地图预览图
        try {
            mapPreviewTexture = new Texture(Gdx.files.internal("textures/map_houshan.png"));
            Image preview = new Image(mapPreviewTexture);
            mapCard.add(preview).width(300f).height(180f).pad(15f).row();
        } catch (Exception e) {
            mapCard.add(new Label("[地图预览]", skin)).pad(20f).row();
        }

        // 地图名称
        Label mapName = new Label("后山", skin);
        mapName.setFontScale(1.3f);
        mapCard.add(mapName).padTop(10f).row();

        // 文字介绍
        Label mapDesc = new Label(
            "这是一座位于北郊的后山，海拔不高但地形复杂。\n" +
            "几座凸起的山峰连绵起伏，山顶有平坦的平台。\n" +
            "挑战者需要从山脚出发，沿着斜坡攀登，\n" +
            "最终登顶主峰，俯瞰整个山谷。\n" +
            "注意：边界有空气墙保护，请勿尝试越界。",
            skin
        );
        mapDesc.setWrap(true);
        mapCard.add(mapDesc).width(400f).pad(15f).row();

        table.add(mapCard).padBottom(30f).row();

        // 进入游戏按钮
        TextButton btnEnter = new TextButton("进入后山", skin);
        btnEnter.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                game.setScreen(new GameScreen(game, false));
            }
        });
        table.add(btnEnter).width(250f).height(60f).padBottom(15f).row();

        // 返回按钮
        TextButton btnBack = new TextButton("返回主菜单", skin);
        btnBack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                game.setScreen(new MainMenuScreen(game));
            }
        });
        table.add(btnBack).width(250f).height(60f);
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
        if (mapPreviewTexture != null) mapPreviewTexture.dispose();
    }
}
