package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EventBusSubscriber
public class KineticBloomProcedure {

    // Helper class to store the results of our equipment scan
    private static class PerkData {
        public int totalPieces = 0;
        public boolean hasEnhanced = false;
    }

    // Helper method to scan ALL equipped items for the kinetic_bloom perk
    private static PerkData getPerkData(LivingEntity livingEntity) {
        PerkData data = new PerkData();
        ResourceLocation kineticBloom = ResourceLocation.fromNamespaceAndPath("destinedtraits", "kinetic_bloom");
        data.totalPieces = WeaponPerkUtils.countOriginArmorPieces(livingEntity, kineticBloom);
        data.hasEnhanced = WeaponPerkUtils.hasEnhancedOriginArmor(livingEntity, kineticBloom);
        return data;
    }

    // =================================================================================
    // EVENT 1: GAIN XP -> CHARGE KINETIC BLOOM
    // =================================================================================
    @SubscribeEvent
    public static void onXpPickup(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        PerkData perkData = getPerkData(player);
        
        if (perkData.totalPieces > 0) {
            MobEffect rawEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "kinetic_bloom_effect"));
            
            if (rawEffect != null) {
                Holder<MobEffect> effectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(rawEffect);
                
                // Only charge if they don't already have the buff primed
                if (!player.hasEffect(effectHolder)) {
                    // Get the XP value of the orb picked up
                    int xpGained = event.getOrb().getValue();
                    
                    // Track XP persistently without needing MCreator global variables
                    int currentCharge = player.getPersistentData().getInt("KineticBloomCharge");
                    currentCharge += xpGained;
                    
                    // IMPORTANT: Replace YOUR_CONFIG_CLASS with the actual class name containing your XPPERKREQUIREMENT variable
                        double traitMultiplier = perkData.totalPieces / 2.0;
                        int baseRequiredXp = (int) Math.ceil(
                            com.doctorfuture.destinedtraits.configuration.DestinedTraitsConfiguration.XPPERKREQUIREMENT.get().intValue() / traitMultiplier);
                        int requiredXp = MasterworkUtils.getStats(player).scaleEfficiencyCost(Math.max(1, baseRequiredXp));
                    
                    if (currentCharge >= requiredXp) {
                        currentCharge = 0; // Reset charge
                        
                        // Apply the buff indefinitely (1,000,000 ticks) until consumed by an impact
                        player.addEffect(new MobEffectInstance(effectHolder, 1000000, perkData.hasEnhanced ? 1 : 0, false, true));
                        
                        // Audio feedback that the armor is charged and ready
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1.0F, 1.2F);
                    }
                    
                    // Save the updated charge
                    player.getPersistentData().putInt("KineticBloomCharge", currentCharge);
                }
            }
        }
    }

    // =================================================================================
    // EVENT 2: KINETIC IMPACT -> CANCEL DAMAGE & SPAWN BULLETS
    // =================================================================================
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Player player)) return;

        // Verify the damage is either Fall Damage or Elytra Wall Collision
        boolean isFallDamage = event.getSource().is(DamageTypes.FALL);
        boolean isElytraCollision = event.getSource().is(DamageTypes.FLY_INTO_WALL);

        if (!isFallDamage && !isElytraCollision) return;

        MobEffect rawEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "kinetic_bloom_effect"));
        if (rawEffect == null) return;
        
        Holder<MobEffect> effectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(rawEffect);

        if (player.hasEffect(effectHolder)) {
            // Check enhanced status via the amplifier
            boolean isEnhanced = player.getEffect(effectHolder).getAmplifier() > 0;
            
            // Capture the damage value before cancelling it
            float damageAmount = event.getNewDamage();

            // 1. Cancel the damage and consume the buff
            event.setNewDamage(0);
            player.removeEffect(effectHolder);

            ServerLevel serverLevel = (ServerLevel) player.level();
            
            // 2. Heavy impact visuals and sound
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 1.0F, 0.7F);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 0.5F, 1.5F);
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 40, 1.0, 1.0, 1.0, 0.1);

            // 3. Scale bullets based on prevented damage
            // Formula: Base 2 bullets + 1 bullet per 4 damage prevented. Caps at 10 (or 15 if enhanced).
            int baseBullets = isEnhanced ? 4 : 3;
            int maxBullets = isEnhanced ? 20 : 15;
            MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
            int baseBulletCount = Math.min((int) Math.floor(damageAmount / 2.0F) + baseBullets, maxBullets);
            double traitMultiplier = getPerkData(player).totalPieces / 2.0;
            int bulletCount = Math.max(1, (int) Math.ceil(baseBulletCount * masterworks.strengthMultiplier() * traitMultiplier));

            // 4. Find hostiles to target
            AABB searchBox = player.getBoundingBox().inflate(20.0D * masterworks.rangeMultiplier());
            List<net.minecraft.world.entity.Mob> targets = serverLevel.getEntitiesOfClass(
                net.minecraft.world.entity.Mob.class, 
                searchBox, 
                m -> m.isAlive() && !m.isAlliedTo(player)
            );

            // 5. Spawn the homing bullets
            for (int i = 0; i < bulletCount; i++) {
                // Determine a target by cycling through the available hostiles
                net.minecraft.world.entity.Mob target = targets.isEmpty() ? null : targets.get(i % targets.size());
                
                // Construct the Shulker Bullet. 
                // Using standard vanilla 1.21 Constructor: (Level, Owner, Target, Axis)
                ShulkerBullet bullet = new ShulkerBullet(serverLevel, player, target, Direction.Axis.Y);
                
                // Disperse their spawn positions slightly so they don't visually overlap completely
                double offsetX = (serverLevel.random.nextDouble() - 0.5) * 2.5;
                double offsetY = 0.5 + serverLevel.random.nextDouble();
                double offsetZ = (serverLevel.random.nextDouble() - 0.5) * 2.5;
                
                bullet.setPos(player.getX() + offsetX, player.getY() + offsetY, player.getZ() + offsetZ);
                
                serverLevel.addFreshEntity(bullet);
            }
        }
    }
}