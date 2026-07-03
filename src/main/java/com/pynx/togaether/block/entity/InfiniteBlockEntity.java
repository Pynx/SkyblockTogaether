package com.pynx.togaether.block.entity;

import com.pynx.togaether.config.CyclesConfig;
import com.pynx.togaether.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class InfiniteBlockEntity extends BlockEntity {

    private long xp = 0;
    private long mined = 0;
    private String selectedCycle = "";
    // Profil de minage (durete + outil du cycle actif), resolu cote serveur
    // et synchronise au client pour l'animation de cassage.
    private float hardness = 1.5F;
    private String tool = "pickaxe";

    public InfiniteBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFINITE_BLOCK.get(), pos, state);
    }

    public long getXp() {
        return this.xp;
    }

    public long getMined() {
        return this.mined;
    }

    public int getXpLevel() {
        return CyclesConfig.levelFor(this.xp);
    }

    public String getSelectedCycle() {
        return this.selectedCycle;
    }

    public float getHardness() {
        return this.hardness;
    }

    public String getTool() {
        return this.tool;
    }

    public void setSelectedCycle(String cycle) {
        this.selectedCycle = cycle;
        this.setChanged();
        this.refreshMiningProfile();
    }

    public void addXp(long amount) {
        this.xp += amount;
        this.setChanged();
        this.refreshMiningProfile();
    }

    public void incrementMined() {
        this.mined++;
        this.setChanged();
    }

    /** Restaure l'etat depuis un bloc ramasse (voir InfiniteBlock#setPlacedBy). */
    public void restore(long xp, long mined, String cycle) {
        this.xp = xp;
        this.mined = mined;
        this.selectedCycle = cycle;
        this.setChanged();
        this.refreshMiningProfile();
    }

    /**
     * Recalcule durete/outil a partir du cycle effectif (serveur uniquement)
     * et pousse la mise a jour aux clients si necessaire.
     */
    public void refreshMiningProfile() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        CyclesConfig.Cycle cycle = CyclesConfig.resolveCycle(this.selectedCycle, this.getXpLevel());
        float newHardness = cycle != null ? cycle.hardness() : 1.5F;
        String newTool = cycle != null ? cycle.tool() : "pickaxe";
        if (newHardness != this.hardness || !newTool.equals(this.tool)) {
            this.hardness = newHardness;
            this.tool = newTool;
            this.setChanged();
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.refreshMiningProfile();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.mined = tag.getLong("Mined");
        this.selectedCycle = tag.getString("Cycle");
        if (tag.contains("Xp")) {
            this.xp = tag.getLong("Xp");
        } else {
            // Migration depuis l'ancien systeme de paliers (1 minage = 1 XP)
            this.xp = this.mined;
        }
        if (tag.contains("Hardness")) {
            this.hardness = tag.getFloat("Hardness");
        }
        if (tag.contains("Tool")) {
            this.tool = tag.getString("Tool");
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Xp", this.xp);
        tag.putLong("Mined", this.mined);
        tag.putString("Cycle", this.selectedCycle);
        tag.putFloat("Hardness", this.hardness);
        tag.putString("Tool", this.tool);
    }

    // --- Synchronisation vers le client (animation de cassage correcte) ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
