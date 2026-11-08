package com.joseph.toothpliers.sound;

import com.joseph.toothpliers.ToothPliersMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ToothPliersSounds {
    private ToothPliersSounds() {}

    public static final SoundEvent SILENT_TP = register("silent_tp");
    public static final SoundEvent UV_ON = register("uv_on");
    public static final SoundEvent UV_OFF = register("uv_off");
    public static final SoundEvent PREVIEW_MODE = register("preview_mode");
    public static final SoundEvent RELEASE_AREA = register("release_area");
    public static final SoundEvent ATTACK = register("attack");
    public static final SoundEvent HIT = register("hit");
    public static final SoundEvent SWITCH_LENS = register("switch_lens");

    // ==================== S2C 音效 ID（用于网络传输） ====================
    public static final int ID_SILENT_TP = 0;
    public static final int ID_UV_ON = 1;
    public static final int ID_UV_OFF = 2;
    public static final int ID_PREVIEW_MODE = 3;
    public static final int ID_RELEASE_AREA = 4;
    public static final int ID_ATTACK = 5;
    public static final int ID_HIT = 6;
    public static final int ID_SWITCH_LENS = 7;

    /** 根据 S2C 音效 ID 获取对应的 SoundEvent */
    public static SoundEvent fromId(int id) {
        return switch (id) {
            case ID_SILENT_TP -> SILENT_TP;
            case ID_UV_ON -> UV_ON;
            case ID_UV_OFF -> UV_OFF;
            case ID_PREVIEW_MODE -> PREVIEW_MODE;
            case ID_RELEASE_AREA -> RELEASE_AREA;
            case ID_ATTACK -> ATTACK;
            case ID_HIT -> HIT;
            case ID_SWITCH_LENS -> SWITCH_LENS;
            default -> null;
        };
    }

    private static SoundEvent register(String name) {
        Identifier id = Identifier.of(ToothPliersMod.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    /** 触发类加载（在 Mod 主入口调用） */
    public static void init() {
        // 静态字段初始化已在类加载时完成
    }
}