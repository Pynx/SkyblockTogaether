package com.pynx.togaether.network;

import com.pynx.togaether.TogaetherMod;
import com.pynx.togaether.block.entity.InfiniteBlockEntity;
import com.pynx.togaether.config.CyclesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModPayloads {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(InfiniteBlockScreenPayload.TYPE, InfiniteBlockScreenPayload.STREAM_CODEC,
                ModPayloads::handleOpenScreen);
        registrar.playToServer(SelectCyclePayload.TYPE, SelectCyclePayload.STREAM_CODEC,
                ModPayloads::handleSelectCycle);
    }

    private static void handleOpenScreen(InfiniteBlockScreenPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist.isClient()) {
            // Classe chargee uniquement cote client (references des classes client-only)
            com.pynx.togaether.client.ClientScreenOpener.open(payload);
        }
    }

    private static void handleSelectCycle(SelectCyclePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = payload.pos();
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof InfiniteBlockEntity be)) {
            return;
        }
        CyclesConfig.Cycle cycle = CyclesConfig.byName(payload.cycle());
        if (cycle == null) {
            return;
        }
        if (cycle.minLevel() > be.getXpLevel()) {
            player.displayClientMessage(Component.translatable(
                    "message." + TogaetherMod.MODID + ".cycle_locked"), true);
            return;
        }
        be.setSelectedCycle(cycle.name());
        player.displayClientMessage(Component.translatable(
                "message." + TogaetherMod.MODID + ".cycle_selected", cycle.name()), true);
    }

    private ModPayloads() {
    }
}
