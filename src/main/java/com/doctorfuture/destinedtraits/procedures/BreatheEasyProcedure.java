package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import com.doctorfuture.destinedtraits.network.DestinedtraitsModVariables;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EventBusSubscriber
public class BreatheEasyProcedure {
    
    @SubscribeEvent
    public static void onPlayerXPChange(PlayerXpEvent.XpChange event) {
        if (event.getEntity() != null) {
            execute(event, event.getEntity(), event.getAmount());
        }
    }

    public static void execute(Entity entity, double amount) {
        execute(null, entity, amount);
    }

    private static void execute(@Nullable Event event, Entity entity, double amount) {
        if (!(entity instanceof Player player) || player.level().isClientSide()) return;
        
        // Prevent the perk from triggering or draining when spending XP
        if (amount <= 0) return;

        @SuppressWarnings("unchecked")
        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

        if (perkComponent != null) {
            ResourceLocation breathe_easy = ResourceLocation.fromNamespaceAndPath("destinedtraits", "breathe_easy");

            int totalBreatheEasyPieces = 0;
            boolean hasEnhancedPiece = false;

            List<ItemStack> itemsToCheck = new ArrayList<>();
            
            // 2. Add all actually equipped armor pieces
            player.getArmorSlots().forEach(itemsToCheck::add);
            
            // 3. Get the offhand item and only add it if it is NOT armor
            ItemStack offhandItem = player.getOffhandItem();
            if (!offhandItem.isEmpty() && !(offhandItem.getItem() instanceof net.minecraft.world.item.ArmorItem)) {
                itemsToCheck.add(offhandItem);
            }
            
            // 4. Run the exact same loop over the combined, filtered list
            for (ItemStack item : itemsToCheck) {
                if (!item.isEmpty()) {
                    List<ResourceLocation> perks = item.getOrDefault(perkComponent, Collections.emptyList());
                    int index = perks.indexOf(breathe_easy);
                    
                    if (index != -1) {
                        totalBreatheEasyPieces++; 
                        
                        int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
                        if (WeaponPerkUtils.isEnhancedAtIndex(item, index)) {
                            hasEnhancedPiece = true;
                        }
                    }
                }
            }

            // 2. Only execute if they are actually wearing breathe easy armor
            if (totalBreatheEasyPieces > 0) {
                MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
                DestinedtraitsModVariables.PlayerVariables vars = player.getData(DestinedtraitsModVariables.PLAYER_VARIABLES);
                
                vars.breatheeasy_progress += (amount * totalBreatheEasyPieces);
                vars.markSyncDirty();

                // 3. Trigger oxygen healing if threshold of 3 is reached
                if (vars.breatheeasy_progress >= masterworks.scaleEfficiencyCost(3)) {
                    
                    // Check if the player's air supply is below the maximum of 300 ticks
                    if (player.getAirSupply() < player.getMaxAirSupply()) {
                        
                        // 30 ticks = 1 bubble. Enhanced restores 2 bubbles, standard restores 1.
                        int airAmount = masterworks.scaleStrength(hasEnhancedPiece ? 60 : 30);
                        
                        // Prevent overhealing past the maximum air level
                        player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + airAmount));
                    }
                    
                    // Always hard reset progress to 0, wiping excess XP
                    vars.breatheeasy_progress = 0; 
                    vars.markSyncDirty();
                }
            }
        }
    }
}