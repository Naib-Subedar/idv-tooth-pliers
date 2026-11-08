package com.joseph.toothpliers.mixin;

import com.joseph.toothpliers.client.ToothPliersClientState;
import com.joseph.toothpliers.config.ToothPliersConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截鼠标视角变化：
 * 1. 迷雾预览模式：X 轴旋转 yaw，Y 轴调整预览距离
 * 2. UV 攻击锁视角：只允许 X 轴，Y 轴直接丢弃（无震动）
 */
@Mixin(Entity.class)
public abstract class EntityLookMixin {

    @Inject(method = "changeLookDirection", at = @At("HEAD"),
            cancellable = true, require = 0)
    private void idv$lockLookDirection(double cursorDeltaX, double cursorDeltaY,
                                       CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof ClientPlayerEntity)) return;

        // ==================== UV 攻击锁视角 ====================
        if (ToothPliersClientState.uvLockPitchActive) {
            // X 轴正常应用
            float newYaw = self.getYaw() + (float) cursorDeltaX * 0.15f;
            self.setYaw(newYaw);
            // Y 轴丢弃，pitch 固定
            self.setPitch(ToothPliersClientState.uvLockedPitch);
            ci.cancel();
            return;
        }

        // ==================== 迷雾预览模式 ====================
        if (ToothPliersClientState.mistPreviewActive) {
            // X 轴 → yaw
            float yawDelta = (float) cursorDeltaX * 0.15f;
            self.setYaw(self.getYaw() + yawDelta);

            // Y 轴 → 距离（上移增大，下移减小）
            double distanceDelta = -cursorDeltaY * 0.5;
            double newDist = ToothPliersClientState.mistPreviewDistance + distanceDelta;
            newDist = Math.max(0, Math.min(ToothPliersConfig.MIST_MAX_DISTANCE, newDist));
            ToothPliersClientState.mistPreviewDistance = newDist;

            ci.cancel();
        }
    }
}