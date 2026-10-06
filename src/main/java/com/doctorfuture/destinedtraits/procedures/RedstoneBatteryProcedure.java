package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "destinedtraits")
public final class RedstoneBatteryProcedure {
    private static final ResourceLocation PERK = ResourceLocation.fromNamespaceAndPath("destinedtraits", "redstone_battery");
    private static final ResourceLocation ATTACK_SPEED_MODIFIER = ResourceLocation.fromNamespaceAndPath("destinedtraits", "redstone_battery_attack_speed");
    private static final String BATTERY = "destinedtraits_redstone_battery";
    private static final String IDLE_TICKS = "destinedtraits_redstone_battery_idle";
    private static final String OVERCLOCK = "destinedtraits_redstone_battery_overclock";

    private RedstoneBatteryProcedure() {}

    public static void handleUseInput(Player player, boolean held) {
        if (player.level().isClientSide()) return;
        if (held) {
            if (WeaponPerkUtils.hasPerk(player.getMainHandItem(), PERK)) {
                player.getPersistentData().putBoolean(OVERCLOCK, true);
            }
        } else {
            player.getPersistentData().putBoolean(OVERCLOCK, false);
        }
    }

    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player player)
                || !WeaponPerkUtils.hasPerk(player.getMainHandItem(), PERK)) return;
        var data = player.getPersistentData();
        data.putFloat(BATTERY, Math.max(0.0F, data.getFloat(BATTERY) - 8.0F));
        data.putInt(IDLE_TICKS, 0);
    }

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();
        if (!WeaponPerkUtils.hasPerk(player.getMainHandItem(), PERK)) return;
        var data = player.getPersistentData();
        float battery = data.getFloat(BATTERY);
        float bonus = battery / 100.0F * 0.5F + (data.getBoolean(OVERCLOCK) ? 0.25F : 0.0F);
        event.setDamageMultiplier(event.getDamageMultiplier() * (1.0F + bonus));
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)
                || !WeaponPerkUtils.hasPerk(player.getMainHandItem(), PERK)) return;
        player.getPersistentData().putFloat(BATTERY,
                Math.min(100.0F, player.getPersistentData().getFloat(BATTERY) + 2.0F));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var data = player.getPersistentData();
        if (!WeaponPerkUtils.hasPerk(player.getMainHandItem(), PERK)) {
            data.putBoolean(OVERCLOCK, false);
            setAttackSpeed(player, 0.0);
            return;
        }

        float battery = data.getFloat(BATTERY);
        boolean overclock = data.getBoolean(OVERCLOCK);
        int idleTicks = data.getInt(IDLE_TICKS);
        if (overclock) {
            battery = Math.max(0.0F, battery - 0.08F);
            if (battery <= 0.0F) data.putBoolean(OVERCLOCK, false);
        } else {
            idleTicks++;
            if (idleTicks >= 100 && player.tickCount % 5 == 0) {
                boolean powered = ExoticEffectUtils.hasNearbyRedstoneSource((ServerLevel) player.level(), player.blockPosition(), 15);
                battery = Math.min(100.0F, battery + (powered ? 1.0F : 0.2F));
            }
        }
        data.putFloat(BATTERY, battery);
        data.putInt(IDLE_TICKS, idleTicks);
        setAttackSpeed(player, battery / 100.0 * 0.8 + (overclock ? 0.6 : 0.0));
        if (player.tickCount % 5 == 0) {
            String status = overclock ? "Overclocking" : "Charge";
            player.displayClientMessage(Component.literal("Redstone Battery: " + status + " ("
                    + Math.round(battery) + "%)"), true);
        }
    }

    private static void setAttackSpeed(Player player, double amount) {
        AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attribute == null) return;
        AttributeModifier current = attribute.getModifier(ATTACK_SPEED_MODIFIER);
        if (current != null && current.amount() == amount) return;
        if (current != null) attribute.removeModifier(ATTACK_SPEED_MODIFIER);
        if (amount > 0.0) {
            attribute.addTransientModifier(new AttributeModifier(ATTACK_SPEED_MODIFIER, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}