package com.doctorfuture.destinedtraits.procedures;

import com.doctorfuture.destinedtraits.perks.MasterworkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = "destinedtraits")
public final class ShriekingEvasionProcedure {
    private static final ResourceLocation PERK = ResourceLocation.fromNamespaceAndPath("destinedtraits", "shrieking_evasion");
    private static final ResourceLocation LEGACY_PERK = ResourceLocation.fromNamespaceAndPath("destinedtraits", "creeping_tendrils");
    private static final int BASE_EFFECT_DURATION = 200;
    private static final double BASE_RADIUS = 8.0;
    private static final Map<LivingEntity, PendingCriticalHit> PENDING_CRITICAL_HITS = new WeakHashMap<>();

    private ShriekingEvasionProcedure() {}

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(event.getTarget() instanceof LivingEntity target)
                || !(event.isVanillaCritical() || event.getDamageMultiplier() > 1.0F)
                || !(WeaponPerkUtils.hasPerk(player.getMainHandItem(), PERK)
                        || WeaponPerkUtils.hasPerk(player.getMainHandItem(), LEGACY_PERK))) return;

        PENDING_CRITICAL_HITS.entrySet().removeIf(entry ->
                entry.getValue().gameTick() < entry.getKey().level().getGameTime());
        PENDING_CRITICAL_HITS.put(target, new PendingCriticalHit(player, target.level().getGameTime()));
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity corpse = event.getEntity();
        PendingCriticalHit criticalHit = PENDING_CRITICAL_HITS.remove(corpse);
        if (corpse.level().isClientSide() || criticalHit == null
                || criticalHit.gameTick() != corpse.level().getGameTime()
                || event.getSource().getEntity() != criticalHit.attacker()) return;

        if (!(corpse.level() instanceof ServerLevel level)) return;
        Player player = criticalHit.attacker();
        MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,
                masterworks.scaleDuration(BASE_EFFECT_DURATION), 0, false, false, false));

        double x = corpse.getX();
        double y = corpse.getY() + corpse.getBbHeight() * 0.5;
        double z = corpse.getZ();
        double radius = BASE_RADIUS * masterworks.rangeMultiplier();
        int effectDuration = masterworks.scaleDuration(BASE_EFFECT_DURATION);

        level.playSound(null, BlockPos.containing(x, y, z), SoundEvents.SCULK_SHRIEKER_SHRIEK,
                SoundSource.HOSTILE, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.SCULK_SOUL, x, y, z, 32, 1.5, 0.7, 1.5, 0.02);
        level.sendParticles(ParticleTypes.SONIC_BOOM, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);

        AABB area = new AABB(x, y, z, x, y, z).inflate(radius);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area,
                candidate -> candidate.isAlive() && candidate.distanceToSqr(x, y, z) <= radius * radius)) {
            mob.addEffect(new MobEffectInstance(MobEffects.DARKNESS, effectDuration, 0, false, true, true));
            mob.setTarget(null);
        }
    }

    private record PendingCriticalHit(Player attacker, long gameTick) {}
}