package com.doctorfuture.destinedtraits.perks;

import com.doctorfuture.destinedtraits.configuration.DestinedTraitsConfiguration;
import com.doctorfuture.destinedtraits.init.ModRegistries;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.util.RandomSource;
import java.util.UUID;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EventBusSubscriber(modid = "destinedtraits")
public class AnvilPerkUpgradeHandler {

    private static final ResourceLocation MOTE_ID = ResourceLocation.fromNamespaceAndPath("destinedtraits", "paracausal_mote");
    private static final ResourceLocation BUILT_DIFFERENT_PERK = ResourceLocation.fromNamespaceAndPath("destinedtraits", "built_different");
    private static final ResourceLocation ADVANCEMENT_ID = ResourceLocation.fromNamespaceAndPath("destinedtraits", "bank_those_motes");
    private static final ResourceLocation MASTERFUL_WORK_ADVANCEMENT_ID = ResourceLocation.fromNamespaceAndPath("destinedtraits", "masterful_work");
    private static final int MAX_TIER = 5;

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack leftItem = event.getLeft();
        ItemStack rightItem = event.getRight();

        if (leftItem.isEmpty() || rightItem.isEmpty()) {
            return;
        }

        // Verify the right slot contains the Paracausal Mote
        ResourceLocation rightItemId = BuiltInRegistries.ITEM.getKey(rightItem.getItem());
        if (!rightItemId.equals(MOTE_ID)) {
            return;
        }

        // Extract current tier (defaults to 1 if missing)
        CustomData customData = leftItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        int currentTier = customData.copyTag().contains("tier") ? customData.copyTag().getInt("tier") : 1;
        boolean hasTier = customData.copyTag().contains("tier");

        // At Tier 5, a mote rerolls an existing masterwork without changing the tier.
        if (currentTier >= MAX_TIER) {
            if (currentTier != MAX_TIER
                    || leftItem.get(ModRegistries.MASTERWORK_COMPONENT) == null
                    || leftItem.get(ModRegistries.MASTERWORK_PENALTY_COMPONENT) == null) {
                return;
            }

            ItemStack outputStack = leftItem.copy();
            outputStack.set(DataComponents.CUSTOM_DATA, customData.update(tag ->
                    tag.putString(MasterworkUtils.PENDING_MASTERWORK_ID, UUID.randomUUID().toString())));
            event.setOutput(outputStack);
            event.setCost(DestinedTraitsConfiguration.TIERUPGRADECOST.get().intValue() * (MAX_TIER - 1));
            event.setMaterialCost(1);
            return;
        }

        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

        if (perkComponent == null) {
            return;
        }

        List<ResourceLocation> currentPerks = new ArrayList<>(leftItem.getOrDefault(perkComponent, Collections.emptyList()));

        // Items with neither a tier nor perks were never tiered; motes must not apply to them.
        if (!hasTier && currentPerks.isEmpty()) {
            return;
        }

        // Add 'built_different' if not present
        if (!currentPerks.contains(BUILT_DIFFERENT_PERK)) {
            currentPerks.add(BUILT_DIFFERENT_PERK);
        }

        ItemStack outputStack = leftItem.copy();
        outputStack.set(perkComponent, currentPerks);

        // Increment tier to nextTier
        int nextTier = currentTier + 1;
        String pendingMasterworkId = nextTier == MAX_TIER ? UUID.randomUUID().toString() : null;
        outputStack.set(DataComponents.CUSTOM_DATA, customData.update(tag -> {
            tag.putInt("tier", nextTier);
            if (pendingMasterworkId != null) {
                tag.putString(MasterworkUtils.PENDING_MASTERWORK_ID, pendingMasterworkId);
            }
        }));

        event.setOutput(outputStack);

        // Dynamically scale XP cost based on current tier
        int baseCost = DestinedTraitsConfiguration.TIERUPGRADECOST.get().intValue();
        int xpCost = baseCost * currentTier;
        
        event.setCost(xpCost);
        event.setMaterialCost(1);
    }

    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        Player player = event.getEntity();

        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack rightItem = event.getRight();
        if (rightItem.isEmpty()) {
            return;
        }

        ResourceLocation rightItemId = BuiltInRegistries.ITEM.getKey(rightItem.getItem());
        if (rightItemId.equals(MOTE_ID)) {
            grantAdvancement(serverPlayer, ADVANCEMENT_ID);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            applyPendingMasterwork(player.getInventory().getItem(slot), player);
        }
        applyPendingMasterwork(player.containerMenu.getCarried(), player);
    }

    private static void applyPendingMasterwork(ItemStack stack, ServerPlayer player) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return;
        var tag = data.copyTag();
        if (tag.getString(MasterworkUtils.PENDING_MASTERWORK_ID).isEmpty()) return;

        boolean hasMasterwork = stack.get(ModRegistries.MASTERWORK_COMPONENT) != null
            && stack.get(ModRegistries.MASTERWORK_PENALTY_COMPONENT) != null;
        boolean applied = hasMasterwork
            ? MasterworkUtils.reroll(stack, player.getRandom())
            : MasterworkUtils.applyRandom(stack, player.getRandom());
        if (applied) {
            grantAdvancement(player, MASTERFUL_WORK_ADVANCEMENT_ID);
        }
    }

    private static void grantAdvancement(ServerPlayer player, ResourceLocation advancementId) {
        AdvancementHolder advancement = player.server.getAdvancements().get(advancementId);
        if (advancement != null) {
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
            if (!progress.isDone()) {
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(advancement, criterion);
                }
            }
        }
    }
}