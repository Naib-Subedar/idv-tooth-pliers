package com.joseph.toothpliers;

import com.joseph.toothpliers.client.ToothPliersClientTick;
import com.joseph.toothpliers.client.ToothPliersHud;
import com.joseph.toothpliers.client.ToothPliersKeys;
import com.joseph.toothpliers.client.ToothPliersRenderer;
import net.fabricmc.api.ClientModInitializer;

public class ToothPliersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ToothPliersKeys.register();
        ToothPliersHud.register();
        ToothPliersClientTick.register();
        ToothPliersRenderer.register();
    }
}