package com.pynx.togaether.registry;

import com.pynx.togaether.TogaetherMod;
import com.pynx.togaether.block.entity.InfiniteBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TogaetherMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfiniteBlockEntity>> INFINITE_BLOCK =
            BLOCK_ENTITIES.register("infinite_block",
                    () -> BlockEntityType.Builder.of(InfiniteBlockEntity::new, ModBlocks.INFINITE_BLOCK.get()).build(null));

    private ModBlockEntities() {
    }
}
