package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

@EventBusSubscriber
public class ExplosiveHeadProcedure {
    
    @SubscribeEvent
    public static void onProjectileHit(ProjectileImpactEvent event) {
        // Restrict the event to only trigger if the projectile is an instance of AbstractArrow
        if (event.getProjectile() instanceof net.minecraft.world.entity.projectile.AbstractArrow) {
            if (event.getProjectile().getOwner() != null) {
                execute(event, event.getProjectile().level(), event.getProjectile().getX(), event.getProjectile().getY(), event.getProjectile().getZ(), event.getProjectile().getOwner());
            }
        }
    }

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        execute(null, world, x, y, z, entity);
    }

    private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
        System.out.println("Step 1: Impact!");
        if (entity == null)
            return;
        double amplifier = 0;
        double enhancedbonus = 0;
        double duration = 0;
        if (!world.isClientSide()) {
            if (entity.isAlive()) {
                System.out.println("Step 2: Alive check passed");
                    if ((entity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY)
                            .getOrDefault((net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
                                    .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks")), java.util.Collections.emptyList())
                            .contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "explosive_head")) == true) {
                            System.out.println("Step 3: Perk check passed");
                        if (java.util.Optional.of(entity instanceof net.minecraft.world.entity.LivingEntity _livEnt ? _livEnt.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY).map(item -> {
                            net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>> comp = (net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
                                    .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
                            java.util.List<net.minecraft.resources.ResourceLocation> perks = item.getOrDefault(comp, java.util.Collections.emptyList());
                            int index = perks.indexOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "explosive_head"));
                            if (index == -1)
                                return false; // The weapon does not have the perk at all
                            int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
                            // Returns true if left perk is tier 3+, or right perk is tier 4
                            return WeaponPerkUtils.isEnhancedAtIndex(item, index);
                        }).orElse(false)) {
                            enhancedbonus = 5;
                        } else {
                            enhancedbonus = 3;
                        } // 1. Trigger the visual particles and audio of an explosion
                    if (world instanceof net.minecraft.server.level.ServerLevel _serverLevel) {
                        if (entity instanceof net.minecraft.world.entity.player.Player _player) {
                            MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(_player);
                            System.out.println("Step 3: Effect display");
                            FlowingVitreumProcedure.trigger(_serverLevel, x, y, z, _player);
                            _serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
                            _serverLevel.playSound(null, net.minecraft.core.BlockPos.containing(x, y, z), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                            // 2. Set your custom damage amount here
                            float explosiveDamage = 2.0F * (float) masterworks.strengthMultiplier();
                            double blastRadius = enhancedbonus * masterworks.rangeMultiplier();
                            // 3. Define the blast area
                            net.minecraft.world.phys.AABB blastBox = new net.minecraft.world.phys.AABB(x - blastRadius, y - blastRadius, z - blastRadius, x + blastRadius, y + blastRadius, z + blastRadius);
                            // 4. Find and damage entities within the radius manually
                            for (net.minecraft.world.entity.Entity target : _serverLevel.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, blastBox, e -> true)) {
                                // Check if they are within a true sphere (not a cube) and prevent damaging the attacking player
                                if (target != _player && target.distanceToSqr(x, y, z) <= (blastRadius * blastRadius)) {
                                    // FIX 2: Set the player as BOTH the direct and indirect source of the explosion
                                    target.hurt(target.damageSources().explosion(_player, _player), explosiveDamage);
                                }
                            }    
                        }
                    }
                }
            }
        }
    }
}