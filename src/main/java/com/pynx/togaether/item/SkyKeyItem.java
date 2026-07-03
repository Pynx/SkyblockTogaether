package com.pynx.togaether.item;

import com.pynx.togaether.ModDimensions;
import com.pynx.togaether.TogaetherMod;
import com.pynx.togaether.portal.SkyPortalShape;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class SkyKeyItem extends Item {

    public SkyKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();

        if (!SkyPortalShape.isFrameBlock(level.getBlockState(clicked))) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();

        if (level.dimension() == ModDimensions.SKYBLOCK_NETHER || level.dimension() == ModDimensions.SKYBLOCK_END) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message." + TogaetherMod.MODID + ".wrong_dimension"), true);
            }
            return InteractionResult.FAIL;
        }

        BlockPos target = clicked.relative(context.getClickedFace());
        Optional<SkyPortalShape> shape = SkyPortalShape.find(level, target);
        if (shape.isEmpty()) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message." + TogaetherMod.MODID + ".invalid_frame"), true);
            }
            return InteractionResult.FAIL;
        }

        shape.get().createPortalBlocks();
        level.playSound(null, target, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.7F, 1.2F);
        if (player != null) {
            player.displayClientMessage(Component.translatable("message." + TogaetherMod.MODID + ".portal_created"), true);
            if (com.pynx.togaether.config.SkyblockConfig.consumeKey() && !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item." + TogaetherMod.MODID + ".sky_key.tooltip"));
    }
}
