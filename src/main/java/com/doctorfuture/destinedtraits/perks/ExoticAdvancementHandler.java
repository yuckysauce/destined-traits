package com.doctorfuture.destinedtraits.perks;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.doctorfuture.destinedtraits.procedures.WeaponPerkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = "destinedtraits")
public final class ExoticAdvancementHandler {
	private static final ResourceLocation DESTINED_TRAITS_ROOT = id("destined_traits_root");
	private static final ResourceLocation ENHANCE = id("enhance");
	private static final ResourceLocation MORE_VAULT_SPACE = id("more_vault_space");
	private static final ResourceLocation LIFE_EXOTIC = id("the_life_exotic");
	private static final ResourceLocation INFERNAL_COLLECTION = id("infernal_collection");
	private static final ResourceLocation PATHFINDER_COLLECTION = id("pathfinder_collection");
	private static final ResourceLocation ABYSSAL_COLLECTION = id("abyssal_collection");
	private static final ResourceLocation ENDFARER_COLLECTION = id("endfarer_collection");
	private static final ResourceLocation RIMEBOUND_COLLECTION = id("rimebound_collection");
	private static final ResourceLocation MARSHLAND_COLLECTION = id("marshland_collection");
	private static final ResourceLocation SKYSCRAPER_COLLECTION = id("skyscraper_collection");
	private static final ResourceLocation SANDSWEEP_COLLECTION = id("sandswept_collection");
	private static final ResourceLocation TREASURE_HUNTER = id("treasure_hunter");
	private static final ResourceLocation COMPLETIONIST = id("completionist");
	private static final String PERKED_ITEMS_COUNT_TAG = "destinedtraits_perked_items_count";
	private static final int MORE_VAULT_SPACE_ITEM_COUNT = 1000;
	private static final Map<ServerPlayer, List<StackCount>> INVENTORY_SNAPSHOTS = new WeakHashMap<>();

	private static final class StackCount {
		private final ItemStack stack;
		private int count;

		private StackCount(ItemStack stack, int count) {
			this.stack = stack.copyWithCount(1);
			this.count = count;
		}
	}

	private static final Map<ResourceLocation, ResourceLocation> ORIGIN_ADVANCEMENTS = Map.ofEntries(
			Map.entry(id("photosynthetic"), id("get_cycadian_arbor")),
			Map.entry(id("forsaken_rite"), id("get_hades_arcanum")),
			Map.entry(id("guillotine_swinging"), id("get_the_executioner")),
			Map.entry(id("soul_of_the_ancients"), id("get_warden_of_eternities")),
			Map.entry(id("ender_artillery"), id("get_the_voidshell")),
			Map.entry(id("tidal_pulse"), id("get_strides_of_the_deep")),
			Map.entry(id("nowhere_fast"), id("get_haakons_kedge")),
			Map.entry(id("soulfire_eminence"), id("get_soulfire_diadem")),
			Map.entry(id("the_old_wounds"), id("get_alchemy_carve")),
			Map.entry(id("north_star"), id("get_polaris")),
			Map.entry(id("superconductor"), id("get_the_pylon")),
			Map.entry(id("clean_landing"), id("get_moonshot")),
			Map.entry(id("redstone_battery"), id("get_crimson_divisor")),
			Map.entry(id("unwelcome"), id("get_silent_phalanx")),
			Map.entry(id("the_agony"), id("get_electric_head")),
			Map.entry(id("fire_when_ready"), id("get_bunker_in_place")),
			Map.entry(id("healing_salve"), id("get_mirewaders")),
			Map.entry(id("walk_without_rhythm"), id("get_sandwalk_wraps")),
			Map.entry(id("second_chance"), id("get_armadan_repulsors"))
	);
	private static final Map<ResourceLocation, String> INFERNAL_CRITERIA = Map.of(
			id("forsaken_rite"), "hades_arcanum",
			id("soulfire_eminence"), "soulfire_diadem",
			id("guillotine_swinging"), "the_executioner"
	);
	private static final Map<ResourceLocation, String> TREASURE_CRITERIA = Map.of(
			id("soul_of_the_ancients"), "warden_of_eternities",
			id("nowhere_fast"), "haakons_kedge",
			id("fire_when_ready"), "fire_when_ready"
	);
	private static final Map<ResourceLocation, String> PATHFINDER_CRITERIA = Map.of(
			id("photosynthetic"), "cycadian_arbor",
			id("the_old_wounds"), "alchemy_carve"
	);
	private static final Map<ResourceLocation, String> ABYSSAL_CRITERIA = Map.of(
			id("nowhere_fast"), "haakons_kedge",
			id("tidal_pulse"), "strides_of_the_deep"
	);
	private static final Map<ResourceLocation, String> ENDFARER_CRITERIA = Map.of(
			id("ender_artillery"), "the_voidshell",
			id("clean_landing"), "moonshot",
			id("second_chance"), "armadan_repulsors"
	);
	private static final Map<ResourceLocation, String> FROSTBOUND_CRITERIA = Map.of(
			id("north_star"), "north_star"
	);
	private static final Map<ResourceLocation, String> SWAMP_CRITERIA = Map.of(
			id("superconductor"), "the_pylon",
			id("healing_salve"), "mirewaders"
	);
	private static final Map<ResourceLocation, String> MOUNTAIN_CRITERIA = Map.of(
			id("redstone_battery"), "crimson_divisor",
			id("the_agony"), "electric_head"
	);
	private static final Map<ResourceLocation, String> DESERT_CRITERIA = Map.of(
			id("unwelcome"), "silent_phalanx",
			id("walk_without_rhythm"), "sandwalk_wraps",
			id("fire_when_ready"), "bunker_in_place"
	);
	private static final Map<ResourceLocation, String> COMPLETIONIST_CRITERIA = Map.ofEntries(
			Map.entry(id("photosynthetic"), "cycadian_arbor"),
			Map.entry(id("forsaken_rite"), "hades_arcanum"),
			Map.entry(id("guillotine_swinging"), "the_executioner"),
			Map.entry(id("soul_of_the_ancients"), "warden_of_eternities"),
			Map.entry(id("ender_artillery"), "the_voidshell"),
			Map.entry(id("tidal_pulse"), "strides_of_the_deep"),
			Map.entry(id("nowhere_fast"), "haakons_kedge"),
			Map.entry(id("soulfire_eminence"), "soulfire_diadem"),
			Map.entry(id("the_old_wounds"), "alchemy_carve"),
			Map.entry(id("north_star"), "polaris"),
			Map.entry(id("superconductor"), "the_pylon"),
			Map.entry(id("clean_landing"), "moonshot"),
			Map.entry(id("redstone_battery"), "crimson_divisor"),
			Map.entry(id("unwelcome"), "silent_phalanx"),
			Map.entry(id("the_agony"), "electric_head"),
			Map.entry(id("fire_when_ready"), "bunker_in_place"),
			Map.entry(id("healing_salve"), "mirewaders"),
			Map.entry(id("walk_without_rhythm"), "sandwalk_wraps"),
			Map.entry(id("second_chance"), "armadan_repulsors")
	);


