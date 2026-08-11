package com.mountainclimb.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mountainclimb.game.audio.AudioManager;
import com.mountainclimb.game.screen.MainMenuScreen;

public class MountainClimbGame extends Game {
    private Skin skin;
    private BitmapFont font;

    @Override
    public void create() {
        try { AudioManager.getInstance().loadSounds(); } catch (Exception e) {}
        try { AudioManager.getInstance().loadBGM(); } catch (Exception e) {}
        createSkin();
        setScreen(new MainMenuScreen(this));
    }

    private void createSkin() {
        skin = new Skin();
        font = loadBitmapFont();
        skin.add("default", font);

        Pixmap defaultPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        defaultPixmap.setColor(1f, 1f, 1f, 1f);
        defaultPixmap.fill();
        TextureRegionDrawable defaultDrawable = new TextureRegionDrawable(new Texture(defaultPixmap));
        skin.add("default", defaultDrawable);

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        skin.add("default", labelStyle);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.downFontColor = Color.YELLOW;

        Pixmap btnUp = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        btnUp.setColor(0.2f, 0.25f, 0.3f, 0.8f);
        btnUp.fill();
        buttonStyle.up = new TextureRegionDrawable(new Texture(btnUp));

        Pixmap btnDown = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        btnDown.setColor(0.3f, 0.4f, 0.5f, 0.9f);
        btnDown.fill();
        buttonStyle.down = new TextureRegionDrawable(new Texture(btnDown));

        skin.add("default", buttonStyle);

        Window.WindowStyle windowStyle = new Window.WindowStyle(font, Color.WHITE, defaultDrawable);
        skin.add("default", windowStyle);
        skin.add("dialog", windowStyle);

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

    private BitmapFont loadBitmapFont() {
        try {
            com.badlogic.gdx.files.FileHandle fntFile = Gdx.files.internal("fonts/game_font.fnt");
            com.badlogic.gdx.files.FileHandle pngFile = Gdx.files.internal("fonts/game_font.png");
            if (fntFile.exists() && pngFile.exists()) {
                BitmapFont f = new BitmapFont(fntFile, pngFile, false);
                Gdx.app.log("Font", "Bitmap font loaded: " + pngFile.length() + " bytes");
                return f;
            }
        } catch (Exception e) {
            Gdx.app.error("Font", "Bitmap font failed: " + e.getMessage());
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
