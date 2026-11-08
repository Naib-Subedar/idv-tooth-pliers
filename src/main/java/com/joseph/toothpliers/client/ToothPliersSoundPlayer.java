package com.joseph.toothpliers.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.SoundEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * 牙钳音效播放器。
 *
 * 特性：同类型音效播放新音效前，先强制停止旧音效（避免叠加）。
 */
public final class ToothPliersSoundPlayer {
    private ToothPliersSoundPlayer() {}

    /** 每个 SoundEvent 当前正在播放的实例 */
    private static final Map<SoundEvent, SoundInstance> PLAYING = new HashMap<>();

    /**
     * 播放牙钳音效。若同类音效正在播放，会强制打断后播放新音效。
     */
    public static void play(SoundEvent event, float volume, float pitch) {
        if (event == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        SoundManager soundManager = mc.getSoundManager();
        if (soundManager == null) return;

        // 1. 停止旧的同类音效
        SoundInstance old = PLAYING.remove(event);
        if (old != null) {
            soundManager.stop(old);
        }

        // 2. 播放新音效
        SoundInstance instance = PositionedSoundInstance.master(event, volume, pitch);
        PLAYING.put(event, instance);
        soundManager.play(instance);
    }

    public static void play(SoundEvent event) {
        play(event, 1.0f, 1.0f);
    }
}