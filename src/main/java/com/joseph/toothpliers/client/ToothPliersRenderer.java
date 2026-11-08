package com.joseph.toothpliers.client;

import com.joseph.toothpliers.ToothPliersMod;
import com.joseph.toothpliers.config.ToothPliersConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.OptionalDouble;

public class ToothPliersRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger("idv-tooth-pliers/renderer");
    private static boolean errorLogged = false;

    private static final Identifier WHITE_TEX =
            Identifier.of("minecraft", "textures/misc/white.png");

    /** 半透明圆柱体填充 */
    private static final RenderLayer MIST_LAYER = RenderLayer.of(
            "idv_mist_fill",
            VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
            VertexFormat.DrawMode.TRIANGLES,
            1536,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.ENTITY_TRANSLUCENT_EMISSIVE_PROGRAM)
                    .texture(new RenderPhase.Texture(WHITE_TEX, false, false))
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                    .build(false)
    );

    /** 迷雾内生物线框：穿透方块可见 */
    private static final RenderLayer MIST_ENTITY_LINES = RenderLayer.of(
            "idv_mist_entity_lines",
            VertexFormats.LINES,
            VertexFormat.DrawMode.LINES,
            1536,
            false,
            false,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.LINES_PROGRAM)
                    .lineWidth(new RenderPhase.LineWidth(OptionalDouble.empty()))
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .build(false)
    );

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            try {
                render(context);
            } catch (Throwable t) {
                if (!errorLogged) {
                    LOGGER.error("Render failed", t);
                    errorLogged = true;
                }
            }
        });
    }

    private static void render(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.options.hudHidden) return;

        // UV 紫框：客户端本地判断（无 S2C 依赖）
        if (mc.player.getMainHandStack().isOf(ToothPliersMod.TOOTH_PLIERS)
                && ToothPliersClientTick.getHeldData().uvEnabled()) {
            renderUvBox(context);
        }

        if (ToothPliersClientState.mistPreviewActive) {
            renderMistPreview(context);
        }

        renderMists(context);
        renderBroadcastUvs(context);
    }

    // ==================== UV 紫框（本地 raycast，零延迟） ====================
    private static void renderUvBox(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        VertexConsumerProvider consumersRaw = context.consumers();
        if (!(consumersRaw instanceof VertexConsumerProvider.Immediate consumers)) return;

        Vec3d camPos = context.camera().getPos();

        // ★ 每帧本地 raycast，跟随当前视角
        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0f);
        int baseDist = ToothPliersConfig.LENS_NORMAL_DISTANCE[ToothPliersClientState.lensLevel];
        double maxDist = Math.min(baseDist, ToothPliersConfig.UV_MAX_DISTANCE);
        Vec3d end = eye.add(look.multiply(maxDist));

        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                eye, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player));

        if (hit.getType() != HitResult.Type.BLOCK) return; // 无命中不画

        Vec3d pos = hit.getPos();
        Vec3d normal = Vec3d.of(hit.getSide().getVector());
        double cx = pos.x + normal.x * 0.01;
        double cy = pos.y + normal.y * 0.01;
        double cz = pos.z + normal.z * 0.01;

        // 水平朝向（一条棱垂直于水平视线）
        double hLen = Math.sqrt(look.x * look.x + look.z * look.z);
        double fx, fz;
        if (hLen > 1e-4) {
            fx = look.x / hLen;
            fz = look.z / hLen;
        } else {
            fx = 0; fz = 1;
        }
        double rx = -fz;
        double rz = fx;

        double halfSize = ToothPliersConfig.UV_AREA_WIDTH / 2.0;

        // 闪光状态仍由服务端 S2C 触发（颜色变化）
        float r, g, b, a;
        if (ToothPliersClientState.uvAttackFlashTicks > 0) {
            r = ToothPliersConfig.UV_FLASH_RED;
            g = ToothPliersConfig.UV_FLASH_GREEN;
            b = ToothPliersConfig.UV_FLASH_BLUE;
            a = 1.0f;
        } else {
            r = ToothPliersConfig.UV_RENDER_RED;
            g = ToothPliersConfig.UV_RENDER_GREEN;
            b = ToothPliersConfig.UV_RENDER_BLUE;
            a = ToothPliersConfig.UV_RENDER_ALPHA;
        }

        matrices.push();
        try {
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
            Matrix4f mat = matrices.peek().getPositionMatrix();

            drawThickQuad(lines, mat,
                    cx, cy, cz,
                    fx, fz, rx, rz,
                    halfSize,
                    r, g, b, a);

            consumers.draw(RenderLayer.getLines());
        } finally {
            matrices.pop();
        }
    }

    // ==================== 迷雾预览 ====================
    private static void renderMistPreview(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        VertexConsumerProvider consumersRaw = context.consumers();
        if (!(consumersRaw instanceof VertexConsumerProvider.Immediate consumers)) return;

        Vec3d camPos = context.camera().getPos();
        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0f);

        double d = ToothPliersClientState.mistPreviewDistance;
        double cx = eye.x + look.x * d;
        double cy = eye.y + look.y * d;
        double cz = eye.z + look.z * d;

        cy = ToothPliersConfig.clampMistCenterY(cy);

        drawMistFilledCylinder(matrices, consumers, camPos, cx, cy, cz);
    }

    // ==================== 迷雾区域 ====================
    private static void renderMists(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
        if (ToothPliersClientState.MISTS.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;
        VertexConsumerProvider consumersRaw = context.consumers();
        if (!(consumersRaw instanceof VertexConsumerProvider.Immediate consumers)) return;
        Vec3d camPos = context.camera().getPos();

        java.util.UUID selfUuid = mc.player.getUuid();

        for (var entry : ToothPliersClientState.MISTS.entrySet()) {
            ToothPliersClientState.MistEntry mist = entry.getValue();

            if (mist.owner.equals(selfUuid)) {
                drawMistFilledCylinder(matrices, consumers, camPos, mist.x, mist.y, mist.z);
            }

            drawMistEntities(matrices, consumers, camPos, mist.x, mist.y, mist.z);
        }
    }

    // ==================== 广播紫框 ====================
    private static void renderBroadcastUvs(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
        if (ToothPliersClientState.BROADCAST_UVS.isEmpty()) return;

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        VertexConsumerProvider consumersRaw = context.consumers();
        if (!(consumersRaw instanceof VertexConsumerProvider.Immediate consumers)) return;

        Vec3d camPos = context.camera().getPos();

        matrices.push();
        try {
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
            Matrix4f mat = matrices.peek().getPositionMatrix();

            for (ToothPliersClientState.BroadcastUv uv : ToothPliersClientState.BROADCAST_UVS) {
                double hLen = Math.sqrt(uv.dirX * uv.dirX + uv.dirZ * uv.dirZ);
                double fx, fz;
                if (hLen > 1e-4) {
                    fx = uv.dirX / hLen;
                    fz = uv.dirZ / hLen;
                } else {
                    fx = 0; fz = 1;
                }
                double rx = -fz;
                double rz = fx;

                drawThickQuad(lines, mat,
                        uv.x, uv.y, uv.z,
                        fx, fz, rx, rz,
                        ToothPliersConfig.UV_AREA_WIDTH / 2.0,
                        ToothPliersConfig.UV_FLASH_RED,
                        ToothPliersConfig.UV_FLASH_GREEN,
                        ToothPliersConfig.UV_FLASH_BLUE,
                        1.0f);
            }

            consumers.draw(RenderLayer.getLines());
        } finally {
            matrices.pop();
        }
    }

    // ==================== 半透明圆柱体 ====================
    private static void drawMistFilledCylinder(MatrixStack matrices,
                                               VertexConsumerProvider.Immediate consumers,
                                               Vec3d camPos,
                                               double cx, double cy, double cz) {
        double r = ToothPliersConfig.MIST_RADIUS;
        double h = ToothPliersConfig.MIST_HEIGHT;
        int sectors = ToothPliersConfig.MIST_FILL_SECTORS;
        float fr = ToothPliersConfig.MIST_FILL_RED;
        float fg = ToothPliersConfig.MIST_FILL_GREEN;
        float fb = ToothPliersConfig.MIST_FILL_BLUE;
        float fa = ToothPliersConfig.MIST_FILL_ALPHA;

        final int LIGHT = 0xF000F0;
        final int OVERLAY = OverlayTexture.DEFAULT_UV;

        float yb = (float) (cy - h / 2.0);
        float yt = (float) (cy + h / 2.0);
        float fcx = (float) cx;
        float fcz = (float) cz;

        matrices.push();
        try {
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);
            VertexConsumer vc = consumers.getBuffer(MIST_LAYER);
            Matrix4f mat = matrices.peek().getPositionMatrix();

            // 侧面
            for (int j = 0; j < sectors; j++) {
                double t1 = 2 * Math.PI * j / sectors;
                double t2 = 2 * Math.PI * (j + 1) / sectors;

                float x1 = (float) (cx + r * Math.cos(t1));
                float z1 = (float) (cz + r * Math.sin(t1));
                float x2 = (float) (cx + r * Math.cos(t2));
                float z2 = (float) (cz + r * Math.sin(t2));

                sphereVertex(vc, mat, x1, yb, z1, fr, fg, fb, fa, LIGHT, OVERLAY);
                sphereVertex(vc, mat, x2, yb, z2, fr, fg, fb, fa, LIGHT, OVERLAY);
                sphereVertex(vc, mat, x2, yt, z2, fr, fg, fb, fa, LIGHT, OVERLAY);
                sphereVertex(vc, mat, x1, yb, z1, fr, fg, fb, fa, LIGHT, OVERLAY);
                sphereVertex(vc, mat, x2, yt, z2, fr, fg, fb, fa, LIGHT, OVERLAY);
                sphereVertex(vc, mat, x1, yt, z1, fr, fg, fb, fa, LIGHT, OVERLAY);

                sphereVertex(vc, mat, x1, yb, z1, fr, fg, fb, fa, LIGHT, OVERLAY);
                sphereVertex(vc, mat, x2, yt, z2, fr, fg, fb, fa, LIGHT, OVERLAY);
                sphereVertex(vc, mat, x2, yb, z2, fr, fg, fb, fa, LIGHT, OVERLAY);
                sphereVertex(vc, mat, x1, yb, z1, fr, fg, fb, fa, LIGHT, OVERLAY);
                sphereVertex(vc, mat, x1, yt, z1, fr, fg, fb, fa, LIGHT, OVERLAY);
                sphereVertex(vc, mat, x2, yt, z2, fr, fg, fb, fa, LIGHT, OVERLAY);
            }

            // 顶面
            for (int j = 0; j < sectors; j++) {
                double t1 = 2 * Math.PI * j / sectors;
                double t2 = 2 * Math.PI * (j + 1) / sectors;
                float x1 = (float) (cx + r * Math.cos(t1));
                float z1 = (float) (cz + r * Math.sin(t1));
                float x2 = (float) (cx + r * Math.cos(t2));
                float z2 = (float) (cz + r * Math.sin(t2));

                sphereVertex(vc, mat, fcx, yt, fcz, fr, fg, fb, fa, LIGHT, OVERLAY);
                sphereVertex(vc, mat, x1, yt, z1, fr, fg, fb, fa, LIGHT, OVERLAY);
                sphereVertex(vc, mat, x2, yt, z2, fr, fg, fb, fa, LIGHT, OVERLAY);

                sphereVertex(vc, mat, fcx, yt, fcz, fr, fg, fb, fa, LIGHT, OVERLAY);
                sphereVertex(vc, mat, x2, yt, z2, fr, fg, fb, fa, LIGHT, OVERLAY);
                sphereVertex(vc, mat, x1, yt, z1, fr, fg, fb, fa, LIGHT, OVERLAY);
            }

            // 底面
            for (int j = 0; j < sectors; j++) {
                double t1 = 2 * Math.PI * j / sectors;
                double t2 = 2 * Math.PI * (j + 1) / sectors;
                float x1 = (float) (cx + r * Math.cos(t1));
                float z1 = (float) (cz + r * Math.sin(t1));
                float x2 = (float) (cx + r * Math.cos(t2));
                float z2 = (float) (cz + r * Math.sin(t2));

                sphereVertex(vc, mat, fcx, yb, fcz, fr, fg, fb, fa, LIGHT, OVERLAY);
                sphereVertex(vc, mat, x1, yb, z1, fr, fg, fb, fa, LIGHT, OVERLAY);
                sphereVertex(vc, mat, x2, yb, z2, fr, fg, fb, fa, LIGHT, OVERLAY);

                sphereVertex(vc, mat, fcx, yb, fcz, fr, fg, fb, fa, LIGHT, OVERLAY);
                sphereVertex(vc, mat, x2, yb, z2, fr, fg, fb, fa, LIGHT, OVERLAY);
                sphereVertex(vc, mat, x1, yb, z1, fr, fg, fb, fa, LIGHT, OVERLAY);
            }

            consumers.draw(MIST_LAYER);
        } finally {
            matrices.pop();
        }
    }

    private static void sphereVertex(VertexConsumer vc, Matrix4f mat,
                                     float x, float y, float z,
                                     float r, float g, float b, float a,
                                     int light, int overlay) {
        vc.vertex(mat, x, y, z)
                .color(r, g, b, a)
                .texture(0.5f, 0.5f)
                .overlay(overlay)
                .light(light)
                .normal(0f, 1f, 0f);
    }

    // ==================== 实体碰撞箱（穿透显示） ====================
    private static void drawMistEntities(MatrixStack matrices,
                                         VertexConsumerProvider.Immediate consumers,
                                         Vec3d camPos,
                                         double cx, double cy, double cz) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        double rSq = ToothPliersConfig.MIST_RADIUS * ToothPliersConfig.MIST_RADIUS;
        double halfH = ToothPliersConfig.MIST_HEIGHT / 2.0;

        matrices.push();
        try {
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);
            VertexConsumer lines = consumers.getBuffer(MIST_ENTITY_LINES);
            Matrix4f mat = matrices.peek().getPositionMatrix();

            float cr = ToothPliersConfig.MIST_BOX_RED;
            float cg = ToothPliersConfig.MIST_BOX_GREEN;
            float cb = ToothPliersConfig.MIST_BOX_BLUE;
            float ca = ToothPliersConfig.MIST_BOX_ALPHA;

            for (Entity e : mc.world.getEntities()) {
                if (e == mc.player) continue;

                double dx = e.getX() - cx;
                double dz = e.getZ() - cz;
                if (dx * dx + dz * dz > rSq) continue;
                if (e.getY() < cy - halfH || e.getY() > cy + halfH) continue;

                Box bb = e.getBoundingBox();
                float x1 = (float) bb.minX, y1 = (float) bb.minY, z1 = (float) bb.minZ;
                float x2 = (float) bb.maxX, y2 = (float) bb.maxY, z2 = (float) bb.maxZ;

                vline(lines, mat, x1, y1, z1, x2, y1, z1, cr, cg, cb, ca);
                vline(lines, mat, x2, y1, z1, x2, y1, z2, cr, cg, cb, ca);
                vline(lines, mat, x2, y1, z2, x1, y1, z2, cr, cg, cb, ca);
                vline(lines, mat, x1, y1, z2, x1, y1, z1, cr, cg, cb, ca);

                vline(lines, mat, x1, y2, z1, x2, y2, z1, cr, cg, cb, ca);
                vline(lines, mat, x2, y2, z1, x2, y2, z2, cr, cg, cb, ca);
                vline(lines, mat, x2, y2, z2, x1, y2, z2, cr, cg, cb, ca);
                vline(lines, mat, x1, y2, z2, x1, y2, z1, cr, cg, cb, ca);

                vline(lines, mat, x1, y1, z1, x1, y2, z1, cr, cg, cb, ca);
                vline(lines, mat, x2, y1, z1, x2, y2, z1, cr, cg, cb, ca);
                vline(lines, mat, x2, y1, z2, x2, y2, z2, cr, cg, cb, ca);
                vline(lines, mat, x1, y1, z2, x1, y2, z2, cr, cg, cb, ca);
            }

            consumers.draw(MIST_ENTITY_LINES);
        } finally {
            matrices.pop();
        }
    }

    // ==================== 工具 ====================
    private static void drawThickQuad(VertexConsumer lines, Matrix4f mat,
                                      double cx, double cy, double cz,
                                      double fx, double fz,
                                      double rx, double rz,
                                      double halfSize,
                                      float r, float g, float b, float a) {
        double thickness = ToothPliersConfig.UV_LINE_THICKNESS;
        int count = Math.max(1, ToothPliersConfig.UV_LINE_COUNT);
        double step = (count > 1) ? thickness / (count - 1) : 0;
        double halfThick = thickness / 2.0;

        double c1x = cx - fx * halfSize - rx * halfSize;
        double c1z = cz - fz * halfSize - rz * halfSize;
        double c2x = cx + fx * halfSize - rx * halfSize;
        double c2z = cz + fz * halfSize - rz * halfSize;
        double c3x = cx + fx * halfSize + rx * halfSize;
        double c3z = cz + fz * halfSize + rz * halfSize;
        double c4x = cx - fx * halfSize + rx * halfSize;
        double c4z = cz - fz * halfSize + rz * halfSize;
        float cyf = (float) cy;

        for (int i = 0; i < count; i++) {
            double off = -halfThick + i * step;
            double ox = rx * off;
            double oz = rz * off;
            vline(lines, mat,
                    (float) (c1x + ox), cyf, (float) (c1z + oz),
                    (float) (c2x + ox), cyf, (float) (c2z + oz),
                    r, g, b, a);
            vline(lines, mat,
                    (float) (c4x + ox), cyf, (float) (c4z + oz),
                    (float) (c3x + ox), cyf, (float) (c3z + oz),
                    r, g, b, a);
        }
        for (int i = 0; i < count; i++) {
            double off = -halfThick + i * step;
            double ox = fx * off;
            double oz = fz * off;
            vline(lines, mat,
                    (float) (c1x + ox), cyf, (float) (c1z + oz),
                    (float) (c4x + ox), cyf, (float) (c4z + oz),
                    r, g, b, a);
            vline(lines, mat,
                    (float) (c2x + ox), cyf, (float) (c2z + oz),
                    (float) (c3x + ox), cyf, (float) (c3z + oz),
                    r, g, b, a);
        }
    }

    private static void vline(VertexConsumer vc, Matrix4f mat,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float r, float g, float b, float a) {
        vc.vertex(mat, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0);
        vc.vertex(mat, x2, y2, z2).color(r, g, b, a).normal(0, 1, 0);
    }
}