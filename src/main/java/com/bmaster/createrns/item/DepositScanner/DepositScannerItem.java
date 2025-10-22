package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.deposit.LevelDepositData;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerItemRenderer;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DepositScannerItem extends Item {
    // This is also enforced on the server side
    private static final int SCANNER_USE_COOLDOWN = LevelDepositData.MIN_COMPUTE_INTERVAL + 10;

    public DepositScannerItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SimpleCustomRenderer.create(this, new LinkedControllerItemRenderer()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (!level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(usedHand));

        if (DepositScannerClientHandler.isTracking()) {
            DepositScannerClientHandler.cancelTracking(true);
        } else {
            DepositScannerClientHandler.discoverDeposit();
            player.getCooldowns().addCooldown(RNSContent.DEPOSIT_SCANNER_ITEM.get(), SCANNER_USE_COOLDOWN);
        }

        return InteractionResultHolder.pass(player.getItemInHand(usedHand));
    }
}
