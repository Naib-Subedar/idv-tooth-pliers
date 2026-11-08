package com.joseph.toothpliers.mixin;

import com.joseph.toothpliers.ToothPliersMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class EnchantmentMixin {

    @Inject(method = "isSupportedItem", at = @At("HEAD"),
            cancellable = true, require = 0)
    private void idv$excludeFromToothPliers(ItemStack stack,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (!stack.isOf(ToothPliersMod.TOOTH_PLIERS)) return;

        Enchantment self = (Enchantment) (Object) this;

        // 在客户端世界上下文中获取动态注册表管理器
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        Registry<Enchantment> registry = client.world.getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT);
        if (registry == null) return;

        // 通过注册表反查当前附魔的 RegistryKey
        RegistryKey<Enchantment> key = registry.getKey(self).orElse(null);
        if (key == null) return;

        // 比较是否是需要排除的附魔
        if (key.equals(Enchantments.SMITE)
                || key.equals(Enchantments.BANE_OF_ARTHROPODS)) {
            cir.setReturnValue(false);
        }
    }
}
