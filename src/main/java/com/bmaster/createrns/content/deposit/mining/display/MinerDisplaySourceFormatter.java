package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.contraption.ContraptionMiningBehaviour;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
final class MinerDisplaySourceFormatter {
    public static final Component NOT_COMPUTED =
            CreateRNS.translatable("display_source.not_computed");
    public static final Component INFINITE =
            CreateRNS.translatable("display_source.infinite");

    private static final Function<Integer, Component> PERCENTAGE = p ->
            CreateRNS.translatable("display_source.percentage", p);
    private static final long THOUSAND = 1_000;
    private static final long MILLION = 1_000_000;

    static MutableComponent getStatus(
            MinerBearingBlockEntity bearing, ContraptionMiningBehaviour behaviour
    ) {
        if (!bearing.isRunning() || bearing.getMovedContraption() == null) {
            return CreateRNS.translatable("display_source.compact_miner_status.status.not_assembled");
        }
        var claimedBlocks = behaviour.getClaimedDepositBlocks();
        if (claimedBlocks == null || claimedBlocks.isEmpty()) {
            return CreateRNS.translatable("display_source.compact_miner_status.status.out_of_resources");
        }
        if (bearing.getSpeed() != 0 && !bearing.isSpeedRequirementFulfilled()) {
            return CreateRNS.translatable("display_source.compact_miner_status.status.too_slow");
        }
        if (behaviour.isTransferring()) {
            return CreateRNS.translatable("display_source.compact_miner_status.status.transferring");
        }
        if (!behaviour.isMining()) {
            return CreateRNS.translatable("display_source.compact_miner_status.status.stalled");
        }
        return CreateRNS.translatable("display_source.compact_miner_status.status.mining");
    }

    static MutableComponent formatCount(long resources) {
        if (resources < THOUSAND) return Component.literal(Long.toString(resources));

        double scaled = resources / (double) (resources < MILLION ? THOUSAND : MILLION);
        scaled = Math.floor(scaled * 10) / 10;
        String key = "display_source." + (resources < MILLION ? "thousand" : "million");
        return CreateRNS.translatable(key, String.format(Locale.ROOT, "%.1f", scaled));
    }

    static Component formattedPercentage(long current, long total, Component dflt) {
        return percentage(current, total).map(PERCENTAGE).orElse(dflt);
    }

    static Optional<Integer> percentage(long current, long total) {
        if (total == 0) return Optional.empty();
        return Optional.of(Mth.clamp((int) Math.round(current * 100d / total), 0, 100));
    }

    private MinerDisplaySourceFormatter() {
    }
}
