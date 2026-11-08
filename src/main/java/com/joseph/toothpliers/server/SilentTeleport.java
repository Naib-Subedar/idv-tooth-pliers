package com.joseph.toothpliers.server;

import com.joseph.toothpliers.ToothPliersMod;
import com.joseph.toothpliers.component.SkillData;
import com.joseph.toothpliers.config.ToothPliersConfig;
import com.joseph.toothpliers.network.ToothPliersNetworking;
import com.joseph.toothpliers.sound.ToothPliersSounds;
import com.joseph.toothpliers.state.SkillState;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class SilentTeleport {
    private SilentTeleport() {}

    public static void execute(ServerPlayerEntity player, SkillState st) {
        if (!st.uvHasArea) return;

        ItemStack stack = player.getMainHandStack();
        SkillData data = stack.getOrDefault(ToothPliersMod.SKILL_DATA, SkillData.DEFAULT);

        ServerWorld world = player.getServerWorld();

        double halfSize = ToothPliersConfig.UV_AREA_WIDTH / 2.0;
        double startX = st.uvHitX + st.uvDirX * halfSize;
        double startZ = st.uvHitZ + st.uvDirZ * halfSize;
        double startY = st.uvHitY;

        Vec3d safe = findSafeSpot(world, startX, startY, startZ, st.uvDirX, st.uvDirZ);

        player.teleport(world, safe.x, safe.y, safe.z, player.getYaw(), player.getPitch());
        player.setVelocity(Vec3d.ZERO);
        player.velocityModified = true;
        player.fallDistance = 0;

        ToothPliersNetworking.broadcastSoundToNearby(player, ToothPliersSounds.ID_SILENT_TP);

        int cost = data.teleportUsed()
                ? ToothPliersConfig.DURABILITY_TELEPORT_2
                : ToothPliersConfig.DURABILITY_TELEPORT_1;
        stack.damage(cost, player, EquipmentSlot.MAINHAND);

        // 后摇：第一段 0.3s，第二段 1s
        st.recoveryTicks = data.teleportUsed()
                ? ToothPliersConfig.TELEPORT_RECOVERY_SECOND
                : ToothPliersConfig.TELEPORT_RECOVERY;
        // ★ 标记为传送后摇（允许开关 UV）
        st.recoveryFromTeleport = true;

        long now = world.getTime();
        if (data.teleportUsed()) {
            stack.set(ToothPliersMod.SKILL_DATA, data.withTeleport(0L, false));
        } else {
            long endTick = now + ToothPliersConfig.TELEPORT_SECOND_WINDOW;
            stack.set(ToothPliersMod.SKILL_DATA, data.withTeleport(endTick, true));
        }
    }

    private static Vec3d findSafeSpot(ServerWorld world,
                                      double startX, double startY, double startZ,
                                      double dirX, double dirZ) {
        for (int offset = 0; offset <= ToothPliersConfig.TELEPORT_MAX_SEARCH; offset++) {
            double x = startX + dirX * offset;
            double z = startZ + dirZ * offset;

            if (isSafe(world, x, startY, z)) return new Vec3d(x, startY, z);
            for (int dy = 1; dy <= 3; dy++) {
                if (isSafe(world, x, startY + dy, z)) return new Vec3d(x, startY + dy, z);
            }
            for (int dy = 1; dy <= 3; dy++) {
                if (isSafe(world, x, startY - dy, z)) return new Vec3d(x, startY - dy, z);
            }
        }
        return new Vec3d(startX, startY, startZ);
    }

    private static boolean isSafe(ServerWorld world, double x, double y, double z) {
        BlockPos pos = BlockPos.ofFloored(x, y, z);

        BlockState atFeet = world.getBlockState(pos);
        if (!atFeet.getCollisionShape(world, pos).isEmpty()) return false;

        BlockPos headPos = pos.up();
        BlockState atHead = world.getBlockState(headPos);
        if (!atHead.getCollisionShape(world, headPos).isEmpty()) return false;

        BlockPos belowPos = pos.down();
        BlockState below = world.getBlockState(belowPos);
        return !below.getCollisionShape(world, belowPos).isEmpty();
    }
}