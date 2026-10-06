package com.doctorfuture.destinedtraits.perks;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;

@EventBusSubscriber(modid = "destinedtraits")
public class TradePerkHandler {

    @SubscribeEvent
    public static void onTraderInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getTarget() instanceof AbstractVillager trader)) return;

        // Jobless villagers have no offers yet; touching them would lock in an empty list.
        if (trader instanceof Villager villager) {
            var profession = villager.getVillagerData().getProfession();
            if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) return;
        }

        // Perk application is a no-op for stacks that already have perks, so repeat visits are safe.
        for (MerchantOffer offer : trader.getOffers()) {
            NonLootPerks.apply(offer.getResult(), (net.minecraft.server.level.ServerLevel) event.getLevel(), trader.position(), trader);
        }
    }
}
