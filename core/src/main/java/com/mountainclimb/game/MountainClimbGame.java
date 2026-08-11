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
        Gdx.app.log("Game", "create() started");
        try {
            AudioManager.getInstance().loadSounds();
            Gdx.app.log("Game", "Sounds loaded");
        } catch (Exception e) {
            Gdx.app.error("Game", "Sound failed: " + e.getMessage());
        }
        try {
            AudioManager.getInstance().loadBGM();
            Gdx.app.log("Game", "BGM loaded");
        } catch (Exception e) {
            Gdx.app.error("Game", "BGM failed: " + e.getMessage());
        }
        createSkin();
        setScreen(new MainMenuScreen(this));
        Gdx.app.log("Game", "create() done");
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
        String[] paths = {"fonts/NotoSansSC.otf", "fonts/NotoSansCJKsc-Regular.otf"};
        for (String path : paths) {
            try {
                com.badlogic.gdx.files.FileHandle f = Gdx.files.internal(path);
                if (f.exists()) {
                    Gdx.app.log("Font", "Loading: " + path + " (" + f.length() + " bytes)");
                    FreeTypeFontGenerator gen = new FreeTypeFontGenerator(f);
                    FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
                    p.size = 32;
                    p.color = Color.WHITE;
                    p.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
                    BitmapFont font = gen.generateFont(p);
                    gen.dispose();
                    Gdx.app.log("Font", "SUCCESS: " + path);
                    return font;
                } else {
                    Gdx.app.log("Font", "Not found: " + path);
                }
            } catch (Exception e) {
                Gdx.app.error("Font", "Failed " + path + ": " + e.getMessage());
            }
        }
        Gdx.app.log("Font", "Using default (no Chinese)");
        BitmapFont df = new BitmapFont();
        df.getData().setScale(2f);
        return df;
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
