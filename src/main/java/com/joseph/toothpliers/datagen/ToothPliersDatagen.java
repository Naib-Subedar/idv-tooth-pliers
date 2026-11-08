package com.joseph.toothpliers.datagen;

import com.joseph.toothpliers.ToothPliersMod;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class ToothPliersDatagen implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator gen) {
        var pack = gen.createPack();
        pack.addProvider(RecipeProvider::new);
    }

    private static class RecipeProvider extends FabricRecipeProvider {
        RecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) {
            super(output, registries);
        }

        @Override
        public void generate(RecipeExporter exporter) {
            // 空 | 铁锭 | 紫水晶碎片
            // 铁锭 | 铁粒 | 铁锭
            // 空 | 铁锭 | 空
            ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, ToothPliersMod.TOOTH_PLIERS, 1)
                    .pattern(" IA")
                    .pattern("INI")
                    .pattern(" I ")
                    .input('I', Items.IRON_INGOT)
                    .input('A', Items.AMETHYST_SHARD)
                    .input('N', Items.IRON_NUGGET)
                    .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
                    .offerTo(exporter);
        }
    }
}