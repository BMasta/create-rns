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
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class ClaimingBehaviour extends BlockEntityBehaviour implements IDepositBlockClaimer {
    protected final Supplier<Direction> operatingDirection;
    protected boolean crossSublevel = false;
    protected int claimedBlocksVersion = 0;
    protected @Nullable Set<BlockPos> claimedDepositBlocks = null;
    protected @Nullable BlockPos crossSublevelStart = null;

    private final SmartBlockEntity sbe;
    // Defers claimer deserialization
    protected @Nullable Tuple<CompoundTag, Boolean> pendingClaimerTag = null;

    protected boolean validateClaimedDepositBlocks = false;

    public abstract boolean isRunning();

    protected abstract boolean isDepositBlockOperable(BlockPos pos);

    public ClaimingBehaviour(SmartBlockEntity sbe, Supplier<Direction> operatingDirection) {
        super(sbe);
        this.sbe = sbe;
        this.operatingDirection = operatingDirection;
    }

    @Override
    public void initialize() {
        var level = getLevel();
        assert level != null;
    }

    @Override
    public void unload() {
        var level = getLevel();
        assert level != null;

        DepositClaimerInstanceHolder.removeClaimer(this);
        level.invalidateCapabilities(getPos());
        if (level.isClientSide) DepositClaimerOutlineRenderer.removeClaimer(this);
    }

    protected void onClaimedBlocksChanged() {
        claimedBlocksVersion++;
    }

    @Override
    public void claimDepositBlocks() {
        var level = getLevel();
        if (level == null || level.isClientSide || !isRunning()) return;

        DepositClaimerInstanceHolder.removeClaimer(this);

        crossSublevel = false;
        claimedDepositBlocks = getClaimableDepositVein().stream()
                .filter(this::isDepositBlockOperable)
                .collect(Collectors.toSet());

        DepositClaimerInstanceHolder.addClaimer(this);
        onClaimedBlocksChanged();

        var pos = getPos();
        CreateRNS.LOGGER.trace("Operator at {}, {}, {} claimed {} deposit blocks",
                pos.getX(), pos.getY(), pos.getZ(), claimedDepositBlocks.size());

        sbe.notifyUpdate();
    }

    @Override
    public void claimCrossSublevelDepositBlocks() {
        var level = getLevel();
        if (level == null || level.isClientSide || crossSublevelStart == null || !isRunning() ||
                claimedDepositBlocks == null || (!crossSublevel && !claimedDepositBlocks.isEmpty())) return;

        DepositClaimerInstanceHolder.removeClaimer(this);

        crossSublevel = true;
        claimedDepositBlocks = getClaimableDepositVein().stream()
                .filter(this::isDepositBlockOperable)
                .collect(Collectors.toSet());

        DepositClaimerInstanceHolder.addClaimer(this);
        onClaimedBlocksChanged();

        var pos = getPos();
        CreateRNS.LOGGER.trace("Operator at {}, {}, {} claimed {} deposit blocks in another sublevel",
                pos.getX(), pos.getY(), pos.getZ(), claimedDepositBlocks.size());
    }

    @Override
    public void write(CompoundTag nbt, Provider provider, boolean clientPacket) {
        super.write(nbt, provider, clientPacket);

        var claimer = new CompoundTag();
        var claimedBlocks = getClaimedDepositBlocks();

        if (claimedBlocks != null) {
            claimer.putLongArray("claimed_blocks", claimedBlocks.stream()
                    .mapToLong(BlockPos::asLong)
                    .toArray());
            var start = getOperatingStart();
            if (start != null) {
                claimer.putBoolean("cross_sublevel", crossSublevel);
                claimer.putLong("start", start.asLong());
            }
        }
        if (clientPacket) {
            claimer.putInt("claimed_blocks_version", claimedBlocksVersion);
        }

        nbt.put("claimer", claimer);
    }

    @Override
    public void read(CompoundTag nbt, Provider provider, boolean clientPacket) {
        super.read(nbt, provider, clientPacket);

        if (!(nbt.get("claimer") instanceof CompoundTag cTag)) return;
        pendingClaimerTag = new Tuple<>(cTag, clientPacket);
    }

    private void readDeferred() {
        var level = getLevel();
        if (pendingClaimerTag == null || level == null) return;
        var cTag = pendingClaimerTag.getA();
        boolean clientPacket = pendingClaimerTag.getB();
        pendingClaimerTag = null;

        // If the tag exists (even if empty), the claimer has finished claiming.
        Set<BlockPos> newlyClaimedBlocks = null;
        if (cTag.get("claimed_blocks") instanceof LongArrayTag claimedArr) {
            newlyClaimedBlocks = claimedArr.stream()
                    .map(a -> BlockPos.of(a.getAsLong()))
                    .collect(Collectors.toSet());
        }
        boolean cs = newlyClaimedBlocks != null && cTag.contains("start") && cTag.getBoolean("cross_sublevel");
        if (cs) crossSublevelStart = BlockPos.of(cTag.getLong("start"));

        boolean hasVersion = cTag.contains("claimed_blocks_version");
        int version = cTag.getInt("claimed_blocks_version");
        if (clientPacket && hasVersion && claimedBlocksVersion == version && Objects.equals(newlyClaimedBlocks, claimedDepositBlocks)) return;

        // If a miner is moved to a different sublevel, it gets recreated with its nbt copied.
        // In such cases we have to ensure that the previously-claimed deposits are invalidated.
        if (!clientPacket && claimedDepositBlocks == null && newlyClaimedBlocks != null && !cs) {
            validateClaimedDepositBlocks = true;
        }

        setClaimedDepositBlocks(newlyClaimedBlocks, cs);
        if (hasVersion) claimedBlocksVersion = version;
    }

    @Override
    public void tick() {
        super.tick();
        var level = getLevel();
        if (level == null) return;

        if (pendingClaimerTag != null) readDeferred();

        if (level.isClientSide) return;

        // Validate that the claimed deposit blocks are in the same sublevel as the miner
        if (claimedDepositBlocks != null && validateClaimedDepositBlocks) {
            validateClaimedDepositBlocks = false;
            var adapter = OperatingSublevelAdapterHolder.getAdapter();
            if (claimedDepositBlocks.isEmpty() || !adapter.isSameSublevel(level, getPos(),
                    level, claimedDepositBlocks.iterator().next())) {
                setClaimedDepositBlocks(null, false);
            }
        }

        if (claimedDepositBlocks == null) {
            claimDepositBlocks();
        }

        refreshCrossSublevelClaim();
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
        return OperatingSublevel.of(level, pos);
    }

    @Override
    public boolean isOperatingCrossSublevel() {
        return crossSublevel;
    }

    @Override
    public @Nullable Set<BlockPos> getClaimedDepositBlocks() {
        return claimedDepositBlocks;
    }

    @Override
    public void setClaimedDepositBlocks(@Nullable Set<BlockPos> claimedBlocks, boolean crossSublevel) {
        var level = getLevel();
        if (level == null) return;

        DepositClaimerInstanceHolder.removeClaimer(this);
        if (level.isClientSide) DepositClaimerOutlineRenderer.removeClaimer(this);

        claimedDepositBlocks = claimedBlocks;
        this.crossSublevel = crossSublevel;
        onClaimedBlocksChanged();

        DepositClaimerInstanceHolder.addClaimer(this);
        if (level.isClientSide) DepositClaimerOutlineRenderer.addClaimer(this);

        else sbe.notifyUpdate();
    }

    protected void refreshCrossSublevelClaim() {
        var level = getLevel();
        var contact = getOperatingContact();
        var dimensions = getDetectionDimensions();
        if (level == null || level.isClientSide || contact == null || dimensions == null ||
                claimedDepositBlocks == null || (!crossSublevel && !claimedDepositBlocks.isEmpty())) return;

        var direction = getOperatingDirection();
        var adapter = OperatingSublevelAdapterHolder.getAdapter();
        var ownSublevel = OperatingSublevel.of(level, contact);

        var detectedBlocks = adapter.getCrossSublevelDepositBlocks(level, ownSublevel, contact, direction, dimensions);
        if (crossSublevelStart == null && detectedBlocks.isEmpty()) return;
        if (crossSublevelStart != null && crossSublevelStart.equals(detectedBlocks.closest())) return;

        // Operating source changed

        var area = getOperatingBoundingBox();

        // Release existing cross-sublevel claim if needed
        if (!claimedDepositBlocks.isEmpty()) {
            setClaimedDepositBlocks(Set.of(), false);
        }

        // Update operating source
        crossSublevelStart = detectedBlocks.closest();

        // Claim new blocks
        claimCrossSublevelDepositBlocks();

        // Let other miners reclaim the area
        if (area != null) IDepositBlockClaimer.reclaimArea(area, this);
    }
}
