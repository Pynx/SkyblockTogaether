package com.pynx.togaether.client;

import com.pynx.togaether.TogaetherMod;
import com.pynx.togaether.network.InfiniteBlockScreenPayload;
import com.pynx.togaether.network.InfiniteBlockScreenPayload.CycleInfo;
import com.pynx.togaether.network.InfiniteBlockScreenPayload.EntryInfo;
import com.pynx.togaether.network.SelectCyclePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Interface du bloc infini.
 * Gauche : navigation a deux niveaux — liste des categories (avec compteur de
 * cycles debloques), puis cycles de la categorie ouverte (avec bouton retour).
 * Droite : detail du cycle consulte (icones, quantites, taux colores par rarete,
 * outil et durete) + bouton d'activation.
 */
public class InfiniteBlockScreen extends Screen {

    private static final int TOP = 64;
    private static final int BUTTON_HEIGHT = 20;

    private static final int CYCLE_SPACING = 22;
    private static final int LIST_ROWS = 7;
    private static final int LIST_WIDTH = 150;

    private static final int PANEL_WIDTH = 225;
    private static final int ROW_HEIGHT = 18;
    private static final int VISIBLE_ROWS = 7;
    private static final int PANEL_HEADER = 28;

    private final InfiniteBlockScreenPayload data;
    private final List<String> categories;
    private String selected;
    private int viewedIndex;
    /** Categorie ouverte, ou null pour la liste des categories. */
    private String openCategory;
    private int listScroll = 0;
    private int entryScroll = 0;

    public InfiniteBlockScreen(InfiniteBlockScreenPayload data) {
        super(Component.translatable("screen." + TogaetherMod.MODID + ".infinite_block.title"));
        this.data = data;
        this.selected = data.selectedCycle();

        LinkedHashSet<String> cats = new LinkedHashSet<>();
        for (CycleInfo cycle : data.cycles()) {
            cats.add(cycle.category());
        }
        this.categories = List.copyOf(cats);

        this.viewedIndex = 0;
        for (int i = 0; i < data.cycles().size(); i++) {
            if (data.cycles().get(i).name().equals(this.selected)) {
                this.viewedIndex = i;
                break;
            }
        }
        // Ouvre directement la categorie du cycle actif
        this.openCategory = this.data.cycles().isEmpty() ? null
                : this.data.cycles().get(this.viewedIndex).category();
        this.listScroll = Math.max(0, this.indexInOpenCategory(this.viewedIndex) - (LIST_ROWS - 1) / 2);
    }

    // ------------------------------------------------------------------
    // Geometrie
    // ------------------------------------------------------------------

    private int listX() {
        return this.width / 2 - 195;
    }

    private int panelX() {
        return this.width / 2 - 30;
    }

    private int panelHeight() {
        return PANEL_HEADER + VISIBLE_ROWS * ROW_HEIGHT;
    }

    private int listPagerY() {
        return TOP + LIST_ROWS * CYCLE_SPACING + 2;
    }

    private int panelBottomY() {
        return TOP + this.panelHeight() + 6;
    }

    // ------------------------------------------------------------------
    // Donnees de la liste courante
    // ------------------------------------------------------------------

    private CycleInfo viewedCycle() {
        return this.data.cycles().get(this.viewedIndex);
    }

    /** Indices (globaux) des cycles de la categorie ouverte. */
    private List<Integer> cyclesInOpenCategory() {
        List<Integer> indices = new ArrayList<>();
        if (this.openCategory != null) {
            List<CycleInfo> cycles = this.data.cycles();
            for (int i = 0; i < cycles.size(); i++) {
                if (cycles.get(i).category().equals(this.openCategory)) {
                    indices.add(i);
                }
            }
        }
        return indices;
    }

    private int indexInOpenCategory(int globalIndex) {
        List<Integer> indices = this.cyclesInOpenCategory();
        for (int i = 0; i < indices.size(); i++) {
            if (indices.get(i) == globalIndex) {
                return i;
            }
        }
        return 0;
    }

