package com.pynx.togaether.network;

import com.pynx.togaether.TogaetherMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Serveur -> client : ouvre l'interface du bloc infini avec son etat. */
public record InfiniteBlockScreenPayload(BlockPos pos, long xp, int level, long nextLevelXp,
                                         String selectedCycle, List<CycleInfo> cycles)
        implements CustomPacketPayload {

    public record EntryInfo(String itemId, int count, double weight) {
    }

    public record CycleInfo(String name, String category, int minLevel, int xpPerMine, float hardness, String tool,
                            List<EntryInfo> entries) {
    }

    public static final Type<InfiniteBlockScreenPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TogaetherMod.MODID, "infinite_block_screen"));

    public static final StreamCodec<FriendlyByteBuf, InfiniteBlockScreenPayload> STREAM_CODEC =
            StreamCodec.of(InfiniteBlockScreenPayload::write, InfiniteBlockScreenPayload::read);

    private static void write(FriendlyByteBuf buf, InfiniteBlockScreenPayload payload) {
        buf.writeBlockPos(payload.pos());
        buf.writeVarLong(payload.xp());
        buf.writeVarInt(payload.level());
        buf.writeLong(payload.nextLevelXp());
        buf.writeUtf(payload.selectedCycle());
        buf.writeVarInt(payload.cycles().size());
        for (CycleInfo cycle : payload.cycles()) {
            buf.writeUtf(cycle.name());
            buf.writeUtf(cycle.category());
            buf.writeVarInt(cycle.minLevel());
            buf.writeVarInt(cycle.xpPerMine());
            buf.writeFloat(cycle.hardness());
            buf.writeUtf(cycle.tool());
            buf.writeVarInt(cycle.entries().size());
            for (EntryInfo entry : cycle.entries()) {
                buf.writeUtf(entry.itemId());
                buf.writeVarInt(entry.count());
                buf.writeDouble(entry.weight());
            }
        }
    }

    private static InfiniteBlockScreenPayload read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        long xp = buf.readVarLong();
        int level = buf.readVarInt();
        long nextLevelXp = buf.readLong();
        String selected = buf.readUtf();
        int cycleCount = buf.readVarInt();
        List<CycleInfo> cycles = new ArrayList<>(cycleCount);
        for (int i = 0; i < cycleCount; i++) {
            String name = buf.readUtf();
            String category = buf.readUtf();
            int minLevel = buf.readVarInt();
            int xpPerMine = buf.readVarInt();
            float hardness = buf.readFloat();
            String tool = buf.readUtf();
            int entryCount = buf.readVarInt();
            List<EntryInfo> entries = new ArrayList<>(entryCount);
            for (int j = 0; j < entryCount; j++) {
                entries.add(new EntryInfo(buf.readUtf(), buf.readVarInt(), buf.readDouble()));
            }
            cycles.add(new CycleInfo(name, category, minLevel, xpPerMine, hardness, tool, entries));
        }
        return new InfiniteBlockScreenPayload(pos, xp, level, nextLevelXp, selected, cycles);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
