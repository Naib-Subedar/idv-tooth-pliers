package com.joseph.toothpliers.client;

import com.joseph.toothpliers.ToothPliersMod;
import com.joseph.toothpliers.component.SkillData;
import com.joseph.toothpliers.config.ToothPliersConfig;
import com.joseph.toothpliers.network.ToothPliersNetworking;
import com.joseph.toothpliers.sound.ToothPliersSounds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;

import java.util.Random;

public class ToothPliersClientTick {

    private static boolean wasHolding = false;
    private static int pendingAttackTicks = -1;

    /** 等待判定回执的最大 tick（前摇 + 挥手 + 余量） */
    private static final int PENDING_ATTACK_TIMEOUT =
            ToothPliersConfig.NORMAL_ATTACK_WINDUP
                    + ToothPliersConfig.SWING_ANIMATION_TICKS
                    + 5;

    private static final int MAX_PITCH_LOCK_TICKS = 30;

    private static final int MIST_LONG_PRESS_TICKS = 8;
    private static final int MIST_PREVIEW_COOLDOWN_TICKS = 10;

    private static final Random RANDOM = new Random();
    private static int lastFlashTicks = 0;

    public static SkillData getHeldData() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return SkillData.DEFAULT;
        ItemStack stack = mc.player.getMainHandStack();
        if (!stack.isOf(ToothPliersMod.TOOTH_PLIERS)) return SkillData.DEFAULT;
        return stack.getOrDefault(ToothPliersMod.SKILL_DATA, SkillData.DEFAULT);
    }

    public static void register() {
        // ==================== S2C：判定 → 挥手 ====================
        ClientPlayNetworking.registerGlobalReceiver(
                ToothPliersNetworking.AttackPerformedPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    MinecraftClient mc = ctx.client();
                    if (mc.player == null) return;

                    // ★ 不清除 pendingAttackTicks，让挥手动画期间继续保持阻塞
                    mc.player.swingHand(Hand.MAIN_HAND);

                    if (ToothPliersClientState.uvLockPitchActive
                            && getHeldData().uvEnabled()) {
                        ToothPliersClientState.uvLockPitchTicks =
                                Math.min(ToothPliersClientState.uvLockPitchTicks, 10);
                    }
                }));

        // ==================== S2C：UV 位置 ====================
        ClientPlayNetworking.registerGlobalReceiver(
                ToothPliersNetworking.UvStatePayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    ToothPliersClientState.uvHasArea = payload.hasArea();
                    ToothPliersClientState.uvHitX = payload.centerX();
                    ToothPliersClientState.uvHitY = payload.centerY();
                    ToothPliersClientState.uvHitZ = payload.centerZ();
                    ToothPliersClientState.uvDirX = payload.dirX();
                    ToothPliersClientState.uvDirZ = payload.dirZ();
                }));

        // ==================== S2C：判定闪光 ====================
        ClientPlayNetworking.registerGlobalReceiver(
                ToothPliersNetworking.UvFlashPayload.ID,
                (payload, ctx) -> ctx.client().execute(() ->
                        ToothPliersClientState.uvAttackFlashTicks =
                                ToothPliersConfig.UV_ATTACK_FLASH_TICKS));

        // ==================== S2C：透镜档位 ====================
        ClientPlayNetworking.registerGlobalReceiver(
                ToothPliersNetworking.LensStatePayload.ID,
                (payload, ctx) -> ctx.client().execute(() ->
                        ToothPliersClientState.lensLevel = payload.lensLevel()));

        // ==================== S2C：后摇状态 ====================
        ClientPlayNetworking.registerGlobalReceiver(
                ToothPliersNetworking.RecoveryStatePayload.ID,
                (payload, ctx) -> ctx.client().execute(() ->
                        ToothPliersClientState.inRecovery = payload.inRecovery()));

        // ==================== S2C：迷雾同步 ====================
        ClientPlayNetworking.registerGlobalReceiver(
                ToothPliersNetworking.MistSyncPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    if (payload.add()) {
                        ToothPliersClientState.addMist(payload.id(),
                                payload.owner(),
                                payload.x(), payload.y(), payload.z());
                    } else {
                        ToothPliersClientState.removeMist(payload.id());
                    }
                }));

        // ==================== S2C：UV 广播 ====================
        ClientPlayNetworking.registerGlobalReceiver(
                ToothPliersNetworking.UvBroadcastPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    ToothPliersClientState.BROADCAST_UVS.add(
                            new ToothPliersClientState.BroadcastUv(
                                    payload.centerX(), payload.centerY(), payload.centerZ(),
                                    payload.dirX(), payload.dirZ(),
                                    payload.durationTicks()));
                }));

        // ==================== S2C：播放音效 ====================
        ClientPlayNetworking.registerGlobalReceiver(
                ToothPliersNetworking.PlaySoundPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    SoundEvent sound = ToothPliersSounds.fromId(payload.soundId());
                    if (sound != null) {
                        ToothPliersSoundPlayer.play(sound);
                    }
                }));

        // ==================== 每 Tick ====================
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.world == null) return;

            // ---- 攻击锁视角状态维护 ----
            if (ToothPliersClientState.uvLockPitchActive) {
                if (!getHeldData().uvEnabled()) {
                    ToothPliersClientState.uvLockPitchActive = false;
                    ToothPliersClientState.uvLockPitchTicks = 0;
                } else {
                    ToothPliersClientState.uvLockPitchTicks--;
                    if (ToothPliersClientState.uvLockPitchTicks <= 0) {
                        ToothPliersClientState.uvLockPitchActive = false;
                        ToothPliersClientState.uvLockPitchTicks = 0;
                    }
                }
            }

            // ---- 闪光倒计时 + 粒子触发 ----
            int flash = ToothPliersClientState.uvAttackFlashTicks;
            if (flash > 0) {
                ToothPliersClientState.uvAttackFlashTicks--;
            }
            if (flash > 0 && lastFlashTicks == 0
                    && getHeldData().uvEnabled()
                    && ToothPliersClientState.uvHasArea) {
                spawnUvBurst(mc);
            }
            lastFlashTicks = flash;

            // ---- 广播紫框倒计时 ----
            for (ToothPliersClientState.BroadcastUv uv
                    : ToothPliersClientState.BROADCAST_UVS) {
                uv.ticksLeft--;
            }
            ToothPliersClientState.BROADCAST_UVS.removeIf(uv -> uv.ticksLeft <= 0);

            boolean hasPliers = mc.player.getMainHandStack().isOf(ToothPliersMod.TOOTH_PLIERS);
            if (!hasPliers) {
                wasHolding = false;
                pendingAttackTicks = -1;
                ToothPliersClientState.mistPreviewActive = false;
                ToothPliersClientState.mistPressTicks = 0;
                ToothPliersClientState.mistKeyDown = false;
                ToothPliersClientState.mistPreviewCooldownTicks = 0;
                ToothPliersClientState.skipNextAttackRelease = false;
                ToothPliersClientState.uvLockPitchActive = false;
                return;
            }

            // ---- R 键 ----
            if (mc.currentScreen == null) {
                tickMistKey(mc);
            }

            // ---- 攻击等待回执超时处理 ----
            if (pendingAttackTicks >= 0) {
                pendingAttackTicks++;

                // 挥手动画已完成 → 清除阻塞
                boolean swingDone = !mc.player.handSwinging
                        || mc.player.handSwingTicks <= 0;

                if (pendingAttackTicks > PENDING_ATTACK_TIMEOUT
                        || (swingDone && pendingAttackTicks > 2)) {
                    pendingAttackTicks = -1;
                    ToothPliersClientState.uvLockPitchActive = false;
                    ToothPliersClientState.uvLockPitchTicks = 0;
                }
            }

            boolean holding = mc.options.attackKey.isPressed();

            if (holding && !wasHolding) {
                wasHolding = true;

                if (ToothPliersClientState.mistPreviewActive) {
                    sendMistRelease(mc, ToothPliersClientState.mistPreviewDistance);
                    ToothPliersClientState.mistPreviewActive = false;

                    ToothPliersClientState.mistPreviewCooldownTicks = MIST_PREVIEW_COOLDOWN_TICKS;
                    ToothPliersClientState.mistKeyDown = true;
                    ToothPliersClientState.mistSkipReleaseOnUp = true;
                    ToothPliersClientState.mistPressTicks = 0;
                    ToothPliersClientState.skipNextAttackRelease = true;
                    return;
                }

                // ★ 正在挥手 → 忽略本次按下
                if (mc.player.handSwinging && mc.player.handSwingTicks < 6) {
                    return;
                }

                if (pendingAttackTicks >= 0) return;

                if (getHeldData().uvEnabled()) {
                    ToothPliersClientState.uvLockPitchActive = true;
                    ToothPliersClientState.uvLockedPitch = mc.player.getPitch();
                    ToothPliersClientState.uvLockPitchTicks = MAX_PITCH_LOCK_TICKS;
                }

            } else if (!holding && wasHolding) {
                wasHolding = false;

                if (ToothPliersClientState.skipNextAttackRelease) {
                    ToothPliersClientState.skipNextAttackRelease = false;
                    return;
                }

                if (ToothPliersClientState.mistPreviewActive) return;

                // ★ 正在挥手 → 忽略本次松开
                if (mc.player.handSwinging && mc.player.handSwingTicks < 6) {
                    return;
                }

                if (pendingAttackTicks >= 0) return;

                ClientPlayNetworking.send(new ToothPliersNetworking.AttackPayload(false));
                pendingAttackTicks = 0;
            }
        });
    }

    // ==================== R 键处理 ====================
    private static void tickMistKey(MinecraftClient mc) {
        boolean keyDown = ToothPliersKeys.KEY_MIST.isPressed();

        if (ToothPliersClientState.mistPreviewCooldownTicks > 0) {
            ToothPliersClientState.mistPreviewCooldownTicks--;
            ToothPliersClientState.mistKeyDown = true;
            ToothPliersClientState.mistPressTicks = 0;
            return;
        }

        if (ToothPliersClientState.mistPreviewActive) {
            if (keyDown && !ToothPliersClientState.mistKeyDown) {
                ToothPliersClientState.mistPreviewActive = false;
                ToothPliersClientState.mistPressTicks = 0;
                ToothPliersClientState.mistSkipReleaseOnUp = true;
                ToothPliersClientState.mistPreviewCooldownTicks = MIST_PREVIEW_COOLDOWN_TICKS;
                ToothPliersClientState.mistKeyDown = true;
                return;
            }
            ToothPliersClientState.mistKeyDown = keyDown;
            return;
        }

        if (keyDown && !ToothPliersClientState.mistKeyDown) {
            ToothPliersClientState.mistKeyDown = true;
            ToothPliersClientState.mistPressTicks = 0;
            ToothPliersClientState.mistSkipReleaseOnUp = false;

        } else if (keyDown) {
            if (ToothPliersClientState.mistSkipReleaseOnUp) {
                return;
            }
            ToothPliersClientState.mistPressTicks++;

            if (ToothPliersClientState.mistPressTicks >= MIST_LONG_PRESS_TICKS) {
                ToothPliersClientState.mistPreviewActive = true;
                ToothPliersClientState.mistPreviewDistance = ToothPliersConfig.MIST_MAX_DISTANCE;

                ToothPliersSoundPlayer.play(ToothPliersSounds.PREVIEW_MODE);

                boolean flyingCreative = false;
                if (mc.player != null) {
                    flyingCreative = mc.player.isCreative()
                            && mc.player.getAbilities().flying;
                }
                if (!flyingCreative) {
                    if (mc.player != null) {
                        mc.player.setPitch(0);
                    }
                }
            }

        } else if (!keyDown && ToothPliersClientState.mistKeyDown) {
            ToothPliersClientState.mistKeyDown = false;

            if (ToothPliersClientState.mistSkipReleaseOnUp) {
                ToothPliersClientState.mistSkipReleaseOnUp = false;
                ToothPliersClientState.mistPressTicks = 0;
                return;
            }

            if (ToothPliersClientState.mistPressTicks < MIST_LONG_PRESS_TICKS) {
                sendMistRelease(mc, ToothPliersConfig.MIST_MAX_DISTANCE);
            }

            ToothPliersClientState.mistPressTicks = 0;
        }
    }

    private static void sendMistRelease(MinecraftClient mc, double distance) {
        if (mc.player == null) return;

        var dir = mc.player.getRotationVec(1.0f);
        double tx = mc.player.getX() + dir.x * distance;
        double ty = mc.player.getY() + dir.y * distance;
        double tz = mc.player.getZ() + dir.z * distance;

        ty = ToothPliersConfig.clampMistCenterY(ty);

        ClientPlayNetworking.send(new ToothPliersNetworking.MistAreaPayload(tx, ty, tz));
    }

    private static void spawnUvBurst(MinecraftClient mc) {
        double cx = ToothPliersClientState.uvHitX;
        double cy = ToothPliersClientState.uvHitY;
        double cz = ToothPliersClientState.uvHitZ;

        int count = ToothPliersConfig.UV_PARTICLE_BURST_COUNT;
        for (int i = 0; i < count; i++) {
            double px = cx + (RANDOM.nextDouble() - 0.5) * 4.0;
            double py = cy + RANDOM.nextDouble() * 3.5;
            double pz = cz + (RANDOM.nextDouble() - 0.5) * 4.0;
            if (mc.world != null) {
                mc.world.addParticle(ParticleTypes.PORTAL,
                        px, py, pz,
                        (RANDOM.nextDouble() - 0.5) * 0.3,
                        0.05 + RANDOM.nextDouble() * 0.1,
                        (RANDOM.nextDouble() - 0.5) * 0.3);
            }
        }
    }
}