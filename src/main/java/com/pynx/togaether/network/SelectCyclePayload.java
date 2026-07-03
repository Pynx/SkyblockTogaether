package com.pynx.togaether.network;

import com.pynx.togaether.TogaetherMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> serveur : selection d'un cycle sur un bloc infini. */
public record SelectCyclePayload(BlockPos pos, String cycle) implements CustomPacketPayload {

    public static final Type<SelectCyclePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TogaetherMod.MODID, "select_cycle"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, SelectCyclePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SelectCyclePayload::pos,
                    ByteBufCodecs.STRING_UTF8, SelectCyclePayload::cycle,
                    SelectCyclePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
