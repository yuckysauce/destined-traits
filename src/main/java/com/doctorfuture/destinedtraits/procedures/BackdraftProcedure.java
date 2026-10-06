package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

@EventBusSubscriber
public class BackdraftProcedure {

    // Helper class to store the results of our equipment scan
    private static class PerkData {
        public int totalPieces = 0;
        public boolean hasEnhanced = false;
    }

    // Helper method to scan armor and offhand for the backdraft perk
    private static PerkData getPerkData(LivingEntity livingEntity) {
        PerkData data = new PerkData();
        @SuppressWarnings("unchecked")
        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

        if (perkComponent == null) return data;

        ResourceLocation backdraft = ResourceLocation.fromNamespaceAndPath("destinedtraits", "backdraft");
        List<ItemStack> itemsToCheck = new ArrayList<>();

        // Add all armor
        livingEntity.getArmorSlots().forEach(itemsToCheck::add);

        // Add offhand (only if not armor)
        ItemStack offhandItem = livingEntity.getOffhandItem();
        if (!offhandItem.isEmpty() && !(offhandItem.getItem() instanceof net.minecraft.world.item.ArmorItem)) {
            itemsToCheck.add(offhandItem);
        }

        for (ItemStack item : itemsToCheck) {
            if (!item.isEmpty()) {
                List<ResourceLocation> perks = item.getOrDefault(perkComponent, Collections.emptyList());
                int index = perks.indexOf(backdraft);

                if (index != -1) {
                    data.totalPieces++;
                    int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
                    if (WeaponPerkUtils.isEnhancedAtIndex(item, index)) {
                        data.hasEnhanced = true;
                    }
                }
            }
        }
        return data;
    }

    // =================================================================================
    // EVENT: GRANTS ARMOR BUFF WHEN NEAR BURNING ENEMIES
    // =================================================================================
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        
        // Only run on the server side to prevent ghost effects
        if (player.level().isClientSide()) return;

        // Run the check every 10 ticks (half a second) to save performance
        if (player.tickCount % 10 != 0) return;

        PerkData perkData = getPerkData(player);
        
        if (perkData.totalPieces > 0) {
            // Define the search area (8 blocks in every direction from the player)
            MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
            AABB searchBox = player.getBoundingBox().inflate(8.0D * masterworks.rangeMultiplier());
            
            // Look for any entity in that box that is an Enemy, is on fire, and is alive
            List<LivingEntity> burningEnemies = player.level().getEntitiesOfClass(
                LivingEntity.class, 
                searchBox, 
                entity -> entity instanceof Enemy && entity.isOnFire() && entity.isAlive()
            );

            // If we found at least one burning enemy
            if (!burningEnemies.isEmpty()) {
                MobEffect rawEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "backdraft_effect"));
                
                if (rawEffect != null) {
                    Holder<MobEffect> backdraftEffect = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(rawEffect);
                    
                    // Normal = Level 1 (Amp 0). Enhanced = Level 2 (Amp 1).
                    int amp = perkData.hasEnhanced ? 1 : 0;
                    
                    // Duration is 30 ticks (1.5 seconds). 
                    // Refreshes every 10 ticks as long as the enemy is near and burning.
                    // The booleans at the end are: ambient=false, showParticles=false, showIcon=true
                    player.addEffect(new MobEffectInstance(backdraftEffect, masterworks.scaleDuration(30), amp, true, true, true));
                }
            }
        }
    }
}