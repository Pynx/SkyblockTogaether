package com.pynx.togaether.portal;

import com.pynx.togaether.config.SkyblockConfig;
import com.pynx.togaether.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Detection d'un cadre de portail skyblock : rectangle vertical de blocs de cadre
 * (configurable, pierre taillee par defaut) entourant une zone d'air.
 * Interieur : de 2x3 a 21x21, comme un portail du Nether.
 */
public final class SkyPortalShape {

    public static final int MIN_WIDTH = 2;
    public static final int MIN_HEIGHT = 3;
    public static final int MAX_SIZE = 21;

    private final LevelAccessor level;
    private final Direction.Axis axis;
    private final BlockPos bottomLeft; // cellule interieure en bas "a gauche" (direction negative de l'axe)
    private final int width;
    private final int height;

    private SkyPortalShape(LevelAccessor level, Direction.Axis axis, BlockPos bottomLeft, int width, int height) {
        this.level = level;
        this.axis = axis;
        this.bottomLeft = bottomLeft;
        this.width = width;
        this.height = height;
    }

    public static boolean isFrameBlock(BlockState state) {
        return state.is(SkyblockConfig.frameBlock());
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir();
    }

    /** Essaie les deux axes horizontaux a partir d'une position d'air presumee interieure. */
    public static Optional<SkyPortalShape> find(LevelAccessor level, BlockPos pos) {
        Optional<SkyPortalShape> shape = find(level, pos, Direction.Axis.X);
        if (shape.isPresent()) {
            return shape;
        }
        return find(level, pos, Direction.Axis.Z);
    }

    public static Optional<SkyPortalShape> find(LevelAccessor level, BlockPos pos, Direction.Axis axis) {
        if (!isEmpty(level.getBlockState(pos))) {
            return Optional.empty();
        }

        Direction left = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        Direction right = left.getOpposite();

        // Descendre jusqu'au sol du cadre
        BlockPos base = pos;
        int guard = 0;
        while (guard++ < MAX_SIZE && isEmpty(level.getBlockState(base.below()))) {
            base = base.below();
        }
        if (!isFrameBlock(level.getBlockState(base.below()))) {
            return Optional.empty();
        }

        // Aller au bord gauche
        BlockPos bottomLeft = base;
        guard = 0;
        while (guard++ < MAX_SIZE
                && isEmpty(level.getBlockState(bottomLeft.relative(left)))
                && isFrameBlock(level.getBlockState(bottomLeft.relative(left).below()))) {
            bottomLeft = bottomLeft.relative(left);
        }
        if (!isFrameBlock(level.getBlockState(bottomLeft.relative(left)))) {
            return Optional.empty();
        }

        // Largeur
        int width = 1;
        while (width < MAX_SIZE
                && isEmpty(level.getBlockState(bottomLeft.relative(right, width)))
                && isFrameBlock(level.getBlockState(bottomLeft.relative(right, width).below()))) {
            width++;
        }
        if (!isFrameBlock(level.getBlockState(bottomLeft.relative(right, width)))) {
            return Optional.empty();
        }
        if (width < MIN_WIDTH) {
            return Optional.empty();
        }

        // Hauteur : chaque rangee doit etre vide avec des murs de cadre des deux cotes.
        int height = 0;
        while (height < MAX_SIZE) {
            BlockPos rowStart = bottomLeft.above(height);
            boolean rowEmpty = true;
            for (int i = 0; i < width; i++) {
                if (!isEmpty(level.getBlockState(rowStart.relative(right, i)))) {
                    rowEmpty = false;
                    break;
                }
            }
            if (!rowEmpty) {
                break;
            }
            if (!isFrameBlock(level.getBlockState(rowStart.relative(left)))
                    || !isFrameBlock(level.getBlockState(rowStart.relative(right, width)))) {
                return Optional.empty();
            }
            height++;
        }
        if (height < MIN_HEIGHT || height >= MAX_SIZE) {
            return Optional.empty();
        }

        // Rangee du haut : cadre complet
        BlockPos topRow = bottomLeft.above(height);
        for (int i = 0; i < width; i++) {
            if (!isFrameBlock(level.getBlockState(topRow.relative(right, i)))) {
                return Optional.empty();
            }
        }

        return Optional.of(new SkyPortalShape(level, axis, bottomLeft, width, height));
    }

    /** Remplit l'interieur avec des blocs de portail skyblock. */
    public void createPortalBlocks() {
        BlockState portal = ModBlocks.SKY_PORTAL.get().defaultBlockState()
                .setValue(com.pynx.togaether.block.SkyPortalBlock.AXIS, this.axis);
        Direction right = this.axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        for (int w = 0; w < this.width; w++) {
            for (int h = 0; h < this.height; h++) {
                this.level.setBlock(this.bottomLeft.relative(right, w).above(h), portal, Block.UPDATE_CLIENTS | 16);
            }
        }
    }

    public BlockPos bottomLeft() {
        return this.bottomLeft;
    }
}
