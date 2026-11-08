package com.joseph.toothpliers.server;

import com.joseph.toothpliers.ToothPliersMod;
import com.joseph.toothpliers.component.SkillData;
import com.joseph.toothpliers.config.ToothPliersConfig;
import com.joseph.toothpliers.item.ToothPliersItem;
import com.joseph.toothpliers.network.ToothPliersNetworking;
import com.joseph.toothpliers.sound.ToothPliersSounds;
import com.joseph.toothpliers.state.SkillState;
import com.joseph.toothpliers.state.ToothPliersState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ToothPliersServerTick {

    private static final Map<UUID, Boolean> PREV_RECOVERY = new HashMap<>();

    private static final int ATTACK_COOLDOWN_TICKS =
            ToothPliersConfig.SWING_ANIMATION_TICKS;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                tickPlayer(player);
            }
            MistAreaManager.tick(server);
        });
    }

    private static SkillData getData(ItemStack stack) {
        return stack.getOrDefault(ToothPliersMod.SKILL_DATA, SkillData.DEFAULT);
    }

    private static void setData(ItemStack stack, SkillData data) {
        stack.set(ToothPliersMod.SKILL_DATA, data);
    }

    private static int getEnchantLevel(ServerPlayerEntity player, ItemStack stack,
                                       RegistryKey<Enchantment> key) {
        RegistryEntry<Enchantment> entry = player.getServerWorld()
                .getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT)
                .getEntry(key)
                .orElse(null);
        if (entry == null) return 0;
        return EnchantmentHelper.getLevel(entry, stack);
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        SkillState st = ToothPliersState.get(player.getUuid());
        ItemStack main = player.getMainHandStack();

        if (main.isOf(ToothPliersMod.TOOTH_PLIERS)) {
            int target = ToothPliersItem.computeMaxDamage(main, player.getServerWorld());
            Integer current = main.get(DataComponentTypes.MAX_DAMAGE);
            if (current == null || current != target) {
                main.set(DataComponentTypes.MAX_DAMAGE, target);
            }
        }

        if (st.windupActive) {
            st.windupTicks--;
            if (st.windupTicks <= 0) {
                st.windupActive = false;
                performAttack(player, st);
            }
        }

        if (main.isOf(ToothPliersMod.TOOTH_PLIERS)) {
            SkillData data = getData(main);
            if (data.uvEnabled()) {
                UltravioletRadiation.updatePosition(player, st);
            } else {
                st.uvHasArea = false;
            }

            long now = player.getWorld().getTime();
            if (data.teleportUsed() && data.teleportWindowEndTick() <= now) {
                setData(main, data.withTeleport(0L, false));
            }
        }

        st.tick();

        if (main.isOf(ToothPliersMod.TOOTH_PLIERS)) {
            ServerPlayNetworking.send(player, new ToothPliersNetworking.UvStatePayload(
                    st.uvHasArea,
                    st.uvHitX, st.uvHitY, st.uvHitZ,
                    st.uvDirX, st.uvDirZ));
        }

        boolean currRec = st.isRecovery();
        Boolean prevRec = PREV_RECOVERY.get(player.getUuid());
        if (prevRec == null || prevRec != currRec) {
            PREV_RECOVERY.put(player.getUuid(), currRec);
            ServerPlayNetworking.send(player,
                    new ToothPliersNetworking.RecoveryStatePayload(currRec));
        }
    }

    public static void startAttack(ServerPlayerEntity player) {
        SkillState st = ToothPliersState.get(player.getUuid());
        if (st.isRecovery() || st.windupActive) return;
        ItemStack stack = player.getMainHandStack();
        if (!stack.isOf(ToothPliersMod.TOOTH_PLIERS)) return;

        SkillData data = getData(stack);
        long now = player.getWorld().getTime();
        if (data.isOnCooldown(now)) return;

        st.windupActive = true;
        st.windupTicks = ToothPliersConfig.NORMAL_ATTACK_WINDUP;
    }

    private static void performAttack(ServerPlayerEntity player, SkillState st) {
        ItemStack stack = player.getMainHandStack();
        if (!stack.isOf(ToothPliersMod.TOOTH_PLIERS)) return;

        ServerPlayNetworking.send(player, new ToothPliersNetworking.AttackPerformedPayload());

        SkillData data = getData(stack);

        double range = ToothPliersConfig.NORMAL_ATTACK_RANGE;
        float baseDamage = ToothPliersConfig.NORMAL_ATTACK_DAMAGE;

        LivingEntity primary = findPrimaryTarget(player, range);
        EndCrystalEntity crystal = findEndCrystalTarget(player, range);

        if (crystal != null) {
            crystal.damage(player.getDamageSources().playerAttack(player), 1.0f);
            stack.damage(ToothPliersConfig.DURABILITY_ATTACK_HIT,
                    player, EquipmentSlot.MAINHAND);
        } else if (primary != null) {
            float damage = applySharpnessNormal(player, stack, baseDamage);
            primary.damage(player.getDamageSources().playerAttack(player), damage);
            applyKnockbackAndFire(player, stack, primary);

            int sweeping = getEnchantLevel(player, stack, Enchantments.SWEEPING_EDGE);
            if (sweeping > 0) {
                for (LivingEntity other : findFanTargets(player, range)) {
                    if (other == primary) continue;
                    other.damage(player.getDamageSources().playerAttack(player),
                            applySharpnessNormal(player, stack, baseDamage));
                }
            }

            stack.damage(ToothPliersConfig.DURABILITY_ATTACK_HIT, player, EquipmentSlot.MAINHAND);
        } else {
            st.recoveryTicks = ToothPliersConfig.NORMAL_ATTACK_RECOVERY;
            st.recoveryFromTeleport = false;
        }

        // ==================== 紫外辐射攻击 ====================
        if (data.uvEnabled()) {
            UltravioletRadiation.updatePosition(player, st);

            float uvDamage = applySharpnessUV(player, stack, ToothPliersConfig.UV_BACTERIA_DAMAGE);

            // 细菌群 AOE，返回命中列表
            List<LivingEntity> bacteriaHits = UltravioletRadiation.applyBacteriaAoe(
                    player, st, primary, uvDamage);
            boolean bacteriaHit = !bacteriaHits.isEmpty();

            // 火焰附加对 UV 细菌群命中的生物生效
            int fire = getEnchantLevel(player, stack, Enchantments.FIRE_ASPECT);
            if (fire > 0) {
                for (LivingEntity hitEntity : bacteriaHits) {
                    hitEntity.setOnFireFor(fire * 4);
                }
            }

            // ★ 判断 UV 命中的生物本身是否在迷雾内
            //   主目标 或 细菌群命中的生物，只要有任意一个在迷雾内 → 触发区域伤害
            boolean hitInsideMist = false;
            ServerWorld sw = player.getServerWorld();

            if (primary != null
                    && MistAreaManager.isInsideAnyMist(
                    sw, primary.getX(), primary.getY(), primary.getZ())) {
                hitInsideMist = true;
            }

            if (!hitInsideMist) {
                for (LivingEntity hit : bacteriaHits) {
                    if (MistAreaManager.isInsideAnyMist(
                            sw, hit.getX(), hit.getY(), hit.getZ())) {
                        hitInsideMist = true;
                        break;
                    }
                }
            }

            // 区域伤害：UV 命中的生物位于迷雾内 → 触发
            if (hitInsideMist) {
                MistAreaManager.applyMistAreaDamage(player, uvDamage, primary);
            }

            // 音效
            int soundId = bacteriaHit
                    ? ToothPliersSounds.ID_HIT
                    : ToothPliersSounds.ID_ATTACK;
            ServerPlayNetworking.send(player,
                    new ToothPliersNetworking.PlaySoundPayload(soundId));

            st.uvAttackFlashTicks = ToothPliersConfig.UV_ATTACK_FLASH_TICKS;
            ServerPlayNetworking.send(player, new ToothPliersNetworking.UvFlashPayload());
            broadcastUvToNearby(player, st);
        }

        long now = player.getWorld().getTime();
        setData(stack, data.withCooldown(now + ATTACK_COOLDOWN_TICKS));
    }

    // ==================== 目标查找 ====================

    private static LivingEntity findPrimaryTarget(ServerPlayerEntity player, double range) {
        World world = player.getWorld();
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        double cosThreshold = Math.cos(Math.toRadians(ToothPliersConfig.ATTACK_FAN_ANGLE));

        LivingEntity best = null;
        double bestAngle = Double.MAX_VALUE;

        Box search = player.getBoundingBox().expand(range);
        for (Entity e : world.getOtherEntities(player, search)) {
            if (!(e instanceof LivingEntity living)) continue;
            if (!living.isAttackable() || living.isSpectator()) continue;

            Vec3d center = living.getBoundingBox().getCenter();
            Vec3d to = center.subtract(eye);
            double dist = to.length();
            if (dist > range || dist < 0.0001) continue;

            double dot = to.normalize().dotProduct(look);
            if (dot < cosThreshold) continue;

            double angle = Math.acos(Math.min(1.0, dot));
            if (angle < bestAngle) {
                bestAngle = angle;
                best = living;
            }
        }
        return best;
    }

    private static EndCrystalEntity findEndCrystalTarget(ServerPlayerEntity player, double range) {
        World world = player.getWorld();
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        double cosThreshold = Math.cos(Math.toRadians(ToothPliersConfig.ATTACK_FAN_ANGLE));

        EndCrystalEntity best = null;
        double bestAngle = Double.MAX_VALUE;

        Box search = player.getBoundingBox().expand(range);
        for (Entity e : world.getOtherEntities(player, search)) {
            if (!(e instanceof EndCrystalEntity crystal)) continue;
            if (crystal.isRemoved()) continue;

            Vec3d center = crystal.getBoundingBox().getCenter();
            Vec3d to = center.subtract(eye);
            double dist = to.length();
            if (dist > range || dist < 0.0001) continue;

            double dot = to.normalize().dotProduct(look);
            if (dot < cosThreshold) continue;

            double angle = Math.acos(Math.min(1.0, dot));
            if (angle < bestAngle) {
                bestAngle = angle;
                best = crystal;
            }
        }
        return best;
    }

    private static List<LivingEntity> findFanTargets(ServerPlayerEntity player, double range) {
        World world = player.getWorld();
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        double cosThreshold = Math.cos(Math.toRadians(ToothPliersConfig.ATTACK_FAN_ANGLE));

        List<LivingEntity> out = new ArrayList<>();
        Box search = player.getBoundingBox().expand(range);
        for (Entity e : world.getOtherEntities(player, search)) {
            if (!(e instanceof LivingEntity living)) continue;
            if (!living.isAttackable() || living.isSpectator()) continue;

            Vec3d to = living.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist > range || dist < 0.0001) continue;

            double dot = to.normalize().dotProduct(look);
            if (dot < cosThreshold) continue;
            out.add(living);
        }
        return out;
    }

    // ==================== 附魔与伤害 ====================

    private static float applySharpnessNormal(ServerPlayerEntity player, ItemStack stack, float base) {
        int lvl = Math.min(5, getEnchantLevel(player, stack, Enchantments.SHARPNESS));
        return base * (1.0f + ToothPliersConfig.SHARPNESS_BONUS_NORMAL[lvl]);
    }

    private static float applySharpnessUV(ServerPlayerEntity player, ItemStack stack, float base) {
        int lvl = Math.min(5, getEnchantLevel(player, stack, Enchantments.SHARPNESS));
        return base * (1.0f + ToothPliersConfig.SHARPNESS_BONUS_UV[lvl]);
    }

    private static void applyKnockbackAndFire(ServerPlayerEntity player, ItemStack stack,
                                              LivingEntity target) {
        int fire = getEnchantLevel(player, stack, Enchantments.FIRE_ASPECT);
        if (fire > 0) target.setOnFireFor(fire * 4);

        int knockback = getEnchantLevel(player, stack, Enchantments.KNOCKBACK);
        if (knockback > 0) {
            Vec3d dir = player.getRotationVec(1.0f).multiply(1, 0, 1).normalize();
            target.addVelocity(dir.x * knockback * 0.5, 0.1, dir.z * knockback * 0.5);
            target.velocityModified = true;
        }
    }

    // ==================== UV 广播 ====================

    private static void broadcastUvToNearby(ServerPlayerEntity source, SkillState st) {
        if (!st.uvHasArea) return;

        ToothPliersNetworking.UvBroadcastPayload payload =
                new ToothPliersNetworking.UvBroadcastPayload(
                        st.uvHitX, st.uvHitY, st.uvHitZ,
                        st.uvDirX, st.uvDirZ,
                        ToothPliersConfig.UV_BROADCAST_DURATION);

        double range = ToothPliersConfig.UV_BROADCAST_RANGE;
        double rSq = range * range;

        for (ServerPlayerEntity other : source.getServer().getPlayerManager().getPlayerList()) {
            if (other == source) continue;
            if (other.getServerWorld() != source.getServerWorld()) continue;

            double dx = other.getX() - source.getX();
            double dy = other.getY() - source.getY();
            double dz = other.getZ() - source.getZ();
            if (dx * dx + dy * dy + dz * dz <= rSq) {
                ServerPlayNetworking.send(other, payload);
            }
        }
    }
}