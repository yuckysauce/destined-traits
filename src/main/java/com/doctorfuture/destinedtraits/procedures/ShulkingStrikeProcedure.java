package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.core.Direction;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import com.doctorfuture.destinedtraits.network.DestinedtraitsModVariables;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber
public class ShulkingStrikeProcedure {

    // Internal cooldown map to prevent sweeping attacks from instantly granting 3 hits
    private static final Map<UUID, Long> lastHitTime = new ConcurrentHashMap<>();
    
    // Internal cooldown map for the 3-second activation lockout
    private static final Map<UUID, Long> lastActivationTime = new ConcurrentHashMap<>();

    // Helper class to store the results of our weapon scan
    private static class PerkData {
        public boolean hasPerk = false;
        public boolean hasEnhanced = false;
    }

    // Helper method to scan the MAIN HAND for the shulking_strike perk
    private static PerkData getWeaponPerk(Player player) {
        PerkData data = new PerkData();
        ItemStack item = player.getMainHandItem();
        
        if (item.isEmpty()) return data;

        @SuppressWarnings("unchecked")
        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

        if (perkComponent != null) {
            List<ResourceLocation> perks = item.getOrDefault(perkComponent, Collections.emptyList());
            int index = perks.indexOf(ResourceLocation.fromNamespaceAndPath("destinedtraits", "shulking_strike"));
            
            if (index != -1) {
                data.hasPerk = true;
                int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
                
                if (WeaponPerkUtils.isOriginEnhanced(item, ResourceLocation.fromNamespaceAndPath("destinedtraits", "shulking_strike"))) {
                    data.hasEnhanced = true;
                }
            }
        }
        return data;
    }

    // =================================================================================
    // EVENT: RAPID HITS -> SPAWN SHULKER BULLET(S)
    // =================================================================================
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        Entity attackerEntity = event.getSource().getEntity();
        LivingEntity target = event.getEntity();

        // Target the owner of the damage source (allows projectiles)
        if (attackerEntity instanceof Player player) {
            PerkData perkData = getWeaponPerk(player);
            
            if (perkData.hasPerk && !player.level().isClientSide() && player.level() instanceof ServerLevel serverLevel) {
                MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
                
                long currentTime = serverLevel.getGameTime();
                long lastActivation = lastActivationTime.getOrDefault(player.getUUID(), 0L);
                
                // 60 tick (3 seconds) cooldown between perk activations
                if (currentTime - lastActivation >= masterworks.scaleEfficiencyCooldown(60)) {
                    
                    long lastTime = lastHitTime.getOrDefault(player.getUUID(), 0L);
                    
                    // 4 tick (0.2 seconds) internal cooldown to prevent sweeping attacks from counting as multiple hits
                    if (currentTime - lastTime >= 4) {
                        lastHitTime.put(player.getUUID(), currentTime);
                        
                        // Rapid Hit Logic using MCreator's global player variables
                        DestinedtraitsModVariables.PlayerVariables vars = player.getData(DestinedtraitsModVariables.PLAYER_VARIABLES);
                        vars.rapidhit_progress += 1;
                        vars.rapidhit_timer = masterworks.scaleDuration(100);

                        // 3 Hits Reached!
                        if (vars.rapidhit_progress >= masterworks.scaleEfficiencyCost(3)) {
                            vars.rapidhit_progress = 0;
                            vars.rapidhit_timer = 0;
                            
                            // Lock out the perk for 3 seconds
                            lastActivationTime.put(player.getUUID(), currentTime);
                            
                            // Determine bullet count
                            int bulletCount = Math.max(1, (int) Math.ceil((perkData.hasEnhanced ? 2 : 1) * masterworks.strengthMultiplier()));
                            
                            // Audio feedback for the proc, playing at the target's location
                            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 1.0F, 1.2F);
                            
                            // Spawn the homing bullets
                            for (int i = 0; i < bulletCount; i++) {
                                // Using standard vanilla 1.21 Constructor: (Level, Owner, Target, Axis)
                                ShulkerBullet bullet = new ShulkerBullet(serverLevel, player, target, Direction.Axis.Y);
                                
                                // Disperse their spawn positions slightly above and around the VICTIM
                                double offsetX = (serverLevel.random.nextDouble() - 0.5) * 2.0;
                                double offsetY = target.getBbHeight() + 0.5 + serverLevel.random.nextDouble();
                                double offsetZ = (serverLevel.random.nextDouble() - 0.5) * 2.0;
                                
                                bullet.setPos(target.getX() + offsetX, target.getY() + offsetY, target.getZ() + offsetZ);
                                
                                serverLevel.addFreshEntity(bullet);
                            }
                        }
                        vars.markSyncDirty();
                    }
                }
            }
        }
    }
}