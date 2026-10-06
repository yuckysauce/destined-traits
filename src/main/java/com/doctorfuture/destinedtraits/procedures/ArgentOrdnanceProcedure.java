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
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import com.doctorfuture.destinedtraits.network.DestinedtraitsModVariables;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EventBusSubscriber
public class ArgentOrdnanceProcedure {

    // Helper class to store the results of our equipment scan
    private static class PerkData {
        public int totalPieces = 0;
        public boolean hasEnhanced = false;
    }

    // Helper method to scan armor and offhand for the argent_ordnance perk
    private static PerkData getPerkData(LivingEntity livingEntity) {
        PerkData data = new PerkData();
        @SuppressWarnings("unchecked")
        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

        if (perkComponent == null) return data;

        ResourceLocation argentOrdnance = ResourceLocation.fromNamespaceAndPath("destinedtraits", "argent_ordnance");
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
                int index = perks.indexOf(argentOrdnance);

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
    // EVENT 1: GRANTS AND STACKS THE BUFF WHEN GAINING XP
    // =================================================================================
    @SubscribeEvent
    public static void onPlayerXPChange(PlayerXpEvent.XpChange event) {
        Player player = event.getEntity();
        double amount = event.getAmount();

        if (player == null || amount <= 0) return;

        PerkData perkData = getPerkData(player);

        if (perkData.totalPieces > 0) {
            DestinedtraitsModVariables.PlayerVariables vars = player.getData(DestinedtraitsModVariables.PLAYER_VARIABLES);
            
            // Add progress based on XP gained and pieces worn
            vars.argent_ordnance_progress += (amount * perkData.totalPieces);

            int requiredProgress = MasterworkUtils.getStats(player).scaleEfficiencyCost(42);
            if (vars.argent_ordnance_progress >= requiredProgress) {
                // Calculate how many stacks to add in case they picked up a massive XP orb
                int stacksToAdd = (int) (vars.argent_ordnance_progress / requiredProgress);
                vars.argent_ordnance_progress %= requiredProgress; // Keep the remainder

                MobEffect rawEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "argent_ordnance_effect"));

                if (rawEffect != null) {
                    // 1.21.1 Requirement: Wrap the effect into a Holder
                    Holder<MobEffect> argentOrdnanceEffect = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(rawEffect);

                    // Minecraft amplifiers start at 0 (Amp 0 = 1 stack)
                    int maxAmp = perkData.hasEnhanced ? 6 : 4;

                    int currentAmp = -1; // -1 means they don't have the effect yet
                    if (player.hasEffect(argentOrdnanceEffect)) {
                        currentAmp = player.getEffect(argentOrdnanceEffect).getAmplifier();
                    }

                    int newAmp = Math.min(currentAmp + stacksToAdd, maxAmp);

                    // Add effect (Duration -1 means infinite duration)
                    player.addEffect(new MobEffectInstance(argentOrdnanceEffect, -1, newAmp, true, true));
                }
            }
            vars.markSyncDirty();
        }
    }

    // =================================================================================
    // EVENT 2: CONSUMES THE BUFF TO INCREASE EXPLOSIVE DAMAGE 
    // =================================================================================
    @SubscribeEvent
    public static void onExplosiveDamageDealt(LivingDamageEvent.Pre event) {
        Entity attackerEntity = event.getSource().getEntity();

        // Check if the attacker is a player
        if (attackerEntity instanceof Player player) {
            
            // Check if the damage type is an explosion
            if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
                
                PerkData perkData = getPerkData(player);
                
                // Ensure they actually have the perk equipped
                if (perkData.totalPieces > 0) {
                    MobEffect rawEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "argent_ordnance_effect"));
                    
                    if (rawEffect != null) {
                        // 1.21.1 Requirement: Wrap the effect into a Holder
                        Holder<MobEffect> argentOrdnanceEffect = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(rawEffect);
                        
                        // If they have the buff, consume it and boost damage
                        if (player.hasEffect(argentOrdnanceEffect)) {
                            
                            // Amp 0 is 1 stack, Amp 4 is 5 stacks, etc.
                            int stacks = player.getEffect(argentOrdnanceEffect).getAmplifier() + 1;
                            
                            // Consume all stacks
                            player.removeEffect(argentOrdnanceEffect);
                            
                            // MATH: Base damage (1.0) + (25% per stack)
                            // Max normal: +250% damage. Max enhanced: +350% damage.
                            float damageMultiplier = 1.0f + (stacks * 0.50f * (float) MasterworkUtils.getStats(player).strengthMultiplier());
                            
                            // Apply the new damage utilizing 1.21's setNewDamage()
                            event.setNewDamage(event.getNewDamage() * damageMultiplier);
                        }
                    }
                }
            }
        }
    }
}