	private ExoticAdvancementHandler() {}

	@SubscribeEvent
	public static void onExoticPickup(ItemEntityPickupEvent.Post event) {
		if (!(event.getPlayer() instanceof ServerPlayer player)) return;

		ItemStack stack = event.getOriginalStack();
		int pickedUpCount = stack.getCount() - event.getCurrentStack().getCount();
		processAcquiredStack(player, stack, pickedUpCount);
		INVENTORY_SNAPSHOTS.put(player, snapshotInventory(player));
	}

	@SubscribeEvent
	public static void onPlayerInventoryTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		List<StackCount> currentSnapshot = snapshotInventory(player);
		List<StackCount> previousSnapshot = INVENTORY_SNAPSHOTS.put(player, currentSnapshot);
		if (previousSnapshot == null) return;

		for (StackCount current : currentSnapshot) {
			int previousCount = findCount(previousSnapshot, current.stack);
			int addedCount = current.count - previousCount;
			if (addedCount > 0) processAcquiredStack(player, current.stack, addedCount);
		}
	}

	private static List<StackCount> snapshotInventory(ServerPlayer player) {
		List<StackCount> snapshot = new ArrayList<>();
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.isEmpty()) continue;

			StackCount matching = findStackCount(snapshot, stack);
			if (matching == null) snapshot.add(new StackCount(stack, stack.getCount()));
			else matching.count += stack.getCount();
		}
		return snapshot;
	}

	private static StackCount findStackCount(List<StackCount> stacks, ItemStack item) {
		for (StackCount candidate : stacks) {
			if (ItemStack.isSameItemSameComponents(candidate.stack, item)) return candidate;
		}
		return null;
	}

	private static int findCount(List<StackCount> stacks, ItemStack item) {
		StackCount match = findStackCount(stacks, item);
		return match == null ? 0 : match.count;
	}

	private static void processAcquiredStack(ServerPlayer player, ItemStack stack, int pickedUpCount) {
		if (pickedUpCount <= 0) return;
		if (!WeaponPerkUtils.getPerks(stack).isEmpty()) {
			award(player, DESTINED_TRAITS_ROOT);
			CustomData perkData = stack.get(DataComponents.CUSTOM_DATA);
			if (perkData != null && perkData.copyTag().getInt("tier") >= 2) {
				award(player, ENHANCE);
			}
			if (recordPerkedItems(player, pickedUpCount)) {
				award(player, MORE_VAULT_SPACE);
			}
		}

		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData == null || !customData.copyTag().getBoolean("is_exotic")) return;

		award(player, LIFE_EXOTIC);
		for (Map.Entry<ResourceLocation, ResourceLocation> entry : ORIGIN_ADVANCEMENTS.entrySet()) {
			if (WeaponPerkUtils.hasPerk(stack, entry.getKey())) {
				award(player, entry.getValue());
			}
		}
		for (Map.Entry<ResourceLocation, String> entry : INFERNAL_CRITERIA.entrySet()) {
			if (WeaponPerkUtils.hasPerk(stack, entry.getKey())) {
				awardCriterion(player, INFERNAL_COLLECTION, entry.getValue());
			}
		}
		for (Map.Entry<ResourceLocation, String> entry : TREASURE_CRITERIA.entrySet()) {
			if (WeaponPerkUtils.hasPerk(stack, entry.getKey())) {
				awardCriterion(player, TREASURE_HUNTER, entry.getValue());
			}
		}
		for (Map.Entry<ResourceLocation, String> entry : PATHFINDER_CRITERIA.entrySet()) {
			if (WeaponPerkUtils.hasPerk(stack, entry.getKey())) {
				awardCriterion(player, PATHFINDER_COLLECTION, entry.getValue());
			}
		}
		for (Map.Entry<ResourceLocation, String> entry : ABYSSAL_CRITERIA.entrySet()) {
			if (WeaponPerkUtils.hasPerk(stack, entry.getKey())) {
				awardCriterion(player, ABYSSAL_COLLECTION, entry.getValue());
			}
		}
		for (Map.Entry<ResourceLocation, String> entry : ENDFARER_CRITERIA.entrySet()) {
			if (WeaponPerkUtils.hasPerk(stack, entry.getKey())) {
				awardCriterion(player, ENDFARER_COLLECTION, entry.getValue());
			}
		}
		for (Map.Entry<ResourceLocation, String> entry : FROSTBOUND_CRITERIA.entrySet()) {
			if (WeaponPerkUtils.hasPerk(stack, entry.getKey())) {
				awardCriterion(player, RIMEBOUND_COLLECTION, entry.getValue());
			}
		}
		for (Map.Entry<ResourceLocation, String> entry : SWAMP_CRITERIA.entrySet()) {
			if (WeaponPerkUtils.hasPerk(stack, entry.getKey())) {
				awardCriterion(player, MARSHLAND_COLLECTION, entry.getValue());
			}
		}
		for (Map.Entry<ResourceLocation, String> entry : MOUNTAIN_CRITERIA.entrySet()) {
			if (WeaponPerkUtils.hasPerk(stack, entry.getKey())) {
				awardCriterion(player, SKYSCRAPER_COLLECTION, entry.getValue());
			}
		}
		for (Map.Entry<ResourceLocation, String> entry : DESERT_CRITERIA.entrySet()) {
			if (WeaponPerkUtils.hasPerk(stack, entry.getKey())) {
				awardCriterion(player, SANDSWEEP_COLLECTION, entry.getValue());
			}
		}
		for (Map.Entry<ResourceLocation, String> entry : COMPLETIONIST_CRITERIA.entrySet()) {
			if (WeaponPerkUtils.hasPerk(stack, entry.getKey())) {
				awardCriterion(player, COMPLETIONIST, entry.getValue());
			}
		}

	}

	private static boolean recordPerkedItems(ServerPlayer player, int pickedUpCount) {
		if (pickedUpCount <= 0) return false;
		var persistentData = player.getPersistentData();
		int currentCount = persistentData.getInt(PERKED_ITEMS_COUNT_TAG);
		int newCount = Math.min(MORE_VAULT_SPACE_ITEM_COUNT, currentCount + pickedUpCount);
		persistentData.putInt(PERKED_ITEMS_COUNT_TAG, newCount);
		return newCount >= MORE_VAULT_SPACE_ITEM_COUNT;
	}

	private static void award(ServerPlayer player, ResourceLocation advancementId) {
		AdvancementHolder advancement = player.server.getAdvancements().get(advancementId);
		if (advancement == null) return;

		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
		for (String criterion : progress.getRemainingCriteria()) {
			player.getAdvancements().award(advancement, criterion);
		}
	}

	private static void awardCriterion(ServerPlayer player, ResourceLocation advancementId, String criterion) {
		AdvancementHolder advancement = player.server.getAdvancements().get(advancementId);
		if (advancement == null) return;

		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
		for (String remainingCriterion : progress.getRemainingCriteria()) {
			if (remainingCriterion.equals(criterion)) {
				player.getAdvancements().award(advancement, criterion);
				return;
			}
		}
	}

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath("destinedtraits", path);
	}
}