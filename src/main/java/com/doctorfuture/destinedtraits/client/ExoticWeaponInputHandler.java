package com.doctorfuture.destinedtraits.client;

import com.doctorfuture.destinedtraits.network.ExoticWeaponInputPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "destinedtraits", value = Dist.CLIENT)
public final class ExoticWeaponInputHandler {
    private static boolean wasUseDown;

    private ExoticWeaponInputHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean useDown = minecraft.player != null && minecraft.screen == null && minecraft.options.keyUse.isDown();
        if (useDown == wasUseDown) return;
        wasUseDown = useDown;
        PacketDistributor.sendToServer(new ExoticWeaponInputPayload(useDown));
    }
}