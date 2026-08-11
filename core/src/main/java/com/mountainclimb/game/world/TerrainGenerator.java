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

public class TerrainGenerator {
    public static class PeakInfo {
        public Vector3 center;
        public float baseRadius;
        public float height;
        public float flatTopRadius;
        public BoundingBox bounds;
        public boolean summitReached = false;

        public PeakInfo(float x, float z, float radius, float h, float flatR) {
            this.center = new Vector3(x, 0, z);
            this.baseRadius = radius;
            this.height = h;
            this.flatTopRadius = flatR;
            this.bounds = new BoundingBox(
                new Vector3(x - radius, 0, z - radius),
                new Vector3(x + radius, h, z + radius)
            );
        }

        public float getHeightAt(float x, float z) {
            float dx = x - center.x;
            float dz = z - center.z;
            float dist = (float)Math.sqrt(dx*dx + dz*dz);
            if (dist > baseRadius) return 0;
            if (dist < flatTopRadius) return height;
            float t = (dist - flatTopRadius) / (baseRadius - flatTopRadius);
            return height * (1f - t * t);
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
    private Model groundModel;
    private ModelInstance groundInstance;
    private Model wallModel;
    private ModelInstance[] wallInstances;
    private List<PeakInfo> peaks = new ArrayList<>();
    private static final int TERRAIN_SEGMENTS = 80;

    public TerrainGenerator() {
        generatePeaks();
        buildGround();
        buildTerrain();
        buildWalls();
    }

    private void generatePeaks() {
        float half = GameConfig.WORLD_SIZE / 2f;
        peaks.add(new PeakInfo(half * 0.3f, half * 0.2f, 25f, 30f, 4f));
        peaks.add(new PeakInfo(-half * 0.4f, half * 0.1f, 20f, 22f, 3f));
        peaks.add(new PeakInfo(half * 0.1f, -half * 0.5f, 15f, 15f, 2.5f));
        peaks.add(new PeakInfo(half * 0.6f, -half * 0.2f, 18f, 18f, 3f));
    }

    private void buildGround() {
        ModelBuilder builder = new ModelBuilder();
        Material groundMat = new Material(ColorAttribute.createDiffuse(new Color(0.15f, 0.35f, 0.1f, 1f)));
        float size = GameConfig.WORLD_SIZE * 2f;
        groundModel = builder.createBox(size, 0.2f, size, groundMat,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        groundInstance = new ModelInstance(groundModel);
        groundInstance.transform.translate(0, -0.1f, 0);
    }

    private void buildTerrain() {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        Material groundMat = new Material(ColorAttribute.createDiffuse(new Color(0.25f, 0.45f, 0.15f, 1f)));
        MeshPartBuilder mpb = builder.part("ground", GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, groundMat);
        float half = GameConfig.WORLD_SIZE / 2f;
        float step = GameConfig.WORLD_SIZE / TERRAIN_SEGMENTS;
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
                Vector3 n1 = calculateNormal(v00, v10, v01);
                Vector3 n2 = calculateNormal(v10, v11, v01);
                short i00 = mpb.vertex(v00, n1, null, null);
                short i10 = mpb.vertex(v10, n1, null, null);
                short i01 = mpb.vertex(v01, n1, null, null);
                short i11 = mpb.vertex(v11, n2, null, null);
                mpb.triangle(i00, i10, i01);
                mpb.triangle(i10, i11, i01);
            }
        }
        terrainModel = builder.end();
        terrainInstance = new ModelInstance(terrainModel);
    }

    public float getTerrainHeight(float x, float z) {
        float y = 0f;
        for (PeakInfo peak : peaks) {
            float ph = peak.getHeightAt(x, z);
            if (ph > y) y = ph;
        }
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

    private void buildWalls() {
        float half = GameConfig.WORLD_SIZE / 2f;
        float height = 50f;
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        Material wallMat = new Material(
            ColorAttribute.createDiffuse(new Color(0.3f, 0.5f, 0.9f, 0.15f)),
            new ColorAttribute(ColorAttribute.createSpecular(Color.WHITE)),
            new com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute(true, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.15f)
        );
        MeshPartBuilder mpb = builder.part("walls", GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, wallMat);
        buildWallQuadIndexed(mpb, new Vector3(-half, 0, half), new Vector3(half, 0, half),
            new Vector3(-half, height, half), new Vector3(half, height, half));
        buildWallQuadIndexed(mpb, new Vector3(half, 0, -half), new Vector3(-half, 0, -half),
            new Vector3(half, height, -half), new Vector3(-half, height, -half));
        buildWallQuadIndexed(mpb, new Vector3(-half, 0, -half), new Vector3(-half, 0, half),
            new Vector3(-half, height, -half), new Vector3(-half, height, half));
        buildWallQuadIndexed(mpb, new Vector3(half, 0, half), new Vector3(half, 0, -half),
            new Vector3(half, height, half), new Vector3(half, height, -half));
        wallModel = builder.end();
        wallInstances = new ModelInstance[] { new ModelInstance(wallModel) };
    }

    private void buildWallQuadIndexed(MeshPartBuilder mpb, Vector3 bl, Vector3 br, Vector3 tl, Vector3 tr) {
        Vector3 normal = calculateNormal(bl, br, tl);
        short iBl = mpb.vertex(bl, normal, null, null);
        short iBr = mpb.vertex(br, normal, null, null);
        short iTl = mpb.vertex(tl, normal, null, null);
        short iTr = mpb.vertex(tr, normal, null, null);
        mpb.triangle(iBl, iBr, iTl);
        mpb.triangle(iBr, iTr, iTl);
    }

    public boolean isInsideWorld(float x, float z) {
        float half = GameConfig.WORLD_SIZE / 2f - GameConfig.PLAYER_RADIUS;
        return x >= -half && x <= half && z >= -half && z <= half;
    }

    public Vector3 clampToWorld(Vector3 pos) {
        float half = GameConfig.WORLD_SIZE / 2f - GameConfig.PLAYER_RADIUS;
        pos.x = MathUtils.clamp(pos.x, -half, half);
        pos.z = MathUtils.clamp(pos.z, -half, half);
        return pos;
    }

    public List<PeakInfo> getPeaks() { return peaks; }
    public ModelInstance getTerrainInstance() { return terrainInstance; }
    public ModelInstance getGroundInstance() { return groundInstance; }
    public ModelInstance[] getWallInstances() { return wallInstances; }

    public void dispose() {
        if (terrainModel != null) terrainModel.dispose();
        if (groundModel != null) groundModel.dispose();
        if (wallModel != null) wallModel.dispose();
    }
}
