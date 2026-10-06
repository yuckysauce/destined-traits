package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "destinedtraits")
public final class WildWovenProcedure {
    private static final ResourceLocation WILD_WOVEN = ResourceLocation.fromNamespaceAndPath("destinedtraits", "wild_woven");
    private static final ResourceLocation REACH_MODIFIER = ResourceLocation.fromNamespaceAndPath("destinedtraits", "wild_woven_reach");
    private static final ResourceLocation KNOCKBACK_MODIFIER = ResourceLocation.fromNamespaceAndPath("destinedtraits", "wild_woven_knockback");
    private static final String STACKS_KEY = "destinedtraits_wild_woven_stacks";
    private static final int MAX_STACKS = 10;
    private static final double BONUS_PER_STACK = 0.1;

    private WildWovenProcedure() {}

    @SubscribeEvent
    public static void onPlayerDealDamage(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof Player player) addStack(player);
    }

    @SubscribeEvent
    public static void onExperiencePickup(PlayerXpEvent.PickupXp event) {
        addStack(event.getEntity());
    }

    @SubscribeEvent
    public static void onItemUseFinished(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof Player player) addStack(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        int armorPieces = WeaponPerkUtils.countOriginArmorPieces(player, WILD_WOVEN);
        if (armorPieces == 0) {
            player.getPersistentData().remove(STACKS_KEY);
            setModifier(player, Attributes.ENTITY_INTERACTION_RANGE, REACH_MODIFIER, 0.0);
            setModifier(player, Attributes.ATTACK_KNOCKBACK, KNOCKBACK_MODIFIER, 0.0);
            return;
        }

        int stacks = player.getPersistentData().getInt(STACKS_KEY);
        double bonus = stacks * BONUS_PER_STACK * (armorPieces / 2.0);
        setModifier(player, Attributes.ENTITY_INTERACTION_RANGE, REACH_MODIFIER, bonus);
        setModifier(player, Attributes.ATTACK_KNOCKBACK, KNOCKBACK_MODIFIER, bonus);
    }

    private static void addStack(Player player) {
        if (player.level().isClientSide() || WeaponPerkUtils.countOriginArmorPieces(player, WILD_WOVEN) == 0) return;
        var data = player.getPersistentData();
        data.putInt(STACKS_KEY, Math.min(MAX_STACKS, data.getInt(STACKS_KEY) + 1));
    }

    private static void setModifier(Player player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            ResourceLocation id, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier current = instance.getModifier(id);
        if (current != null && current.amount() == amount) return;
        if (current != null) instance.removeModifier(id);
        if (amount > 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}