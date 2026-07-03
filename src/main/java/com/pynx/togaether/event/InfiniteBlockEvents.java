package com.pynx.togaether.event;

import com.pynx.togaether.TogaetherMod;
import com.pynx.togaether.block.entity.InfiniteBlockEntity;
import com.pynx.togaether.config.CyclesConfig;
import com.pynx.togaether.registry.ModBlocks;
import com.pynx.togaether.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Le bloc infini ne se casse jamais : chaque "minage" est annule et remplace
 * par un drop tire du cycle selectionne, et donne l'XP du cycle au bloc.
 * Casser accroupi permet de ramasser le bloc avec son XP.
 */
public final class InfiniteBlockEvents {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.INFINITE_BLOCK.get())) {
            return;
        }

        Player player = event.getPlayer();

        event.setCanceled(true);

        if (!(level.getBlockEntity(pos) instanceof InfiniteBlockEntity be)) {
            return;
        }

        // Accroupi : on ramasse le bloc avec son XP, son niveau et son cycle
        if (player != null && player.isShiftKeyDown()) {
            CompoundTag stored = new CompoundTag();
            stored.putLong("Xp", be.getXp());
            stored.putLong("Mined", be.getMined());
            stored.putInt("Level", be.getXpLevel());
            stored.putString("Cycle", be.getSelectedCycle());
            ItemStack pickup = new ItemStack(ModItems.INFINITE_BLOCK_ITEM.get());
            pickup.set(DataComponents.CUSTOM_DATA, CustomData.of(stored));

            level.removeBlock(pos, false);
            Block.popResource(level, pos, pickup);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
            return;
        }

        be.incrementMined();
        CyclesConfig.Cycle cycle = CyclesConfig.resolveCycle(be.getSelectedCycle(), be.getXpLevel());
        if (cycle == null) {
            return;
        }

        ItemStack drop = CyclesConfig.roll(level.random, cycle);
        if (!drop.isEmpty()) {
            Block.popResource(level, pos.above(), drop);
        }

        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS,
                1.0F, 0.8F + level.random.nextFloat() * 0.4F);
        level.sendParticles(ParticleTypes.PORTAL,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                12, 0.3, 0.3, 0.3, 0.05);

        grantXp(level, pos, be, cycle.xpPerMine(), player);
    }

    /** Ajoute de l'XP au bloc et gere le passage de niveau (message + effets). */
    public static void grantXp(ServerLevel level, BlockPos pos, InfiniteBlockEntity be, long amount, @Nullable Player player) {
        int before = be.getXpLevel();
        be.addXp(amount);
        int after = be.getXpLevel();
        if (after > before) {
            if (player != null) {
                player.displayClientMessage(Component.translatable(
                        "message." + TogaetherMod.MODID + ".level_up", after), false);
            }
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.0F);
            level.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    30, 0.4, 0.4, 0.4, 0.08);
        }
    }

    private InfiniteBlockEvents() {
    }
}
