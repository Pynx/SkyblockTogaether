package com.pynx.togaether.portal;

import com.pynx.togaether.config.SkyblockConfig;
import com.pynx.togaether.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Construction de portails "prets a l'emploi" : portail skyblock (cadre configurable)
 * et portail du Nether (obsidienne), tous deux orientes sur l'axe X.
 * Interieur 2x3, cadre 4x5. La position passee est la cellule interieure basse "gauche".
 */
public final class PortalBuilder {

    private static final int FLAGS = Block.UPDATE_ALL;

    /** Reconstruit un portail skyblock a l'ancre si aucun bloc de portail n'y est present. */
    public static void ensureSkyPortal(ServerLevel level, BlockPos interiorBottomLeft) {
        for (BlockPos p : BlockPos.betweenClosed(interiorBottomLeft.offset(-2, -1, -2), interiorBottomLeft.offset(2, 3, 2))) {
            if (level.getBlockState(p).is(ModBlocks.SKY_PORTAL.get())) {
                return;
            }
        }
        buildSkyPortal(level, interiorBottomLeft);
    }

    public static void buildSkyPortal(ServerLevel level, BlockPos interiorBottomLeft) {
        buildFramedPortal(level, interiorBottomLeft,
                SkyblockConfig.frameBlock().defaultBlockState(),
                ModBlocks.SKY_PORTAL.get().defaultBlockState()
                        .setValue(com.pynx.togaether.block.SkyPortalBlock.AXIS, Direction.Axis.X));
    }

    /** Construit un portail du Nether flottant (avec plateforme) et retourne la cellule interieure basse. */
    public static BlockPos buildNetherPortal(ServerLevel level, BlockPos interiorBottomLeft) {
        buildFramedPortal(level, interiorBottomLeft,
                Blocks.OBSIDIAN.defaultBlockState(),
                Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, Direction.Axis.X));

        // Plateforme d'accueil de part et d'autre du portail
        BlockState platform = level.dimension() == com.pynx.togaether.ModDimensions.SKYBLOCK_NETHER
                ? Blocks.NETHERRACK.defaultBlockState()
                : Blocks.COBBLESTONE.defaultBlockState();
        int y = interiorBottomLeft.getY() - 1;
        for (int dx = -2; dx <= 3; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos p = new BlockPos(interiorBottomLeft.getX() + dx, y, interiorBottomLeft.getZ() + dz);
                if (level.getBlockState(p).isAir()) {
                    level.setBlock(p, platform, FLAGS);
                }
            }
        }
        return interiorBottomLeft;
    }

    /**
     * Cadre 4 (large) x 5 (haut) dans le plan X, interieur 2x3.
     * interiorBottomLeft = cellule interieure de plus petit X.
     */
    private static void buildFramedPortal(ServerLevel level, BlockPos interiorBottomLeft, BlockState frame, BlockState portal) {
        int x0 = interiorBottomLeft.getX() - 1; // colonne gauche du cadre
        int y0 = interiorBottomLeft.getY() - 1; // rangee basse du cadre
        int z = interiorBottomLeft.getZ();

        for (int dx = 0; dx < 4; dx++) {
            for (int dy = 0; dy < 5; dy++) {
                BlockPos p = new BlockPos(x0 + dx, y0 + dy, z);
                boolean isFrame = dx == 0 || dx == 3 || dy == 0 || dy == 4;
                level.setBlock(p, isFrame ? frame : portal, isFrame ? FLAGS : (Block.UPDATE_CLIENTS | 16));
            }
        }

        // Degager l'espace devant/derriere l'interieur
        for (int dx = 1; dx <= 2; dx++) {
            for (int dy = 1; dy <= 3; dy++) {
                for (int dz = -1; dz <= 1; dz += 2) {
                    BlockPos p = new BlockPos(x0 + dx, y0 + dy, z + dz);
                    if (!level.getBlockState(p).isAir()) {
                        level.removeBlock(p, false);
                    }
                }
            }
        }
    }

    private PortalBuilder() {
    }
}
