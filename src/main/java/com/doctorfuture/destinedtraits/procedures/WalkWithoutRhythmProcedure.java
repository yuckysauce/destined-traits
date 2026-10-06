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
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "destinedtraits")
public final class WalkWithoutRhythmProcedure {
    private static final ResourceLocation PERK = ResourceLocation.fromNamespaceAndPath("destinedtraits", "walk_without_rhythm");
    private static final String COMBAT_TICKS = "destinedtraits_sandwalk_combat_ticks";
    private static final String COMBAT_SECONDS = "destinedtraits_sandwalk_combat_seconds";
    private static final String TICK_COUNTER = "destinedtraits_sandwalk_tick_counter";

    private WalkWithoutRhythmProcedure() {}

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof Player player && ExoticEffectUtils.hasArmorPerk(player, PERK)) {
            player.getPersistentData().putInt(COMBAT_TICKS, 100);
        }
        if (event.getSource().getEntity() instanceof Player attacker
                && ExoticEffectUtils.hasArmorPerk(attacker, PERK)) {
            attacker.getPersistentData().putInt(COMBAT_TICKS, 100);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !ExoticEffectUtils.hasArmorPerk(player, PERK)) return;
        var data = player.getPersistentData();
        int combatTicks = data.getInt(COMBAT_TICKS);
        if (combatTicks <= 0) {
            data.putInt(COMBAT_SECONDS, 0);
            return;
        }
        data.putInt(COMBAT_TICKS, combatTicks - 1);
        int tickCounter = data.getInt(TICK_COUNTER) + 1;
        if (tickCounter < 20) {
            data.putInt(TICK_COUNTER, tickCounter);
            return;
        }
        data.putInt(TICK_COUNTER, 0);
        int seconds = data.getInt(COMBAT_SECONDS) + 1;
        data.putInt(COMBAT_SECONDS, seconds);
        float chance = Math.min(0.08F, 0.002F + seconds * 0.001F);
        if (player.getRandom().nextFloat() < chance && player.level() instanceof ServerLevel level) {
            createVortex(player, level);
        }
    }

    private static void createVortex(Player player, ServerLevel level) {
        double radius = 8.0;
        AABB area = player.getBoundingBox().inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity instanceof Enemy && entity.isAlive())) {
            ExoticEffectUtils.launchAway(target, player.position(), 0.9, 1.8);
        }
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(),
                60, 3.0, 0.3, 3.0, 0.12);
        level.playSound(null, player.blockPosition(), SoundEvents.SAND_PLACE,
                SoundSource.PLAYERS, 1.1F, 0.55F);
    }
}