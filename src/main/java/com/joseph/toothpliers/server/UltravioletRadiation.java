package com.joseph.toothpliers.server;

import com.joseph.toothpliers.config.ToothPliersConfig;
import com.joseph.toothpliers.state.SkillState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public final class UltravioletRadiation {
    private UltravioletRadiation() {}

    public static void updatePosition(ServerPlayerEntity player, SkillState st) {
        World world = player.getWorld();
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);

        int baseDist = ToothPliersConfig.LENS_NORMAL_DISTANCE[st.lensLevel];
        double maxDist = Math.min(baseDist, ToothPliersConfig.UV_MAX_DISTANCE);

        Vec3d end = eye.add(look.multiply(maxDist));

        BlockHitResult hit = world.raycast(new RaycastContext(
                eye, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player));

        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3d pos = hit.getPos();
            Vec3d normal = Vec3d.of(hit.getSide().getVector());
            st.uvHitX = pos.x + normal.x * 0.01;
            st.uvHitY = pos.y + normal.y * 0.01;
            st.uvHitZ = pos.z + normal.z * 0.01;
            st.uvHasArea = true;

            double hLen = Math.sqrt(look.x * look.x + look.z * look.z);
            if (hLen > 1e-4) {
                st.uvDirX = look.x / hLen;
                st.uvDirZ = look.z / hLen;
            } else {
                st.uvDirX = 0;
                st.uvDirZ = 1;
            }
        } else {
            st.uvHasArea = false;
        }
    }

    /** 辐照区域伤害判定 Box（向上 4 格，向下 3 格） */
    public static Box getDamageBox(SkillState st) {
        double halfSize = ToothPliersConfig.UV_AREA_WIDTH / 2.0;
        return new Box(
                st.uvHitX - halfSize,
                st.uvHitY - ToothPliersConfig.UV_DAMAGE_DOWN,
                st.uvHitZ - halfSize,
                st.uvHitX + halfSize,
                st.uvHitY + ToothPliersConfig.UV_DAMAGE_HEIGHT,
                st.uvHitZ + halfSize);
    }

    /**
     * 细菌群 AOE。
     * 返回命中的生物列表，调用方可对其应用额外效果（如火焰附加）。
     */
    public static List<LivingEntity> applyBacteriaAoe(ServerPlayerEntity player, SkillState st,
                                                      LivingEntity primaryTarget,
                                                      float damage) {
        List<LivingEntity> hitList = new ArrayList<>();
        if (!st.uvHasArea) return hitList;

        World world = player.getWorld();
        Box box = getDamageBox(st);
        List<Entity> entities = world.getOtherEntities(player, box);

        for (Entity e : entities) {
            if (!(e instanceof LivingEntity living)) continue;
            if (!living.isAttackable() || living.isSpectator()) continue;
            if (living == primaryTarget) continue;
            living.damage(player.getDamageSources().playerAttack(player), damage);
            hitList.add(living);
        }
        return hitList;
    }
}