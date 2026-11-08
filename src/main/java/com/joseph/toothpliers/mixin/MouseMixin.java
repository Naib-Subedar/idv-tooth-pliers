package com.joseph.toothpliers.mixin;

import com.joseph.toothpliers.ToothPliersMod;
import com.joseph.toothpliers.client.ToothPliersKeys;
import com.joseph.toothpliers.network.ToothPliersNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截鼠标右键事件：
 * 玩家手持牙钳时，右键只用来切换紫外辐射，
 * 不触发原版 use 逻辑（吃食物、放置方块、交互方块等）。
 *
 * 这样设计的原因：原版 options.useKey 也绑定右键，
 * 若不拦截，右键事件会被原版消费，KEY_UV.wasPressed() 检测不到。
 */
@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void idv$handleToothPliersMouse(long window, int button, int action, int mods,
                                            CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.currentScreen != null) return; // GUI 打开时不拦截
        if (!mc.player.getMainHandStack().isOf(ToothPliersMod.TOOTH_PLIERS)) return;

        // 玩家若把 UV 键改绑到其它鼠标键，仍通过 wasPressed 逻辑处理；
        // 这里只处理"默认键位 = 右键"的情况
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                && ToothPliersKeys.KEY_UV.getDefaultKey().getCode() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            ClientPlayNetworking.send(new ToothPliersNetworking.ToggleUvPayload());
            ci.cancel();
        }
    }
}