package com.joseph.toothpliers.state;

public class SkillState {
    /** 透镜档位（按玩家，与具体牙钳无关）。默认 3 档（30 格） */
    public int lensLevel = 2;

    // ==================== UV 位置缓存（每 tick 重算） ====================
    public double uvHitX, uvHitY, uvHitZ;
    public boolean uvHasArea = false;
    public double uvDirX, uvDirZ;

    // ==================== 攻击前摇 / 后摇 ====================
    public boolean windupActive = false;
    public int windupTicks = 0;
    public int recoveryTicks = 0;
    public boolean isRecovery() { return recoveryTicks > 0; }

    /** ★ 当前后摇是否来自传送（传送后摇期间允许开关 UV） */
    public boolean recoveryFromTeleport = false;

    /** 判定闪光剩余 tick */
    public int uvAttackFlashTicks = 0;

    public void tick() {
        if (recoveryTicks > 0) recoveryTicks--;
        if (uvAttackFlashTicks > 0) uvAttackFlashTicks--;
    }
}