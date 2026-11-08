package com.joseph.toothpliers.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ToothPliersClientState {
    public static int lensLevel = 2;

    public static boolean uvHasArea = false;
    public static double uvHitX, uvHitY, uvHitZ;
    public static double uvDirX = 0, uvDirZ = 1;

    public static int uvAttackFlashTicks = 0;

    // ==================== 攻击锁视角（Mixin 拦截 Y 轴） ====================
    /** 是否处于攻击锁视角状态 */
    public static boolean uvLockPitchActive = false;
    /** 锁定的 pitch 值 */
    public static float uvLockedPitch = 0f;
    /** 剩余锁定 tick */
    public static int uvLockPitchTicks = 0;

    // ==================== 迷雾（含 owner） ====================
    public static class MistEntry {
        public final UUID owner;
        public final double x, y, z;
        public MistEntry(UUID owner, double x, double y, double z) {
            this.owner = owner;
            this.x = x; this.y = y; this.z = z;
        }
    }

    public static final Map<Long, MistEntry> MISTS = new ConcurrentHashMap<>();

    public static void addMist(long id, UUID owner, double x, double y, double z) {
        MISTS.put(id, new MistEntry(owner, x, y, z));
    }

    public static void removeMist(long id) {
        MISTS.remove(id);
    }

    // ==================== 迷雾预览模式 ====================
    public static boolean mistPreviewActive = false;
    public static double mistPreviewDistance = 35.0;
    public static int mistPressTicks = 0;
    public static boolean mistKeyDown = false;
    public static boolean mistSkipReleaseOnUp = false;
    public static int mistPreviewCooldownTicks = 0;
    public static boolean skipNextAttackRelease = false;

    // ==================== 后摇状态 ====================
    public static boolean inRecovery = false;

    // ==================== 广播的 UV 紫框（其他玩家） ====================
    public static final List<BroadcastUv> BROADCAST_UVS = new CopyOnWriteArrayList<>();

    public static class BroadcastUv {
        public final double x, y, z, dirX, dirZ;
        public int ticksLeft;

        public BroadcastUv(double x, double y, double z,
                           double dirX, double dirZ, int ticksLeft) {
            this.x = x; this.y = y; this.z = z;
            this.dirX = dirX; this.dirZ = dirZ;
            this.ticksLeft = ticksLeft;
        }
    }
}