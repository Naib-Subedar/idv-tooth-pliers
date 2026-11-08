package com.joseph.toothpliers.config;

public final class ToothPliersConfig {
    private ToothPliersConfig() {}

    // ==================== 攻击距离 ====================
    public static final double NORMAL_ATTACK_RANGE = 2.0;
    public static final double ATTACK_FAN_ANGLE = 45.0;

    // ==================== 伤害 ====================
    public static final float NORMAL_ATTACK_DAMAGE = 3.5f;
    public static final float UV_BACTERIA_DAMAGE = 2.0f;

    // ==================== 前摇 / 后摇（tick） ====================
    public static final int NORMAL_ATTACK_WINDUP = 4;
    public static final int NORMAL_ATTACK_RECOVERY = 2;
    public static final int TELEPORT_RECOVERY = 6;

    // ==================== 耐久 ====================
    public static final int BASE_DURABILITY = 500;
    public static final int[] UNBREAKING_BONUS = {0, 50, 100, 150};
    public static final int DURABILITY_UV_TOGGLE_ON = 1;
    public static final int DURABILITY_MIST = 2;
    public static final int DURABILITY_TELEPORT_1 = 1;
    public static final int DURABILITY_TELEPORT_2 = 2;
    public static final int DURABILITY_ATTACK_HIT = 1;

    // ==================== 紫外辐射 ====================
    public static final int UV_MAX_DISTANCE = 35;
    public static final int UV_AREA_WIDTH = 4;
    public static final int UV_DAMAGE_HEIGHT = 4;   // 向上 4 格
    public static final int UV_DAMAGE_DOWN = 3;     // ★ 向下 3 格

    // ==================== 紫外辐射渲染 ====================
    public static final float UV_RENDER_RED   = 0.7f;
    public static final float UV_RENDER_GREEN = 0.0f;
    public static final float UV_RENDER_BLUE  = 1.0f;
    public static final float UV_RENDER_ALPHA = 1.0f;

    public static final float UV_FLASH_RED   = 1.0f;
    public static final float UV_FLASH_GREEN = 0.5f;
    public static final float UV_FLASH_BLUE  = 1.0f;

    public static final int UV_ATTACK_FLASH_TICKS = 10;
    public static final double UV_LINE_THICKNESS = 0.06;
    public static final int UV_LINE_COUNT = 3;
    public static final int UV_PARTICLE_BURST_COUNT = 15;

    // ==================== UV 广播 ====================
    public static final int UV_BROADCAST_DURATION = 4;
    public static final double UV_BROADCAST_RANGE = 64.0;

    // ==================== 角度透镜 ====================
    public static final int[] LENS_NORMAL_DISTANCE = {10, 20, 30};

    // ==================== 沉默现身 ====================
    public static final int TELEPORT_SECOND_WINDOW = 100;
    public static final int TELEPORT_MAX_SEARCH = 10;
    public static final int TELEPORT_RECOVERY_SECOND = 20;

    // ==================== 迷雾区域 ====================
    public static final double MIST_RADIUS = 20.0;
    public static final double MIST_HEIGHT = 14.0;
    public static final int MIST_DURATION = 500;
    public static final int MIST_MAX_DISTANCE = 35;
    public static final double MIST_SPEED_MULTIPLIER = 0.5;


    public static final float MIST_BOX_RED   = 1.0f;
    public static final float MIST_BOX_GREEN = 0.4f;
    public static final float MIST_BOX_BLUE  = 1.0f;
    public static final float MIST_BOX_ALPHA = 0.8f;

    // ==================== 迷雾填充渲染 ====================
    public static final float MIST_FILL_RED   = 0.65f;
    public static final float MIST_FILL_GREEN = 0.20f;
    public static final float MIST_FILL_BLUE  = 1.00f;
    public static final float MIST_FILL_ALPHA = 0.15f;
    public static final int MIST_FILL_RINGS = 32;
    public static final int MIST_FILL_SECTORS = 64;

    // ==================== 迷雾 Y 轴边界 ====================
    /** 球体边缘允许的最低 Y 坐标 */
    public static final double MIST_MIN_Y = -64.0;
    /** 球体边缘允许的最高 Y 坐标 */
    public static final double MIST_MAX_Y = 330.0;

    // ==================== 锋利附魔加成 ====================
    /** 普通攻击命中加成（索引 = 锋利等级 0~5） */
    public static final float[] SHARPNESS_BONUS_NORMAL = {0f, 0.25f, 0.30f, 0.35f, 0.40f, 0.45f};
    /** 紫外辐射技能命中加成（索引 = 锋利等级 0~5） */
    public static final float[] SHARPNESS_BONUS_UV     = {0f, 1.80f, 1.85f, 1.90f, 1.95f, 2.00f};

    /** 把迷雾几何中心 y 夹到合法范围（圆柱上下边缘在 [MIN_Y, MAX_Y] 内） */
    public static double clampMistCenterY(double centerY) {
        double halfH = MIST_HEIGHT / 2.0;
        double minCenter = MIST_MIN_Y + halfH;
        double maxCenter = MIST_MAX_Y - halfH;
        return Math.clamp(centerY, minCenter, maxCenter);
    }

    // ==================== 音效广播 ====================
    /** 技能音效广播范围（格） */
    public static final double SOUND_BROADCAST_RANGE = 40.0;


    /** 挥手动画时长（tick） */
    public static final int SWING_ANIMATION_TICKS = 6;
}