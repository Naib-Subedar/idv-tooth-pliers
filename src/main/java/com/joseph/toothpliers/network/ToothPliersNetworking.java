package com.joseph.toothpliers.network;

import com.joseph.toothpliers.ToothPliersMod;
import com.joseph.toothpliers.component.SkillData;
import com.joseph.toothpliers.config.ToothPliersConfig;
import com.joseph.toothpliers.server.MistAreaManager;
import com.joseph.toothpliers.server.SilentTeleport;
import com.joseph.toothpliers.sound.ToothPliersSounds;
import com.joseph.toothpliers.state.SkillState;
import com.joseph.toothpliers.state.ToothPliersState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.UUID;

public class ToothPliersNetworking {

    // ======================= C2S 包 =======================

    public record ToggleUvPayload() implements CustomPayload {
        public static final Id<ToggleUvPayload> ID =
                new Id<>(Identifier.of(ToothPliersMod.MOD_ID, "toggle_uv"));
        public static final PacketCodec<RegistryByteBuf, ToggleUvPayload> CODEC =
                PacketCodec.unit(new ToggleUvPayload());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SetLensPayload(int level) implements CustomPayload {
        public static final Id<SetLensPayload> ID =
                new Id<>(Identifier.of(ToothPliersMod.MOD_ID, "set_lens"));
        public static final PacketCodec<RegistryByteBuf, SetLensPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.VAR_INT, SetLensPayload::level, SetLensPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SilentTeleportPayload() implements CustomPayload {
        public static final Id<SilentTeleportPayload> ID =
                new Id<>(Identifier.of(ToothPliersMod.MOD_ID, "silent_tp"));
        public static final PacketCodec<RegistryByteBuf, SilentTeleportPayload> CODEC =
                PacketCodec.unit(new SilentTeleportPayload());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record MistAreaPayload(double x, double y, double z) implements CustomPayload {
        public static final Id<MistAreaPayload> ID =
                new Id<>(Identifier.of(ToothPliersMod.MOD_ID, "mist_area"));
        public static final PacketCodec<RegistryByteBuf, MistAreaPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.DOUBLE, MistAreaPayload::x,
                        PacketCodecs.DOUBLE, MistAreaPayload::y,
                        PacketCodecs.DOUBLE, MistAreaPayload::z,
                        MistAreaPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record AttackPayload(boolean charged) implements CustomPayload {
        public static final Id<AttackPayload> ID =
                new Id<>(Identifier.of(ToothPliersMod.MOD_ID, "attack"));
        public static final PacketCodec<RegistryByteBuf, AttackPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.BOOL, AttackPayload::charged, AttackPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ======================= S2C 包 =======================

    public record AttackPerformedPayload() implements CustomPayload {
        public static final Id<AttackPerformedPayload> ID =
                new Id<>(Identifier.of(ToothPliersMod.MOD_ID, "attack_performed"));
        public static final PacketCodec<RegistryByteBuf, AttackPerformedPayload> CODEC =
                PacketCodec.unit(new AttackPerformedPayload());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record UvStatePayload(
            boolean hasArea,
            double centerX, double centerY, double centerZ,
            double dirX, double dirZ)
            implements CustomPayload {
        public static final Id<UvStatePayload> ID =
                new Id<>(Identifier.of(ToothPliersMod.MOD_ID, "uv_state"));

        public static final PacketCodec<RegistryByteBuf, UvStatePayload> CODEC = new PacketCodec<>() {
            @Override
            public UvStatePayload decode(RegistryByteBuf buf) {
                return new UvStatePayload(
                        buf.readBoolean(),
                        buf.readDouble(), buf.readDouble(), buf.readDouble(),
                        buf.readDouble(), buf.readDouble());
            }

            @Override
            public void encode(RegistryByteBuf buf, UvStatePayload v) {
                buf.writeBoolean(v.hasArea());
                buf.writeDouble(v.centerX());
                buf.writeDouble(v.centerY());
                buf.writeDouble(v.centerZ());
                buf.writeDouble(v.dirX());
                buf.writeDouble(v.dirZ());
            }
        };

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record UvFlashPayload() implements CustomPayload {
        public static final Id<UvFlashPayload> ID =
                new Id<>(Identifier.of(ToothPliersMod.MOD_ID, "uv_flash"));
        public static final PacketCodec<RegistryByteBuf, UvFlashPayload> CODEC =
                PacketCodec.unit(new UvFlashPayload());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record LensStatePayload(int lensLevel) implements CustomPayload {
        public static final Id<LensStatePayload> ID =
                new Id<>(Identifier.of(ToothPliersMod.MOD_ID, "lens_state"));
        public static final PacketCodec<RegistryByteBuf, LensStatePayload> CODEC =
                PacketCodec.tuple(PacketCodecs.VAR_INT, LensStatePayload::lensLevel,
                        LensStatePayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record RecoveryStatePayload(boolean inRecovery) implements CustomPayload {
        public static final Id<RecoveryStatePayload> ID =
                new Id<>(Identifier.of(ToothPliersMod.MOD_ID, "recovery_state"));
        public static final PacketCodec<RegistryByteBuf, RecoveryStatePayload> CODEC =
                PacketCodec.tuple(PacketCodecs.BOOL, RecoveryStatePayload::inRecovery,
                        RecoveryStatePayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record MistSyncPayload(boolean add, long id, UUID owner,
                                  double x, double y, double z)
            implements CustomPayload {
        public static final Id<MistSyncPayload> ID =
                new Id<>(Identifier.of(ToothPliersMod.MOD_ID, "mist_sync"));
        public static final PacketCodec<RegistryByteBuf, MistSyncPayload> CODEC = new PacketCodec<>() {
            @Override
            public MistSyncPayload decode(RegistryByteBuf buf) {
                return new MistSyncPayload(
                        buf.readBoolean(),
                        buf.readLong(),
                        buf.readUuid(),
                        buf.readDouble(), buf.readDouble(), buf.readDouble());
            }
            @Override
            public void encode(RegistryByteBuf buf, MistSyncPayload v) {
                buf.writeBoolean(v.add());
                buf.writeLong(v.id());
                buf.writeUuid(v.owner());
                buf.writeDouble(v.x());
                buf.writeDouble(v.y());
                buf.writeDouble(v.z());
            }
        };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record UvBroadcastPayload(
            double centerX, double centerY, double centerZ,
            double dirX, double dirZ,
            int durationTicks)
            implements CustomPayload {
        public static final Id<UvBroadcastPayload> ID =
                new Id<>(Identifier.of(ToothPliersMod.MOD_ID, "uv_broadcast"));

        public static final PacketCodec<RegistryByteBuf, UvBroadcastPayload> CODEC = new PacketCodec<>() {
            @Override
            public UvBroadcastPayload decode(RegistryByteBuf buf) {
                return new UvBroadcastPayload(
                        buf.readDouble(), buf.readDouble(), buf.readDouble(),
                        buf.readDouble(), buf.readDouble(),
                        buf.readVarInt());
            }
            @Override
            public void encode(RegistryByteBuf buf, UvBroadcastPayload v) {
                buf.writeDouble(v.centerX());
                buf.writeDouble(v.centerY());
                buf.writeDouble(v.centerZ());
                buf.writeDouble(v.dirX());
                buf.writeDouble(v.dirZ());
                buf.writeVarInt(v.durationTicks());
            }
        };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** 播放音效（服务端 → 客户端） */
    public record PlaySoundPayload(int soundId) implements CustomPayload {
        public static final Id<PlaySoundPayload> ID =
                new Id<>(Identifier.of(ToothPliersMod.MOD_ID, "play_sound"));
        public static final PacketCodec<RegistryByteBuf, PlaySoundPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.VAR_INT, PlaySoundPayload::soundId,
                        PlaySoundPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ======================= 辅助 =======================

    public static void broadcastSoundToNearby(ServerPlayerEntity source, int soundId) {
        PlaySoundPayload payload = new PlaySoundPayload(soundId);

        double range = ToothPliersConfig.SOUND_BROADCAST_RANGE;
        double rSq = range * range;

        for (ServerPlayerEntity p : Objects.requireNonNull(source.getServer()).getPlayerManager().getPlayerList()) {
            if (p.getServerWorld() != source.getServerWorld()) continue;

            double dx = p.getX() - source.getX();
            double dy = p.getY() - source.getY();
            double dz = p.getZ() - source.getZ();
            if (dx * dx + dy * dy + dz * dz <= rSq) {
                ServerPlayNetworking.send(p, payload);
            }
        }
    }

    // ======================= 注册 =======================

    public static void registerServerReceivers() {
        // ---------- C2S 注册 ----------
        PayloadTypeRegistry.playC2S().register(ToggleUvPayload.ID, ToggleUvPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SetLensPayload.ID, SetLensPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SilentTeleportPayload.ID, SilentTeleportPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(MistAreaPayload.ID, MistAreaPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AttackPayload.ID, AttackPayload.CODEC);

        // ---------- S2C 注册 ----------
        PayloadTypeRegistry.playS2C().register(AttackPerformedPayload.ID, AttackPerformedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(UvStatePayload.ID, UvStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(UvFlashPayload.ID, UvFlashPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LensStatePayload.ID, LensStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RecoveryStatePayload.ID, RecoveryStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MistSyncPayload.ID, MistSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(UvBroadcastPayload.ID, UvBroadcastPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlaySoundPayload.ID, PlaySoundPayload.CODEC);

        // ==================== UV 开关 ====================
        ServerPlayNetworking.registerGlobalReceiver(ToggleUvPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            ctx.server().execute(() -> {
                ItemStack stack = player.getMainHandStack();
                if (!stack.isOf(ToothPliersMod.TOOTH_PLIERS)) return;

                SkillState st = ToothPliersState.get(player.getUuid());
                // ★ 后摇期间：仅传送后摇允许开关 UV
                if (st.isRecovery() && !st.recoveryFromTeleport) return;

                SkillData data = stack.getOrDefault(ToothPliersMod.SKILL_DATA, SkillData.DEFAULT);
                boolean newUv = !data.uvEnabled();

                if (newUv) {
                    stack.damage(ToothPliersConfig.DURABILITY_UV_TOGGLE_ON,
                            player, net.minecraft.entity.EquipmentSlot.MAINHAND);
                    broadcastSoundToNearby(player, ToothPliersSounds.ID_UV_ON);
                } else {
                    broadcastSoundToNearby(player, ToothPliersSounds.ID_UV_OFF);
                }

                // ★ 只改一个字段
                stack.set(ToothPliersMod.SKILL_DATA, data.withUv(newUv));
            });
        });

        // ==================== 角度透镜 ====================
        ServerPlayNetworking.registerGlobalReceiver(SetLensPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            ctx.server().execute(() -> {
                SkillState st = ToothPliersState.get(player.getUuid());
                int newLevel = Math.clamp(payload.level(), 0, 2);
                if (st.lensLevel != newLevel) {
                    st.lensLevel = newLevel;
                    ServerPlayNetworking.send(player, new LensStatePayload(newLevel));
                }
            });
        });

        // ==================== 沉默现身 ====================
        ServerPlayNetworking.registerGlobalReceiver(SilentTeleportPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            ctx.server().execute(() -> {
                ItemStack stack = player.getMainHandStack();
                if (!stack.isOf(ToothPliersMod.TOOTH_PLIERS)) return;

                SkillData data = stack.getOrDefault(ToothPliersMod.SKILL_DATA, SkillData.DEFAULT);
                SkillState st = ToothPliersState.get(player.getUuid());

                if (!data.uvEnabled() || !st.uvHasArea) return;

                long now = player.getWorld().getTime();
                if (st.isRecovery() && !data.hasTeleportWindow(now)) return;

                SilentTeleport.execute(player, st);
            });
        });

        // ==================== 迷雾区域 ====================
        ServerPlayNetworking.registerGlobalReceiver(MistAreaPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            ctx.server().execute(() -> {
                ItemStack stack = player.getMainHandStack();
                if (!stack.isOf(ToothPliersMod.TOOTH_PLIERS)) return;

                double safeY = ToothPliersConfig.clampMistCenterY(payload.y());

                SkillData data = stack.getOrDefault(ToothPliersMod.SKILL_DATA, SkillData.DEFAULT);
                long endTick = player.getWorld().getTime() + ToothPliersConfig.MIST_DURATION;

                // ★ 只改迷雾相关字段
                stack.set(ToothPliersMod.SKILL_DATA,
                        data.withMist(true, endTick, payload.x(), safeY, payload.z()));

                stack.damage(ToothPliersConfig.DURABILITY_MIST,
                        player, net.minecraft.entity.EquipmentSlot.MAINHAND);

                MistAreaManager.addMist(player, payload.x(), safeY, payload.z());
                broadcastSoundToNearby(player, ToothPliersSounds.ID_RELEASE_AREA);
            });
        });

        // ==================== 攻击 ====================
        ServerPlayNetworking.registerGlobalReceiver(AttackPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            ctx.server().execute(() ->
                    com.joseph.toothpliers.server.ToothPliersServerTick.startAttack(player));
        });
    }
}