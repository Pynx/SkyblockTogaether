package com.pynx.togaether.block;

import com.pynx.togaether.TogaetherMod;
import com.pynx.togaether.block.entity.InfiniteBlockEntity;
import com.pynx.togaether.config.CyclesConfig;
import com.pynx.togaether.event.InfiniteBlockEvents;
import com.pynx.togaether.item.XpShardItem;
import com.pynx.togaether.network.InfiniteBlockScreenPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InfiniteBlock extends BaseEntityBlock {

    public static final com.mojang.serialization.MapCodec<InfiniteBlock> CODEC = simpleCodec(InfiniteBlock::new);

    public InfiniteBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfiniteBlockEntity(pos, state);
    }

    /** Restaure XP/cycle si le bloc pose vient d'un ramassage (accroupi + casser). */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        net.minecraft.world.item.component.CustomData data =
                stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data != null && !level.isClientSide
                && level.getBlockEntity(pos) instanceof InfiniteBlockEntity be) {
            net.minecraft.nbt.CompoundTag tag = data.copyTag();
            if (tag.contains("Xp")) {
                be.restore(tag.getLong("Xp"), tag.getLong("Mined"), tag.getString("Cycle"));
            }
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Vitesse de minage dynamique : durete et outil de predilection viennent du
     * cycle actif (synchronises sur le block entity). L'outil est evalue via un
     * "bloc proxy" (pierre pour la pioche, buche pour la hache...), ce qui prend
     * en compte le materiau de l'outil, Efficacite, Celerite, l'eau, etc.
     */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        float hardness = 1.5F;
        String tool = "pickaxe";
        if (level.getBlockEntity(pos) instanceof InfiniteBlockEntity be) {
            hardness = be.getHardness();
            tool = be.getTool();
        }
        BlockState proxy = toolProxy(tool);
        float speed = player.getDestroySpeed(proxy != null ? proxy : state);
        return Math.max(0.0F, speed / hardness / 30.0F);
    }

    @Nullable
    private static BlockState toolProxy(String tool) {
        return switch (tool) {
            case "pickaxe" -> net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
            case "axe" -> net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState();
            case "shovel" -> net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
            case "hoe" -> net.minecraft.world.level.block.Blocks.HAY_BLOCK.defaultBlockState();
            case "sword" -> net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState();
            default -> null; // "any" : vitesse de base quelle que soit la main
        };
    }

    /** Eclats d'XP (objets admin) : consommes pour donner de l'XP au bloc. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof XpShardItem shard) {
            if (level instanceof ServerLevel serverLevel
                    && serverLevel.getBlockEntity(pos) instanceof InfiniteBlockEntity be) {
                InfiniteBlockEvents.grantXp(serverLevel, pos, be, shard.amount(), player);
                player.displayClientMessage(Component.translatable(
                        "message." + TogaetherMod.MODID + ".xp_given", shard.amount()), true);
                stack.shrink(1);
                serverLevel.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.8F, 1.2F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Clic droit : ouvre l'interface (XP, niveau, cycles). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof InfiniteBlockEntity be) {
            int blockLevel = be.getXpLevel();
            List<InfiniteBlockScreenPayload.CycleInfo> cycles = CyclesConfig.cycles().stream()
                    .map(c -> new InfiniteBlockScreenPayload.CycleInfo(c.name(), c.category(), c.minLevel(), c.xpPerMine(),
                            c.hardness(), c.tool(),
                            c.entries().stream()
                                    .map(e -> new InfiniteBlockScreenPayload.EntryInfo(
                                            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.item()).toString(),
                                            e.count(), e.weight()))
                                    .toList()))
                    .toList();
            CyclesConfig.Cycle current = CyclesConfig.resolveCycle(be.getSelectedCycle(), blockLevel);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer,
                    new InfiniteBlockScreenPayload(pos, be.getXp(), blockLevel,
                            CyclesConfig.nextLevelXp(be.getXp()),
                            current == null ? "" : current.name(), cycles));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
