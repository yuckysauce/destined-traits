package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

import com.doctorfuture.destinedtraits.network.DestinedtraitsModVariables;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import javax.annotation.Nullable;

@EventBusSubscriber
public class GlassThemProcedure {
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (event.getEntity() != null) {
            execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getSource().getEntity());
        }
    }

    public static void execute(LevelAccessor world, double x, double y, double z, Entity sourceentity) {
        execute(null, world, x, y, z, sourceentity);
    }

    private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity sourceentity) {
        if (sourceentity == null)
            return;
        double amplifier = 0;
        double enhancedbonus = 0;
        double duration = 0;
        
        if (!world.isClientSide()) {
            if (sourceentity instanceof Player _player) {
                if ((sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY)
                        .getOrDefault((net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
                                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks")), java.util.Collections.emptyList())
                        .contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "glass_them")) == true) {
                    
                    // Determine Tier Bonus
                    if (java.util.Optional.of(sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY).map(item -> {
                        net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>> comp = (net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
                                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
                        java.util.List<net.minecraft.resources.ResourceLocation> perks = item.getOrDefault(comp, java.util.Collections.emptyList());
                        int index = perks.indexOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "glass_them"));
                        if (index == -1)
                            return false; // The weapon does not have the perk at all
                        int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
                        return WeaponPerkUtils.isOriginEnhanced(item, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "glass_them"));
                    }).orElse(false)) {
                        enhancedbonus = 5;
                    } else {
                        enhancedbonus = 3;
                    }
                    MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(_player);

                    // Increment Rapid Kill Progress
                    {
                        DestinedtraitsModVariables.PlayerVariables _vars = sourceentity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES);
                        _vars.rapidkill_progress = sourceentity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES).rapidkill_progress
                            + (35 * masterworks.efficiencyMultiplier());
                        _vars.rapidkill_timer = masterworks.scaleDuration(100);
                        _vars.markSyncDirty();
                    }

                    // Trigger Explosion if Progress is >= 100
                    if (sourceentity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES).rapidkill_progress >= masterworks.scaleEfficiencyCost(100)) {
                        if (world instanceof net.minecraft.server.level.ServerLevel _serverLevel) {
                            // 1. Trigger the visual particles and audio of an explosion
                            FlowingVitreumProcedure.trigger(_serverLevel, x, y, z, _player);
                            _serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
                            _serverLevel.playSound(null, net.minecraft.core.BlockPos.containing(x, y, z), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                            _serverLevel.playSound(null, net.minecraft.core.BlockPos.containing(x, y, z), net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                            // 2. Set your custom damage amount here
                            float glassThemDamage = 4.0F * (float) masterworks.strengthMultiplier();
                            double blastRadius = enhancedbonus * masterworks.rangeMultiplier();
                            // 3. Define the blast area
                            net.minecraft.world.phys.AABB blastBox = new net.minecraft.world.phys.AABB(x - blastRadius, y - blastRadius, z - blastRadius, x + blastRadius, y + blastRadius, z + blastRadius);
                            // 4. Find and damage entities within the radius manually
                            for (net.minecraft.world.entity.Entity target : _serverLevel.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, blastBox, e -> true)) {
                                // Check if they are within a true sphere (not a cube) and prevent damaging the attacking player
                                if (target != _player && target.distanceToSqr(x, y, z) <= (blastRadius * blastRadius)) {
                                    
                                    // FIX 1: Strip invulnerability frames so the explosion damage always connects
                                    target.invulnerableTime = 0;
                                    
                                    // FIX 2: Set the player as BOTH the direct and indirect source of the explosion
                                    target.hurt(target.damageSources().explosion(_player, _player), glassThemDamage);
                                }
                            }
                        }

                        // Reset Rapid Kill Progress after explosion triggers
                        {
                            DestinedtraitsModVariables.PlayerVariables _vars = sourceentity.getData(DestinedtraitsModVariables.PLAYER_VARIABLES);
                            _vars.rapidkill_timer = 0;
                            _vars.rapidkill_progress = 0;
                            _vars.markSyncDirty();
                        }
                    }
                }
            }
        }
    }
}