package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = "destinedtraits")
public final class SuperconductorProcedure {
    private static final ResourceLocation PERK = ResourceLocation.fromNamespaceAndPath("destinedtraits", "superconductor");
    private static final String CHARGED_ARROW = "destinedtraits_superconductor_arrow";
    private static final String COOLDOWN = "destinedtraits_superconductor_cooldown";
    private static final int COOLDOWN_TICKS = 200;
    private static final Map<UUID, Long> CHARGED_SHOTS = new HashMap<>();

    private SuperconductorProcedure() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int cooldown = player.getPersistentData().getInt(COOLDOWN);
        if (cooldown > 0) player.getPersistentData().putInt(COOLDOWN, cooldown - 1);
        if (player.tickCount % 5 == 0 && WeaponPerkUtils.hasPerk(player.getMainHandItem(), PERK)) {
            if (cooldown > 0) {
                player.displayClientMessage(Component.literal("Superconductor: Lightning recharging ("
                    + ((cooldown + 19) / 20) + "s)"), true);
            } else {
                String status = player.isUsingItem()
                    ? "Fully draw and release to call lightning" : "Ready";
                player.displayClientMessage(Component.literal("Superconductor: " + status), true);
            }
        }
    }

    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        Player player = event.getEntity();
        if (event.getLevel().isClientSide() || !WeaponPerkUtils.hasPerk(event.getBow(), PERK)
                || event.getCharge() < 80 || player.getPersistentData().getInt(COOLDOWN) > 0) return;
        CHARGED_SHOTS.put(player.getUUID(), player.level().getGameTime());
        player.getPersistentData().putInt(COOLDOWN, COOLDOWN_TICKS);
    }

    @SubscribeEvent
    public static void onArrowJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow) || !(arrow.getOwner() instanceof Player player)) return;
        Long chargedTick = CHARGED_SHOTS.get(player.getUUID());
        if (chargedTick != null && player.level().getGameTime() - chargedTick <= 1) {
            arrow.getPersistentData().putBoolean(CHARGED_ARROW, true);
            CHARGED_SHOTS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)
                || !arrow.getPersistentData().getBoolean(CHARGED_ARROW)
                || !(arrow.level() instanceof ServerLevel level)) return;
        Vec3 impact = event.getRayTraceResult().getLocation();
        var bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(impact.x, impact.y, impact.z);
            if (arrow.getOwner() instanceof ServerPlayer player) bolt.setCause(player);
            level.addFreshEntity(bolt);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, impact.x, impact.y, impact.z, 28, 0.8, 0.8, 0.8, 0.08);
        level.playSound(null, arrow.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.8F, 1.1F);
        arrow.discard();
        event.setCanceled(true);
    }
}