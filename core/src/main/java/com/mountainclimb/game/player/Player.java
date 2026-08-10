package com.mountainclimb.game.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.mountainclimb.game.GameConfig;

/**
 * 玩家3D模型与控制器
 * 使用程序化生成的胶囊体作为玩家模型
 */
public class Player {
    private Model playerModel;
    private ModelInstance playerInstance;
    private Vector3 position;
    private Vector3 velocity;
    private float yaw;   // 水平旋转角度（度）
    private float pitch; // 垂直俯仰角度（度）
    private boolean grounded = true;
    private float height;
    private float radius;

    // 碰撞体积
    private Vector3 collisionCenter;
    private float collisionRadius;
    private float collisionHeight;

    public Player(float startX, float startY, float startZ) {
        this.position = new Vector3(startX, startY, startZ);
        this.velocity = new Vector3(0, 0, 0);
        this.yaw = 0f;
        this.pitch = 0f;
        this.height = GameConfig.PLAYER_HEIGHT;
        this.radius = GameConfig.PLAYER_RADIUS;
        this.collisionRadius = radius;
        this.collisionHeight = height;
        this.collisionCenter = new Vector3();

        buildModel();
    }

    /**
     * 程序化构建玩家模型（球体代替胶囊体，兼容性更好）
     */
    private void buildModel() {
        ModelBuilder builder = new ModelBuilder();

        Material bodyMat = new Material(ColorAttribute.createDiffuse(new Color(0.2f, 0.4f, 0.8f, 1f))); // 蓝色身体

        // 使用内置方法创建球体，避免手动构建网格的API兼容性问题
        playerModel = builder.createSphere(
            radius * 2f, height, radius * 2f,
            16, 16,
            bodyMat,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );

        playerInstance = new ModelInstance(playerModel);
        updateTransform();
    }

    /**
     * 移动玩家
     * @param moveDir 相对于玩家朝向的移动方向 (x=左右, z=前后)
     * @param delta 时间步长
     */
    public void move(Vector2 moveDir, float delta, float terrainHeight) {
        if (moveDir.len2() < GameConfig.JOYSTICK_DEADZONE * GameConfig.JOYSTICK_DEADZONE) {
            velocity.x = 0;
            velocity.z = 0;
            return;
        }

        // 根据yaw角度将局部移动方向转换为世界方向
        float radYaw = (float)Math.toRadians(yaw);
        float sin = MathUtils.sin(radYaw);
        float cos = MathUtils.cos(radYaw);

        // moveDir: x = 左右, y(z) = 前后
        // 转换到世界坐标：
        // worldX = moveX * cos(90-yaw) + moveZ * cos(-yaw) ... 简化：
        float worldMoveX = moveDir.x * cos + moveDir.y * sin;
        float worldMoveZ = -moveDir.x * sin + moveDir.y * cos;

        velocity.x = worldMoveX * GameConfig.PLAYER_SPEED;
        velocity.z = worldMoveZ * GameConfig.PLAYER_SPEED;

        // 更新位置
        position.x += velocity.x * delta;
        position.z += velocity.z * delta;

        // 更新朝向（让模型面向移动方向）
        if (moveDir.len2() > 0.01f) {
            float targetYaw = (float)Math.toDegrees(MathUtils.atan2(-worldMoveX, -worldMoveZ));
            // 平滑旋转
            yaw = lerpAngle(yaw, targetYaw, 5f * delta);
        }
    }

    /**
     * 处理爬坡：根据地形高度调整Y坐标
     */
    public void applyTerrain(float terrainHeight) {
        float targetY = terrainHeight + height / 2f;
        position.y = targetY;
        grounded = true;
    }

    /**
     * 应用重力（如果不在地面上）
     */
    public void applyGravity(float delta, float terrainHeight) {
        float feetY = position.y - height / 2f;
        if (feetY > terrainHeight + 0.1f) {
            velocity.y += GameConfig.GRAVITY * delta;
            position.y += velocity.y * delta;
            grounded = false;
        } else {
            // 落地
            position.y = terrainHeight + height / 2f;
            velocity.y = 0;
            grounded = true;
        }
    }

    /**
     * 视角旋转（触摸滑动）
     */
    public void rotate(float deltaYaw, float deltaPitch, float sensitivityMultiplier) {
        yaw += deltaYaw * sensitivityMultiplier;
        pitch += deltaPitch * sensitivityMultiplier;
        // 限制俯仰角
        pitch = MathUtils.clamp(pitch, -80f, 80f);
        // 保持yaw在 0~360
        yaw = (yaw % 360f + 360f) % 360f;
    }

    public void updateTransform() {
        playerInstance.transform.idt();
        playerInstance.transform.translate(position);
        playerInstance.transform.rotate(Vector3.Y, yaw);
    }

    /**
     * 获取相机位置（第三人称视角，位于玩家后方上方）
     */
    public Vector3 getCameraPosition() {
        float radYaw = (float)Math.toRadians(yaw);
        float radPitch = (float)Math.toRadians(pitch);

        float cosYaw = MathUtils.cos(radYaw);
        float sinYaw = MathUtils.sin(radYaw);
        float cosPitch = MathUtils.cos(radPitch);
        float sinPitch = MathUtils.sin(radPitch);

        Vector3 camPos = new Vector3(
            position.x + GameConfig.CAMERA_DISTANCE * sinYaw * cosPitch,
            position.y + GameConfig.CAMERA_HEIGHT + GameConfig.CAMERA_DISTANCE * sinPitch,
            position.z + GameConfig.CAMERA_DISTANCE * cosYaw * cosPitch
        );
        return camPos;
    }

    public Vector3 getLookAt() {
        return position.cpy().add(0, height * 0.5f, 0);
    }

    public Vector3 getPosition() {
        return position;
    }

    public void setPosition(Vector3 pos) {
        this.position.set(pos);
        updateTransform();
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public ModelInstance getModelInstance() {
        return playerInstance;
    }

    public Vector3 getCollisionCenter() {
        collisionCenter.set(position.x, position.y, position.z);
        return collisionCenter;
    }

    public float getCollisionRadius() {
        return collisionRadius;
    }

    public float getCollisionHeight() {
        return collisionHeight;
    }

    public boolean isMoving() {
        return velocity.len2() > 0.01f;
    }

    public boolean isGrounded() {
        return grounded;
    }

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
