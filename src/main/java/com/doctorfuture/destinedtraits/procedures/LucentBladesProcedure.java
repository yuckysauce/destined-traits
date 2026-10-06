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
public class LucentBladesProcedure {

    // Helper class to store the results of our equipment scan
    private static class PerkData {
        public int totalPieces = 0;
        public boolean hasEnhanced = false;
    }

    // Helper method to scan armor and offhand for the lucent_blades perk
    private static PerkData getPerkData(LivingEntity livingEntity) {
        PerkData data = new PerkData();
        @SuppressWarnings("unchecked")
        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

        if (perkComponent == null) return data;

        ResourceLocation lucentBlades = ResourceLocation.fromNamespaceAndPath("destinedtraits", "lucent_blades");
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
                int index = perks.indexOf(lucentBlades);

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
            MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
            
            // Add progress based on XP gained and pieces worn
            vars.lucent_blades_progress += (amount * perkData.totalPieces);

            int requiredXp = masterworks.scaleEfficiencyCost(42);
            if (vars.lucent_blades_progress >= requiredXp) {
                int stacksToAdd = (int) (vars.lucent_blades_progress / requiredXp);
                vars.lucent_blades_progress %= requiredXp;

                MobEffect rawEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "lucent_blades_effect"));

                if (rawEffect != null) {
                    Holder<MobEffect> lucentEffect = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(rawEffect);

                    // Minecraft amplifiers start at 0. Max 5 stacks = Amp 4.
                    // Enhanced does not increase stack cap for this perk.
                    int maxAmp = 4; 

                    int currentAmp = -1;
                    if (player.hasEffect(lucentEffect)) {
                        currentAmp = player.getEffect(lucentEffect).getAmplifier();
                    }

                    int newAmp = Math.min(currentAmp + stacksToAdd, maxAmp);

                    // Apply the stack marker (Duration -1 = infinite)
                    player.addEffect(new MobEffectInstance(lucentEffect, -1, newAmp, true, true));
                }
            }
            vars.markSyncDirty();
        }
    }

    // =================================================================================
    // EVENT 2: CONSUMES ONE STACK ON MELEE HIT TO GRANT ATTACK SPEED
    // =================================================================================
    @SubscribeEvent
    public static void onMeleeDamageDealt(LivingDamageEvent.Pre event) {
        Entity attackerEntity = event.getSource().getEntity();
        Entity directEntity = event.getSource().getDirectEntity();

        // Check if the attacker is a player AND the direct cause of damage is also the player
        // (This prevents arrows, potions, or magic from triggering the melee effect)
        if (attackerEntity instanceof Player player && directEntity == player) {
            
            PerkData perkData = getPerkData(player);
            
            if (perkData.totalPieces > 0) {
                MobEffect rawStackEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "lucent_blades_effect"));
                MobEffect rawActiveEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "lucent_blades_active_effect"));
                
                if (rawStackEffect != null && rawActiveEffect != null) {
                    Holder<MobEffect> stackEffect = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(rawStackEffect);
                    Holder<MobEffect> activeEffect = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(rawActiveEffect);
                    
                    // If they have stacks available
                    if (player.hasEffect(stackEffect)) {
                        
                        // 1. Consume exactly one stack
                        int currentAmp = player.getEffect(stackEffect).getAmplifier();
                        if (currentAmp > 0) {
                            // If they have multiple stacks, replace with one tier lower
                            player.addEffect(new MobEffectInstance(stackEffect, -1, currentAmp - 1, false, true));
                        } else {
                            // If it's the last stack (Amp 0), remove it completely
                            player.removeEffect(stackEffect);
                        }
                        
                        // 2. Grant the attack speed buff
                        // Duration: 60 ticks (3 seconds)
                        // Amplifier: 0 (Level 1) normally, or 1 (Level 2) if enhanced
                        int activeDuration = MasterworkUtils.getStats(player).scaleDuration(5);
                        int activeAmp = perkData.hasEnhanced ? 1 : 0;
                        
                        player.addEffect(new MobEffectInstance(activeEffect, activeDuration, activeAmp, false, true));
                    }
                }
            }
        }
    }
}