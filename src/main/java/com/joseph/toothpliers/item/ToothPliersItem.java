package com.joseph.toothpliers.item;

import com.joseph.toothpliers.config.ToothPliersConfig;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class ToothPliersItem extends Item {

    public ToothPliersItem(Settings settings) {
        super(settings);
    }

    /**
     * 动态耐久：基础 500 + 耐久附魔加成。
     * 1.21.1 中 getMaxDamage 不再接收 ItemStack 参数，
     * 因此耐久加成通过 DataComponentTypes.MAX_DAMAGE 在每 tick 或使用时刷新。
     * 这里提供一个静态工具方法，供 ServerTick 调用。
     */
    public static int computeMaxDamage(ItemStack stack, ServerWorld world) {
        int level = EnchantmentHelper.getLevel(
                world.getRegistryManager()
                        .get(RegistryKeys.ENCHANTMENT)
                        .getEntry(Enchantments.UNBREAKING)
                        .orElseThrow(),
                stack);
        level = Math.min(level, 3);
        return ToothPliersConfig.BASE_DURABILITY + ToothPliersConfig.UNBREAKING_BONUS[level];
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return TypedActionResult.pass(user.getStackInHand(hand));
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ingredient.isOf(net.minecraft.item.Items.IRON_INGOT)
                || ingredient.isOf(net.minecraft.item.Items.IRON_NUGGET)
                || ingredient.isOf(net.minecraft.item.Items.AMETHYST_SHARD);
    }
}