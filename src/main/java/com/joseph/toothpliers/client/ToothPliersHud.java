package com.joseph.toothpliers.client;

import com.joseph.toothpliers.ToothPliersMod;
import com.joseph.toothpliers.component.SkillData;
import com.joseph.toothpliers.config.ToothPliersConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class ToothPliersHud {

    private static final Identifier ICON_UV =
            Identifier.of("minecraft", "textures/item/spyglass.png");
    private static final Identifier ICON_LENS =
            Identifier.of("idv-tooth-pliers", "textures/gui/purple_stained_glass_block.png");
    private static final Identifier ICON_TP =
            Identifier.of("minecraft", "textures/item/ender_pearl.png");
    private static final Identifier ICON_MIST =
            Identifier.of("idv-tooth-pliers", "textures/gui/purple_stained_glass.png");

    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP  = 2;
    private static final int HOTBAR_HALF_WIDTH = 91;
    private static final int HOTBAR_HEIGHT = 22;
    private static final int SIDE_MARGIN = 4;
    private static final int LABEL_GAP = 2;

    public static void register() {
        HudRenderCallback.EVENT.register(ToothPliersHud::render);
    }

    private static void render(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        if (mc.currentScreen != null) return;
        if (mc.player.isSpectator()) return;
        if (!mc.player.getMainHandStack().isOf(ToothPliersMod.TOOTH_PLIERS)) return;

        int screenW = ctx.getScaledWindowWidth();
        int screenH = ctx.getScaledWindowHeight();
        int centerX = screenW / 2;

        int hotbarTop = screenH - HOTBAR_HEIGHT;
        int baseY = hotbarTop + (HOTBAR_HEIGHT - ICON_SIZE) / 2;
        int labelY = baseY - LABEL_GAP - mc.textRenderer.fontHeight;

        int slot = ICON_SIZE + ICON_GAP;
        int groupWidth = slot * 3 - ICON_GAP;

        // 预览模式：只画迷雾图标
        if (ToothPliersClientState.mistPreviewActive) {
            int rightStartX = centerX + HOTBAR_HALF_WIDTH + SIDE_MARGIN;
            int mistX = rightStartX + 2 * slot;
            drawIcon(ctx, ICON_MIST, mistX, baseY);
            ctx.drawBorder(mistX - 1, baseY - 1, ICON_SIZE + 2, ICON_SIZE + 2, 0xFFAA00FF);
            drawKeyLabel(ctx, mc, ToothPliersKeys.KEY_MIST, mistX, labelY);
            return;
        }

        // ==================== 左侧：角度透镜 ====================
        int leftStartX = centerX - HOTBAR_HALF_WIDTH - SIDE_MARGIN - groupWidth;
        KeyBinding[] lensKeys = {
                ToothPliersKeys.KEY_LENS_1,
                ToothPliersKeys.KEY_LENS_2,
                ToothPliersKeys.KEY_LENS_3
        };
        for (int i = 0; i < 3; i++) {
            int x = leftStartX + i * slot;
            drawIcon(ctx, ICON_LENS, x, baseY);

            if (ToothPliersClientState.lensLevel == i) {
                ctx.drawBorder(x - 1, baseY - 1, ICON_SIZE + 2, ICON_SIZE + 2, 0xFFAA00FF);
            }

            String num = String.valueOf(ToothPliersConfig.LENS_NORMAL_DISTANCE[i]);
            int numW = mc.textRenderer.getWidth(num);
            ctx.drawText(mc.textRenderer, num,
                    x + ICON_SIZE - numW - 1,
                    baseY + ICON_SIZE - 9,
                    0xFFFFFF, true);

            drawKeyLabel(ctx, mc, lensKeys[i], x, labelY);
        }

        // ==================== 右侧：UV / 传送 / 迷雾 ====================
        int rightStartX = centerX + HOTBAR_HALF_WIDTH + SIDE_MARGIN;
        KeyBinding[] rightKeys = {
                ToothPliersKeys.KEY_UV,
                ToothPliersKeys.KEY_TELEPORT,
                ToothPliersKeys.KEY_MIST
        };
        Identifier[] rightIcons = { ICON_UV, ICON_TP, ICON_MIST };

        SkillData data = ToothPliersClientTick.getHeldData();
        long now = mc.world != null ? mc.world.getTime() : 0;

        // ★ 迷雾图标激活条件：预览模式 或 存在自己放置的活跃迷雾
        boolean hasOwnMist = false;
        UUID selfUuid = mc.player.getUuid();
        for (ToothPliersClientState.MistEntry entry
                : ToothPliersClientState.MISTS.values()) {
            if (entry.owner.equals(selfUuid)) {
                hasOwnMist = true;
                break;
            }
        }
        boolean mistIconActive = hasOwnMist;

        boolean[] rightActive = {
                data.uvEnabled(),
                data.hasTeleportWindow(now),
                mistIconActive
        };

        for (int i = 0; i < 3; i++) {
            int x = rightStartX + i * slot;
            drawIcon(ctx, rightIcons[i], x, baseY);

            if (rightActive[i]) {
                ctx.drawBorder(x - 1, baseY - 1, ICON_SIZE + 2, ICON_SIZE + 2, 0xFFAA00FF);
            }

            drawKeyLabel(ctx, mc, rightKeys[i], x, labelY);
        }
    }

    private static void drawIcon(DrawContext ctx, Identifier icon, int x, int y) {
        ctx.fill(x - 1, y - 1, x + ICON_SIZE + 1, y + ICON_SIZE + 1, 0x80000000);
        ctx.drawTexture(icon, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    private static void drawKeyLabel(DrawContext ctx, MinecraftClient mc,
                                     KeyBinding kb, int iconX, int y) {
        String label = "[" + kb.getBoundKeyLocalizedText().getString() + "]";
        int w = mc.textRenderer.getWidth(label);
        int x = iconX + (ICON_SIZE - w) / 2;
        ctx.drawText(mc.textRenderer, label, x, y, 0xAAAAAA, true);
    }
}