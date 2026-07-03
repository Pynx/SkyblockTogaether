package com.pynx.togaether.registry;

import com.pynx.togaether.TogaetherMod;
import com.pynx.togaether.block.InfiniteBlock;
import com.pynx.togaether.block.SkyPortalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TogaetherMod.MODID);

    public static final DeferredBlock<SkyPortalBlock> SKY_PORTAL = BLOCKS.register("sky_portal",
            () -> new SkyPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .noCollission()
                    .strength(-1.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 11)
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)));

    public static final DeferredBlock<InfiniteBlock> INFINITE_BLOCK = BLOCKS.register("infinite_block",
            () -> new InfiniteBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIAMOND)
                    .strength(1.2F, 3600000.0F)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 7)
                    .noLootTable()));

    private ModBlocks() {
    }
}
