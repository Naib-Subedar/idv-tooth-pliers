package com.joseph.toothpliers.mixin;

import com.joseph.toothpliers.ToothPliersMod;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    /** 拦截左键攻击 */
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void idv$blockDoAttack(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        if (mc.player != null && mc.player.getMainHandStack().isOf(ToothPliersMod.TOOTH_PLIERS)) {
            cir.setReturnValue(false);
        }
    }

    /** 拦截左键挖方块 */
    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void idv$blockBlockBreaking(boolean breaking, CallbackInfo ci) {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        if (mc.player != null && mc.player.getMainHandStack().isOf(ToothPliersMod.TOOTH_PLIERS)) {
            ci.cancel();
        }
    }

    /**
     * 拦截右键物品使用。
     * 玩家手持牙钳时，右键不应该触发任何物品 use（比如吃食物、放置方块等），
     * 只应该由我们的 KEY_UV.wasPressed() 检测来处理 UV 切换。
     * 因为 doItemUse 会消费 options.useKey.wasPressed() 的状态，
     * 如果不拦截，KEY_UV（和 useKey 同键）永远检测不到。
     */
    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void idv$blockItemUse(CallbackInfo ci) {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        if (mc.player != null && mc.player.getMainHandStack().isOf(ToothPliersMod.TOOTH_PLIERS)) {
            ci.cancel();
        }
    }
}