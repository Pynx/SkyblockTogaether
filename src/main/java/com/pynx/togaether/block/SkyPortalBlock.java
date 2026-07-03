package com.pynx.togaether.block;

import com.pynx.togaether.ModDimensions;
import com.pynx.togaether.config.SkyblockConfig;
import com.pynx.togaether.portal.PortalBuilder;
import com.pynx.togaether.portal.SkyPortalShape;
import com.pynx.togaether.world.IslandGenerator;
import com.pynx.togaether.world.IslandsData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SkyPortalBlock extends Block implements Portal {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    protected static final VoxelShape X_AXIS_AABB = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
    protected static final VoxelShape Z_AXIS_AABB = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);

    public SkyPortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_AXIS_AABB : X_AXIS_AABB;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction.Axis portalAxis = state.getValue(AXIS);
        boolean inPlane = direction.getAxis() == Direction.Axis.Y || direction.getAxis() == portalAxis;
        if (inPlane && !neighborState.is(this) && !SkyPortalShape.isFrameBlock(neighborState)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        if (entity instanceof Player player) {
            return Math.max(1, level.getGameRules().getInt(player.getAbilities().invulnerable
                    ? GameRules.RULE_PLAYERS_NETHER_PORTAL_CREATIVE_DELAY
                    : GameRules.RULE_PLAYERS_NETHER_PORTAL_DEFAULT_DELAY));
        }
        return 0;
    }

    @Nullable
    @Override
    public DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        MinecraftServer server = level.getServer();
        IslandsData data = IslandsData.get(server);

        if (level.dimension() == ModDimensions.SKYBLOCK) {
            // Retour vers le point d'origine du portail (l'ancre)
            IslandsData.Island island = data.findNearestIsland(pos.getX(), pos.getZ(), Long.MAX_VALUE);
            if (island == null) {
                return null;
            }
            ServerLevel dest = server.getLevel(island.anchorDim());
            if (dest == null) {
                dest = server.overworld();
            }
            BlockPos anchor = island.anchorPos();
            PortalBuilder.ensureSkyPortal(dest, anchor);
            return transition(dest, entity, anchor);
        }

        if (level.dimension() == ModDimensions.SKYBLOCK_NETHER || level.dimension() == ModDimensions.SKYBLOCK_END) {
            return null;
        }

        // Depuis l'overworld (ou toute autre dimension) : vers la dimension skyblock
        ServerLevel dest = server.getLevel(ModDimensions.SKYBLOCK);
        if (dest == null) {
            return null;
        }
        double scale = DimensionType.getTeleportationScale(level.dimensionType(), dest.dimensionType());
        int x = (int) Math.round(pos.getX() * scale);
        int z = (int) Math.round(pos.getZ() * scale);

        IslandsData.Island island = data.findNearestIsland(x, z, SkyblockConfig.minIslandDistance());
        if (island == null) {
            BlockPos center = new BlockPos(x, SkyblockConfig.islandHeight(), z);
            island = IslandGenerator.createIsland(dest, center, level.dimension(), pos.immutable());
            data.addIsland(island);
        } else {
            PortalBuilder.ensureSkyPortal(dest, island.portalPos());
        }
        return transition(dest, entity, island.portalPos());
    }

    private static DimensionTransition transition(ServerLevel dest, Entity entity, BlockPos pos) {
        return new DimensionTransition(dest, Vec3.atBottomCenterOf(pos), Vec3.ZERO,
                entity.getYRot(), entity.getXRot(), DimensionTransition.PLAY_PORTAL_SOUND);
    }

    @Override
    public Portal.Transition getLocalTransition() {
        return Portal.Transition.CONFUSION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5F, random.nextFloat() * 0.4F + 0.8F, false);
        }
        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double dx = (random.nextDouble() - 0.5) * 0.5;
            double dy = (random.nextDouble() - 0.5) * 0.5;
            double dz = (random.nextDouble() - 0.5) * 0.5;
            int j = random.nextInt(2) * 2 - 1;
            if (!level.getBlockState(pos.west()).is(this) && !level.getBlockState(pos.east()).is(this)) {
                x = pos.getX() + 0.5 + 0.25 * j;
                dx = random.nextDouble() * 2.0 * j;
            } else {
                z = pos.getZ() + 0.5 + 0.25 * j;
                dz = random.nextDouble() * 2.0 * j;
            }
            level.addParticle(ParticleTypes.PORTAL, x, y, z, dx, dy, dz);
        }
    }
}
