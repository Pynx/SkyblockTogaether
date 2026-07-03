package com.pynx.togaether.world;

import com.pynx.togaether.portal.PortalBuilder;
import com.pynx.togaether.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Genere une ile flottante : terrain, arbre, bloc infini au centre,
 * coffre de depart et portail de retour.
 */
public final class IslandGenerator {

    private static final int RADIUS = 9;

    /**
     * @param center    centre de l'ile ; center.getY() = couche d'herbe (les joueurs marchent a Y+1)
     * @param anchorDim dimension d'origine du portail
     * @param anchorPos position du portail d'origine (cellule interieure)
     * @return l'ile creee (non enregistree dans IslandsData)
     */
    public static IslandsData.Island createIsland(ServerLevel level, BlockPos center,
                                                  ResourceKey<Level> anchorDim, BlockPos anchorPos) {
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        // Terrain : herbe en surface, terre en dessous, pierre en profondeur (forme de goutte)
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > RADIUS + 0.2) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;
                set(level, x, cy, z, Blocks.GRASS_BLOCK.defaultBlockState());
                set(level, x, cy - 1, z, Blocks.DIRT.defaultBlockState());
                set(level, x, cy - 2, z, Blocks.DIRT.defaultBlockState());
                int depth = (int) Math.round((RADIUS - dist) * 0.9);
                for (int dy = 3; dy < 3 + depth; dy++) {
                    set(level, x, cy - dy, z, Blocks.STONE.defaultBlockState());
                }
            }
        }

        // Arbre (chene) decale du centre
        placeTree(level, new BlockPos(cx - 5, cy + 1, cz - 4));

        // Bloc infini au centre, sur la surface
        set(level, cx, cy + 1, cz, ModBlocks.INFINITE_BLOCK.get().defaultBlockState());

        // Coffre de depart
        BlockPos chestPos = new BlockPos(cx + 2, cy + 1, cz);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(Items.OAK_SAPLING, 2));
            chest.setItem(1, new ItemStack(Items.BREAD, 6));
            chest.setItem(2, new ItemStack(Items.BONE_MEAL, 4));
            chest.setItem(3, new ItemStack(Items.WATER_BUCKET));
        }

        // Portail de retour vers l'ancre
        BlockPos portalPos = new BlockPos(cx + 5, cy + 1, cz);
        PortalBuilder.buildSkyPortal(level, portalPos);

        return new IslandsData.Island(center.immutable(), portalPos, anchorDim, anchorPos.immutable());
    }

    private static void placeTree(ServerLevel level, BlockPos base) {
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState();
        for (int i = 0; i < 5; i++) {
            set(level, base.getX(), base.getY() + i, base.getZ(), log);
        }
        for (int dy = 3; dy <= 5; dy++) {
            int r = dy == 5 ? 1 : 2;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dz == 0 && dy < 5) {
                        continue;
                    }
                    if (Math.abs(dx) == r && Math.abs(dz) == r && dy != 4) {
                        continue;
                    }
                    BlockPos p = base.offset(dx, dy, dz);
                    if (level.getBlockState(p).isAir()) {
                        level.setBlock(p, leaves, Block.UPDATE_ALL);
                    }
                }
            }
        }
        set(level, base.getX(), base.getY() + 5, base.getZ(), leaves);
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_ALL);
    }

    private IslandGenerator() {
    }
}
