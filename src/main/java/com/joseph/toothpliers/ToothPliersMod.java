package com.joseph.toothpliers;

import com.joseph.toothpliers.component.SkillData;
import com.joseph.toothpliers.config.ToothPliersConfig;
import com.joseph.toothpliers.item.ToothPliersItem;
import com.joseph.toothpliers.network.ToothPliersNetworking;
import com.joseph.toothpliers.server.ToothPliersServerTick;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToothPliersMod implements ModInitializer {
    public static final String MOD_ID = "idv-tooth-pliers";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // ==================== 技能状态组件 ====================
    public static final ComponentType<SkillData> SKILL_DATA = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MOD_ID, "skill_data"),
            ComponentType.<SkillData>builder()
                    .codec(SkillData.CODEC)
                    .build()
    );

    // ==================== 牙钳物品 ====================
    public static final Item TOOTH_PLIERS = register("tooth_pliers", new ToothPliersItem(
            new Item.Settings()
                    .maxCount(1)
                    .maxDamage(ToothPliersConfig.BASE_DURABILITY)
                    .attributeModifiers(buildAttributes())
                    .rarity(Rarity.RARE)
                    .component(SKILL_DATA, SkillData.DEFAULT)
    ));

    private static AttributeModifiersComponent buildAttributes() {
        return AttributeModifiersComponent.builder()
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                        new EntityAttributeModifier(
                                Item.BASE_ATTACK_DAMAGE_MODIFIER_ID,
                                2.5,
                                EntityAttributeModifier.Operation.ADD_VALUE),
                        net.minecraft.component.type.AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED,
                        new EntityAttributeModifier(
                                Item.BASE_ATTACK_SPEED_MODIFIER_ID,
                                -2.4,
                                EntityAttributeModifier.Operation.ADD_VALUE),
                        net.minecraft.component.type.AttributeModifierSlot.MAINHAND)
                .build();
    }

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(MOD_ID, name), item);
    }

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
                .register(entries -> entries.add(TOOTH_PLIERS));
        // ★ 触发音效注册
        com.joseph.toothpliers.sound.ToothPliersSounds.init();

        ToothPliersNetworking.registerServerReceivers();
        ToothPliersServerTick.register();

        LOGGER.info("[IDV Tooth Pliers] Initialized.");
    }
}