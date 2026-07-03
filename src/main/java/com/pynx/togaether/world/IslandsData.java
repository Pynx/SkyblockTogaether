package com.pynx.togaether.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Donnees persistantes : liste des iles du monde skyblock, avec leur ancre d'origine.
 */
public class IslandsData extends SavedData {

    private static final String DATA_NAME = "togaether_islands";

    public record Island(BlockPos center, BlockPos portalPos, ResourceKey<Level> anchorDim, BlockPos anchorPos) {
    }

    private final List<Island> islands = new ArrayList<>();

    public static IslandsData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(IslandsData::new, IslandsData::load, null), DATA_NAME);
    }

    public void addIsland(Island island) {
        this.islands.add(island);
        this.setDirty();
    }

    @Nullable
    public Island findNearestIsland(int x, int z, long maxDistance) {
        Island best = null;
        long bestDistSq = -1;
        long maxSq = maxDistance >= 100000L ? Long.MAX_VALUE : maxDistance * maxDistance;
        for (Island island : this.islands) {
            long dx = island.center().getX() - x;
            long dz = island.center().getZ() - z;
            long distSq = dx * dx + dz * dz;
            if (distSq <= maxSq && (bestDistSq < 0 || distSq < bestDistSq)) {
                bestDistSq = distSq;
                best = island;
            }
        }
        return best;
    }

    public static IslandsData load(CompoundTag tag, HolderLookup.Provider registries) {
        IslandsData data = new IslandsData();
        ListTag list = tag.getList("Islands", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            BlockPos center = NbtUtils.readBlockPos(entry, "Center").orElse(BlockPos.ZERO);
            BlockPos portal = NbtUtils.readBlockPos(entry, "Portal").orElse(center);
            BlockPos anchor = NbtUtils.readBlockPos(entry, "Anchor").orElse(BlockPos.ZERO);
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.parse(entry.getString("AnchorDim")));
            data.islands.add(new Island(center, portal, dim, anchor));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Island island : this.islands) {
            CompoundTag entry = new CompoundTag();
            entry.put("Center", NbtUtils.writeBlockPos(island.center()));
            entry.put("Portal", NbtUtils.writeBlockPos(island.portalPos()));
            entry.put("Anchor", NbtUtils.writeBlockPos(island.anchorPos()));
            entry.putString("AnchorDim", island.anchorDim().location().toString());
            list.add(entry);
        }
        tag.put("Islands", list);
        return tag;
    }
}
