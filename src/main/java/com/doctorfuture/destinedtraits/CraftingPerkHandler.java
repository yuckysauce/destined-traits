package com.doctorfuture.destinedtraits.perks;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Optional;

@EventBusSubscriber(modid = "destinedtraits")
public class CraftingPerkHandler {

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Level level = event.getEntity().level();
        
        if (level.isClientSide()) {
            return;
        }

        Player player = event.getEntity();
        ItemStack craftedItem = event.getCrafting();
        
        // STOP here if the item shouldn't have perks (e.g., stackable materials)
        if (!NonLootPerks.isEligibleForPerks(craftedItem)) {
            return;
        }

        craftedItem.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.EMPTY,
            data -> data.update(tag -> {
                tag.putInt("tier_min", 1);
                tag.putInt("tier_max", 1);
            }));
            
        NonLootPerks.apply(craftedItem, (ServerLevel) level, player.position(), player);
    }
}