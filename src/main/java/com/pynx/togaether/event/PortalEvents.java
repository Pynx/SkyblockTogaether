package com.pynx.togaether.event;

import com.pynx.togaether.ModDimensions;
import com.pynx.togaether.config.SkyblockConfig;
import com.pynx.togaether.portal.PortalBuilder;
import com.pynx.togaether.registry.ModBlocks;
import com.pynx.togaether.world.IslandsData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Redirige les portails vanilla a l'interieur des dimensions skyblock :
 * - portail du Nether dans skyblock <-> skyblock_nether (echelle 1:8, comme en vanilla)
 * - portail de l'End dans skyblock -> skyblock_end (zone par ile), et retour spatial
 * Permet aussi d'allumer des portails du Nether dans ces dimensions (vanilla ne le
 * permet que dans l'overworld et le Nether).
 */
public final class PortalEvents {

    /** Ticks passes dans un portail du Nether (notre propre compteur). */
    private static final Map<UUID, Integer> NETHER_PORTAL_TICKS = new HashMap<>();
    /** Cooldown apres une redirection, pour ne pas repartir immediatement. */
    private static final Map<UUID, Integer> REDIRECT_COOLDOWN = new HashMap<>();
    private static final Set<UUID> PENDING_TELEPORT = new HashSet<>();
    private static final int ARRIVAL_COOLDOWN = 300;

    // ------------------------------------------------------------------
    // Portails du Nether dans les dimensions skyblock : on neutralise le
    // processus vanilla (qui enverrait vers le vrai Nether) en maintenant
    // le cooldown de portail, et on gere nous-memes le delai puis le voyage.
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        ResourceKey<Level> dim = level.dimension();
        if (dim != ModDimensions.SKYBLOCK && dim != ModDimensions.SKYBLOCK_NETHER) {
            return;
        }

        UUID id = entity.getUUID();
        BlockState inside = level.getBlockState(entity.blockPosition());
        if (!inside.is(Blocks.NETHER_PORTAL)) {
            NETHER_PORTAL_TICKS.remove(id);
            REDIRECT_COOLDOWN.computeIfPresent(id, (k, v) -> v > 1 ? v - 1 : null);
            return;
        }

        // Tant que le cooldown est actif, NetherPortalBlock.entityInside ne peut pas
        // armer le teleporteur vanilla : on le maintient donc en permanence ici.
        entity.setPortalCooldown();

        int cooldown = REDIRECT_COOLDOWN.getOrDefault(id, 0);
        if (cooldown > 0) {
            REDIRECT_COOLDOWN.put(id, cooldown - 1);
            NETHER_PORTAL_TICKS.remove(id);
            return;
        }

        if (!entity.isAlive() || entity.isPassenger() || entity.isVehicle() || entity.isSpectator()) {
            return;
        }

