package com.bmaster.createrns.compat.sable;

import com.bmaster.createrns.content.deposit.operating.space.OperatingSpaceAdapter;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class SableOperatingSpaceAdapter implements OperatingSpaceAdapter {
    public static OperatingSpaceAdapter create() {
        return new SableOperatingSpaceAdapter();
    }

    private SableOperatingSpaceAdapter() {}

    @Override
    public OperatingSpace getOperatingSpace(Level level, BlockPos pos) {
        var subLevel = SableCompanion.INSTANCE.getContaining(level, pos);
        var identity = subLevel == null ? OperatingSpace.MAIN_SPACE : subLevel.getUniqueId().toString();
        return new OperatingSpace(level.dimension(), identity);
    }

    @Override
    public List<DepositGroupCandidate> findRemoteDepositGroup(OperatingSpaceScanContext context) {
        return List.of();
    }
}
