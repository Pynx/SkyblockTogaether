package com.pynx.togaether;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class ModDimensions {

    public static final ResourceKey<Level> SKYBLOCK =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(TogaetherMod.MODID, "skyblock"));
    public static final ResourceKey<Level> SKYBLOCK_NETHER =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(TogaetherMod.MODID, "skyblock_nether"));
    public static final ResourceKey<Level> SKYBLOCK_END =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(TogaetherMod.MODID, "skyblock_end"));

    public static boolean isSkyblockDimension(ResourceKey<Level> key) {
        return key == SKYBLOCK || key == SKYBLOCK_NETHER || key == SKYBLOCK_END;
    }

    private ModDimensions() {
    }
}
