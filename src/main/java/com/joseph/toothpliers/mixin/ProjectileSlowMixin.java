package com.joseph.toothpliers.mixin;

import com.joseph.toothpliers.server.MistAreaManager;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 弹射物进入迷雾时速度降低 85%，离开时恢复，并抵消重力避免下坠。
 */
@Mixin(ProjectileEntity.class)
public abstract class ProjectileSlowMixin {

    /** 迷雾内速度系数：保留 15% 速度 */
    @Unique
    private static final double IDV_MIST_SPEED_FACTOR = 0.15;

    /** 重力补偿：每 tick 补回的 y 速度，抵消原版重力（≈0.05/tick） */
    @Unique
    private static final double IDV_GRAVITY_COMPENSATION = 0.05;

    @Unique
    private boolean idv$inMist = false;

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void idv$slowProjectile(CallbackInfo ci) {
        ProjectileEntity self = (ProjectileEntity) (Object) this;
        if (!(self.getWorld() instanceof ServerWorld sw)) return;

        boolean inMist = MistAreaManager.isInsideAnyMist(
                sw, self.getX(), self.getY(), self.getZ());

        if (inMist) {
            if (!idv$inMist) {
                // 刚进入迷雾：速度 × 0.15
                self.setVelocity(self.getVelocity().multiply(IDV_MIST_SPEED_FACTOR));
            }
            // 重力补偿：抵消本 tick 的重力下坠
            Vec3d v = self.getVelocity();
            self.setVelocity(v.x, v.y + IDV_GRAVITY_COMPENSATION, v.z);
            self.velocityModified = true;

        } else if (idv$inMist) {
            // 刚离开迷雾：恢复原速
            self.setVelocity(self.getVelocity().multiply(1.0 / IDV_MIST_SPEED_FACTOR));
            self.velocityModified = true;
        }

        idv$inMist = inMist;
    }
}