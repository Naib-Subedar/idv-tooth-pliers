package com.joseph.toothpliers.mixin;

import com.joseph.toothpliers.server.MistAreaManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 迷雾内生物攻击频率下降 50%。
 *
 * 原版 MobEntity.tryAttack 每 tick 由 AI 调用，返回 true 表示本次攻击命中。
 * 在迷雾内 50% 概率直接取消本次尝试，让攻击间隔拉长一倍。
 */
@Mixin(MobEntity.class)
public abstract class MobAttackSlowMixin {

    @Inject(method = "tryAttack", at = @At("HEAD"),
            cancellable = true, require = 0)
    private void idv$slowMobAttack(Entity target, CallbackInfoReturnable<Boolean> cir) {
        MobEntity self = (MobEntity) (Object) this;
        if (!(self.getWorld() instanceof ServerWorld sw)) return;

        if (MistAreaManager.isInsideAnyMist(sw, self.getX(), self.getY(), self.getZ())) {
            if (self.getRandom().nextFloat() < 0.5f) {
                cir.setReturnValue(false);
            }
        }
    }
}