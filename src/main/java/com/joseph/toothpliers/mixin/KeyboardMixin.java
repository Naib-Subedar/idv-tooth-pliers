package com.joseph.toothpliers.mixin;

import com.joseph.toothpliers.ToothPliersMod;
import com.joseph.toothpliers.client.ToothPliersClientState;
import com.joseph.toothpliers.client.ToothPliersKeys;
import com.joseph.toothpliers.client.ToothPliersSoundPlayer;
import com.joseph.toothpliers.network.ToothPliersNetworking;
import com.joseph.toothpliers.sound.ToothPliersSounds;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void idv$handleToothPliersKeys(long window, int key, int scancode,
                                           int action, int modifiers, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.currentScreen != null) return;
        if (!mc.player.getMainHandStack().isOf(ToothPliersMod.TOOTH_PLIERS)) return;

        // ==================== 后摇锁定 ====================
        if (ToothPliersClientState.inRecovery) {
            // 后摇期间允许切换透镜
            if (ToothPliersKeys.KEY_LENS_1.matchesKey(key, scancode)) {
                sendLens(0); ci.cancel(); return;
            }
            if (ToothPliersKeys.KEY_LENS_2.matchesKey(key, scancode)) {
                sendLens(1); ci.cancel(); return;
            }
            if (ToothPliersKeys.KEY_LENS_3.matchesKey(key, scancode)) {
                sendLens(2); ci.cancel(); return;
            }

            // 后摇期间其它锁定
            if (key == GLFW.GLFW_KEY_E
                    || key == GLFW.GLFW_KEY_Q
                    || key == GLFW.GLFW_KEY_F
                    || (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9)) {
                ci.cancel();
                return;
            }
        }

        // ==================== 预览模式 ====================
        if (ToothPliersClientState.mistPreviewActive) {
            if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
                ci.cancel();
            }
            return;
        }

        // ---- 处理透镜切换 ----
        if (ToothPliersKeys.KEY_LENS_1.matchesKey(key, scancode)) {
            sendLens(0); ci.cancel(); return;
        }
        if (ToothPliersKeys.KEY_LENS_2.matchesKey(key, scancode)) {
            sendLens(1); ci.cancel(); return;
        }
        if (ToothPliersKeys.KEY_LENS_3.matchesKey(key, scancode)) {
            sendLens(2); ci.cancel(); return;
        }

        // ---- 屏蔽数字键 1-9 ----
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            ci.cancel();
        }
    }

    private static void sendLens(int level) {
        ClientPlayNetworking.send(new ToothPliersNetworking.SetLensPayload(level));
        // ★ 本地播放切换音效
        ToothPliersSoundPlayer.play(ToothPliersSounds.SWITCH_LENS);
    }
}