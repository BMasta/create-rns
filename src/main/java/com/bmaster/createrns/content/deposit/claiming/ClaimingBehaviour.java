package com.bmaster.createrns.content.deposit.operating;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapterHolder;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class ClaimingBehaviour extends BlockEntityBehaviour implements IDepositBlockClaimer {
    protected final Supplier<Direction> operatingDirection;
    protected @Nullable OperatingSelection operatingSelection = null;
    protected @Nullable Set<BlockPos> claimedDepositBlocks = null;
    protected @Nullable BlockPos crossSublevelOperatingSource = null;
    private final SmartBlockEntity sbe;

    public abstract boolean isRunning();

    protected abstract boolean isDepositBlockOperable(BlockPos pos);

    protected abstract void onOperatingSelectionChanged();

    public ClaimingBehaviour(SmartBlockEntity sbe, Supplier<Direction> operatingDirection) {
        super(sbe);
        this.sbe = sbe;
        this.operatingDirection = operatingDirection;
    }

    public @Nullable OperatingSelection getOperatingSelection() {
        return operatingSelection;
    }

    @Override
    public void claimDepositBlocks() {
        var level = getLevel();
        if (level == null || level.isClientSide) return;
        var anchor = getOperatingAnchor();
        if (anchor == null) return;

        claimedDepositBlocks = getClaimableDepositVein(level).stream()
                .filter(this::isDepositBlockOperable)
                .collect(Collectors.toSet());

        tryReInitOperatingSelection();

        var pos = getPos();
        CreateRNS.LOGGER.trace("Operator at {}, {}, {} claimed {} deposit blocks", pos.getX(), pos.getY(), pos.getZ(),
                claimedDepositBlocks.size());

        sbe.notifyUpdate();
    }

    @Override
    public void write(CompoundTag nbt, Provider provider, boolean clientPacket) {
        super.write(nbt, provider, clientPacket);

        nbt.put("claimer", serializeDepositBlockClaimer(provider));

        if (clientPacket && operatingSelection != null) {
            nbt.put("selection", OperatingSelection.toNBT(operatingSelection));
        }
    }

    @Override
    public void read(CompoundTag nbt, Provider provider, boolean clientPacket) {
        super.read(nbt, provider, clientPacket);

        if (nbt.get("claimer") instanceof CompoundTag claimerTag) {
            deserializeDepositBlockClaimer(provider, claimerTag);
        }

        operatingSelection = null;
        if (clientPacket) {
            operatingSelection = OperatingSelection.fromNBT(nbt.getCompound("selection"));
        }
        onOperatingSelectionChanged();
    }

    @Override
    public void tick() {
        super.tick();

        var level = getLevel();
        if (level == null || level.isClientSide) return;

        if (isRunning() && claimedDepositBlocks != null && claimedDepositBlocks.isEmpty()) checkDepositBlockAreaChanges();

        if (!isOperatingSelectionValid()) {
            clearOperatingSelection();
            claimDepositBlocks();
            if (operatingSelection == null) sbe.notifyUpdate();
        }

    }

    @Override
    public BlockPos getBlockPos() {
        return getPos();
    }

    @Override
    public Direction getOperatingDirection() {
        return operatingDirection.get();
    }

    @Override
    public @Nullable Level getLevel() {
        return sbe.getLevel();
    }

    @Override
    public @Nullable Set<BlockPos> getClaimedDepositBlocks() {
        return claimedDepositBlocks;
    }

    @Override
    public void setClaimedDepositBlocks(@Nullable Set<BlockPos> claimedBlocks) {
        claimedDepositBlocks = claimedBlocks == null ? null : new ObjectOpenHashSet<>(claimedBlocks);
    }

    protected void clearOperatingSelection() {
        operatingSelection = null;
        setClaimedDepositBlocks(null);
        onOperatingSelectionChanged();
    }

    protected boolean tryInitOperatingSelection() {
        if (operatingSelection != null) return true;
        return tryReInitOperatingSelection();
    }

    protected boolean tryReInitOperatingSelection() {
        var level = getLevel();
        var anchor = getOperatingAnchor();
        if (level == null || anchor == null || claimedDepositBlocks == null) return false;

        operatingSelection = new OperatingSelection(false,
                OperatingSublevelAdapterHolder.getAdapter().getOperatingSublevel(level, anchor), claimedDepositBlocks);
        onOperatingSelectionChanged();
        return true;
    }

    protected void checkDepositBlockAreaChanges() {
        var level = getLevel();
        var anchor = getOperatingAnchor();
        var dimensions = getCrossSublevelOperatingDimensions();
        if (level == null || level.isClientSide) return;
        if (anchor == null || dimensions == null) {
            crossSublevelOperatingSource = null;
            return;
        }

        var direction = getOperatingDirection();
        var adapter = OperatingSublevelAdapterHolder.getAdapter();
        var ownSublevel = adapter.getOperatingSublevel(level, anchor);

        var detectedBlocks = adapter.getCrossSublevelDepositBlocks(level, ownSublevel, anchor, direction, dimensions);
        if (crossSublevelOperatingSource == null && detectedBlocks.isEmpty()) return;
        if (crossSublevelOperatingSource != null && detectedBlocks.contains(crossSublevelOperatingSource)) return;

        BlockPos reportedPos;
        if (crossSublevelOperatingSource != null) {
            reportedPos = crossSublevelOperatingSource;
            crossSublevelOperatingSource = null;
        } else {
            crossSublevelOperatingSource = detectedBlocks.iterator().next();
            reportedPos = crossSublevelOperatingSource;
        }

        var message = Component.literal("[Create RNS] Miner " +
                (crossSublevelOperatingSource != null ? "now" : "no longer") + " tracks deposit block at " +
                reportedPos.getX() + ", " + reportedPos.getY() + ", " + reportedPos.getZ());
        for (var player : level.players()) player.sendSystemMessage(message);
    }

    private boolean isOperatingSelectionValid() {
        if (operatingSelection == null) return false;

        // Remote selection validation is added with cross-sublevel target discovery.
        if (operatingSelection.crossSublevel) return true;

        var level = getLevel();
        var anchor = getOperatingAnchor();
        if (level == null || anchor == null) return false;
        var anchorSpace = OperatingSublevelAdapterHolder.getAdapter().getOperatingSublevel(level, anchor);
        return operatingSelection.sublevel.equals(anchorSpace);
    }
}
