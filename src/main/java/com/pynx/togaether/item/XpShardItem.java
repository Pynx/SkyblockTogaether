package com.pynx.togaether.item;

import com.pynx.togaether.TogaetherMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Objet admin (creatif uniquement, pas de recette) : consomme sur un bloc infini
 * pour lui donner de l'XP. La logique d'application est dans InfiniteBlock#useItemOn.
 */
public class XpShardItem extends Item {

    private final long amount;

    public XpShardItem(long amount, Properties properties) {
        super(properties);
        this.amount = amount;
    }

    public long amount() {
        return this.amount;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item." + TogaetherMod.MODID + ".xp_shard.tooltip", this.amount)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item." + TogaetherMod.MODID + ".xp_shard.tooltip_admin")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
