package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

@EventBusSubscriber(modid = "destinedtraits")
public final class WeaponSynergyPerksProcedure {
	private static final ResourceLocation BAIT_AND_SWITCH = id("bait_and_switch");
	private static final ResourceLocation ONE_FOR_ALL = id("one_for_all");
	private static final ResourceLocation ENVIOUS_ARSENAL = id("envious_arsenal");
	private static final String BAIT_WEAPONS = "DestinedTraitsBaitWeapons";
	private static final String BAIT_WINDOW_END = "DestinedTraitsBaitWindowEnd";
	private static final String BAIT_BUFF_END = "DestinedTraitsBaitBuffEnd";
	private static final String BAIT_PERK_WEAPON_HIT = "DestinedTraitsBaitPerkWeaponHit";
	private static final String ONE_FOR_ALL_TARGETS = "DestinedTraitsOneForAllTargets";
	private static final String ONE_FOR_ALL_WINDOW_END = "DestinedTraitsOneForAllWindowEnd";
	private static final String ONE_FOR_ALL_BUFF_END = "DestinedTraitsOneForAllBuffEnd";

	private WeaponSynergyPerksProcedure() {}

	@SubscribeEvent
	public static void onDamagePre(LivingDamageEvent.Pre event) {
		if (!(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide()) return;
		ItemStack weapon = player.getMainHandItem();
		long gameTime = player.level().getGameTime();
		float damage = event.getNewDamage();
		MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);

		if (WeaponPerkUtils.hasPerk(weapon, ONE_FOR_ALL)
				&& player.getPersistentData().getLong(ONE_FOR_ALL_BUFF_END) > gameTime) {
			float bonus = WeaponPerkUtils.isEnhanced(weapon, ONE_FOR_ALL) ? 0.40F : 0.35F;
			damage *= 1.0F + bonus * (float) masterworks.strengthMultiplier();
		}
		if (WeaponPerkUtils.hasPerk(weapon, BAIT_AND_SWITCH)
				&& player.getPersistentData().getLong(BAIT_BUFF_END) > gameTime) {
			float bonus = WeaponPerkUtils.isEnhanced(weapon, BAIT_AND_SWITCH) ? 0.35F : 0.30F;
			damage *= 1.0F + bonus * (float) masterworks.strengthMultiplier();
		}
		event.setNewDamage(damage);
	}

	@SubscribeEvent
	public static void onDamagePost(LivingDamageEvent.Post event) {
		Entity attacker = event.getSource().getEntity();
		if (!(attacker instanceof Player player) || player.level().isClientSide() || event.getEntity() == player) return;
		ItemStack weapon = player.getMainHandItem();
		if (weapon.isEmpty() || !isWeapon(weapon)) return;

		if (hasBaitAndSwitchInInventory(player)) recordBaitAndSwitchHit(player, weapon);
		if (WeaponPerkUtils.hasPerk(weapon, ONE_FOR_ALL)) recordOneForAllHit(player, event.getEntity(), weapon);
	}

	@SubscribeEvent
	public static void onOtherWeaponKill(LivingDeathEvent event) {
		if (!(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide()) return;
		ItemStack killingWeapon = player.getMainHandItem();
		if (!isWeapon(killingWeapon)) return;

		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack candidate = player.getInventory().getItem(slot);
			if (candidate == killingWeapon || !WeaponPerkUtils.hasPerk(candidate, ENVIOUS_ARSENAL) || !candidate.isDamageableItem()) continue;
			MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
			int repair = Math.max(1, (int) Math.ceil(candidate.getMaxDamage()
					* (WeaponPerkUtils.isEnhanced(candidate, ENVIOUS_ARSENAL) ? 0.15 : 0.10) * masterworks.strengthMultiplier()));
			candidate.setDamageValue(Math.max(0, candidate.getDamageValue() - repair));
		}
	}

	private static void recordOneForAllHit(Player player, LivingEntity target, ItemStack weapon) {
		var data = player.getPersistentData();
		long now = player.level().getGameTime();
		ListTag targets = data.getList(ONE_FOR_ALL_TARGETS, Tag.TAG_STRING);
		if (now > data.getLong(ONE_FOR_ALL_WINDOW_END)) {
			targets.clear();
			data.putLong(ONE_FOR_ALL_WINDOW_END, now + 100);
		}
		String targetId = target.getUUID().toString();
		if (!targets.contains(targetId)) targets.add(net.minecraft.nbt.StringTag.valueOf(targetId));
		data.put(ONE_FOR_ALL_TARGETS, targets);
		if (targets.size() >= 3) {
			data.putLong(ONE_FOR_ALL_BUFF_END, now + MasterworkUtils.getStats(player).scaleDuration(200));
			targets.clear();
			data.putLong(ONE_FOR_ALL_WINDOW_END, 0);
		}
	}

	private static void recordBaitAndSwitchHit(Player player, ItemStack weapon) {
		var data = player.getPersistentData();
		long now = player.level().getGameTime();
		ListTag weapons = data.getList(BAIT_WEAPONS, Tag.TAG_STRING);
		if (weapons.isEmpty() || now > data.getLong(BAIT_WINDOW_END)) {
			weapons.clear();
			data.putLong(BAIT_WINDOW_END, now + 70);
			data.putBoolean(BAIT_PERK_WEAPON_HIT, false);
		}
		String weaponId = BuiltInRegistries.ITEM.getKey(weapon.getItem()).toString();
		if (!weapons.contains(weaponId)) weapons.add(net.minecraft.nbt.StringTag.valueOf(weaponId));
		if (WeaponPerkUtils.hasPerk(weapon, BAIT_AND_SWITCH)) data.putBoolean(BAIT_PERK_WEAPON_HIT, true);
		data.put(BAIT_WEAPONS, weapons);
		if (weapons.size() >= 3 && data.getBoolean(BAIT_PERK_WEAPON_HIT)) {
			data.putLong(BAIT_BUFF_END, now + MasterworkUtils.getStats(player).scaleDuration(200));
			weapons.clear();
			data.putLong(BAIT_WINDOW_END, 0);
			data.putBoolean(BAIT_PERK_WEAPON_HIT, false);
		}
	}

	private static boolean hasBaitAndSwitchInInventory(Player player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (WeaponPerkUtils.hasPerk(player.getInventory().getItem(slot), BAIT_AND_SWITCH)) return true;
		}
		return false;
	}

	private static boolean isWeapon(ItemStack stack) {
		return stack.getItem() instanceof TieredItem
				|| stack.getItem() instanceof ProjectileWeaponItem
				|| stack.getItem() instanceof TridentItem
				|| stack.getItem() instanceof MaceItem
				|| stack.is(ItemTags.WEAPON_ENCHANTABLE);
	}

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath("destinedtraits", path);
	}
}