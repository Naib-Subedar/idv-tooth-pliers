package com.joseph.toothpliers.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SkillData(
        boolean uvEnabled,
        long teleportWindowEndTick,
        boolean teleportUsed,
        long attackCooldownEndTick,
        boolean mistActive,
        long mistEndTick,
        double mistX, double mistY, double mistZ
) {
    public static final SkillData DEFAULT = new SkillData(
            false, 0L, false, 0L, false, 0L, 0, 0, 0);

    public static final Codec<SkillData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.fieldOf("uv").forGetter(SkillData::uvEnabled),
            Codec.LONG.fieldOf("tpEnd").forGetter(SkillData::teleportWindowEndTick),
            Codec.BOOL.fieldOf("tpu").forGetter(SkillData::teleportUsed),
            Codec.LONG.fieldOf("acEnd").forGetter(SkillData::attackCooldownEndTick),
            Codec.BOOL.fieldOf("mist").forGetter(SkillData::mistActive),
            Codec.LONG.fieldOf("mistEnd").forGetter(SkillData::mistEndTick),
            Codec.DOUBLE.fieldOf("mx").forGetter(SkillData::mistX),
            Codec.DOUBLE.fieldOf("my").forGetter(SkillData::mistY),
            Codec.DOUBLE.fieldOf("mz").forGetter(SkillData::mistZ)
    ).apply(inst, SkillData::new));

    // ==================== 辅助方法 ====================

    /** 传送窗口是否有效 */
    public boolean hasTeleportWindow(long currentTick) {
        return teleportUsed && teleportWindowEndTick > currentTick;
    }

    /** 攻击是否在冷却中 */
    public boolean isOnCooldown(long currentTick) {
        return attackCooldownEndTick > currentTick;
    }

    // ==================== 单字段修改 ====================

    public SkillData withUv(boolean uv) {
        return new SkillData(uv, teleportWindowEndTick, teleportUsed,
                attackCooldownEndTick, mistActive, mistEndTick,
                mistX, mistY, mistZ);
    }

    public SkillData withTeleport(long windowEndTick, boolean used) {
        return new SkillData(uvEnabled, windowEndTick, used,
                attackCooldownEndTick, mistActive, mistEndTick,
                mistX, mistY, mistZ);
    }

    public SkillData withCooldown(long cooldownEndTick) {
        return new SkillData(uvEnabled, teleportWindowEndTick, teleportUsed,
                cooldownEndTick, mistActive, mistEndTick,
                mistX, mistY, mistZ);
    }

    public SkillData withMist(boolean mist, long endTick, double x, double y, double z) {
        return new SkillData(uvEnabled, teleportWindowEndTick, teleportUsed,
                attackCooldownEndTick, mist, endTick, x, y, z);
    }
}