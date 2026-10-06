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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import com.doctorfuture.destinedtraits.network.DestinedtraitsModVariables;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import java.util.Collections;
import java.util.List;

@EventBusSubscriber
public class SporeSwarmProcedure {

    // Helper class to store the results of our weapon scan
    private static class PerkData {
        public boolean hasPerk = false;
        public boolean hasEnhanced = false;
    }

    // Helper method to scan the MAIN HAND for the spore_swarm perk (Weapon Trait)
    private static PerkData getWeaponPerk(Player player) {
        PerkData data = new PerkData();
        ItemStack item = player.getMainHandItem();
        
        if (item.isEmpty()) return data;

        @SuppressWarnings("unchecked")
        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

        if (perkComponent != null) {
            List<ResourceLocation> perks = item.getOrDefault(perkComponent, Collections.emptyList());
            int index = perks.indexOf(ResourceLocation.fromNamespaceAndPath("destinedtraits", "spore_swarm"));
            
            if (index != -1) {
                data.hasPerk = true;
                int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
                
                if (WeaponPerkUtils.isOriginEnhanced(item, ResourceLocation.fromNamespaceAndPath("destinedtraits", "spore_swarm"))) {
                    data.hasEnhanced = true;
                }
            }
        }
        return data;
    }

    // Helper method to safely stack the Spore Swarm effect on a target
    private static void addSporeStacks(LivingEntity target, int stacksToAdd, boolean isEnhanced, Player source) {
        MobEffect rawEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "spore_swarm_effect"));
        if (rawEffect != null) {
            Holder<MobEffect> effectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(rawEffect);
            
            // Max stacks: 11 normally (Amp 10), 14 if enhanced (Amp 13)
            int maxAmp = isEnhanced ? 13 : 10;
            int currentAmp = -1;
            
            if (target.hasEffect(effectHolder)) {
                currentAmp = target.getEffect(effectHolder).getAmplifier();
            }
            
            int newAmp = Math.min(currentAmp + stacksToAdd, maxAmp);
            
            // Duration: 200 ticks (10 seconds)
            int duration = source == null ? 200 : MasterworkUtils.getStats(source).scaleDuration(200);
            MobEffectInstance appliedEffect = new MobEffectInstance(effectHolder, duration, newAmp, false, true);
            target.addEffect(appliedEffect);
            if (source != null) {
                MasterworkUtils.applyScaledEffectModifiers(target, appliedEffect, MasterworkUtils.getStats(source));
            }
        }
    }

    // =================================================================================
    // EVENT 1: APPLY SPORES ON CRITICAL HIT
    // =================================================================================
    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(event.getTarget() instanceof LivingEntity target)) return;

        if (event.isVanillaCritical() || event.getDamageMultiplier() > 1.0f) {
            PerkData perkData = getWeaponPerk(player);
            
            if (perkData.hasPerk) {
                addSporeStacks(target, perkData.hasEnhanced ? 2 : 1, perkData.hasEnhanced, player);
            }
        }
    }

    // =================================================================================
    // EVENT 2: RAPID HITS AND DAMAGE INCREASE
    // =================================================================================
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        Entity attackerEntity = event.getSource().getEntity();
        LivingEntity target = event.getEntity();

        if (attackerEntity instanceof Player player) {
            PerkData perkData = getWeaponPerk(player);
            
            if (perkData.hasPerk) {
                // 1. Rapid Hit Logic
                if (!player.level().isClientSide()) {
                    MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
                    DestinedtraitsModVariables.PlayerVariables vars = player.getData(DestinedtraitsModVariables.PLAYER_VARIABLES);
                    vars.rapidhit_progress += 1;
                    vars.rapidhit_timer = masterworks.scaleDuration(100);

                    // 3 Hits Reached!
                    if (vars.rapidhit_progress >= masterworks.scaleEfficiencyCost(3)) {
                        vars.rapidhit_progress = 0;
                        vars.rapidhit_timer = 0;
                        
                        // Apply spores to the target being hit
                        addSporeStacks(target, perkData.hasEnhanced ? 2 : 1, perkData.hasEnhanced, player);
                    }
                    vars.markSyncDirty();
                }

                // 2. Damage Increase Logic
                MobEffect rawEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "spore_swarm_effect"));
                
                if (rawEffect != null) {
                    Holder<MobEffect> effectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(rawEffect);
                    
                    if (target.hasEffect(effectHolder)) {
                        int stacks = target.getEffect(effectHolder).getAmplifier() + 1;
                        
                        // Increase damage by 30% per stack
                        float damageMultiplier = 1.0f + (stacks * 0.3f * (float) MasterworkUtils.getStats(player).strengthMultiplier());
                        event.setNewDamage(event.getNewDamage() * damageMultiplier);
                    }
                }
            }
        }
    }

    // =================================================================================
    // EVENT 3: DEATH EXPLOSION
    // =================================================================================
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        LivingEntity dyingEntity = event.getEntity();
        Entity attackerEntity = event.getSource().getEntity();
        LevelAccessor world = dyingEntity.level();

        if (world.isClientSide()) return;

        Player player = null;
        if (attackerEntity instanceof Player p) {
            player = p;
        }

        // Spore Explosion (Triggers if the dying entity has spores)
        MobEffect rawEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "spore_swarm_effect"));
        
        if (rawEffect != null) {
            Holder<MobEffect> effectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(rawEffect);
            
            if (dyingEntity.hasEffect(effectHolder)) {
                int stacks = dyingEntity.getEffect(effectHolder).getAmplifier() + 1;
                        MasterworkUtils.Stats masterworks = player == null
                                ? new MasterworkUtils.Stats(100.0, 100.0, 100.0, 100.0)
                                : MasterworkUtils.getStats(player);
                
                // Blast properties scale with stacks
                        float explosionDamage = (float) (0.4F * stacks * masterworks.strengthMultiplier());
                        double radius = (3.0D + (stacks * 0.1D)) * masterworks.rangeMultiplier();

                if (world instanceof ServerLevel serverLevel) {
                    // Trigger custom visuals if a player caused it
                    if (player != null) {
                        FlowingVitreumProcedure.trigger(serverLevel, dyingEntity.getX(), dyingEntity.getY(), dyingEntity.getZ(), player);
                    }

                    // Standard Visuals and Sound
                    serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, dyingEntity.getX(), dyingEntity.getY() + 1, dyingEntity.getZ(), 30 * stacks, radius * 0.4, radius * 0.4, radius * 0.4, 0.1);
                    serverLevel.playSound(null, dyingEntity.blockPosition(), SoundEvents.BEE_DEATH, SoundSource.PLAYERS, 1.0F, 1.0F);
                    serverLevel.playSound(null, dyingEntity.blockPosition(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);

                    // Find and damage entities within the blast radius
                    AABB blastBox = dyingEntity.getBoundingBox().inflate(radius);
                    List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, blastBox, e -> e.isAlive() && e != attackerEntity && e != dyingEntity);
                    
                    for (LivingEntity target : targets) {
                        if (target.distanceToSqr(dyingEntity) <= (radius * radius)) {
                            target.invulnerableTime = 0; // Strip I-frames so damage connects
                            
                            // Give kill credit to the player if they caused it, otherwise generic magic damage
                            DamageSource damageSource = player != null 
                                ? target.damageSources().mobAttack(player) 
                                : target.damageSources().magic();
                                
                            target.hurt(damageSource, explosionDamage);

                            // Spread Spores to the survivors (1 stack)
                            addSporeStacks(target, 1, false, player);
                        }
                    }
                }
            }
        }
    }
}