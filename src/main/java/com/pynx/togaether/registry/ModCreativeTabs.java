package com.pynx.togaether.registry;

import com.pynx.togaether.TogaetherMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TogaetherMod.MODID);

    static {
        TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + TogaetherMod.MODID))
                .icon(() -> new ItemStack(ModItems.SKY_KEY.get()))
                .displayItems((parameters, output) -> {
                    output.accept(ModItems.SKY_KEY.get());
                    output.accept(ModItems.INFINITE_BLOCK_ITEM.get());
                    output.accept(ModItems.XP_SHARD_1.get());
                    output.accept(ModItems.XP_SHARD_10.get());
                    output.accept(ModItems.XP_SHARD_1000.get());
                    output.accept(ModItems.XP_SHARD_10000.get());
                })
                .build());
    }

    private ModCreativeTabs() {
    }
}
