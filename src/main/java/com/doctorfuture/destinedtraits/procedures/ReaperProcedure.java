package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

import com.doctorfuture.destinedtraits.init.DestinedtraitsModMobEffects;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

@EventBusSubscriber
public class ReaperProcedure {
    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() != null) {
            execute(event, event.getEntity());
        }
    }

    public static void execute(Entity entity) {
        execute(null, entity);
    }

    @SuppressWarnings("unchecked")
    private static void execute(@Nullable Event event, Entity entity) {
        // Ensure the entity is valid and is a LivingEntity before proceeding
        if (entity == null || !(entity instanceof LivingEntity livingEntity)) return;

        // Fetch the DataComponentType safely
        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
        
        if (perkComponent != null) {
            ResourceLocation reaper = ResourceLocation.fromNamespaceAndPath("destinedtraits", "reaper");
            
            boolean hasPerk = false;
            boolean isEnhanced = false;
            int totalReaperPieces = 0;

            List<ItemStack> itemsToCheck = new ArrayList<>();
            
            // 2. Add all actually equipped armor pieces
            // Using "livingEntity" here to catch both players and mobs
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
                    int index = perks.indexOf(reaper);
                    
                    if (index != -1) {
                        hasPerk = true;
                        totalReaperPieces++; 
                        
                        int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
                        if (WeaponPerkUtils.isEnhancedAtIndex(item, index)) {
                            isEnhanced = true;
                        }
                    }
                }
            }

            // Apply the appropriate potion effects based on the results of our loop
            if (hasPerk && !livingEntity.level().isClientSide()) {
                MasterworkUtils.Stats masterworks = livingEntity instanceof net.minecraft.world.entity.player.Player player
                        ? MasterworkUtils.getStats(player) : new MasterworkUtils.Stats(100.0, 100.0, 100.0, 100.0);
                if (isEnhanced) {
                    livingEntity.addEffect(new MobEffectInstance(DestinedtraitsModMobEffects.REAPER_EFFECT, masterworks.scaleDuration(300), 1, true, true));
                } else {
                    livingEntity.addEffect(new MobEffectInstance(DestinedtraitsModMobEffects.REAPER_EFFECT, masterworks.scaleDuration(200), 0, true, true));
                }
            }
        }
    }
}