package com.bmaster.createrns.mixin.simulated;

import com.bmaster.createrns.compat.aeronautics.ScannerNavigationTarget;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerItem;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.RenderableNavigationTarget;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nullable;

@Mixin(DepositScannerItem.class)
public abstract class DepositScannerItemMixin implements RenderableNavigationTarget {
    @Override
    public @Nullable Vec3 getTarget(NavTableBlockEntity table, ItemStack stack) {
        return ScannerNavigationTarget.INSTANCE.get().getTarget(table, stack);
    }

    @Override
    public void renderInNavTable(
            ItemStack stack, NavTableBlockEntity table, BlockState state, float partialTicks, PoseStack ms,
            MultiBufferSource buffer, int light, int overlay
    ) {

        var selectedIcon = ScannerNavigationTarget.getTrackedDepositIcon(table);
        boolean found = ScannerNavigationTarget.isLocationFound(table);
        DepositScannerItemRenderer.renderInNavTable(
                stack, selectedIcon, found, ms, buffer, table.getLevel(), light, overlay);
    }
}
