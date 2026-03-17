package com.bmaster.createrns.content.deposit.scanning;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.Nullable;

public class DepositScannerLocateContext {
    private static final ThreadLocal<DepositCandidateFilter> FILTER = new ThreadLocal<>();

    private DepositScannerLocateContext() {}

    public static @Nullable DepositCandidateFilter getFilter() {
        return FILTER.get();
    }

    public static Scope push(DepositCandidateFilter filter) {
        FILTER.set(filter);
        return new Scope();
    }

    public static void clear() {
        FILTER.remove();
    }

    @FunctionalInterface
    public interface DepositCandidateFilter {
        boolean shouldIgnore(ServerLevel level, Structure structure, ChunkPos chunkPos);
    }

    public static class Scope implements AutoCloseable {
        private boolean closed;

        @Override
        public void close() {
            if (closed) return;
            clear();
            closed = true;
        }
    }
}
