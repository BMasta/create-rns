package com.bmaster.createrns.content.deposit.mining;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.BiFunction;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface IHaveAdaptiveGoggleInformation extends IHaveGoggleInformation {
    /// Must have a mining behavior
    KineticBlockEntity getTargetBlockEntity();

    /// Rendered if not shifted or if all secondaries failed to render
    default List<BiFunction<Context, List<Component>, Boolean>> getPrimarySections() {
        return List.of();
    }

    /// Rendered if shifted or if all primaries failed to render
    default List<BiFunction<Context, List<Component>, Boolean>> getSecondarySections() {
        return List.of();
    }

    /// Rendered regardless at the top
    default List<BiFunction<Context, List<Component>, Boolean>> getMandatoryTopSections() {
        return List.of();
    }

    /// Rendered regardless at the bottom
    default List<BiFunction<Context, List<Component>, Boolean>> getMandatoryBottomSections() {
        return List.of();
    }

    @Override
    default boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        var be = getTargetBlockEntity();
        var beLoc = ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(be.getType());
        if (beLoc == null) return false;
        var langId = beLoc.getPath();
        var primary = isPlayerSneaking ? getSecondarySections() : getPrimarySections();
        var secondary = isPlayerSneaking ? getPrimarySections() : getSecondarySections();
        var mandatoryTop = getMandatoryTopSections();
        var mandatoryBottom = getMandatoryBottomSections();
        boolean added = false;

        for (var mt : mandatoryTop) {
            if (mt.apply(new Context(be, langId, !added, false), tooltip)) added = true;
        }

        for (var p : primary) {
            if (p.apply(new Context(be, langId, !added, false), tooltip)) added = true;
        }

        if (!added) {
            for (var s : secondary) {
                if (s.apply(new Context(be, langId, !added, true), tooltip)) added = true;
            }
        }

        for (var mb : mandatoryBottom) {
            if (mb.apply(new Context(be, langId, !added, false), tooltip)) added = true;
        }

        return added;
    }

    record Context(BlockEntity target, String langId, boolean isFirstSection, boolean isBackupSection) {}
}
