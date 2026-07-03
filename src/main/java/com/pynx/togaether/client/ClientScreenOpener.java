package com.pynx.togaether.client;

import com.pynx.togaether.network.InfiniteBlockScreenPayload;
import net.minecraft.client.Minecraft;

/** Isole les references aux classes client-only (jamais chargee sur serveur dedie). */
public final class ClientScreenOpener {

    public static void open(InfiniteBlockScreenPayload payload) {
        Minecraft.getInstance().setScreen(new InfiniteBlockScreen(payload));
    }

    private ClientScreenOpener() {
    }
}
