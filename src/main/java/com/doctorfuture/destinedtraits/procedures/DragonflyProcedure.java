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


import javax.annotation.Nullable;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

@EventBusSubscriber
public class DragonflyProcedure {
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
	        if (sourceentity instanceof net.minecraft.world.entity.player.Player _player && _player.fallDistance > 0.0F && !_player.onGround() && !_player.onClimbable() && !_player.isInWater()
	                && !_player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) && !_player.isPassenger() && !_player.isSprinting()) {
	            if (sourceentity instanceof Player) {
	                if ((sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY)
	                        .getOrDefault((net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
	                                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks")), java.util.Collections.emptyList())
	                        .contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "dragonfly")) == true) {
	                    if (java.util.Optional.of(sourceentity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY).map(item -> {
	                        net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>> comp = (net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
	                                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
	                        java.util.List<net.minecraft.resources.ResourceLocation> perks = item.getOrDefault(comp, java.util.Collections.emptyList());
	                        int index = perks.indexOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "dragonfly"));
	                        if (index == -1)
	                            return false; // The weapon does not have the perk at all
	                        int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
	                        // Returns true if left perk is tier 3+, or right perk is tier 4
	                        return WeaponPerkUtils.isEnhancedAtIndex(item, index);
	                    }).orElse(false)) {
	                        enhancedbonus = 8;
	                    } else {
	                        enhancedbonus = 5;
	                    }
	                    if (world instanceof net.minecraft.server.level.ServerLevel _serverLevel) {
	                        // 1. Trigger the visual particles and audio of an explosion
	                        FlowingVitreumProcedure.trigger(_serverLevel, x, y, z, _player);
	                        _serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
	                        _serverLevel.playSound(null, net.minecraft.core.BlockPos.containing(x, y, z), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
	                        // 2. Set your custom damage amount here
	                        MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(_player);
	                        float dragonflyDamage = 6.0F * (float) masterworks.strengthMultiplier();
	                        dragonflyDamage *= (float) masterworks.strengthMultiplier();
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
	                                target.hurt(target.damageSources().explosion(_player, _player), dragonflyDamage);
	                                
	                                // 2. Fetch the Fire Aspect enchantment from the 1.21 server registry
	                                net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> fireAspect = _serverLevel.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT);
	                                // 3. Get the level of Fire Aspect on the attacker's main hand item (returns 0 if none)
	                                int fireLevel = _player.getMainHandItem().getEnchantmentLevel(fireAspect);
	                                // 4. If the weapon has the enchantment, apply fire
	                                if (fireLevel > 0) {
	                                    // Vanilla Fire Aspect applies 4 seconds of fire per level of the enchantment
	                                    target.igniteForSeconds(4 * fireLevel);
	                                }
	                            }
	                        }
	                    }
	                }
	            }
	        }
	    }
    }
}