    /** Nombre de lignes de cycles visibles (une ligne est reservee au bouton retour). */
    private int visibleCycleRows() {
        return LIST_ROWS - 1;
    }

    private int currentListSize() {
        return this.openCategory == null ? this.categories.size() : this.cyclesInOpenCategory().size();
    }

    private int currentListVisible() {
        return this.openCategory == null ? LIST_ROWS : this.visibleCycleRows();
    }

    private int maxListScroll() {
        return Math.max(0, this.currentListSize() - this.currentListVisible());
    }

    private int maxEntryScroll() {
        return Math.max(0, this.viewedCycle().entries().size() - VISIBLE_ROWS);
    }

    private boolean categoryContainsSelected(String category) {
        for (CycleInfo cycle : this.data.cycles()) {
            if (cycle.category().equals(category) && cycle.name().equals(this.selected)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Widgets
    // ------------------------------------------------------------------

    @Override
    protected void init() {
        this.listScroll = Math.max(0, Math.min(this.listScroll, this.maxListScroll()));

        if (this.openCategory == null) {
            this.initCategoryList();
        } else {
            this.initCycleList();
        }

        // Pagination de la liste (saut d'une page)
        if (this.currentListSize() > this.currentListVisible()) {
            int page = this.currentListVisible();
            Button up = Button.builder(Component.literal("^"), b -> this.scrollList(-page))
                    .bounds(this.listX() + LIST_WIDTH - 44, this.listPagerY(), 20, BUTTON_HEIGHT)
                    .build();
            up.active = this.listScroll > 0;
            this.addRenderableWidget(up);

            Button down = Button.builder(Component.literal("v"), b -> this.scrollList(page))
                    .bounds(this.listX() + LIST_WIDTH - 20, this.listPagerY(), 20, BUTTON_HEIGHT)
                    .build();
            down.active = this.listScroll < this.maxListScroll();
            this.addRenderableWidget(down);
        }

        // --- Panneau de detail ---
        CycleInfo viewed = this.viewedCycle();
        this.entryScroll = Math.max(0, Math.min(this.entryScroll, this.maxEntryScroll()));

        int bottomY = this.panelBottomY();
        boolean entryPager = viewed.entries().size() > VISIBLE_ROWS;
        int activateWidth = entryPager ? PANEL_WIDTH - 50 : PANEL_WIDTH;

        Button activate = Button.builder(
                        Component.translatable("screen." + TogaetherMod.MODID + ".infinite_block.activate"),
                        b -> this.select(viewed))
                .bounds(this.panelX(), bottomY, activateWidth, BUTTON_HEIGHT)
                .build();
        activate.active = this.data.level() >= viewed.minLevel() && !viewed.name().equals(this.selected);
        this.addRenderableWidget(activate);

        if (entryPager) {
            Button up = Button.builder(Component.literal("^"), b -> this.scrollEntries(-VISIBLE_ROWS))
                    .bounds(this.panelX() + PANEL_WIDTH - 44, bottomY, 20, BUTTON_HEIGHT)
                    .build();
            up.active = this.entryScroll > 0;
            this.addRenderableWidget(up);

            Button down = Button.builder(Component.literal("v"), b -> this.scrollEntries(VISIBLE_ROWS))
                    .bounds(this.panelX() + PANEL_WIDTH - 20, bottomY, 20, BUTTON_HEIGHT)
                    .build();
            down.active = this.entryScroll < this.maxEntryScroll();
            this.addRenderableWidget(down);
        }

        // Fermer
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(this.width / 2 - 50, bottomY + 26, 100, BUTTON_HEIGHT)
                .build());
    }

    private void initCategoryList() {
        int y = TOP;
        int end = Math.min(this.listScroll + LIST_ROWS, this.categories.size());
        for (int i = this.listScroll; i < end; i++) {
            String category = this.categories.get(i);
            int count = 0;
            int unlocked = 0;
            for (CycleInfo cycle : this.data.cycles()) {
                if (cycle.category().equals(category)) {
                    count++;
                    if (this.data.level() >= cycle.minLevel()) {
                        unlocked++;
                    }
                }
            }

            MutableComponent label = Component.literal(category + " (" + unlocked + "/" + count + ")");
            if (this.categoryContainsSelected(category)) {
                label = label.withStyle(ChatFormatting.GREEN);
            } else if (unlocked == 0) {
                label = label.withStyle(ChatFormatting.GRAY);
            }

            Button button = Button.builder(label, b -> this.openCategory(category))
                    .bounds(this.listX(), y, LIST_WIDTH, BUTTON_HEIGHT)
                    .build();
            this.addRenderableWidget(button);
            y += CYCLE_SPACING;
        }
    }

    private void initCycleList() {
        // Bouton retour vers les categories
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen." + TogaetherMod.MODID + ".infinite_block.back"),
                        b -> this.closeCategory())
                .bounds(this.listX(), TOP, LIST_WIDTH, BUTTON_HEIGHT)
                .build());

        List<Integer> indices = this.cyclesInOpenCategory();
        int y = TOP + CYCLE_SPACING;
        int end = Math.min(this.listScroll + this.visibleCycleRows(), indices.size());
        for (int i = this.listScroll; i < end; i++) {
            int globalIndex = indices.get(i);
            CycleInfo cycle = this.data.cycles().get(globalIndex);
            boolean isSelected = cycle.name().equals(this.selected);
            boolean isViewed = globalIndex == this.viewedIndex;

            MutableComponent label = Component.literal(cycle.name());
            if (isSelected) {
                label = label.withStyle(ChatFormatting.GREEN);
            } else if (this.data.level() < cycle.minLevel()) {
                label = label.withStyle(ChatFormatting.GRAY);
            }
            if (isViewed) {
                label = Component.literal("> ").append(label);
            }

            Button button = Button.builder(label, b -> this.view(globalIndex))
                    .bounds(this.listX(), y, LIST_WIDTH, BUTTON_HEIGHT)
                    .build();
            button.active = !isViewed;
            this.addRenderableWidget(button);
            y += CYCLE_SPACING;
        }
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void openCategory(String category) {
        this.openCategory = category;
        this.listScroll = 0;
        this.rebuildWidgets();
    }

    private void closeCategory() {
        String current = this.openCategory;
        this.openCategory = null;
        this.listScroll = Math.max(0, this.categories.indexOf(current) - LIST_ROWS / 2);
        this.rebuildWidgets();
    }

    private void view(int globalIndex) {
        this.viewedIndex = globalIndex;
        this.entryScroll = 0;
        this.rebuildWidgets();
    }

    private void scrollList(int delta) {
        int next = Math.max(0, Math.min(this.listScroll + delta, this.maxListScroll()));
        if (next != this.listScroll) {
            this.listScroll = next;
            this.rebuildWidgets();
        }
    }

    private void scrollEntries(int delta) {
        int next = Math.max(0, Math.min(this.entryScroll + delta, this.maxEntryScroll()));
        if (next != this.entryScroll) {
            this.entryScroll = next;
            this.rebuildWidgets();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (scrollY != 0) {
            int direction = scrollY > 0 ? -1 : 1;
            if (mouseX >= this.panelX() - 4) {
                this.scrollEntries(direction);
            } else {
                this.scrollList(direction);
            }
            return true;
        }
        return false;
    }

    private void select(CycleInfo cycle) {
        this.selected = cycle.name();
        PacketDistributor.sendToServer(new SelectCyclePayload(this.data.pos(), cycle.name()));
        this.rebuildWidgets();
    }

    // ------------------------------------------------------------------
    // Rendu
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // En-tete global
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        Component stats = Component.translatable(
                "screen." + TogaetherMod.MODID + ".infinite_block.stats",
                this.data.level(), this.data.xp());
        guiGraphics.drawCenteredString(this.font, stats, this.width / 2, 32, 0xA0E0FF);
        Component next = this.data.nextLevelXp() < 0
                ? Component.translatable("screen." + TogaetherMod.MODID + ".infinite_block.max_level")
                : Component.translatable("screen." + TogaetherMod.MODID + ".infinite_block.next_level",
                        this.data.nextLevelXp());
        guiGraphics.drawCenteredString(this.font, next, this.width / 2, 44, 0x909090);

        // Indicateur de page de la liste
        int size = this.currentListSize();
        if (size > this.currentListVisible()) {
            String page = (this.listScroll + 1) + "-"
                    + Math.min(this.listScroll + this.currentListVisible(), size)
                    + " / " + size;
            guiGraphics.drawString(this.font, page, this.listX() + 2, this.listPagerY() + 6, 0x909090);
        }

        // Panneau de detail
        CycleInfo cycle = this.viewedCycle();
        int px = this.panelX();
        int py = TOP;
        guiGraphics.fill(px - 4, py - 4, px + PANEL_WIDTH + 4, py + this.panelHeight(), 0x90000000);

        Component header = Component.translatable(
                "screen." + TogaetherMod.MODID + ".infinite_block.cycle",
                cycle.name(), cycle.minLevel(), cycle.xpPerMine());
        guiGraphics.drawString(this.font, header, px, py + 2, 0xFFD770);

        Component toolLine = Component.translatable(
                "screen." + TogaetherMod.MODID + ".infinite_block.tool",
                Component.translatable("tool." + TogaetherMod.MODID + "." + cycle.tool()),
                String.format(Locale.ROOT, "%.1f", cycle.hardness()));
        guiGraphics.drawString(this.font, toolLine, px, py + 14, 0xB0B0B0);

        double totalWeight = 0;
        for (EntryInfo entry : cycle.entries()) {
            totalWeight += entry.weight();
        }

        List<EntryInfo> entries = cycle.entries();
        int rows = Math.min(VISIBLE_ROWS, entries.size() - this.entryScroll);
        for (int row = 0; row < rows; row++) {
            EntryInfo entry = entries.get(this.entryScroll + row);
            int rowY = py + PANEL_HEADER + row * ROW_HEIGHT;

            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.itemId()));
            ItemStack stack = new ItemStack(item == Items.AIR ? Items.BARRIER : item, entry.count());
            guiGraphics.renderItem(stack, px + 2, rowY);
            if (entry.count() > 1) {
                guiGraphics.renderItemDecorations(this.font, stack, px + 2, rowY);
            }

            double percent = totalWeight > 0 ? entry.weight() / totalWeight * 100.0 : 0;
            int color = rarityColor(percent);

            Component name = item == Items.AIR
                    ? Component.literal(entry.itemId())
                    : item.getDescription();
            guiGraphics.drawString(this.font, name, px + 24, rowY + 4, color);

            String percentText = formatPercent(percent) + "%";
            guiGraphics.drawString(this.font, percentText,
                    px + PANEL_WIDTH - this.font.width(percentText) - 2, rowY + 4, color);
        }
    }

    /**
     * Couleur du nom selon la rarete du drop :
     * < 0.1% orange (legendaire), 0.1-1% mauve (epique), 1-5% bleu (rare),
     * 5-15% vert (peu commun), >= 15% blanc (commun).
     */
    private static int rarityColor(double percent) {
        if (percent < 0.1) {
            return 0xFFAA00;
        }
        if (percent < 1) {
            return 0xC24DFF;
        }
        if (percent < 5) {
            return 0x5C8BFF;
        }
        if (percent < 15) {
            return 0x6BE36B;
        }
        return 0xFFFFFF;
    }

    private static String formatPercent(double percent) {
        if (percent >= 10) {
            return String.format(Locale.ROOT, "%.0f", percent);
        }
        if (percent >= 1) {
            return String.format(Locale.ROOT, "%.1f", percent);
        }
        return String.format(Locale.ROOT, "%.2f", percent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
