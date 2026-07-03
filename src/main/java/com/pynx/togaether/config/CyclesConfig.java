package com.pynx.togaether.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Cycles du bloc infini, charges depuis config/togaether-cycles.json.
 * Au premier lancement, la config riche embarquee dans le jar (50 niveaux,
 * 56 cycles categorises) est copiee ; a defaut, des valeurs minimales sont ecrites.
 *
 * Un cycle = un pool d'objets (tirage pondere, poids decimaux), une categorie,
 * un niveau minimum, l'XP donnee au bloc par minage, une durete et un outil
 * de predilection. Le niveau du bloc est defini par les seuils "levelThresholds".
 */
public final class CyclesConfig {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "togaether-cycles.json";

    public record Entry(Item item, int count, double weight) {
    }

    /**
     * @param category categorie affichee dans l'interface (navigation a deux niveaux)
     * @param hardness durete du bloc quand ce cycle est actif (pierre = 1.5, obsidienne = 50)
     * @param tool     outil de predilection : pickaxe, axe, shovel, hoe, sword ou any
     */
    public record Cycle(String name, String category, int minLevel, int xpPerMine,
                        float hardness, String tool, List<Entry> entries) {
    }

    private static final Set<String> VALID_TOOLS =
            Set.of("pickaxe", "axe", "shovel", "hoe", "sword", "any");

    private static List<Cycle> cycles = defaults();
    private static long[] levelThresholds = defaultThresholds();

    public static void load() {
        Path file = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                // Config par defaut riche (50 niveaux) embarquee dans le jar
                try (InputStream bundled = CyclesConfig.class.getResourceAsStream("/assets/togaether/default_cycles.json")) {
                    if (bundled != null) {
                        Files.copy(bundled, file);
                        LOGGER.info("[togaether] Config de cycles par defaut (50 niveaux) copiee vers {}", file);
                    }
                }
                if (!Files.exists(file)) {
                    writeDefaults(file);
                    LOGGER.info("[togaether] Fichier de cycles cree : {}", file);
                }
            }
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();

            List<Long> thresholds = new ArrayList<>();
            for (JsonElement el : root.getAsJsonArray("levelThresholds")) {
                thresholds.add(el.getAsLong());
            }
            thresholds.sort(Long::compare);

