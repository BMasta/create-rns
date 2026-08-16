package com.bmaster.createrns.compat.aeronautics;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSItems;
import com.bmaster.createrns.RNSMisc;
import com.bmaster.createrns.RNSSoundEvents;
import com.bmaster.createrns.content.deposit.info.ServerDepositLocation;
import com.bmaster.createrns.content.deposit.spec.DepositSpecLookup;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.index.SimDataComponents;
import dev.simulated_team.simulated.index.SimRegistries;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ScannerNavigationTarget implements NavigationTarget {
    public static final float DEADZONE = 4.0F;

    private static final String TRACKED_STRUCTURE_TAG = CreateRNS.ID + ":tracked_deposit_structure";
    private static final String TRACKED_LOCATION_TAG = CreateRNS.ID + ":tracked_deposit_location";
    private static final String PRECISE_LOCATION_TAG = CreateRNS.ID + ":precise_deposit_location";
    private static final String PRECISION_LAST_UPDATED_TAG = CreateRNS.ID + ":precision_last_updated";

    private static final int PRECISION_UPDATE_COOLDOWN = 20;

    private static final DeferredRegister<NavigationTarget> NAVIGATION_TARGETS =
            DeferredRegister.create(SimRegistries.Keys.NAVIGATION_TARGET, CreateRNS.ID);
    public static final Supplier<ScannerNavigationTarget> INSTANCE = NAVIGATION_TARGETS.register(
            "deposit_scanner", ScannerNavigationTarget::new);

    public static ItemStack getTrackedDepositIcon(NavTableBlockEntity table) {
        var level = table.getLevel();
        if (level == null) return ItemStack.EMPTY;

        var tableData = table.getPersistentData();
        if (!tableData.contains(TRACKED_STRUCTURE_TAG, Tag.TAG_STRING)) return ItemStack.EMPTY;

        var id = ResourceLocation.tryParse(tableData.getString(TRACKED_STRUCTURE_TAG));
        if (id == null) return ItemStack.EMPTY;

        var depositKey = ResourceKey.create(Registries.STRUCTURE, id);
        var icon = DepositSpecLookup.getScannerIcon(level.registryAccess(), depositKey);
        return icon != null ? icon : ItemStack.EMPTY;
    }

    public static boolean isLocationFound(NavTableBlockEntity table) {
        var level = table.getLevel();
        if (level == null) return false;

        var tableData = table.getPersistentData();
        if (!tableData.contains(PRECISE_LOCATION_TAG)) return false;

        var target = BlockPos.of(tableData.getLong(PRECISE_LOCATION_TAG)).getCenter();
        var distSqr = target.subtract(table.getProjectedSelfPos()).horizontalDistanceSqr();

        return distSqr <= DEADZONE * DEADZONE;
    }

    public static void register(IEventBus modBus) {
        NAVIGATION_TARGETS.register(modBus);
        modBus.addListener(ScannerNavigationTarget::modifyDefaultComponents);
    }

    private static void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        event.modify(RNSItems.DEPOSIT_SCANNER.get(), components ->
                components.set(SimDataComponents.TARGET, INSTANCE.get()));
    }

    @Override
    public float getDeadzone() {
        return DEADZONE;
    }

    @Override
    public @Nullable Vec3 getTarget(NavTableBlockEntity table, ItemStack stack) {
        if (!(table.getLevel() instanceof ServerLevel level)) return null;
        var tableData = table.getPersistentData();
        if (!tableData.contains(TRACKED_LOCATION_TAG) || !tableData.contains(PRECISION_LAST_UPDATED_TAG)) return null;

        Vec3 target;
        var loc = ServerDepositLocation.of(level, tableData.getCompound(TRACKED_LOCATION_TAG));
        if (tableData.contains(PRECISE_LOCATION_TAG)) {
            target = BlockPos.of(tableData.getLong(PRECISE_LOCATION_TAG)).getCenter();

            // Mark deposit as found if it's close enough
            if (!loc.isFound(level)) {
                var distSqr = target.subtract(table.getProjectedSelfPos()).horizontalDistanceSqr();
                if (distSqr <= DEADZONE * DEADZONE)  {
                    loc.setFound(level, true);
                    RNSSoundEvents.DEPOSIT_FOUND.playServer(level, table.getBlockPos());
                }
            }
        } else {
            int lastUpdated = tableData.getInt(PRECISION_LAST_UPDATED_TAG);
            if (lastUpdated < PRECISION_UPDATE_COOLDOWN) {
                tableData.putInt(PRECISION_LAST_UPDATED_TAG, lastUpdated + 1);
            } else {
                tableData.putInt(PRECISION_LAST_UPDATED_TAG, 1);
                if (loc.computePreciseLocation()) {
                    tableData.putLong(PRECISE_LOCATION_TAG, loc.getLocation().asLong());
                }
            }
            target = loc.getLocation().getCenter();
        }

        return target;
    }

    @Override
    public void onInsert(ItemStack stack, NavTableBlockEntity table, @Nullable Player player) {
        if (player == null) return;

        var tableData = table.getPersistentData();

        if (!(table.getLevel() instanceof ServerLevel level)) return;
        var data = level.getData(RNSMisc.LEVEL_DEPOSIT_DATA.get());
        var deposit = data.getDepositTrackedBy(player.getUUID());

        if (deposit == null) {
            tableData.remove(TRACKED_STRUCTURE_TAG);
            tableData.remove(TRACKED_LOCATION_TAG);
            tableData.remove(PRECISE_LOCATION_TAG);
            tableData.remove(PRECISION_LAST_UPDATED_TAG);
        } else {
            tableData.putString(TRACKED_STRUCTURE_TAG, deposit.getKey().location().toString());
            tableData.put(TRACKED_LOCATION_TAG, deposit.serialize());
            tableData.putInt(PRECISION_LAST_UPDATED_TAG, PRECISION_UPDATE_COOLDOWN);
        }
    }

    @Override
    public void onExtract(ItemStack stack, NavTableBlockEntity table, @Nullable Player player) {
        var tableData = table.getPersistentData();
        tableData.remove(TRACKED_STRUCTURE_TAG);
        tableData.remove(TRACKED_LOCATION_TAG);
        tableData.remove(PRECISE_LOCATION_TAG);
        tableData.remove(PRECISION_LAST_UPDATED_TAG);
    }
}
