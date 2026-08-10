package com.mountainclimb.game.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.mountainclimb.game.GameConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 地形生成器：程序化生成后山地图
 * 包含：地面、多座山峰（平顶）、空气墙边界
 */
public class TerrainGenerator {

    /** 山峰信息 */
    public static class PeakInfo {
        public Vector3 center;    // 山中心位置
        public float baseRadius;  // 基底半径
        public float height;      // 高度
        public float flatTopRadius; // 平顶半径
        public BoundingBox bounds; // 碰撞边界盒
        public boolean summitReached = false; // 是否已登顶

        public PeakInfo(float x, float z, float radius, float h, float flatR) {
            this.center = new Vector3(x, 0, z);
            this.baseRadius = radius;
            this.height = h;
            this.flatTopRadius = flatR;
            // 计算AABB碰撞盒
            this.bounds = new BoundingBox(
                new Vector3(x - radius, 0, z - radius),
                new Vector3(x + radius, h, z + radius)
            );
        }

        public boolean contains(Vector3 point, float radius) {
            float dx = point.x - center.x;
            float dz = point.z - center.z;
            float dist = (float)Math.sqrt(dx*dx + dz*dz);
            return dist < baseRadius + radius;
        }

        public float getHeightAt(float x, float z) {
            float dx = x - center.x;
            float dz = z - center.z;
            float dist = (float)Math.sqrt(dx*dx + dz*dz);
            if (dist > baseRadius) return 0;
            if (dist < flatTopRadius) return height;
            // 平滑斜坡
            float t = (dist - flatTopRadius) / (baseRadius - flatTopRadius);
            return height * (1f - t * t); // 使用平方缓动，让山坡更自然
        }

        public boolean isOnSummit(Vector3 pos, float threshold) {
            float dx = pos.x - center.x;
            float dz = pos.z - center.z;
            float dist = (float)Math.sqrt(dx*dx + dz*dz);
            return dist < flatTopRadius && pos.y >= height - threshold;
        }
    }

    private Model terrainModel;
    private ModelInstance terrainInstance;
    private Model wallModel;
    private ModelInstance[] wallInstances;
    private List<PeakInfo> peaks = new ArrayList<>();

    private static final int TERRAIN_SEGMENTS = 80; // 地形网格细分
    private static final float GROUND_Y = 0f;

    public TerrainGenerator() {
        generatePeaks();
        buildTerrain();
        buildWalls();
    }

    /**
     * 定义几座山峰的参数
     */
    private void generatePeaks() {
        float half = GameConfig.WORLD_SIZE / 2f;
        // 主峰（最大，中心偏右）
        peaks.add(new PeakInfo(half * 0.3f, half * 0.2f, 25f, 30f, 4f));
        // 左侧山
        peaks.add(new PeakInfo(-half * 0.4f, half * 0.1f, 20f, 22f, 3f));
        // 远处小山
        peaks.add(new PeakInfo(half * 0.1f, -half * 0.5f, 15f, 15f, 2.5f));
        // 右侧丘陵
        peaks.add(new PeakInfo(half * 0.6f, -half * 0.2f, 18f, 18f, 3f));
    }

    /**
     * 使用 ModelBuilder 构建地形网格
     */
    private void buildTerrain() {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();

        Material groundMat = new Material(ColorAttribute.createDiffuse(new Color(0.25f, 0.45f, 0.15f))); // 绿色草地
        MeshPartBuilder mpb = builder.part("ground", GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.ColorPacked,
            groundMat);

        float half = GameConfig.WORLD_SIZE / 2f;
        float step = GameConfig.WORLD_SIZE / TERRAIN_SEGMENTS;

        // 生成网格顶点
        for (int i = 0; i < TERRAIN_SEGMENTS; i++) {
            for (int j = 0; j < TERRAIN_SEGMENTS; j++) {
                float x0 = -half + i * step;
                float z0 = -half + j * step;
                float x1 = x0 + step;
                float z1 = z0 + step;

                float h00 = getTerrainHeight(x0, z0);
                float h10 = getTerrainHeight(x1, z0);
                float h01 = getTerrainHeight(x0, z1);
                float h11 = getTerrainHeight(x1, z1);

                Vector3 v00 = new Vector3(x0, h00, z0);
                Vector3 v10 = new Vector3(x1, h10, z0);
                Vector3 v01 = new Vector3(x0, h01, z1);
                Vector3 v11 = new Vector3(x1, h11, z1);

                // 计算法线
                Vector3 n1 = calculateNormal(v00, v10, v01);
                Vector3 n2 = calculateNormal(v10, v11, v01);

                // 两个三角形组成一个四边形
                mpb.triangle(v00, n1, v10, n1, v01, n1);
                mpb.triangle(v10, n2, v11, n2, v01, n2);
            }
        }

        terrainModel = builder.end();
        terrainInstance = new ModelInstance(terrainModel);
    }

