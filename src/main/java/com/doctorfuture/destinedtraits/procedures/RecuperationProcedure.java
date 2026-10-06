package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
public class RecuperationProcedure {
    
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
        if (!(entity instanceof LivingEntity livingEntity)) return;
        
        // Prevent the perk from triggering or draining when spending XP
        if (amount <= 0) return;

        @SuppressWarnings("unchecked")
        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

        if (perkComponent != null) {
            ResourceLocation recuperation = ResourceLocation.fromNamespaceAndPath("destinedtraits", "recuperation");

            int totalRecuperationPieces = 0;
            boolean hasEnhancedPiece = false;

            // 1. Create a list to hold all the items we want to check
            List<ItemStack> itemsToCheck = new ArrayList<>();
            
            // 2. Add all actually equipped armor pieces
            livingEntity.getArmorSlots().forEach(itemsToCheck::add);
            
            // 3. Get the offhand item and only add it if it is NOT armor
            ItemStack offhandItem = livingEntity.getOffhandItem();
            if (!offhandItem.isEmpty() && !(offhandItem.getItem() instanceof net.minecraft.world.item.ArmorItem)) {
                itemsToCheck.add(offhandItem);
            }
            
            // 4. Run the exact same loop over the combined, filtered list
            for (ItemStack item : itemsToCheck) {
                if (!item.isEmpty()) {
                    List<ResourceLocation> perks = item.getOrDefault(perkComponent, Collections.emptyList());
                    int index = perks.indexOf(recuperation);
                    
                    if (index != -1) {
                        totalRecuperationPieces++; 
                        
                        int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
                        if (WeaponPerkUtils.isEnhancedAtIndex(item, index)) {
                            hasEnhancedPiece = true;
                        }
                    }
                }
            }

            // 2. Only execute if they are actually wearing Recuperation armor
            if (totalRecuperationPieces > 0) {
                DestinedtraitsModVariables.PlayerVariables vars = entity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES);
                MasterworkUtils.Stats masterworks = entity instanceof net.minecraft.world.entity.player.Player player
                        ? MasterworkUtils.getStats(player) : new MasterworkUtils.Stats(100.0, 100.0, 100.0, 100.0);
                
                // Add to progress (XP amount multiplied by how many pieces they are wearing)
                vars.recuperation_progress += (amount * totalRecuperationPieces);
                vars.markSyncDirty();

                // 3. Trigger healing if threshold is reached
                int requiredXp = masterworks.scaleEfficiencyCost(42);
                if (vars.recuperation_progress >= requiredXp) {
                    float healingAmount = (float) masterworks.scaleStrength(hasEnhancedPiece ? 2 : 1);
                    livingEntity.setHealth(livingEntity.getHealth() + healingAmount);
                    
                    // Reset progress (or subtract 42 if you want "rollover" progress for large XP drops)
                    vars.recuperation_progress %= requiredXp;
                    vars.markSyncDirty();
                }
            }
        }
    }
}