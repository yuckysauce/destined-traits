package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "destinedtraits")
public final class HealingSalveProcedure {
    private static final ResourceLocation PERK = ResourceLocation.fromNamespaceAndPath("destinedtraits", "healing_salve");
    private static final String COOLDOWN = "destinedtraits_mirewaders_cooldown";
    private static final String FOOD_LEFT = "destinedtraits_mirewaders_food_left";
    private static final String ABSORPTION_BONUS = "destinedtraits_mirewaders_absorption_bonus";
    private static final String ABSORPTION_TIME = "destinedtraits_mirewaders_absorption_time";
    private static final int COOLDOWN_TICKS = 6000;
    private static final int ABSORPTION_TICKS = 300;

    private HealingSalveProcedure() {}

    @SubscribeEvent
    public static void onLethalDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()
                || !ExoticEffectUtils.hasArmorPerk(player, PERK)
                || player.getPersistentData().getInt(COOLDOWN) > 0
                || player.getFoodData().getFoodLevel() <= 0 || event.getNewDamage() < player.getHealth()) return;

        event.setNewDamage(0.0F);
        var data = player.getPersistentData();
        data.putInt(COOLDOWN, COOLDOWN_TICKS);
        data.putInt(FOOD_LEFT, player.getFoodData().getFoodLevel());
        data.putInt(ABSORPTION_TIME, ABSORPTION_TICKS);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
        var data = player.getPersistentData();
        int cooldown = data.getInt(COOLDOWN);
        if (cooldown > 0) data.putInt(COOLDOWN, cooldown - 1);

        int foodLeft = data.getInt(FOOD_LEFT);
        if (foodLeft > 0) {
            int foodLevel = player.getFoodData().getFoodLevel();
            float absorption = player.getAbsorptionAmount();
            if (foodLevel <= 0 || absorption >= 40.0F) {
                data.putInt(FOOD_LEFT, 0);
            } else {
                player.getFoodData().setFoodLevel(foodLevel - 1);
                float gained = Math.min(2.0F, 40.0F - absorption);
                player.setAbsorptionAmount(absorption + gained);
                data.putFloat(ABSORPTION_BONUS, data.getFloat(ABSORPTION_BONUS) + gained);
                data.putInt(FOOD_LEFT, foodLeft - 1);
            }
            if (data.getInt(FOOD_LEFT) <= 0) data.putInt(ABSORPTION_TIME, ABSORPTION_TICKS);
        }

        int absorptionTime = data.getInt(ABSORPTION_TIME);
        if (absorptionTime > 0 && data.getInt(FOOD_LEFT) <= 0) {
            absorptionTime--;
            if (absorptionTime == 0) {
                player.setAbsorptionAmount(Math.max(0.0F, player.getAbsorptionAmount() - data.getFloat(ABSORPTION_BONUS)));
                data.remove(ABSORPTION_BONUS);
                data.remove(ABSORPTION_TIME);
            } else {
                data.putInt(ABSORPTION_TIME, absorptionTime);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        var data = player.getPersistentData();
        data.remove(COOLDOWN);
        data.remove(FOOD_LEFT);
        data.remove(ABSORPTION_BONUS);
        data.remove(ABSORPTION_TIME);
    }
}