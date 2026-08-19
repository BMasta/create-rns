package com.bmaster.createrns.content.deposit.claiming;

import com.bmaster.createrns.CreateRNS;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
    protected int claimedBlocksVersion = 0;
    protected @Nullable Set<BlockPos> claimedDepositBlocks = null;

    private final SmartBlockEntity sbe;
    // Defers claimer deserialization
    protected @Nullable Tuple<CompoundTag, Boolean> pendingClaimerTag = null;

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
        sbe.invalidateCaps();
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
    public void write(CompoundTag nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);

        var claimer = new CompoundTag();
        var claimedBlocks = getClaimedDepositBlocks();

        if (claimedBlocks != null) {
            claimer.putLongArray("claimed_blocks", claimedBlocks.stream()
                    .mapToLong(BlockPos::asLong)
                    .toArray());
        }
        if (clientPacket) {
            claimer.putInt("claimed_blocks_version", claimedBlocksVersion);
        }

        nbt.put("claimer", claimer);
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);

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

        boolean hasVersion = cTag.contains("claimed_blocks_version");
        int version = cTag.getInt("claimed_blocks_version");
        if (clientPacket && hasVersion && claimedBlocksVersion == version && Objects.equals(newlyClaimedBlocks, claimedDepositBlocks)) return;

        setClaimedDepositBlocks(newlyClaimedBlocks);
        if (hasVersion) claimedBlocksVersion = version;
    }

    @Override
    public void tick() {
        super.tick();
        var level = getLevel();
        if (level == null) return;

        if (pendingClaimerTag != null) readDeferred();

        if (level.isClientSide) return;

        if (claimedDepositBlocks == null) {
            claimDepositBlocks();
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
        var level = getLevel();
        if (level == null) return;

        DepositClaimerInstanceHolder.removeClaimer(this);
        if (level.isClientSide) DepositClaimerOutlineRenderer.removeClaimer(this);

        claimedDepositBlocks = claimedBlocks;
        onClaimedBlocksChanged();

        DepositClaimerInstanceHolder.addClaimer(this);
        if (level.isClientSide) DepositClaimerOutlineRenderer.addClaimer(this);

        else sbe.notifyUpdate();
    }
}
