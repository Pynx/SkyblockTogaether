package com.pynx.togaether.item;

import com.pynx.togaether.TogaetherMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** BlockItem du bloc infini : affiche l'XP/niveau/cycle stockes quand le bloc a ete ramasse. */
public class InfiniteBlockItem extends BlockItem {

    public InfiniteBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag tag = data.copyTag();
            if (tag.contains("Xp")) {
                tooltip.add(Component.translatable("item." + TogaetherMod.MODID + ".infinite_block.stored",
                        tag.getInt("Level"), tag.getLong("Xp")).withStyle(ChatFormatting.AQUA));
                String cycle = tag.getString("Cycle");
                if (!cycle.isEmpty()) {
                    tooltip.add(Component.translatable("item." + TogaetherMod.MODID + ".infinite_block.stored_cycle",
                            cycle).withStyle(ChatFormatting.GRAY));
                }
            }
        }
        tooltip.add(Component.translatable("item." + TogaetherMod.MODID + ".infinite_block.tooltip")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
