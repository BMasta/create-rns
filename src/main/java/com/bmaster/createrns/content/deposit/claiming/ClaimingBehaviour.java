package com.bmaster.createrns.content.deposit.claiming;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapter.OperatingSublevel;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapterHolder;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
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
    protected @Nullable ClaimedDepositBlocks claimedDepositBlocks = null;
    protected @Nullable BlockPos crossSublevelOperatingSource = null;
    private final SmartBlockEntity sbe;

    protected boolean validateClaimedDepositBlocks = false;

    public abstract boolean isRunning();

    protected abstract boolean isDepositBlockOperable(BlockPos pos);

    protected abstract void onClaimedBlocksChanged();

    public ClaimingBehaviour(SmartBlockEntity sbe, Supplier<Direction> operatingDirection) {
        super(sbe);
        this.sbe = sbe;
        this.operatingDirection = operatingDirection;
    }

    @Override
    public void initialize() {
        var level = getLevel();
        assert level != null;

        DepositClaimerInstanceHolder.addClaimer(this, level);
    }

    @Override
    public void unload() {
        var level = getLevel();
        assert level != null;

        DepositClaimerInstanceHolder.removeClaimer(this, level);
        level.invalidateCapabilities(getPos());
        if (level.isClientSide) DepositClaimerOutlineRenderer.removeClaimer(this);
    }

    @Override
    public void claimDepositBlocks() {
        var level = getLevel();
        if (level == null || level.isClientSide || !isRunning()) return;
        var anchor = getOperatingAnchor();
        if (anchor == null) return;
        var pos = getPos();
        claimedDepositBlocks = new ClaimedDepositBlocks(Set.copyOf(getClaimableDepositVein(level).stream()
                .filter(this::isDepositBlockOperable)
                .collect(Collectors.toSet())), false);
        onClaimedBlocksChanged();

        CreateRNS.LOGGER.trace("Operator at {}, {}, {} claimed {} deposit blocks", pos.getX(), pos.getY(), pos.getZ(),
                claimedDepositBlocks.positions().size());

        sbe.notifyUpdate();
    }

    @Override
    public void write(CompoundTag nbt, Provider provider, boolean clientPacket) {
        super.write(nbt, provider, clientPacket);

        var claimer = new CompoundTag();
        var claimedBlocks = getClaimedDepositBlocks();

        if (claimedBlocks != null) {
            claimer.putLongArray("claimed_blocks", claimedBlocks.positions().stream()
                    .mapToLong(BlockPos::asLong)
                    .toArray());
            claimer.putBoolean("cross_sublevel", claimedBlocks.crossSublevel());
        }

        nbt.put("claimer", claimer);
    }

    @Override
    public void read(CompoundTag nbt, Provider provider, boolean clientPacket) {
        super.read(nbt, provider, clientPacket);

        if (!(nbt.get("claimer") instanceof CompoundTag cTag)) return;

        // If the tag exists (even if empty), the claimer has finished claiming.
        Set<BlockPos> newlyClaimedBlocks = null;
        if (cTag.get("claimed_blocks") instanceof LongArrayTag claimedArr) {
            newlyClaimedBlocks = claimedArr.stream()
                    .map(a -> BlockPos.of(a.getAsLong()))
                    .collect(Collectors.toSet());
        }
        boolean crossSublevel = cTag.contains("claimed_blocks") && cTag.getBoolean("cross_sublevel");
        var claimedBlocks = (newlyClaimedBlocks != null)
                ? new ClaimedDepositBlocks(newlyClaimedBlocks, crossSublevel)
                : null;
        if (claimedBlocks == null && claimedDepositBlocks == null) return;
        if (claimedBlocks != null && claimedBlocks.equals(claimedDepositBlocks)) return;

        // If a miner is moved to a different sublevel, it gets recreated with its nbt copied
        // In such cases we have to ensure that the previously-claimed deposits are invalidated
        if (!clientPacket && claimedDepositBlocks == null && newlyClaimedBlocks != null && !crossSublevel) {
            validateClaimedDepositBlocks = true;
        }

        setClaimedDepositBlocks(claimedBlocks);
    }

    @Override
    public void tick() {
        super.tick();

        var level = getLevel();
        if (level == null || level.isClientSide) return;

        // Validate that the claimed deposit blocks are in the same sublevel as the miner
        if (claimedDepositBlocks != null && validateClaimedDepositBlocks) {
            validateClaimedDepositBlocks = false;
            var positions = claimedDepositBlocks.positions();
            var adapter = OperatingSublevelAdapterHolder.getAdapter();
            if (positions.isEmpty() || !adapter.isSameSublevel(level, getPos(), level, positions.iterator().next())) {
                setClaimedDepositBlocks(null);
            }
        }

        if (claimedDepositBlocks == null) {
            claimDepositBlocks();
        }

        if (isRunning() && claimedDepositBlocks != null && claimedDepositBlocks.positions().isEmpty()) {
            checkDepositBlockAreaChanges();
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
    public @Nullable OperatingSublevel getSublevel() {
        var level = getLevel();
        var pos = getPos();
        if (level == null || pos == null) return null;
        return OperatingSublevelAdapterHolder.getAdapter().getOperatingSublevel(level, pos);
    }

    @Override
    public @Nullable ClaimedDepositBlocks getClaimedDepositBlocks() {
        return claimedDepositBlocks;
    }

    @Override
    public void setClaimedDepositBlocks(@Nullable ClaimedDepositBlocks claimedBlocks) {
        var level = getLevel();
        if (level == null || (claimedBlocks == null && claimedDepositBlocks == null)) return;
        if (claimedBlocks != null && claimedBlocks.equals(claimedDepositBlocks)) return;

        if (level.isClientSide) DepositClaimerOutlineRenderer.removeClaimer(this);
        claimedDepositBlocks = claimedBlocks;
        onClaimedBlocksChanged();
        if (level.isClientSide) DepositClaimerOutlineRenderer.addClaimer(this);
        else sbe.notifyUpdate();
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
}
