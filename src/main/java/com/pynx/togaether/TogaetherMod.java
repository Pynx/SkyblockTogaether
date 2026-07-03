package com.pynx.togaether;

import com.pynx.togaether.config.CyclesConfig;
import com.pynx.togaether.config.SkyblockConfig;
import com.pynx.togaether.event.InfiniteBlockEvents;
import com.pynx.togaether.event.PortalEvents;
import com.pynx.togaether.network.ModPayloads;
import com.pynx.togaether.registry.ModBlockEntities;
import com.pynx.togaether.registry.ModBlocks;
import com.pynx.togaether.registry.ModCreativeTabs;
import com.pynx.togaether.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

@Mod(TogaetherMod.MODID)
public class TogaetherMod {

    public static final String MODID = "togaether";

    public TogaetherMod(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, SkyblockConfig.SPEC);
        modEventBus.addListener(ModPayloads::register);

        NeoForge.EVENT_BUS.register(PortalEvents.class);
        NeoForge.EVENT_BUS.register(InfiniteBlockEvents.class);
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent e) -> CyclesConfig.load());
    }
}
