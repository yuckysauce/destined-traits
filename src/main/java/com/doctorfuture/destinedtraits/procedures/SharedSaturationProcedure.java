package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

@EventBusSubscriber
public class SharedSaturationProcedure {

    // Helper class to store the results of our equipment scan
    private static class PerkData {
        public int totalPieces = 0;
        public boolean hasEnhanced = false;
    }

    // Helper method to scan ALL equipped items for the shared_saturation perk
    private static PerkData getPerkData(LivingEntity livingEntity) {
        PerkData data = new PerkData();
        ResourceLocation sharedSaturation = ResourceLocation.fromNamespaceAndPath("destinedtraits", "shared_saturation");
        data.totalPieces = WeaponPerkUtils.countOriginArmorPieces(livingEntity, sharedSaturation);
        data.hasEnhanced = WeaponPerkUtils.hasEnhancedOriginArmor(livingEntity, sharedSaturation);
        return data;
    }

    // =================================================================================
    // EVENT: GRANTS HUNGER AND SATURATION TO NEARBY PLAYERS WHEN YOU FINISH EATING
    // =================================================================================
    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        // 1. Ensure this is a player and we are on the server side
        if (event.getEntity() instanceof Player sharer && !sharer.level().isClientSide()) {
            
            // 2. 1.21.1 Component Check: Ensure the item they just finished using is Food
            if (event.getItem().has(DataComponents.FOOD)) {
                
                PerkData perkData = getPerkData(sharer);
                
                if (perkData.totalPieces > 0) {
                    MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(sharer);
                    // Define sharing radius (10 blocks)
                    double traitMultiplier = perkData.totalPieces / 2.0;
                    double radius = 10.0D * masterworks.rangeMultiplier() * traitMultiplier;
                    AABB searchBox = sharer.getBoundingBox().inflate(radius);
                    
                    // Find all OTHER players within a true sphere radius
                    List<Player> nearbyPlayers = sharer.level().getEntitiesOfClass(
                        Player.class, 
                        searchBox, 
                        p -> p != sharer && p.isAlive() && p.distanceToSqr(sharer) <= (radius * radius)
                    );
                    
                    if (!nearbyPlayers.isEmpty()) {
                        
                        // Define how much Hunger (Food Level) and Saturation to give
                        // Vanilla steak gives 8 food and 0.8 saturation.
                        // Normal perk gives 2 food (1 drumstick). Enhanced gives 4 food (2 drumsticks).
                        int foodToGive = Math.max(1, masterworks.scaleStrength((int) Math.round((perkData.hasEnhanced ? 4 : 2) * traitMultiplier)));
                        float satToGive = (float) masterworks.scaleStrength((perkData.hasEnhanced ? 0.4 : 0.2) * traitMultiplier);
                        
                        for (Player receiver : nearbyPlayers) {
                            // Add food and saturation directly to the nearby player's food data
                            receiver.getFoodData().eat(foodToGive, satToGive);
                        }
                    }
                }
            }
        }
    }
}