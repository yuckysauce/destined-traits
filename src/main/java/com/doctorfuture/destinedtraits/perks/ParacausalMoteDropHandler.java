package com.doctorfuture.destinedtraits.perks;

import com.doctorfuture.destinedtraits.configuration.DestinedTraitsConfiguration;
import com.doctorfuture.destinedtraits.init.DestinedtraitsModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

@EventBusSubscriber(modid = "destinedtraits")
public final class ParacausalMoteDropHandler {
    private static final String XP_PROGRESS_KEY = "destinedtraits_paracausal_mote_xp";

    private ParacausalMoteDropHandler() {}

    @SubscribeEvent
    public static void onExperiencePickup(PlayerXpEvent.PickupXp event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        int requirement = DestinedTraitsConfiguration.XPMOTEREQUIREMENT.get();
        long accumulatedXp = player.getPersistentData().getLong(XP_PROGRESS_KEY) + event.getOrb().getValue();
        while (accumulatedXp >= requirement) {
            accumulatedXp -= requirement;
            ItemStack mote = new ItemStack(DestinedtraitsModItems.PARACAUSAL_MOTE.get());
            if (!player.getInventory().add(mote) && !mote.isEmpty()) {
                player.drop(mote, false);
            }
        }

        player.getPersistentData().putLong(XP_PROGRESS_KEY, accumulatedXp);
    }
}