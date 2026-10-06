package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import java.util.Collections;
import java.util.List;

@EventBusSubscriber
public class ForsakenRiteProcedure {

    // Helper class to store perk scan results
    private static class PerkData {
        public boolean hasPerk = false;
    }

    // Helper method to scan the item for the Forsaken Rite perk
    private static PerkData getForsakenRiteData(ItemStack item) {
        PerkData data = new PerkData();
        if (item.isEmpty()) return data;

        @SuppressWarnings("unchecked")
        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

        if (perkComponent != null) {
            List<ResourceLocation> perks = item.getOrDefault(perkComponent, Collections.emptyList());
            if (perks.contains(ResourceLocation.fromNamespaceAndPath("destinedtraits", "forsaken_rite"))) {
                data.hasPerk = true;
            }
        }
        return data;
    }

    // =================================================================================
    // EVENT 1: TAG THE ARROW WHEN FIRED
    // =================================================================================
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        
        // Check if the entity spawning is an arrow
        if (event.getEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow) {
            // Check if the shooter is a LivingEntity (like a Player)
            if (arrow.getOwner() instanceof LivingEntity shooter) {
                // Check both hands in case the bow is in the offhand
                if (getForsakenRiteData(shooter.getMainHandItem()).hasPerk || getForsakenRiteData(shooter.getOffhandItem()).hasPerk) {
                    // Tag the arrow so we know it belongs to the Hades Arcanum
                    arrow.getPersistentData().putBoolean("forsaken_rite_arrow", true);
                }
            }
        }
    }

    // =================================================================================
    // EVENT 2: ARROW HITS GROUND OR EXISTING CIRCLE -> CREATE OR EXPAND CIRCLE
    // =================================================================================
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getProjectile().level().isClientSide()) return;
        
        if (event.getProjectile() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow) {
            
            // Check if it's our custom arrow and hasn't already triggered
            if (arrow.getPersistentData().getBoolean("forsaken_rite_arrow") && !arrow.getPersistentData().getBoolean("forsaken_rite_triggered")) {
                
                ServerLevel level = (ServerLevel) arrow.level();
                Vec3 loc = event.getRayTraceResult().getLocation();
                MasterworkUtils.Stats masterworks = arrow.getOwner() instanceof Player shooter
                    ? MasterworkUtils.getStats(shooter) : new MasterworkUtils.Stats(100.0, 100.0, 100.0, 100.0);
                
                // 1. Find nearby ritual circles to see if we shot an existing one
                List<AreaEffectCloud> existingCircles = level.getEntitiesOfClass(AreaEffectCloud.class, new AABB(loc, loc).inflate(12.0 * masterworks.rangeMultiplier()), 
                    aec -> aec.getPersistentData().getBoolean("forsaken_rite_circle"));
                
                boolean absorbed = false;
                
                for (AreaEffectCloud aec : existingCircles) {
                    // If the arrow landed inside this circle's radius
                    if (loc.distanceToSqr(aec.position()) <= (aec.getRadius() * aec.getRadius())) {
                        
                        arrow.getPersistentData().putBoolean("forsaken_rite_triggered", true);
                        
                        // Increase radius and duration (Cap radius at 10 blocks, duration at 30 seconds / 600 ticks)
                        float newRadius = (float) Math.min(aec.getRadius() + 0.3F * masterworks.rangeMultiplier(), 10.0F);
                        int newDuration = Math.min(aec.getDuration() + masterworks.scaleDuration(100), 600);
                        
                        aec.setRadius(newRadius);
                        aec.setDuration(newDuration);
                        
                        // Visual/Audio cue for the expansion
                        level.sendParticles(ParticleTypes.SCULK_SOUL, aec.getX(), aec.getY() + 0.2, aec.getZ(), 20, newRadius * 0.5, 0.1, newRadius * 0.5, 0.05);
                        level.playSound(null, aec.getX(), aec.getY(), aec.getZ(), SoundEvents.SOUL_SAND_STEP, SoundSource.PLAYERS, 1.0F, 0.5F);
          				level.playSound(null, aec.getX(), aec.getY(), aec.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.8F);
                        
                        absorbed = true;
                        arrow.discard(); // Destroy the arrow so it doesn't linger since the ritual absorbed it
                        break; // Only expand one circle
                    }
                }
                
                // 2. If it didn't hit an existing circle, check if it hit the ground to spawn a new one
                if (!absorbed && event.getRayTraceResult().getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) event.getRayTraceResult();
                    
                    // Direction.UP means it hit the top face of the block (the ground)
                    if (blockHit.getDirection() == Direction.UP) {
                        
                        arrow.getPersistentData().putBoolean("forsaken_rite_triggered", true);
                        
                        // Create the ritual circle using a vanilla AreaEffectCloud for easy particle and lifespan management
                        AreaEffectCloud aec = new AreaEffectCloud(level, loc.x, loc.y, loc.z);
                        aec.setOwner(arrow.getOwner() instanceof LivingEntity owner ? owner : null);
                        aec.setRadius((float) masterworks.rangeMultiplier());
                        aec.setDuration(masterworks.scaleDuration(200));
                        aec.setWaitTime(0); // Make the cloud active instantly
                        
                        // FIX: Must use a SimpleParticleType. Vibration is complex. Swapped to Sculk Soul to maintain aesthetic.
                        aec.setParticle(ParticleTypes.SOUL); 
                        
                        aec.setRadiusPerTick(0); // Prevents the circle from shrinking over time
                        
                        // Tag the circle so we can find it in our tick event
                        aec.getPersistentData().putBoolean("forsaken_rite_circle", true);
                        
                        level.addFreshEntity(aec);
                        
                        // FIX: Removed .value()
                        level.playSound(null, loc.x, loc.y, loc.z, SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 1.0F, 0.8F);
                    }
                }
            }
        }
    }

    // =================================================================================
    // EVENT 3: RITUAL CIRCLE LOGIC -> APPLY BUFFS AND DEBUFFS
    // =================================================================================
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        
        // Find our specific custom AreaEffectClouds
        if (event.getEntity() instanceof AreaEffectCloud aec && aec.getPersistentData().getBoolean("forsaken_rite_circle")) {
            
            // Only run the distance calculations every 5 ticks to save heavy server performance
            if (aec.tickCount % 5 == 0) { 
                ServerLevel level = (ServerLevel) aec.level();
                float radius = aec.getRadius();
                MasterworkUtils.Stats masterworks = aec.getOwner() instanceof Player owner
                        ? MasterworkUtils.getStats(owner) : new MasterworkUtils.Stats(100.0, 100.0, 100.0, 100.0);
                
                // Get all living entities within a rough box around the circle
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, aec.getBoundingBox().inflate(radius));
                
                MobEffect playerEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "forsaken_rite_effect"));
                MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "forsaken_rite_debuff"));
                
                if (playerEffect != null && mobEffect != null) {
                    Holder<MobEffect> playerEffectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(playerEffect);
                    Holder<MobEffect> mobEffectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mobEffect);
                    
                    for (LivingEntity target : targets) {
                        // Math check to ensure they are inside the circular radius, not just the square bounding box
                        if (target.distanceToSqr(aec) <= (radius * radius)) {
                            
                            // Give a short 25-tick duration effect so it naturally wears off ~1 second after they step out
                            if (target instanceof Player) {
                                MobEffectInstance appliedEffect = new MobEffectInstance(playerEffectHolder, masterworks.scaleDuration(25), 0, false, true, true);
                                target.addEffect(appliedEffect);
                                MasterworkUtils.applyScaledEffectModifiers(target, appliedEffect, masterworks);
                                
                                // FIX: Replaced loc.x/y/z with target.getX/Y/Z, removed .value(), and lowered volume to 0.15F to prevent ear-rape every 5 ticks
                                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1F, 0.5F);
                            } else {
                                MobEffectInstance appliedEffect = new MobEffectInstance(mobEffectHolder, masterworks.scaleDuration(25), 0, false, true, true);
                                target.addEffect(appliedEffect);
                                if (aec.getOwner() instanceof Player owner) {
                                    MasterworkUtils.applyScaledEffectModifiers(target, appliedEffect, MasterworkUtils.getStats(owner));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // =================================================================================
    // EVENT 4: DROP XP ON MOB DEATH
    // =================================================================================
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        
        // Ensure this only runs on the server and ignores Players dying
        if (entity.level().isClientSide() || entity instanceof Player) return; 
        
        MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "forsaken_rite_debuff"));
        
        if (mobEffect != null) {
            Holder<MobEffect> mobEffectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mobEffect);
            
            // Check if the mob died while having our debuff
            if (entity.hasEffect(mobEffectHolder)) {
                ServerLevel level = (ServerLevel) entity.level();
                
                // Spawn a customized amount of XP (Between 5 and 10 XP)
                int xpAmount = 5 + level.random.nextInt(6);
                
                // Drop the experience orb physically in the world where the mob died
                net.minecraft.world.entity.ExperienceOrb.award(level, entity.position(), xpAmount);
            }
        }
    }

    // =================================================================================
    // EVENT 5: COLLECTING XP -> EXPANDS CIRCLE
    // =================================================================================
    @SubscribeEvent
    public static void onXPPickup(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        
        ServerLevel level = (ServerLevel) player.level();
        Vec3 loc = player.position();
        MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
        
        // Scan for circles around the player
        List<AreaEffectCloud> existingCircles = level.getEntitiesOfClass(AreaEffectCloud.class, player.getBoundingBox().inflate(12.0 * masterworks.rangeMultiplier()), 
            aec -> aec.getPersistentData().getBoolean("forsaken_rite_circle"));
        
        boolean boosted = false;
        
        for (AreaEffectCloud aec : existingCircles) {
            // Check if player is standing inside the radius
            if (loc.distanceToSqr(aec.position()) <= (aec.getRadius() * aec.getRadius())) {
                
                // Boost radius slightly and duration (Capped at same limits)
                float newRadius = (float) Math.min(aec.getRadius() + 0.1F * masterworks.rangeMultiplier(), 10.0F);
                int newDuration = Math.min(aec.getDuration() + masterworks.scaleDuration(60), 600);
                
                aec.setRadius(newRadius);
                aec.setDuration(newDuration);
                
                // Subtle visual feedback
                level.sendParticles(ParticleTypes.SOUL, aec.getX(), aec.getY() + 0.2, aec.getZ(), 10, newRadius * 0.5, 0.1, newRadius * 0.5, 0.05);
                boosted = true;
            }
        }
        
        // Play an extra ambient sound so the player knows the circle "fed" on the XP
        if (boosted) {
            level.playSound(null, loc.x, loc.y, loc.z, SoundEvents.SOUL_SAND_STEP, SoundSource.PLAYERS, 1.0F, 0.5F);
            level.playSound(null, loc.x, loc.y, loc.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }
}