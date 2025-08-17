package com.bmaster.createrns.item.DepositScanner;

import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class DepositScannerItem extends Item {
    public DepositScannerItem(Properties pProperties) {
        super(pProperties);
    }

    @OnlyIn(Dist.CLIENT)
    private void toggleActive() {
        DepositScannerClientHandler.toggle();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SimpleCustomRenderer.create(this, new DepositScannerItemRenderer()));
    }
}
