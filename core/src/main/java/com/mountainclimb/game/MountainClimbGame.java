package com.mountainclimb.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mountainclimb.game.audio.AudioManager;
import com.mountainclimb.game.screen.MainMenuScreen;

/**
 * 游戏主类（LibGDX Game）
 */
public class MountainClimbGame extends Game {
    private Skin skin;
    private BitmapFont font;

    @Override
    public void create() {
        // 初始化音频
        AudioManager.getInstance().loadSounds();
        AudioManager.getInstance().loadBGM();

        // 创建皮肤
        createSkin();

        // 进入主菜单
        setScreen(new MainMenuScreen(this));
    }

    /**
     * 创建程序化的 Skin（无需外部皮肤文件）
     */
    private void createSkin() {
        skin = new Skin();

        // 生成中文字体（如果外部字体存在则使用，否则用默认字体）
        try {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
                Gdx.files.internal("fonts/NotoSansCJK-Regular.ttc")
            );
            FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
            param.size = 32;
            param.color = Color.WHITE;
            param.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
            font = generator.generateFont(param);
            generator.dispose();
        } catch (Exception e) {
            Gdx.app.log("MountainClimbGame", "Custom font not found, using default");
            font = new BitmapFont();
            font.getData().setScale(2f);
        }
        skin.add("default", font);

        // 样式定义
        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        skin.add("default", labelStyle);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.downFontColor = Color.YELLOW;

        // 创建按钮背景（使用 Pixmap 程序化生成）
        Pixmap pixmapUp = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmapUp.setColor(0.2f, 0.25f, 0.3f, 0.8f);
        pixmapUp.fill();
        buttonStyle.up = new TextureRegionDrawable(new Texture(pixmapUp));

        Pixmap pixmapDown = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmapDown.setColor(0.3f, 0.4f, 0.5f, 0.9f);
        pixmapDown.fill();
        buttonStyle.down = new TextureRegionDrawable(new Texture(pixmapDown));

        Pixmap pixmapOver = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmapOver.setColor(0.25f, 0.3f, 0.35f, 0.85f);
        pixmapOver.fill();
        buttonStyle.over = new TextureRegionDrawable(new Texture(pixmapOver));

        skin.add("default", buttonStyle);

        // Slider 样式
        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        Pixmap knobPixmap = new Pixmap(30, 30, Pixmap.Format.RGBA8888);
        knobPixmap.setColor(Color.LIGHT_GRAY);
        knobPixmap.fillCircle(15, 15, 14);
        sliderStyle.knob = new TextureRegionDrawable(new Texture(knobPixmap));

        Pixmap bgPixmap = new Pixmap(200, 10, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(0.3f, 0.3f, 0.3f, 1f);
        bgPixmap.fill();
        sliderStyle.background = new TextureRegionDrawable(new Texture(bgPixmap));
        skin.add("default-horizontal", sliderStyle);

        // ScrollPane 样式
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        skin.add("default", scrollStyle);
    }

    public Skin getSkin() {
        return skin;
    }

    public BitmapFont getFont() {
        return font;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (skin != null) skin.dispose();
        if (font != null) font.dispose();
        AudioManager.getInstance().dispose();
    }
}
