package com.pynx.togaether.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class SkyblockConfig {

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue ISLAND_HEIGHT;
    private static final ModConfigSpec.IntValue MIN_ISLAND_DISTANCE;
    private static final ModConfigSpec.IntValue NETHER_PORTAL_HEIGHT;
    private static final ModConfigSpec.ConfigValue<String> FRAME_BLOCK;
    private static final ModConfigSpec.BooleanValue CONSUME_KEY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("islands");
        ISLAND_HEIGHT = builder
                .comment("Hauteur (Y) a laquelle les iles flottantes sont generees dans la dimension skyblock.")
                .defineInRange("islandHeight", 120, 16, 300);
        MIN_ISLAND_DISTANCE = builder
                .comment("Distance minimale (en blocs) entre deux iles. Un portail cree a moins de cette distance",
                        "d'une ile existante sera relie a cette ile au lieu d'en creer une nouvelle.")
                .defineInRange("minIslandDistance", 250, 32, 100000);
        builder.pop();

        builder.push("portal");
        FRAME_BLOCK = builder
                .comment("Bloc utilise pour le cadre du portail skyblock (id de bloc).")
                .define("frameBlock", "minecraft:chiseled_stone_bricks");
        CONSUME_KEY = builder
                .comment("Si true, la cle celeste est consommee lors de l'activation d'un portail.")
                .define("consumeKey", false);
        NETHER_PORTAL_HEIGHT = builder
                .comment("Hauteur (Y) des portails du Nether generes dans le nether skyblock.")
                .defineInRange("netherPortalHeight", 64, 8, 240);
        builder.pop();

        SPEC = builder.build();
    }

    public static int islandHeight() {
        return ISLAND_HEIGHT.get();
    }

    public static int minIslandDistance() {
        return MIN_ISLAND_DISTANCE.get();
    }

    public static int netherPortalHeight() {
        return NETHER_PORTAL_HEIGHT.get();
    }

    public static boolean consumeKey() {
        return CONSUME_KEY.get();
    }

    public static Block frameBlock() {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(FRAME_BLOCK.get()));
        return block == Blocks.AIR ? Blocks.CHISELED_STONE_BRICKS : block;
    }

    private SkyblockConfig() {
    }
}
