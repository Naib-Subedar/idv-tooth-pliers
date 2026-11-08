package com.joseph.toothpliers.client;

import com.joseph.toothpliers.ToothPliersMod;
import com.joseph.toothpliers.network.ToothPliersNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ToothPliersKeys {

    public static KeyBinding KEY_UV;
    public static KeyBinding KEY_LENS_1;
    public static KeyBinding KEY_LENS_2;
    public static KeyBinding KEY_LENS_3;
    public static KeyBinding KEY_TELEPORT;
    public static KeyBinding KEY_MIST;

    public static void register() {
        KEY_UV       = reg("uv",       InputUtil.Type.MOUSE,  GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        KEY_LENS_1   = reg("lens_1",   InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M);
        KEY_LENS_2   = reg("lens_2",   InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_COMMA);
        KEY_LENS_3   = reg("lens_3",   InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_PERIOD);
        KEY_TELEPORT = reg("teleport", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G);
        KEY_MIST     = reg("mist",     InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            boolean holding = client.player.getMainHandStack().isOf(ToothPliersMod.TOOTH_PLIERS);
            if (!holding) return;

            if (ToothPliersClientState.mistPreviewActive) return;

            if (KEY_UV.wasPressed()) {
                ClientPlayNetworking.send(new ToothPliersNetworking.ToggleUvPayload());
            }
            if (KEY_TELEPORT.wasPressed()) {
                ClientPlayNetworking.send(new ToothPliersNetworking.SilentTeleportPayload());
            }
        });
    }

    private static KeyBinding reg(String name, InputUtil.Type type, int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.idv-tooth-pliers." + name,
                type, code,
                "category.idv-tooth-pliers"));
    }
}