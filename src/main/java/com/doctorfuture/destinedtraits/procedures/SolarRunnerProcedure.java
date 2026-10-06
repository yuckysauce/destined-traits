package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import com.doctorfuture.destinedtraits.init.DestinedtraitsModMobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

@EventBusSubscriber
public class SolarRunnerProcedure {

    // Helper class to store the results of our equipment scan
    private static class PerkData {
        public int totalPieces = 0;
        public boolean hasEnhanced = false;
    }

    // Helper method to scan armor and offhand for the solar_runner perk
    private static PerkData getPerkData(LivingEntity livingEntity) {
        PerkData data = new PerkData();
        @SuppressWarnings("unchecked")
        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

        if (perkComponent == null) return data;

        ResourceLocation solarRunner = ResourceLocation.fromNamespaceAndPath("destinedtraits", "solar_runner");
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
                int index = perks.indexOf(solarRunner);

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
    // EVENT: GRANTS MOVEMENT SPEED WHILE ON FIRE
    // =================================================================================
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        
        // Only run on the server side to prevent ghost effects / desyncs
        if (player.level().isClientSide()) return;

        // Run the check every 10 ticks (half a second) rather than every single tick to save performance
        if (player.tickCount % 10 != 0) return;

        // Check if the player is actively burning
        if (player.isOnFire()) {
            PerkData perkData = getPerkData(player);
            
            if (perkData.totalPieces > 0) {
                    MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
                // Normal = Speed I (Amp 0). Enhanced = Speed II (Amp 1).
                int amp = perkData.hasEnhanced ? 1 : 0;
                
                // Duration is 30 ticks (1.5 seconds). 
                // Since we refresh it every 10 ticks, it will stay active smoothly while burning, 
                // and cleanly drop off a second after the fire goes out.
                // The booleans at the end are: ambient=false, showParticles=false, showIcon=true
                player.addEffect(new MobEffectInstance(DestinedtraitsModMobEffects.SOLAR_RUNNER_EFFECT, masterworks.scaleDuration(30), amp, true, true, true));
            }
        }
    }
}