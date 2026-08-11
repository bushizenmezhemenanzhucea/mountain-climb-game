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

public class MountainClimbGame extends Game {
    private Skin skin;
    private BitmapFont font;

    @Override
    public void create() {
        AudioManager.getInstance().loadSounds();
        AudioManager.getInstance().loadBGM();
        createSkin();
        setScreen(new MainMenuScreen(this));
    }

    private void createSkin() {
        skin = new Skin();
        font = loadChineseFont();
        skin.add("default", font);

        Pixmap defaultPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        defaultPixmap.setColor(1f, 1f, 1f, 1f);
        defaultPixmap.fill();
        skin.add("default", new TextureRegionDrawable(new Texture(defaultPixmap)));

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        skin.add("default", labelStyle);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.downFontColor = Color.YELLOW;

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

        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        skin.add("default", scrollStyle);
    }

    private BitmapFont loadChineseFont() {
        try {
            com.badlogic.gdx.files.FileHandle fontFile = Gdx.files.internal("fonts/NotoSansSC.otf");
            if (fontFile.exists()) {
                FreeTypeFontGenerator generator = new FreeTypeFontGenerator(fontFile);
                FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
                param.size = 32;
                param.color = Color.WHITE;
                param.characters = "开始游戏继续设置更新日志检查退出后山选择地图进入音效音量背景音乐视角灵敏度保存返回主菜单V初始版本D爬山热系统关闭成功登顶保存进度返回主菜单这是一款位于北郊的后山海拔不高但地形复杂几座凸起的山峰连绵起伏山顶有平坦的平台挑战者需要从山脚出发沿着斜坡攀登最终登顶主峰俯瞰整个山谷注意边界有空气墙保护请勿尝试越界1234567890.%";
                BitmapFont f = generator.generateFont(param);
                generator.dispose();
                Gdx.app.log("Font", "Loaded Chinese font from assets");
                return f;
            }
        } catch (Exception e) {
            Gdx.app.log("Font", "Failed: " + e.getMessage());
        }
        BitmapFont defaultFont = new BitmapFont();
        defaultFont.getData().setScale(2f);
        return defaultFont;
    }

    public Skin getSkin() { return skin; }
    public BitmapFont getFont() { return font; }

    @Override
    public void dispose() {
        super.dispose();
        if (skin != null) skin.dispose();
        if (font != null) font.dispose();
        AudioManager.getInstance().dispose();
    }
}
