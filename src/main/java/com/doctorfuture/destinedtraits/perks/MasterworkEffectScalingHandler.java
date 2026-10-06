package com.doctorfuture.destinedtraits.perks;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "destinedtraits")
public final class MasterworkEffectScalingHandler {
    private MasterworkEffectScalingHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MasterworkUtils.applyScaledEffectModifiers(player);
        }
    }
}
