package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.block.MiningBehaviour;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.List;
import java.util.function.BiFunction;

import static net.minecraft.ChatFormatting.GRAY;

public interface IHaveAdaptiveGoggleInformation extends IHaveGoggleInformation {
    /// Must have a mining behavior
    KineticBlockEntity getTargetBlockEntity();

    String getLangIdentifier();

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
        var langId = getLangIdentifier();
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
