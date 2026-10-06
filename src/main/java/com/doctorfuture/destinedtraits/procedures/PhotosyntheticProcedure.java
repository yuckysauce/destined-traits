package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

import java.util.Collections;
import java.util.List;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

@EventBusSubscriber
public class PhotosyntheticProcedure {

    // Helper class to store the results of our weapon scan
    private static class PerkData {
        public boolean hasPerk = false;
        public boolean hasEnhanced = false;
    }

    // Helper method to scan the item for the Photosynthetic perk and its enhanced tier
    private static PerkData getPhotosyntheticData(ItemStack item) {
        PerkData data = new PerkData();
        if (item.isEmpty()) return data;

        @SuppressWarnings("unchecked")
        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

        if (perkComponent != null) {
            List<ResourceLocation> perks = item.getOrDefault(perkComponent, Collections.emptyList());
            int index = perks.indexOf(ResourceLocation.fromNamespaceAndPath("destinedtraits", "photosynthetic"));
            
            if (index != -1) {
                data.hasPerk = true;
                int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
                
                if (WeaponPerkUtils.isOriginEnhanced(item, ResourceLocation.fromNamespaceAndPath("destinedtraits", "photosynthetic"))) {
                    data.hasEnhanced = true;
                }
            }
        }
        return data;
    }

    // Adapted from your SporeSwarmProcedure to add 1 stack and return the new total
    private static int applyAndGetSporeStacks(LivingEntity target, MasterworkUtils.Stats masterworks) {
        MobEffect rawEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "spore_swarm_effect"));
        if (rawEffect != null) {
            Holder<MobEffect> effectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(rawEffect);
            
            int currentAmp = -1;
            if (target.hasEffect(effectHolder)) {
                currentAmp = target.getEffect(effectHolder).getAmplifier();
            }
            
            // Add 1 stack (cap at amp 13 to match enhanced limit)
            int newAmp = Math.min(currentAmp + 1, 13); 
            
            MobEffectInstance appliedEffect = new MobEffectInstance(effectHolder, masterworks.scaleDuration(200), newAmp, false, true);
            target.addEffect(appliedEffect);
            MasterworkUtils.applyScaledEffectModifiers(target, appliedEffect, masterworks);
            return newAmp + 1; // Return human-readable stack count (Amplifier + 1)
        }
        return 0;
    }

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        // Run only on the server
        if (event.getEntity().level().isClientSide()) return;
        
        // Ensure the blocker is a player
        if (!(event.getEntity() instanceof Player player)) return;
        
        // Get the entity that caused the damage being blocked
        Entity attacker = event.getDamageSource().getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;
        
        // Check for the perk data on the active blocking item first
        PerkData activeData = getPhotosyntheticData(player.getUseItem());
        
        // Failsafe: if getUseItem() is empty during the event for any reason, check hands directly
        if (!activeData.hasPerk) {
            activeData = getPhotosyntheticData(player.getOffhandItem());
            if (!activeData.hasPerk) {
                activeData = getPhotosyntheticData(player.getMainHandItem());
            }
        }
        
        // Terminate if the player does not have the Photosynthetic perk equipped
        if (!activeData.hasPerk) return;
        
        // 1. Apply Spores to the attacker and get their current stack count
        MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
        int currentStacks = applyAndGetSporeStacks(livingAttacker, masterworks);
        
        ServerLevel serverLevel = (ServerLevel) player.level();
        
        // 2. Play a burst of particles and sound to confirm the shield reflect worked
        serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, 
                player.getX(), player.getY() + 1.0, player.getZ(), 
                15, 0.5, 0.5, 0.5, 0.05);
        
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.BEE_STING, SoundSource.PLAYERS, 1.0F, 1.0F);

        // 3. Deal a tick of damage back to the attacker for hit feedback
        livingAttacker.hurt(livingAttacker.damageSources().playerAttack(player), (float) masterworks.strengthMultiplier());

        // 4. Hunger Restoration Logic based on Spore Stacks
        int hungerToRestore = 0;
        
        if (currentStacks >= 8) {
            hungerToRestore = activeData.hasEnhanced ? 3 : 2;
        } else if (currentStacks >= 5) {
            hungerToRestore = activeData.hasEnhanced ? 2 : 1;
        }

        if (hungerToRestore > 0) {
            hungerToRestore = Math.max(1, (int) Math.round(hungerToRestore * masterworks.strengthMultiplier()));
            int currentFood = player.getFoodData().getFoodLevel();
            
            // Cap food level at 20 (Vanilla maximum)
            if (currentFood < 20) {
                player.getFoodData().setFoodLevel(Math.min(20, currentFood + hungerToRestore));
            }
        }
    }
}