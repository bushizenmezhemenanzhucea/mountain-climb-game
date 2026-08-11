package com.mountainclimb.game.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.mountainclimb.game.GameConfig;

public class Player {
    private Model playerModel;
    private ModelInstance playerInstance;
    private Vector3 position;
    private Vector3 velocity;
    private float yaw;
    private float pitch;
    private boolean grounded = true;
    private float height;
    private float radius;

    public Player(float startX, float startY, float startZ) {
        this.position = new Vector3(startX, startY, startZ);
        this.velocity = new Vector3(0, 0, 0);
        this.yaw = 0f;
        this.pitch = 20f;
        this.height = GameConfig.PLAYER_HEIGHT;
        this.radius = GameConfig.PLAYER_RADIUS;
        buildModel();
    }

    private void buildModel() {
        ModelBuilder builder = new ModelBuilder();
        Material bodyMat = new Material(ColorAttribute.createDiffuse(new Color(0.2f, 0.4f, 0.8f, 1f)));
        playerModel = builder.createCapsule(
            radius, height - radius * 2f, 16,
            bodyMat,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
        playerInstance = new ModelInstance(playerModel);
        updateTransform();
    }

    public void move(Vector2 moveDir, float delta, float terrainHeight) {
        if (moveDir.len2() < GameConfig.JOYSTICK_DEADZONE * GameConfig.JOYSTICK_DEADZONE) {
            velocity.x = 0;
            velocity.z = 0;
            return;
        }

        float radYaw = (float)Math.toRadians(yaw);
        float sin = MathUtils.sin(radYaw);
        float cos = MathUtils.cos(radYaw);

        float worldMoveX = moveDir.x * cos + moveDir.y * sin;
        float worldMoveZ = -moveDir.x * sin + moveDir.y * cos;

        velocity.x = worldMoveX * GameConfig.PLAYER_SPEED;
        velocity.z = worldMoveZ * GameConfig.PLAYER_SPEED;

        position.x += velocity.x * delta;
        position.z += velocity.z * delta;

        if (moveDir.len2() > 0.01f) {
            float targetYaw = (float)Math.toDegrees(MathUtils.atan2(worldMoveX, worldMoveZ));
            yaw = lerpAngle(yaw, targetYaw, 5f * delta);
        }
    }

    public void applyGravity(float delta, float terrainHeight) {
        float feetY = position.y - height / 2f;
        if (feetY > terrainHeight + 0.1f) {
            velocity.y += GameConfig.GRAVITY * delta;
            position.y += velocity.y * delta;
            grounded = false;
        } else {
            position.y = terrainHeight + height / 2f;
            velocity.y = 0;
            grounded = true;
        }
    }

    public void rotate(float deltaYaw, float deltaPitch, float sensitivityMultiplier) {
        yaw += deltaYaw * sensitivityMultiplier;
        pitch += deltaPitch * sensitivityMultiplier;
        pitch = MathUtils.clamp(pitch, 10f, 60f);
        yaw = (yaw % 360f + 360f) % 360f;
    }

    public void updateTransform() {
        playerInstance.transform.idt();
        playerInstance.transform.translate(position);
        playerInstance.transform.rotate(Vector3.Y, yaw);
    }

    public Vector3 getCameraPosition() {
        float radYaw = (float)Math.toRadians(yaw);
        float radPitch = (float)Math.toRadians(pitch);
        
        // 第三人称：相机在玩家后方（yaw + 180°）
        float camDist = GameConfig.CAMERA_DISTANCE;
        float camYaw = radYaw + MathUtils.PI;
        
        float camX = position.x + MathUtils.sin(camYaw) * MathUtils.cos(radPitch) * camDist;
        float camZ = position.z + MathUtils.cos(camYaw) * MathUtils.cos(radPitch) * camDist;
        float camY = position.y + GameConfig.CAMERA_HEIGHT + MathUtils.sin(radPitch) * camDist * 0.5f;
        
        return new Vector3(camX, camY, camZ);
    }

    public Vector3 getLookAt() {
        return new Vector3(position.x, position.y + height * 0.5f, position.z);
    }

    public Vector3 getPosition() { return position; }
    public void setPosition(Vector3 pos) {
        this.position.set(pos);
        updateTransform();
    }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public ModelInstance getModelInstance() { return playerInstance; }
    public boolean isGrounded() { return grounded; }

    private float lerpAngle(float from, float to, float t) {
        float diff = to - from;
        while (diff > 180f) diff -= 360f;
        while (diff < -180f) diff += 360f;
        return from + diff * t;
    }

    public void dispose() {
        if (playerModel != null) playerModel.dispose();
    }
}