        int required = requiredPortalTicks(level, entity);
        int ticks = NETHER_PORTAL_TICKS.merge(id, 1, Integer::sum);
        if (ticks >= required) {
            NETHER_PORTAL_TICKS.remove(id);
            teleportThroughNetherPortal(level, entity);
        }
    }

    private static int requiredPortalTicks(ServerLevel level, Entity entity) {
        if (entity instanceof Player player) {
            return Math.max(1, level.getGameRules().getInt(player.getAbilities().invulnerable
                    ? GameRules.RULE_PLAYERS_NETHER_PORTAL_CREATIVE_DELAY
                    : GameRules.RULE_PLAYERS_NETHER_PORTAL_DEFAULT_DELAY));
        }
        return 20;
    }

    private static void teleportThroughNetherPortal(ServerLevel level, Entity entity) {
        MinecraftServer server = level.getServer();
        boolean toNether = level.dimension() == ModDimensions.SKYBLOCK;
        ServerLevel dest = server.getLevel(toNether ? ModDimensions.SKYBLOCK_NETHER : ModDimensions.SKYBLOCK);
        if (dest == null) {
            return;
        }

        double scale = DimensionType.getTeleportationScale(level.dimensionType(), dest.dimensionType());
        BlockPos scaled = dest.getWorldBorder().clampToBounds(entity.getX() * scale, entity.getY(), entity.getZ() * scale);

        Optional<BlockPos> existing = dest.getPortalForcer()
                .findClosestPortalPosition(scaled, toNether, dest.getWorldBorder());

        BlockPos portalPos = existing.orElseGet(() -> {
            int y = toNether ? SkyblockConfig.netherPortalHeight() : SkyblockConfig.islandHeight() + 1;
            return PortalBuilder.buildNetherPortal(dest, new BlockPos(scaled.getX(), y, scaled.getZ()));
        });

        Entity result = entity.changeDimension(new DimensionTransition(dest, Vec3.atBottomCenterOf(portalPos),
                Vec3.ZERO, entity.getYRot(), entity.getXRot(), DimensionTransition.PLAY_PORTAL_SOUND));
        if (result != null) {
            result.setPortalCooldown();
            REDIRECT_COOLDOWN.put(result.getUUID(), ARRIVAL_COOLDOWN);
        }
    }

    // ------------------------------------------------------------------
    // Portails de l'End + filets de securite : on annule le voyage vanilla
    // et on redirige vers nos dimensions au tick suivant.
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onTravelToDimension(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        ResourceKey<Level> from = level.dimension();
        ResourceKey<Level> to = event.getDimension();
        if (!ModDimensions.isSkyblockDimension(from) || ModDimensions.isSkyblockDimension(to)) {
            return; // nos propres redirections passent librement
        }

        MinecraftServer server = level.getServer();

        // Portail de l'End construit dans le skyblock (ou son nether) -> end skyblock
        if (to == Level.END && (from == ModDimensions.SKYBLOCK || from == ModDimensions.SKYBLOCK_NETHER)) {
            event.setCanceled(true);
            schedule(server, entity, () -> teleportToSkyblockEnd(server, entity));
            return;
        }

        // Retour depuis l'end skyblock (portail de retour) -> skyblock
        if (from == ModDimensions.SKYBLOCK_END && (to == Level.END || to == Level.OVERWORLD)) {
            event.setCanceled(true);
            schedule(server, entity, () -> teleportBackFromEnd(server, entity));
            return;
        }

        // Filet de securite : aucun voyage vers le vrai Nether/Overworld depuis nos dimensions,
        // sauf via notre portail celeste (retour vers l'ancre, gere par SkyPortalBlock).
        if ((to == Level.NETHER || to == Level.OVERWORLD)
                && !level.getBlockState(entity.blockPosition()).is(ModBlocks.SKY_PORTAL.get())) {
            event.setCanceled(true);
        }
    }

    private static void schedule(MinecraftServer server, Entity entity, Runnable action) {
        if (!PENDING_TELEPORT.add(entity.getUUID())) {
            return;
        }
        server.tell(new TickTask(server.getTickCount() + 1, () -> {
            PENDING_TELEPORT.remove(entity.getUUID());
            if (!entity.isRemoved()) {
                action.run();
            }
        }));
    }

    /**
     * Chaque ile possede sa propre zone d'End : la plateforme est generee aux
     * coordonnees du centre de l'ile (echelle 1:1), comme la liaison du Nether.
     */
    private static void teleportToSkyblockEnd(MinecraftServer server, Entity entity) {
        ServerLevel end = server.getLevel(ModDimensions.SKYBLOCK_END);
        if (end == null || !(entity.level() instanceof ServerLevel from)) {
            return;
        }
        // Position ramenee a l'echelle du skyblock (x8 depuis le nether skyblock)
        double scale = DimensionType.getTeleportationScale(from.dimensionType(),
                server.getLevel(ModDimensions.SKYBLOCK) != null
                        ? server.getLevel(ModDimensions.SKYBLOCK).dimensionType()
                        : end.dimensionType());
        int x = (int) Math.round(entity.getX() * scale);
        int z = (int) Math.round(entity.getZ() * scale);

        IslandsData.Island island = IslandsData.get(server).findNearestIsland(x, z, Long.MAX_VALUE);
        BlockPos base = island != null
                ? new BlockPos(island.center().getX(), 64, island.center().getZ())
                : new BlockPos(0, 64, 0);
        base = end.getWorldBorder().clampToBounds(base.getX(), base.getY(), base.getZ());

        ensureEndPlatform(end, base);
        Entity result = entity.changeDimension(new DimensionTransition(end, Vec3.atBottomCenterOf(base.above()),
                Vec3.ZERO, Direction.WEST.toYRot(), 0.0F, DimensionTransition.PLAY_PORTAL_SOUND));
        if (result != null) {
            result.setPortalCooldown();
        }
    }

    /** Retour spatial : l'entite revient vers l'ile la plus proche de sa position dans l'End (1:1). */
    private static void teleportBackFromEnd(MinecraftServer server, Entity entity) {
        ServerLevel sky = server.getLevel(ModDimensions.SKYBLOCK);
        if (sky == null) {
            return;
        }
        IslandsData.Island island = IslandsData.get(server)
                .findNearestIsland((int) entity.getX(), (int) entity.getZ(), Long.MAX_VALUE);
        BlockPos target = island != null
                ? island.center().above()
                : new BlockPos(0, SkyblockConfig.islandHeight() + 1, 0);
        Vec3 spot = findSafeSpotNear(sky, target);
        Entity result = entity.changeDimension(new DimensionTransition(sky, spot,
                Vec3.ZERO, entity.getYRot(), entity.getXRot(), DimensionTransition.PLAY_PORTAL_SOUND));
        if (result != null) {
            result.setPortalCooldown();
        }
    }

    /** Plateforme d'arrivee + portail de retour dans l'end skyblock, a la position donnee. */
    private static void ensureEndPlatform(ServerLevel end, BlockPos base) {
        if (end.getBlockState(base).is(Blocks.OBSIDIAN)) {
            return;
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                end.setBlock(base.offset(dx, 0, dz), Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_ALL);
                for (int dy = 1; dy <= 3; dy++) {
                    BlockPos p = base.offset(dx, dy, dz);
                    if (!end.getBlockState(p).isAir()) {
                        end.removeBlock(p, false);
                    }
                }
            }
        }
        // Pont vers le portail de retour
        end.setBlock(base.offset(3, 0, 0), Blocks.END_STONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);
        // Portail de retour : bassin 3x3 de blocs de portail de l'End, borde de pierre de l'End
        BlockPos portalCenter = base.offset(6, 0, 0);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos p = portalCenter.offset(dx, 0, dz);
                boolean border = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                end.setBlock(p, border
                        ? Blocks.END_STONE_BRICKS.defaultBlockState()
                        : Blocks.END_PORTAL.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static Vec3 findSafeSpotNear(ServerLevel level, BlockPos pos) {
        for (int r = 0; r <= 4; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    for (int dy = -1; dy <= 2; dy++) {
                        BlockPos feet = pos.offset(dx, dy, dz);
                        BlockPos ground = feet.below();
                        BlockState groundState = level.getBlockState(ground);
                        if (groundState.isFaceSturdy(level, ground, Direction.UP)
                                && !groundState.is(Blocks.END_PORTAL)
                                && level.getBlockState(feet).isAir()
                                && level.getBlockState(feet.above()).isAir()) {
                            return Vec3.atBottomCenterOf(feet);
                        }
                    }
                }
            }
        }
        return Vec3.atBottomCenterOf(pos.above());
    }

    // ------------------------------------------------------------------
    // Allumage des portails du Nether dans les dimensions skyblock
    // (vanilla ne le permet que dans l'overworld et le Nether).
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        ResourceKey<Level> dim = level.dimension();
        if (dim != ModDimensions.SKYBLOCK && dim != ModDimensions.SKYBLOCK_NETHER) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
            return;
        }
        if (level.isClientSide) {
            return;
        }
        BlockPos firePos = event.getPos().relative(event.getFace());
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            Optional<PortalShape> shape = PortalShape.findEmptyPortalShape(level, firePos, axis);
            if (shape.isPresent()) {
                shape.get().createPortalBlocks();
                level.playSound(null, firePos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                Player player = event.getEntity();
                if (!player.getAbilities().instabuild) {
                    if (stack.is(Items.FLINT_AND_STEEL)) {
                        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(event.getHand()));
                    } else {
                        stack.shrink(1);
                    }
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }
    }

    private PortalEvents() {
    }
}
