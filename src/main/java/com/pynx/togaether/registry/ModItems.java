package com.pynx.togaether.registry;

import com.pynx.togaether.TogaetherMod;
import com.pynx.togaether.item.InfiniteBlockItem;
import com.pynx.togaether.item.SkyKeyItem;
import com.pynx.togaether.item.XpShardItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TogaetherMod.MODID);

    public static final DeferredItem<SkyKeyItem> SKY_KEY = ITEMS.register("sky_key",
            () -> new SkyKeyItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<InfiniteBlockItem> INFINITE_BLOCK_ITEM =
            ITEMS.register("infinite_block",
                    () -> new InfiniteBlockItem(ModBlocks.INFINITE_BLOCK.get(), new Item.Properties()));

    // Objets admin (creatif uniquement) : donnent de l'XP au bloc infini
    public static final DeferredItem<XpShardItem> XP_SHARD_1 = ITEMS.register("xp_shard_1",
            () -> new XpShardItem(1, new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<XpShardItem> XP_SHARD_10 = ITEMS.register("xp_shard_10",
            () -> new XpShardItem(10, new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<XpShardItem> XP_SHARD_1000 = ITEMS.register("xp_shard_1000",
            () -> new XpShardItem(1000, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<XpShardItem> XP_SHARD_10000 = ITEMS.register("xp_shard_10000",
            () -> new XpShardItem(10000, new Item.Properties().rarity(Rarity.EPIC)));

    private ModItems() {
    }
}
