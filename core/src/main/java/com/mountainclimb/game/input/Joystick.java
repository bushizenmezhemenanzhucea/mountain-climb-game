package com.mountainclimb.game.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mountainclimb.game.GameConfig;

/**
 * 圆形虚拟摇杆
 * 位置：屏幕左下角
 * 功能：往哪个方向滑，玩家就往哪个方向走
 */
public class Joystick extends Actor {
    private TextureRegion baseTexture;
    private TextureRegion knobTexture;
    private Vector2 center = new Vector2();
    private Vector2 knobPos = new Vector2();
    private float baseRadius;
    private float knobRadius;
    private float maxDist;
    private boolean touched = false;
    private int touchPointer = -1;

    public Joystick(float size, float knobSize) {
        this.baseRadius = size / 2f;
        this.knobRadius = knobSize / 2f;
        this.maxDist = baseRadius - knobRadius - 5f;
        setSize(size, size);

        // 程序化生成圆形纹理
        baseTexture = createCircleTexture((int)size, new Color(0.2f, 0.2f, 0.2f, 0.5f));
        knobTexture = createCircleTexture((int)knobSize, new Color(0.6f, 0.6f, 0.6f, 0.8f));

        // 默认放在左下角
        setPosition(30f, 30f);

        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (touched && touchPointer != pointer) return false;
                Vector2 local = new Vector2(x, y);
                if (local.dst(baseRadius, baseRadius) <= baseRadius) {
                    touched = true;
                    touchPointer = pointer;
                    updateKnob(local);
                    return true;
                }
                return false;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (touched && touchPointer == pointer) {
                    updateKnob(new Vector2(x, y));
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (touched && touchPointer == pointer) {
                    touched = false;
                    touchPointer = -1;
                    knobPos.set(0, 0);
                }
            }
        });
    }

    private void updateKnob(Vector2 local) {
        Vector2 delta = local.cpy().sub(baseRadius, baseRadius);
        if (delta.len() > maxDist) {
            delta.setLength(maxDist);
        }
        knobPos.set(delta);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        center.set(getX() + baseRadius, getY() + baseRadius);
        // 绘制底座
        batch.draw(baseTexture, getX(), getY(), getWidth(), getHeight());
        // 绘制摇杆按钮
        float kx = center.x + knobPos.x - knobRadius;
        float ky = center.y + knobPos.y - knobRadius;
        batch.draw(knobTexture, kx, ky, knobRadius * 2f, knobRadius * 2f);
    }

    /**
     * 获取归一化的方向向量（-1 ~ 1）
     */
    public Vector2 getDirection() {
        if (!touched) return Vector2.Zero;
        return knobPos.cpy().scl(1f / maxDist);
    }

    public boolean isTouched() {
        return touched;
    }

    @Override
    public void setPosition(float x, float y) {
        super.setPosition(x, y);
        center.set(x + baseRadius, y + baseRadius);
    }

    private TextureRegion createCircleTexture(int size, Color color) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        pixmap.setColor(color);
        pixmap.fillCircle(size / 2, size / 2, size / 2 - 2);
        // 边框
        pixmap.setColor(color.r * 1.2f, color.g * 1.2f, color.b * 1.2f, color.a);
        for (int i = 0; i < 3; i++) {
            pixmap.drawCircle(size / 2, size / 2, size / 2 - 1 - i);
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegion(texture);
    }

    public void dispose() {
        if (baseTexture != null) baseTexture.getTexture().dispose();
        if (knobTexture != null) knobTexture.getTexture().dispose();
    }
}
