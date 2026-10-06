package com.doctorfuture.destinedtraits.procedures;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = "destinedtraits")
public class PerpetualMotionProcedure {
	private static final ResourceLocation PERPETUAL_MOTION = ResourceLocation.fromNamespaceAndPath("destinedtraits", "perpetual_motion");
	private static final ResourceLocation RISING_DEEP = ResourceLocation.fromNamespaceAndPath("destinedtraits", "rising_deep");
	private static final ResourceLocation PM_SPEED = ResourceLocation.fromNamespaceAndPath("destinedtraits", "perpetual_motion_handling");
	private static final ResourceLocation PM_DRAW_SPEED = ResourceLocation.fromNamespaceAndPath("destinedtraits", "perpetual_motion_draw_speed");
	private static final ResourceLocation PM_STABILITY = ResourceLocation.fromNamespaceAndPath("destinedtraits", "perpetual_motion_stability");
	private static final ResourceLocation RISING_DEEP_RANGE = ResourceLocation.fromNamespaceAndPath("destinedtraits", "rising_deep_range");
	private static final Map<Player, Integer> MOVEMENT_TICKS = new WeakHashMap<>();

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide()) return;

		ItemStack weapon = player.getMainHandItem();
		MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
		boolean hasPerpetualMotion = WeaponPerkUtils.hasPerk(weapon, PERPETUAL_MOTION);
		int movementTicks = hasPerpetualMotion && player.getDeltaMovement().horizontalDistanceSqr() > 0.0004
				? Math.min(masterworks.scaleEfficiencyCost(200), MOVEMENT_TICKS.getOrDefault(player, 0) + 1) : 0;
		if (movementTicks > 0) MOVEMENT_TICKS.put(player, movementTicks);
		else MOVEMENT_TICKS.remove(player);

		boolean enhanced = hasPerpetualMotion && WeaponPerkUtils.isEnhanced(weapon, PERPETUAL_MOTION);
		int longCharge = masterworks.scaleEfficiencyCost(200);
		int shortCharge = masterworks.scaleEfficiencyCost(40);
		double handlingBonus = movementTicks >= longCharge ? (enhanced ? 0.30 : 0.20)
				: movementTicks >= shortCharge ? (enhanced ? 0.15 : 0.10) : 0.0;
		handlingBonus *= masterworks.strengthMultiplier();
		setModifier(player, Attributes.ATTACK_SPEED, PM_SPEED, handlingBonus);
		setModifier(player, ALObjects.Attributes.DRAW_SPEED, PM_DRAW_SPEED, handlingBonus);
		setModifier(player, Attributes.KNOCKBACK_RESISTANCE, PM_STABILITY,
				movementTicks >= longCharge ? (enhanced ? 0.40 : 0.25) * masterworks.strengthMultiplier()
						: movementTicks >= shortCharge ? (enhanced ? 0.20 : 0.10) * masterworks.strengthMultiplier() : 0.0);

		int repairInterval = masterworks.scaleEfficiencyCooldown(20);
		if (movementTicks >= shortCharge && movementTicks % repairInterval == 0 && weapon.isDamageableItem() && weapon.getDamageValue() > 0) {
			weapon.setDamageValue(Math.max(0, weapon.getDamageValue() - Math.max(1, masterworks.scaleStrength(enhanced ? 2 : 1))));
		}

		boolean hasRisingDeep = WeaponPerkUtils.hasPerk(weapon, RISING_DEEP) && player.isUnderWater();
		double rangeBonus = hasRisingDeep ? (WeaponPerkUtils.isEnhanced(weapon, RISING_DEEP) ? 3.0 : 1.5) * masterworks.rangeMultiplier() : 0.0;
		setModifier(player, Attributes.ENTITY_INTERACTION_RANGE, RISING_DEEP_RANGE, rangeBonus);
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
