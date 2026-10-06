package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

@EventBusSubscriber
public class FlowingVitreumProcedure {

    /**
     * Trigger 1: Standard Explosions (TNT, Creepers, Fireballs)
     */
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        LivingEntity source = event.getExplosion().getIndirectSourceEntity();
        if (source instanceof Player player) {
            trigger(level, event.getExplosion().center().x, event.getExplosion().center().y, event.getExplosion().center().z, player);
        }
    }

    /**
     * Trigger 2: Firework Explosions (Airbursts or Direct Hits)
     */
    @SubscribeEvent
    public static void onFireworkExplode(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        // Check if the entity leaving the world is a Firework
        if (event.getEntity() instanceof FireworkRocketEntity firework) {
            
            // A removal reason of DISCARDED means the firework finished its lifecycle/fuse and detonated.
            // This ignores fireworks removed by chunk unloads or teleporting through portals.
            if (firework.isRemoved() && firework.getRemovalReason() == Entity.RemovalReason.DISCARDED) {
                
                // Get the entity that shot the firework
                if (firework.getOwner() instanceof Player player) {
                    trigger(event.getLevel(), firework.getX(), firework.getY(), firework.getZ(), player);
                }
            }
        }
    }

    /**
     * Core Check: Verifies the player has the armor perk and initiates the spawn.
     */
    public static void trigger(Level level, double x, double y, double z, Player player) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        @SuppressWarnings("unchecked")
        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
        
        if (perkComponent != null) {
            ResourceLocation flowingVitreum = ResourceLocation.fromNamespaceAndPath("destinedtraits", "flowing_vitreum");
            
            int armorPieces = WeaponPerkUtils.countOriginArmorPieces(player, flowingVitreum);
            if (armorPieces > 0) {
                boolean isEnhanced = WeaponPerkUtils.hasEnhancedOriginArmor(player, flowingVitreum);
                spawnVitreumPool(serverLevel, x, y, z, player, isEnhanced, armorPieces / 2.0, MasterworkUtils.getStats(player));
            }
        }
    }

    /**
     * Core Action: Spawns the damaging glass shard pool.
     */
    private static void spawnVitreumPool(ServerLevel level, double x, double y, double z, Player owner, boolean isEnhanced,
            double traitMultiplier, MasterworkUtils.Stats masterworks) {
        level.playSound(null, net.minecraft.core.BlockPos.containing(x, y, z), net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        AreaEffectCloud shardPool = new AreaEffectCloud(level, x, y, z);
        shardPool.setOwner(owner);
        
        float radius = (float) ((isEnhanced ? 4.5f : 2.5f) * traitMultiplier * masterworks.rangeMultiplier());
        int duration = masterworks.scaleDuration(Math.max(1, (int) Math.round((isEnhanced ? 160 : 80) * traitMultiplier)));
        int damageTier = isEnhanced ? 1 : 0;  
        
        shardPool.setRadius(radius);
        shardPool.setDuration(duration);
        shardPool.setRadiusOnUse(-0.5f); 
        shardPool.setWaitTime(5); 
        shardPool.setRadiusPerTick(-shardPool.getRadius() / (float) shardPool.getDuration()); 
        
        shardPool.setParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState()));
        // Fetch your custom effect from the registry
        java.util.Optional<net.minecraft.core.Holder.Reference<net.minecraft.world.effect.MobEffect>> flowingVitreumOpt = 
            net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "flowing_vitreum_effect")
            );
            
        if (flowingVitreumOpt.isPresent()) {
            // Apply the custom effect instead of vanilla HARM
            shardPool.addEffect(new MobEffectInstance(flowingVitreumOpt.get(), 1, damageTier));
        }
        
        level.addFreshEntity(shardPool);
    }
}