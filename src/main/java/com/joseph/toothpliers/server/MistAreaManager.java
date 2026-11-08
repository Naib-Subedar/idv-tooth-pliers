package com.joseph.toothpliers.server;

import com.joseph.toothpliers.ToothPliersMod;
import com.joseph.toothpliers.config.ToothPliersConfig;
import com.joseph.toothpliers.network.ToothPliersNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class MistAreaManager {

    public static class MistInstance {
        public final long id;
        public final UUID owner;
        public final ServerWorld world;
        /** 圆柱几何中心 */
        public final double x, y, z;
        public final long endTick;
        public final Identifier modifierId;
        public Set<UUID> insideEntities = new HashSet<>();

        public MistInstance(long id, UUID owner, ServerWorld world,
                            double x, double y, double z, long endTick) {
            this.id = id;
            this.owner = owner;
            this.world = world;
            this.x = x; this.y = y; this.z = z;
            this.endTick = endTick;
            this.modifierId = Identifier.of(ToothPliersMod.MOD_ID, "mist_slow_" + id);
        }

        public double getMinY() { return y - ToothPliersConfig.MIST_HEIGHT / 2.0; }
        public double getMaxY() { return y + ToothPliersConfig.MIST_HEIGHT / 2.0; }
    }

    private static final List<MistInstance> INSTANCES = new ArrayList<>();
    private static long NEXT_ID = 0;

    public static void addMist(ServerPlayerEntity owner, double x, double y, double z) {
        ServerWorld world = owner.getServerWorld();
        long endTick = world.getTime() + ToothPliersConfig.MIST_DURATION;
        MistInstance inst = new MistInstance(NEXT_ID++, owner.getUuid(), world, x, y, z, endTick);
        INSTANCES.add(inst);
        broadcastSync(Objects.requireNonNull(owner.getServer()), true, inst);
    }

    public static void tick(MinecraftServer server) {
        Iterator<MistInstance> it = INSTANCES.iterator();
        while (it.hasNext()) {
            MistInstance inst = it.next();
            if (inst.world.getTime() >= inst.endTick) {
                clearAllModifiers(inst);
                broadcastSync(server, false, inst);
                it.remove();
            } else {
                tickInstance(inst);
            }
        }
    }

    /** 判断点是否在圆柱体内 */
    private static boolean isInsideCylinder(MistInstance inst, double x, double y, double z) {
        double rSq = ToothPliersConfig.MIST_RADIUS * ToothPliersConfig.MIST_RADIUS;
        double dx = x - inst.x;
        double dz = z - inst.z;
        if (dx * dx + dz * dz > rSq) return false;
        return y >= inst.getMinY() && y <= inst.getMaxY();
    }

    private static void tickInstance(MistInstance inst) {
        double r = ToothPliersConfig.MIST_RADIUS;
        double minY = inst.getMinY();
        double maxY = inst.getMaxY();
        double rSq = r * r;

        Box search = new Box(
                inst.x - r - 5, minY - 5, inst.z - r - 5,
                inst.x + r + 5, maxY + 5, inst.z + r + 5);

        List<LivingEntity> list = inst.world.getEntitiesByClass(LivingEntity.class, search, e -> true);
        Set<UUID> insideThisTick = new HashSet<>();

        for (LivingEntity e : list) {
            if (e instanceof ServerPlayerEntity) continue;

            double dx = e.getX() - inst.x;
            double dz = e.getZ() - inst.z;
            double distXZSq = dx * dx + dz * dz;
            boolean insideXZ = distXZSq <= rSq;
            boolean insideY = e.getY() >= minY && e.getY() <= maxY;
            boolean inside = insideXZ && insideY;

            EntityAttributeInstance attr = e.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);

            if (inside) {
                insideThisTick.add(e.getUuid());
                if (attr != null && attr.getModifier(inst.modifierId) == null) {
                    attr.addTemporaryModifier(new EntityAttributeModifier(
                            inst.modifierId,
                            ToothPliersConfig.MIST_SPEED_MULTIPLIER - 1.0,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
            } else {
                // 只在"上一 tick 在内部"（试图从内出去）时处理
                if (inst.insideEntities.contains(e.getUuid())) {
                    // 只在 XZ 方向推回（Y 方向越界不推回，避免卡方块）
                    if (!insideXZ) {
                        double distXZ = Math.sqrt(distXZSq);
                        if (distXZ > 0.001) {
                            double scale = (r - 0.5) / distXZ;
                            double tx = inst.x + dx * scale;
                            double tz = inst.z + dz * scale;
                            double ty = Math.max(e.getY(), minY);
                            ty = Math.min(ty, maxY - 0.1);

                            Vec3d safe = findSafePosition(inst.world, tx, ty, tz, e);
                            if (safe != null) {
                                e.refreshPositionAndAngles(safe.x, safe.y, safe.z,
                                        e.getYaw(), e.getPitch());
                                e.setVelocity(Vec3d.ZERO);
                                e.velocityModified = true;
                                insideThisTick.add(e.getUuid());
                            } else {
                                e.setVelocity(Vec3d.ZERO);
                                e.velocityModified = true;
                            }
                        }
                    }
                    // Y 方向越界：不推回，保留减速
                    if (attr != null && attr.getModifier(inst.modifierId) == null) {
                        attr.addTemporaryModifier(new EntityAttributeModifier(
                                inst.modifierId,
                                ToothPliersConfig.MIST_SPEED_MULTIPLIER - 1.0,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                    }
                }
            }
        }

        inst.insideEntities = insideThisTick;
    }

    private static Vec3d findSafePosition(ServerWorld world,
                                          double x, double y, double z,
                                          LivingEntity entity) {
        double height = entity.getHeight();

        if (isSafe(world, x, y, z, height)) return new Vec3d(x, y, z);
        for (int dy = 1; dy <= 3; dy++) {
            if (isSafe(world, x, y + dy, z, height)) return new Vec3d(x, y + dy, z);
        }
        for (int dy = 1; dy <= 3; dy++) {
            if (isSafe(world, x, y - dy, z, height)) return new Vec3d(x, y - dy, z);
        }
        return null;
    }

    private static boolean isSafe(ServerWorld world, double x, double y, double z, double height) {
        BlockPos feetPos = BlockPos.ofFloored(x, y, z);
        BlockState atFeet = world.getBlockState(feetPos);
        if (!atFeet.getCollisionShape(world, feetPos).isEmpty()) return false;

        if (height > 1.0) {
            BlockPos headPos = feetPos.up();
            BlockState atHead = world.getBlockState(headPos);
            if (!atHead.getCollisionShape(world, headPos).isEmpty()) return false;
        }

        BlockPos belowPos = feetPos.down();
        BlockState below = world.getBlockState(belowPos);
        return !below.getCollisionShape(world, belowPos).isEmpty();
    }

    private static void clearAllModifiers(MistInstance inst) {
        double r = ToothPliersConfig.MIST_RADIUS;
        Box search = new Box(
                inst.x - r - 5, inst.getMinY() - 5, inst.z - r - 5,
                inst.x + r + 5, inst.getMaxY() + 5, inst.z + r + 5);

        List<LivingEntity> list = inst.world.getEntitiesByClass(LivingEntity.class, search, e -> true);
        for (LivingEntity e : list) {
            EntityAttributeInstance attr = e.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (attr != null && attr.getModifier(inst.modifierId) != null) {
                attr.removeModifier(inst.modifierId);
            }
        }
    }

    private static void broadcastSync(MinecraftServer server, boolean add, MistInstance inst) {
        ToothPliersNetworking.MistSyncPayload payload = new ToothPliersNetworking.MistSyncPayload(
                add, inst.id, inst.owner, inst.x, inst.y, inst.z);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    /** 判断一个点是否在任何活跃迷雾圆柱体内 */
    public static boolean isInsideAnyMist(ServerWorld world, double x, double y, double z) {
        for (MistInstance inst : INSTANCES) {
            if (inst.world != world) continue;
            if (isInsideCylinder(inst, x, y, z)) return true;
        }
        return false;
    }

    /**
     * 精确判定：UV 紫框（AABB）是否与任意迷雾圆柱体相交。
     */
    public static boolean isUvBoxOverlappingAnyMist(ServerWorld world,
                                                    double uvX, double uvY, double uvZ,
                                                    double uvHalfSize, double uvHeight) {
        double uvMinX = uvX - uvHalfSize;
        double uvMaxX = uvX + uvHalfSize;
        double uvMinY = uvY - ToothPliersConfig.UV_DAMAGE_DOWN;
        double uvMaxY = uvY + uvHeight;
        double uvMinZ = uvZ - uvHalfSize;
        double uvMaxZ = uvZ + uvHalfSize;

        double mistRSq = ToothPliersConfig.MIST_RADIUS * ToothPliersConfig.MIST_RADIUS;

        for (MistInstance inst : INSTANCES) {
            if (inst.world != world) continue;

            // XZ 平面：AABB 在 XZ 上最近点与圆心距离
            double closestX = Math.max(uvMinX, Math.min(inst.x, uvMaxX));
            double closestZ = Math.max(uvMinZ, Math.min(inst.z, uvMaxZ));
            double dx = closestX - inst.x;
            double dz = closestZ - inst.z;
            if (dx * dx + dz * dz > mistRSq) continue;

            // Y 轴：AABB 与圆柱 Y 范围是否有重叠
            if (uvMaxY < inst.getMinY() || uvMinY > inst.getMaxY()) continue;

            return true;
        }
        return false;
    }

    /**
     * 迷雾区域 AOE 伤害（圆柱范围内）。
     */
    public static void applyMistAreaDamage(ServerPlayerEntity source,
                                           float damage,
                                           LivingEntity primary) {
        if (INSTANCES.isEmpty()) return;

        boolean anyHit = false;
        double r = ToothPliersConfig.MIST_RADIUS;

        for (MistInstance inst : INSTANCES) {
            if (inst.world != source.getServerWorld()) continue;

            Set<UUID> damaged = new HashSet<>();
            Box search = new Box(
                    inst.x - r, inst.getMinY(), inst.z - r,
                    inst.x + r, inst.getMaxY(), inst.z + r);

            List<LivingEntity> list =
                    inst.world.getEntitiesByClass(LivingEntity.class, search, e -> true);

            for (LivingEntity entity : list) {
                if (entity == source) continue;
                if (entity.isSpectator()) continue;
                if (!entity.isAttackable()) continue;

                if (!isInsideCylinder(inst, entity.getX(), entity.getY(), entity.getZ())) continue;

                if (entity instanceof ServerPlayerEntity && entity != primary) {
                    continue;
                }

                if (!damaged.add(entity.getUuid())) continue;

                entity.timeUntilRegen = 0;
                entity.damage(source.getDamageSources().playerAttack(source), damage);
                anyHit = true;
            }
        }

    }
}