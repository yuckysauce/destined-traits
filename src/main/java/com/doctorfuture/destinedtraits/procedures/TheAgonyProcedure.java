package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = "destinedtraits")
public final class TheAgonyProcedure {
    private static final ResourceLocation PERK = ResourceLocation.fromNamespaceAndPath("destinedtraits", "the_agony");

    private TheAgonyProcedure() {}

    @SubscribeEvent
    public static void onMeleeDamageTaken(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player wearer) || wearer.level().isClientSide()
                || event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)
                || !(event.getSource().getEntity() instanceof LivingEntity attacker)
                || !ExoticEffectUtils.hasArmorPerk(wearer, PERK)
                || wearer.getRandom().nextFloat() >= 0.18F) return;

        ServerLevel level = (ServerLevel) wearer.level();
        boolean powered = ExoticEffectUtils.hasNearbyRedstoneSource(level, wearer.blockPosition(), 5);
        double radius = powered ? 9.0 : 5.0;
        float damage = powered ? 7.0F : 3.0F;
        AABB bounds = attacker.getBoundingBox().inflate(radius);
        List<LivingEntity> chain = level.getEntitiesOfClass(LivingEntity.class, bounds,
                entity -> entity instanceof Enemy && entity.isAlive() && entity.distanceToSqr(attacker) <= radius * radius);
        chain.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(attacker)));
        int limit = powered ? 8 : 4;
        int hitCount = 0;
        for (LivingEntity target : chain) {
            if (hitCount >= limit) break;
            target.hurt(wearer.damageSources().lightningBolt(), damage);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() * 0.6,
                    target.getZ(), 12, 0.25, 0.35, 0.25, 0.02);
            hitCount++;
        }
        level.playSound(null, wearer.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS, 0.4F, 1.6F);
        if (hitCount >= 5) wearer.heal(powered ? 6.0F : 4.0F);
    }
}