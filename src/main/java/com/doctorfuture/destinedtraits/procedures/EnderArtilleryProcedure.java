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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.core.Direction;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

import com.doctorfuture.destinedtraits.network.DestinedtraitsModVariables;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber
public class EnderArtilleryProcedure {

    // Internal cooldown map to prevent sweeping attacks from spawning 30 bullets instantly
    private static final Map<UUID, Long> lastBulletSpawn = new ConcurrentHashMap<>();

    // Helper class to store the results of our equipment scan
    private static class PerkData {
        public int totalPieces = 0;
        public boolean hasEnhanced = false;
    }

    // Helper method to scan ALL equipped items for the ender_artillery perk
    private static PerkData getPerkData(LivingEntity livingEntity) {
        PerkData data = new PerkData();
        @SuppressWarnings("unchecked")
        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

        if (perkComponent == null) return data;

        ResourceLocation enderArtillery = ResourceLocation.fromNamespaceAndPath("destinedtraits", "ender_artillery");
        List<ItemStack> itemsToCheck = new ArrayList<>();

        // Add armor, main hand, and offhand
        livingEntity.getArmorSlots().forEach(itemsToCheck::add);
        itemsToCheck.add(livingEntity.getMainHandItem());
        itemsToCheck.add(livingEntity.getOffhandItem());

        for (ItemStack item : itemsToCheck) {
            if (item != null && !item.isEmpty()) {
                List<ResourceLocation> perks = item.getOrDefault(perkComponent, Collections.emptyList());
                int index = perks.indexOf(enderArtillery);

                if (index != -1) {
                    data.totalPieces++;
                    int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
                    
                    if (WeaponPerkUtils.isOriginEnhanced(item, enderArtillery)) {
                        data.hasEnhanced = true;
                    }
                }
            }
        }
        return data;
    }

    // =================================================================================
    // EVENT 1: HIT MOBS -> BUILD PROGRESS OR SPAWN BULLETS IF EFFECT IS ACTIVE
    // =================================================================================
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        Entity attackerEntity = event.getSource().getEntity();
        LivingEntity target = event.getEntity();

        if (attackerEntity instanceof Player player && !player.level().isClientSide() && player.level() instanceof ServerLevel serverLevel) {
            PerkData perkData = getPerkData(player);
            MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
            
            if (perkData.totalPieces > 0) {
                MobEffect rawEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "ender_artillery_effect"));
                Holder<MobEffect> effectHolder = rawEffect != null ? BuiltInRegistries.MOB_EFFECT.wrapAsHolder(rawEffect) : null;
                
                if (effectHolder != null && player.hasEffect(effectHolder)) {
                    
                    // ====================================================
                    // STAGE 2: EFFECT IS ACTIVE - SPAWN BULLETS ON HIT
                    // ====================================================
                    
                    long currentTime = serverLevel.getGameTime();
                    long lastSpawnTime = lastBulletSpawn.getOrDefault(player.getUUID(), 0L);
                    
                    // Very short internal cooldown: 4 ticks (0.2 seconds).
                    // This allows rapid bullet firing but prevents a single sweep attack from crashing the game.
                    if (currentTime - lastSpawnTime >= masterworks.scaleEfficiencyCooldown(10)) {
                        lastBulletSpawn.put(player.getUUID(), currentTime);
                        
                        // Spawn 4 bullets if enhanced, otherwise 3
                        int bulletCount = Math.max(1, (int) Math.ceil((perkData.hasEnhanced ? 2 : 1) * masterworks.strengthMultiplier()));
                        
                        // Audio feedback for the bullet launch
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 1.0F, 1.2F);
                        
                        // Spawn the homing bullets
                        for (int i = 0; i < bulletCount; i++) {
                            ShulkerBullet bullet = new ShulkerBullet(serverLevel, player, target, Direction.Axis.Y);
                            
                            // Disperse their spawn positions slightly above the player
                            double offsetX = (serverLevel.random.nextDouble() - 0.5) * 1.5;
                            double offsetY = 1.0 + serverLevel.random.nextDouble();
                            double offsetZ = (serverLevel.random.nextDouble() - 0.5) * 1.5;
                            
                            bullet.setPos(player.getX() + offsetX, player.getY() + offsetY, player.getZ() + offsetZ);
                            
                            serverLevel.addFreshEntity(bullet);
                        }
                    }
                    
                } else {
                    
                    // ====================================================
                    // STAGE 1: NO EFFECT YET - BUILD PROGRESS
                    // ====================================================
                    
                    DestinedtraitsModVariables.PlayerVariables vars = player.getData(DestinedtraitsModVariables.PLAYER_VARIABLES);
                    vars.enderartillery_progress += 1;
                    vars.enderartillery_timer = masterworks.scaleDuration(100);
                    
                    int threshold = masterworks.scaleEfficiencyCost(perkData.hasEnhanced ? 18 : 20);
                    
                    if (vars.enderartillery_progress >= threshold) {
                        // Reset progress
                        vars.enderartillery_progress = 0;
                        vars.enderartillery_timer = 0;
                        
                        // Apply the Ender Artillery effect for 10 seconds (200 ticks)
                        if (effectHolder != null) {
                            player.addEffect(new MobEffectInstance(effectHolder, masterworks.scaleDuration(200), perkData.hasEnhanced ? 1 : 0, true, true));
                        }
                        
                        // Audio feedback that the artillery is primed and ready
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1.0F, 0.8F);
                    }
                    
                    vars.markSyncDirty();
                }
            }
        }
    }

    // =================================================================================
    // EVENT 2: SHULKER BULLET IMPACT -> TRIGGER FLOWING VITREUM
    // =================================================================================
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof ShulkerBullet bullet) {
            if (!bullet.level().isClientSide() && bullet.level() instanceof ServerLevel serverLevel) {
                if (bullet.getOwner() instanceof Player player) {
                    
                    // Trigger Flowing Vitreum exactly where the bullet detonates
                    FlowingVitreumProcedure.trigger(
                        serverLevel, 
                        bullet.getX(), 
                        bullet.getY(), 
                        bullet.getZ(), 
                        player
                    );
                }
            }
        }
    }
}