            List<Cycle> loaded = new ArrayList<>();
            for (JsonElement cycleEl : root.getAsJsonArray("cycles")) {
                JsonObject cycleObj = cycleEl.getAsJsonObject();
                String name = cycleObj.get("name").getAsString();
                String category = cycleObj.has("category") ? cycleObj.get("category").getAsString() : "Divers";
                int minLevel = cycleObj.has("minLevel") ? cycleObj.get("minLevel").getAsInt() : 1;
                int xpPerMine = cycleObj.has("xpPerMine") ? cycleObj.get("xpPerMine").getAsInt() : 1;
                float hardness = cycleObj.has("hardness") ? cycleObj.get("hardness").getAsFloat() : 1.5F;
                String tool = cycleObj.has("tool")
                        ? cycleObj.get("tool").getAsString().toLowerCase(Locale.ROOT) : "pickaxe";
                if (!VALID_TOOLS.contains(tool)) {
                    LOGGER.warn("[togaether] Outil inconnu '{}' pour le cycle {}, 'any' utilise", tool, name);
                    tool = "any";
                }
                List<Entry> entries = new ArrayList<>();
                for (JsonElement entryEl : cycleObj.getAsJsonArray("entries")) {
                    JsonObject entryObj = entryEl.getAsJsonObject();
                    ResourceLocation id = ResourceLocation.parse(entryObj.get("item").getAsString());
                    Item item = BuiltInRegistries.ITEM.get(id);
                    if (item == Items.AIR) {
                        LOGGER.warn("[togaether] Item inconnu ignore dans le cycle {} : {}", name, id);
                        continue;
                    }
                    int count = entryObj.has("count") ? entryObj.get("count").getAsInt() : 1;
                    double weight = entryObj.has("weight") ? entryObj.get("weight").getAsDouble() : 1.0;
                    if (weight <= 0) {
                        LOGGER.warn("[togaether] Poids invalide ({}) pour {} dans le cycle {}, ignore", weight, id, name);
                        continue;
                    }
                    entries.add(new Entry(item, Math.max(1, count), weight));
                }
                if (!entries.isEmpty()) {
                    loaded.add(new Cycle(name, category, Math.max(1, minLevel), Math.max(0, xpPerMine),
                            Math.max(0.05F, hardness), tool, entries));
                }
            }
            if (!loaded.isEmpty() && !thresholds.isEmpty()) {
                cycles = List.copyOf(loaded);
                levelThresholds = thresholds.stream().mapToLong(Long::longValue).toArray();
                LOGGER.info("[togaether] {} cycles et {} niveaux charges", cycles.size(), levelThresholds.length);
            } else {
                LOGGER.warn("[togaether] Config de cycles vide, valeurs par defaut utilisees");
                cycles = defaults();
                levelThresholds = defaultThresholds();
            }
        } catch (Exception e) {
            LOGGER.error("[togaether] Impossible de lire {}, valeurs par defaut utilisees", file, e);
            cycles = defaults();
            levelThresholds = defaultThresholds();
        }
    }

    public static List<Cycle> cycles() {
        return cycles;
    }

    /** Niveau du bloc pour une XP donnee (1..maxLevel). */
    public static int levelFor(long xp) {
        int level = 0;
        for (long threshold : levelThresholds) {
            if (xp >= threshold) {
                level++;
            }
        }
        return Math.max(1, level);
    }

    public static int maxLevel() {
        return levelThresholds.length;
    }

    /** XP cumulee necessaire pour le niveau suivant, ou -1 si niveau max. */
    public static long nextLevelXp(long xp) {
        int level = levelFor(xp);
        return level >= levelThresholds.length ? -1 : levelThresholds[level];
    }

    @Nullable
    public static Cycle byName(String name) {
        for (Cycle cycle : cycles) {
            if (cycle.name().equals(name)) {
                return cycle;
            }
        }
        return null;
    }

    /**
     * Cycle effectif : le cycle selectionne s'il existe et est debloque,
     * sinon le premier cycle debloque de la liste.
     */
    @Nullable
    public static Cycle resolveCycle(String selectedName, int level) {
        Cycle selected = byName(selectedName);
        if (selected != null && selected.minLevel() <= level) {
            return selected;
        }
        for (Cycle cycle : cycles) {
            if (cycle.minLevel() <= level) {
                return cycle;
            }
        }
        return null;
    }

    /** Tirage pondere dans le pool du cycle (les poids sont des decimaux libres). */
    public static ItemStack roll(RandomSource random, Cycle cycle) {
        double totalWeight = 0;
        for (Entry entry : cycle.entries()) {
            totalWeight += entry.weight();
        }
        if (totalWeight <= 0) {
            return ItemStack.EMPTY;
        }
        double pick = random.nextDouble() * totalWeight;
        for (Entry entry : cycle.entries()) {
            pick -= entry.weight();
            if (pick < 0) {
                return new ItemStack(entry.item(), entry.count());
            }
        }
        return ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------

    private static void writeDefaults(Path file) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject root = new JsonObject();

        JsonArray thresholds = new JsonArray();
        for (long t : defaultThresholds()) {
            thresholds.add(t);
        }
        root.add("levelThresholds", thresholds);

        JsonArray cyclesJson = new JsonArray();
        for (Cycle cycle : defaults()) {
            JsonObject cycleObj = new JsonObject();
            cycleObj.addProperty("name", cycle.name());
            cycleObj.addProperty("category", cycle.category());
            cycleObj.addProperty("minLevel", cycle.minLevel());
            cycleObj.addProperty("xpPerMine", cycle.xpPerMine());
            cycleObj.addProperty("hardness", cycle.hardness());
            cycleObj.addProperty("tool", cycle.tool());
            JsonArray entriesJson = new JsonArray();
            for (Entry entry : cycle.entries()) {
                JsonObject entryObj = new JsonObject();
                entryObj.addProperty("item", BuiltInRegistries.ITEM.getKey(entry.item()).toString());
                entryObj.addProperty("count", entry.count());
                entryObj.addProperty("weight", entry.weight());
                entriesJson.add(entryObj);
            }
            cycleObj.add("entries", entriesJson);
            cyclesJson.add(cycleObj);
        }
        root.add("cycles", cyclesJson);
        Files.createDirectories(file.getParent());
        Files.writeString(file, gson.toJson(root));
    }

    private static long[] defaultThresholds() {
        return new long[]{0, 30, 100, 300, 600, 1000};
    }

    /** Petit jeu de secours si la config embarquee est introuvable. */
    private static List<Cycle> defaults() {
        return List.of(
                new Cycle("Basics", "Nature", 1, 1, 1.0F, "axe", List.of(
                        new Entry(Items.OAK_LOG, 1, 30),
                        new Entry(Items.DIRT, 1, 25),
                        new Entry(Items.COBBLESTONE, 1, 30),
                        new Entry(Items.OAK_SAPLING, 1, 0.1),
                        new Entry(Items.WHEAT_SEEDS, 1, 0.25),
                        new Entry(Items.BONE_MEAL, 1, 6))),
                new Cycle("Farm", "Nature", 2, 1, 0.8F, "hoe", List.of(
                        new Entry(Items.WATER_BUCKET, 1, 0.5),
                        new Entry(Items.LAVA_BUCKET, 1, 1),
                        new Entry(Items.SAND, 1, 30),
                        new Entry(Items.GRAVEL, 1, 30),
                        new Entry(Items.CLAY_BALL, 2, 20),
                        new Entry(Items.SUGAR_CANE, 1, 0.1),
                        new Entry(Items.PUMPKIN_SEEDS, 1, 0.15),
                        new Entry(Items.MELON_SEEDS, 1, 0.15),
                        new Entry(Items.SWEET_BERRIES, 1, 0.1),
                        new Entry(Items.COAL, 1, 8),
                        new Entry(Items.FLINT, 1, 8))),
                new Cycle("Mines", "Mining", 3, 2, 2.0F, "pickaxe", List.of(
                        new Entry(Items.COBBLESTONE, 2, 80),
                        new Entry(Items.COAL_ORE, 1, 4),
                        new Entry(Items.IRON_ORE, 1, 3),
                        new Entry(Items.COPPER_ORE, 1, 4),
                        new Entry(Items.GOLD_ORE, 1, 2.5),
                        new Entry(Items.REDSTONE_ORE, 1, 3),
                        new Entry(Items.LAPIS_ORE, 1, 2.5),
                        new Entry(Items.ICE, 1, 5),
                        new Entry(Items.MOSSY_COBBLESTONE, 1, 4),
                        new Entry(Items.BAMBOO, 1, 0.1),
                        new Entry(Items.CACTUS, 1, 0.1),
                        new Entry(Items.COW_SPAWN_EGG, 1, 0.03),
                        new Entry(Items.SHEEP_SPAWN_EGG, 1, 0.03),
                        new Entry(Items.PIG_SPAWN_EGG, 1, 0.03),
                        new Entry(Items.CHICKEN_SPAWN_EGG, 1, 0.03))),
                new Cycle("Nether", "Nether", 4, 3, 2.5F, "pickaxe", List.of(
                        new Entry(Items.NETHERRACK, 2, 60),
                        new Entry(Items.OBSIDIAN, 1, 6),
                        new Entry(Items.SOUL_SAND, 1, 20),
                        new Entry(Items.NETHER_WART, 1, 0.05),
                        new Entry(Items.GLOWSTONE, 1, 3),
                        new Entry(Items.NETHER_QUARTZ_ORE, 1, 3),
                        new Entry(Items.BLAZE_ROD, 1, 2),
                        new Entry(Items.MAGMA_BLOCK, 1, 20),
                        new Entry(Items.CRYING_OBSIDIAN, 1, 2),
                        new Entry(Items.GHAST_TEAR, 1, 1),
                        new Entry(Items.VILLAGER_SPAWN_EGG, 1, 0.01))),
                new Cycle("Treasures", "Special", 5, 4, 3.0F, "pickaxe", List.of(
                        new Entry(Items.DIAMOND_ORE, 1, 0.8),
                        new Entry(Items.EMERALD_ORE, 1, 1),
                        new Entry(Items.ANCIENT_DEBRIS, 1, 0.5),
                        new Entry(Items.ENDER_PEARL, 1, 5),
                        new Entry(Items.EXPERIENCE_BOTTLE, 2, 6),
                        new Entry(Items.CHEST, 1, 4),
                        new Entry(Items.AMETHYST_SHARD, 1, 5),
                        new Entry(Items.NAME_TAG, 1, 0.3),
                        new Entry(Items.SADDLE, 1, 0.05))),
                new Cycle("End", "End", 6, 5, 3.0F, "pickaxe", List.of(
                        new Entry(Items.END_STONE, 2, 60),
                        new Entry(Items.ENDER_EYE, 1, 3),
                        new Entry(Items.END_PORTAL_FRAME, 1, 1.5),
                        new Entry(Items.CHORUS_FLOWER, 1, 0.05),
                        new Entry(Items.SHULKER_SHELL, 1, 1),
                        new Entry(Items.DRAGON_BREATH, 1, 1),
                        new Entry(Items.ELYTRA, 1, 0.5))));
    }

    private CyclesConfig() {
    }
}
