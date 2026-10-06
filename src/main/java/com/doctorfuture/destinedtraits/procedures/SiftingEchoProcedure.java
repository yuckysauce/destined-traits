package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "destinedtraits")
public final class SiftingEchoProcedure {
    private static final ResourceLocation PERK = ResourceLocation.fromNamespaceAndPath("destinedtraits", "sifting_echo");
    private static final String HUNGER_TICKS_KEY = "destinedtraits_sifting_echo_hunger_ticks";
    private static final String CHECK_TICKS_KEY = "destinedtraits_sifting_echo_check_ticks";
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final int BASE_HUNGER_INTERVAL_TICKS = 200;
    private static final double DETECTION_MARGIN = 4.0;
    private static final double SEARCH_RADIUS = 64.0;

    private SiftingEchoProcedure() {}

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();
        if (!event.isVanillaCritical() || player.level().isClientSide() || !player.isInvisible()) return;

        double armorMultiplier = WeaponPerkUtils.getOriginArmorMultiplier(player, PERK);
        if (armorMultiplier <= 0.0) return;

        event.setDamageMultiplier(event.getDamageMultiplier() * (1.0F + (float) (0.25 * armorMultiplier)));
    }

    @SubscribeEvent
    public static void onMobChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(event.getNewAboutToBeSetTarget() instanceof Player player)
                || player.level().isClientSide() || !player.isInvisible()
                || WeaponPerkUtils.countOriginArmorPieces(player, PERK) == 0) return;

        if (mob.distanceTo(player) > getDetectionRange(mob, true)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        double armorMultiplier = WeaponPerkUtils.getOriginArmorMultiplier(player, PERK);
        var data = player.getPersistentData();
        if (armorMultiplier <= 0.0) {
            data.remove(HUNGER_TICKS_KEY);
            data.remove(CHECK_TICKS_KEY);
            return;
        }

        int checkTicks = data.getInt(CHECK_TICKS_KEY) + 1;
        if (checkTicks < CHECK_INTERVAL_TICKS) {
            data.putInt(CHECK_TICKS_KEY, checkTicks);
            return;
        }
        data.putInt(CHECK_TICKS_KEY, 0);

        if (player.isInvisible()) clearDistantMobTargets(player);

        if (player.getFoodData().getFoodLevel() >= 20 || !isJustOutsideMobDetection(player)) {
            data.putInt(HUNGER_TICKS_KEY, 0);
            return;
        }

        int hungerInterval = Math.max(1, (int) Math.ceil(BASE_HUNGER_INTERVAL_TICKS / armorMultiplier));
        int hungerTicks = data.getInt(HUNGER_TICKS_KEY) + CHECK_INTERVAL_TICKS;
        if (hungerTicks >= hungerInterval) {
            player.getFoodData().eat(1, 0.0F);
            hungerTicks -= hungerInterval;
        }
        data.putInt(HUNGER_TICKS_KEY, hungerTicks);
    }

    private static boolean isJustOutsideMobDetection(ServerPlayer player) {
        AABB searchBox = player.getBoundingBox().inflate(SEARCH_RADIUS);
        boolean nearDetectionRange = false;

        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, searchBox,
                candidate -> candidate.isAlive() && candidate.getType().getCategory() == MobCategory.MONSTER)) {
            var followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
            if (followRange == null) continue;

            double distance = mob.distanceTo(player);
            double detectionRange = getDetectionRange(mob, player.isInvisible());
            if (distance <= detectionRange) return false;
            if (distance <= detectionRange + DETECTION_MARGIN) nearDetectionRange = true;
        }

        return nearDetectionRange;
    }

    private static double getDetectionRange(Mob mob, boolean invisibleWithPerk) {
        var followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange == null) return 0.0;
        return followRange.getValue() * (invisibleWithPerk ? 0.07 : 1.0);
    }

    private static void clearDistantMobTargets(ServerPlayer player) {
        AABB searchBox = player.getBoundingBox().inflate(SEARCH_RADIUS);
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, searchBox,
                candidate -> candidate.isAlive() && candidate.getTarget() == player)) {
            if (mob.distanceTo(player) > getDetectionRange(mob, true)) {
                mob.setTarget(null);
            }
        }
    }

}