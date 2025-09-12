package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.deposit.capability.DepositIndex;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DepositScannerItem extends Item {
    // This is also enforced on the server side
    private static final int SCANNER_DISCOVER_COOLDOWN = DepositIndex.MIN_COMPUTE_INTERVAL + 10;
    private long scannerLastDiscovered = 0;

    public DepositScannerItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SimpleCustomRenderer.create(this, new DepositScannerItemRenderer()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        var time = level.getGameTime();
        var cooldowns = player.getCooldowns();

        if (DepositScannerClientHandler.isTracking()) {
            DepositScannerClientHandler.cancelTracking();
        } else if (scannerLastDiscovered + SCANNER_DISCOVER_COOLDOWN < time) {
            DepositScannerClientHandler.discoverDeposit();
            scannerLastDiscovered = time;
            cooldowns.addCooldown(RNSContent.DEPOSIT_SCANNER_ITEM.get(), SCANNER_DISCOVER_COOLDOWN);
        }
        return InteractionResultHolder.pass(player.getItemInHand(usedHand));
    }
}