    /**
     * 获取某位置的地面高度（含山峰叠加）
     */
    public float getTerrainHeight(float x, float z) {
        float y = GROUND_Y;
        for (PeakInfo peak : peaks) {
            float ph = peak.getHeightAt(x, z);
            if (ph > y) y = ph;
        }
        // 添加一些随机起伏让地面更自然（除山峰区域外）
        boolean onPeak = false;
        for (PeakInfo peak : peaks) {
            float dx = x - peak.center.x;
            float dz = z - peak.center.z;
            if (Math.sqrt(dx*dx + dz*dz) < peak.baseRadius) {
                onPeak = true;
                break;
            }
        }
        if (!onPeak) {
            y += MathUtils.sin(x * 0.1f) * MathUtils.cos(z * 0.1f) * 0.5f;
        }
        return y;
    }

    private Vector3 calculateNormal(Vector3 v1, Vector3 v2, Vector3 v3) {
        Vector3 a = v2.cpy().sub(v1);
        Vector3 b = v3.cpy().sub(v1);
        return a.crs(b).nor();
    }

    /**
     * 构建正方体空气墙（4面透明墙 + 底部）
     */
    private void buildWalls() {
        float half = GameConfig.WORLD_SIZE / 2f;
        float height = 50f;

        ModelBuilder builder = new ModelBuilder();
        builder.begin();

        // 半透明蓝色材质
        Material wallMat = new Material(
            ColorAttribute.createDiffuse(new Color(0.3f, 0.5f, 0.9f, 0.15f)),
            new ColorAttribute(ColorAttribute.createSpecular(Color.WHITE)),
            new com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute(true, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.15f)
        );

        MeshPartBuilder mpb = builder.part("walls", GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
            wallMat);

        // 4面墙
        // 前面 (z = +half)
        buildWallQuad(mpb, new Vector3(-half, 0, half), new Vector3(half, 0, half),
            new Vector3(-half, height, half), new Vector3(half, height, half));
        // 后面 (z = -half)
        buildWallQuad(mpb, new Vector3(half, 0, -half), new Vector3(-half, 0, -half),
            new Vector3(half, height, -half), new Vector3(-half, height, -half));
        // 左面 (x = -half)
        buildWallQuad(mpb, new Vector3(-half, 0, -half), new Vector3(-half, 0, half),
            new Vector3(-half, height, -half), new Vector3(-half, height, half));
        // 右面 (x = +half)
        buildWallQuad(mpb, new Vector3(half, 0, half), new Vector3(half, 0, -half),
            new Vector3(half, height, half), new Vector3(half, height, -half));

        wallModel = builder.end();
        wallInstances = new ModelInstance[] { new ModelInstance(wallModel) };
    }

    private void buildWallQuad(MeshPartBuilder mpb, Vector3 bl, Vector3 br, Vector3 tl, Vector3 tr) {
        Vector3 normal = calculateNormal(bl, br, tl);
        mpb.triangle(bl, normal, br, normal, tl, normal);
        mpb.triangle(br, normal, tr, normal, tl, normal);
    }

    // ===== 碰撞检测 =====

    /**
     * 检测位置是否在世界边界内
     */
    public boolean isInsideWorld(float x, float z) {
        float half = GameConfig.WORLD_SIZE / 2f - GameConfig.PLAYER_RADIUS;
        return x >= -half && x <= half && z >= -half && z <= half;
    }

    /**
     * 将位置限制在世界边界内（空气墙碰撞）
     */
    public Vector3 clampToWorld(Vector3 pos) {
        float half = GameConfig.WORLD_SIZE / 2f - GameConfig.PLAYER_RADIUS;
        pos.x = MathUtils.clamp(pos.x, -half, half);
        pos.z = MathUtils.clamp(pos.z, -half, half);
        return pos;
    }

    /**
     * 获取山峰列表（用于碰撞和登顶检测）
     */
    public List<PeakInfo> getPeaks() {
        return peaks;
    }

    // ===== 渲染 =====

    public ModelInstance getTerrainInstance() {
        return terrainInstance;
    }

    public ModelInstance[] getWallInstances() {
        return wallInstances;
    }

    public void dispose() {
        if (terrainModel != null) terrainModel.dispose();
        if (wallModel != null) wallModel.dispose();
    }
}
