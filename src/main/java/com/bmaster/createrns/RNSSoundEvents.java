package com.bmaster.createrns;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;

public class RNSSoundEvents {
    public static final List<RNSSoundEntry> ALL = new ArrayList<>();

    public static RNSSoundEntry SCANNER_SCROLL = create("scanner_scroll")
            .category(SoundSource.PLAYERS)
            .build();

    public static RNSSoundEntry SCANNER_CLICK = create("scanner_click", 2)
            .category(SoundSource.PLAYERS)
            .build();

    public static RNSSoundEntry SCANNER_DISCOVERY_PING = create("scanner_discovery_ping", 2)
            .category(SoundSource.PLAYERS)
            .build();

    public static RNSSoundEntry SCANNER_DISCOVERY_SUCCESS = create("scanner_discovery_success")
            .category(SoundSource.PLAYERS)
            .build();

    public static RNSSoundEntry SCANNER_TRACKING_PING = create("scanner_tracking_ping")
            .category(SoundSource.PLAYERS)
            .build();

    public static RNSSoundEntry DEPOSIT_FOUND = create("deposit_found")
            .category(SoundSource.PLAYERS)
            .build();

    public static RNSSoundEntry MINING = create("mining", 3)
            .category(SoundSource.AMBIENT)
            .build();

    public static RNSSoundEntry MINING_RESONANCE_ACCENT = create("mining_resonance_accent")
            .category(SoundSource.AMBIENT)
            .build();

    public static RNSSoundEntry MINED = create("mined")
            .category(SoundSource.AMBIENT)
            .build();


    public static RNSSoundEntryBuilder create(String id) {
        return new RNSSoundEntryBuilder(id, 1);
    }

    public static RNSSoundEntryBuilder create(String id, int n_sounds) {
        return new RNSSoundEntryBuilder(id, n_sounds);
    }

    public static void register(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, helper -> {
            for (var e : ALL) {
                e.register(helper);
            }
        });
    }

    public static class RNSSoundEntryBuilder {
        private final ResourceLocation id;
        private final int n_sounds;
        private SoundSource category = SoundSource.MASTER;

        public RNSSoundEntryBuilder(String id, int n_sounds) {
            this.id = CreateRNS.asResource(id);
            this.n_sounds = n_sounds;
        }

        public RNSSoundEntryBuilder category(SoundSource category) {
            this.category = category;
            return this;
        }

        public RNSSoundEntry build() {
            var e = new RNSSoundEntry(id, category, n_sounds);
            ALL.add(e);
            return e;
        }
    }

    public static class RNSSoundEntry {
        public final ResourceLocation id;
        public final SoundSource category;
        public final int n_sounds;
        public List<SoundEvent> events = null;

        public RNSSoundEntry(ResourceLocation id, SoundSource category, int n_sounds) {
            this.id = id;
            this.category = category;
            this.n_sounds = n_sounds;

        }

        public void register(RegisterEvent.RegisterHelper<SoundEvent> helper) {
            this.events = new ArrayList<>(n_sounds);
            for (int i = 0; i < n_sounds; ++i) {
                var curId = getIdOf(i);
                var event = SoundEvent.createVariableRangeEvent(curId);
                this.events.add(event);
                helper.register(curId, event);
            }
        }

        public void playClient(Level level, BlockPos pos, float volume, float pitch, boolean fade) {
            if (events == null || !level.isClientSide) return;
            for (var e : events) {
                level.playLocalSound(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, e, category,
                        volume, pitch, fade);
            }
        }

        public void playClient(Level level, BlockPos pos) {
            playClient(level, pos, 1, 1, true);
        }

        public void playClient(Level level, Vec3 pos, float volume, float pitch, boolean fade) {
            if (events == null || !level.isClientSide) return;
            for (var e : events) {
                level.playLocalSound(pos.x, pos.y, pos.z, e, category, volume, pitch, fade);
            }
        }

        public void playClient(Level level, Vec3 pos) {
            playClient(level, pos, 1, 1, true);
        }

        public void playClient(Level level, float x, float y, float z, float volume, float pitch, boolean fade) {
            if (events == null || !level.isClientSide) return;
            for (var e : events) {
                level.playLocalSound(x, y + 0.5f, z + 0.5f, e, category, volume, pitch, fade);
            }
        }

        public void playClient(Level level, float x, float y, float z) {
            playClient(level, x, y, z, 1, 1, true);
        }

        public void playServer(Level level, BlockPos pos, float volume, float pitch) {
            if (events == null || level.isClientSide) return;
            for (var e : events) {
                level.playSound(null, pos, e, category, volume, pitch);
            }
        }

        public void playServer(Level level, BlockPos pos) {
            playServer(level, pos, 1, 1);
        }

        private ResourceLocation getIdOf(int i) {
            return (i == 0) ? id : ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                    id.getPath() + "_compounded_" + i);
        }
    }
}
