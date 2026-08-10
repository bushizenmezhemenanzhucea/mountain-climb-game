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
     * 程序化构建玩家模型（胶囊体）
     */
    private void buildModel() {
        ModelBuilder builder = new ModelBuilder();

        Material bodyMat = new Material(ColorAttribute.createDiffuse(new Color(0.2f, 0.4f, 0.8f))); // 蓝色身体
        Material headMat = new Material(ColorAttribute.createDiffuse(new Color(0.9f, 0.7f, 0.5f))); // 肤色头部

        builder.begin();

        // 身体（圆柱体）
        builder.part("body", com.badlogic.gdx.graphics.GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
            bodyMat);

        // 这里简化处理，使用一个组合模型
        // 实际使用一个胶囊形状：圆柱 + 两个半球

        // 简化：使用一个球体代替（后续可扩展为更复杂模型）
        com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder mpb = builder.part("player", com.badlogic.gdx.graphics.GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
            bodyMat);

        // 创建胶囊体近似（球体 + 圆柱体）
        createCapsule(mpb, radius, height);

        playerModel = builder.end();
        playerInstance = new ModelInstance(playerModel);
        updateTransform();
    }

    /**
     * 程序化创建胶囊体网格
     */
    private void createCapsule(com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder mpb, float r, float h) {
        int segments = 16;
        int rings = 8;
        float halfH = h / 2f - r;

        // 创建半球（上部和下部）和中间圆柱
        // 简化为一个拉伸的球体
        for (int i = 0; i < rings; i++) {
            float theta1 = (float)(i * Math.PI / rings);
            float theta2 = (float)((i + 1) * Math.PI / rings);

            for (int j = 0; j < segments; j++) {
                float phi1 = (float)(j * 2f * Math.PI / segments);
                float phi2 = (float)((j + 1) * 2f * Math.PI / segments);

                Vector3 v1 = spherePoint(theta1, phi1, r, halfH);
                Vector3 v2 = spherePoint(theta2, phi1, r, halfH);
                Vector3 v3 = spherePoint(theta1, phi2, r, halfH);
                Vector3 v4 = spherePoint(theta2, phi2, r, halfH);

                Vector3 n1 = v1.cpy().nor();
                Vector3 n2 = v2.cpy().nor();
                Vector3 n3 = v3.cpy().nor();
                Vector3 n4 = v4.cpy().nor();

                mpb.triangle(v1, n1, v2, n2, v3, n3);
                mpb.triangle(v2, n2, v4, n4, v3, n3);
            }
        }
    }

    private Vector3 spherePoint(float theta, float phi, float r, float halfH) {
        float y = r * MathUtils.cos(theta);
        float sinTheta = MathUtils.sin(theta);
        float x = r * sinTheta * MathUtils.cos(phi);
        float z = r * sinTheta * MathUtils.sin(phi);

        // 拉伸成胶囊形状
        if (y > 0) y += halfH;
        else y -= halfH;

        return new Vector3(x, y, z);
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
