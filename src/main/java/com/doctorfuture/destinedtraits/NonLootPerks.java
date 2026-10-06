package com.doctorfuture.destinedtraits.perks;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.CommonHooks;

import java.util.Optional;

/** Runs a stack through the data-driven global loot modifiers so non-loot sources share loot's biome rules. */
public final class NonLootPerks {
    private static final ResourceLocation SOURCE_ID = ResourceLocation.fromNamespaceAndPath("destinedtraits", "non_loot_source");
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private NonLootPerks() {}

    /** True while a non-loot stack is being processed; exotic injections must not fire then. */
    public static boolean isActive() {
        return ACTIVE.get();
    }

    /** Checks if an item is a valid target for perks (e.g., tools, weapons, armor). */
    public static boolean isEligibleForPerks(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // Checks if the item has durability. This naturally filters out 
        // basic stackables like sticks, ingots, blocks, and food.
        return stack.has(DataComponents.MAX_DAMAGE);
    }

    public static void apply(ItemStack stack, ServerLevel level, Vec3 origin, Entity entity) {
        if (stack.isEmpty()) return;

        LootParams.Builder params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin);
        if (entity != null) params.withParameter(LootContextParams.THIS_ENTITY, entity);
        LootContext context = new LootContext.Builder(params.create(LootContextParamSets.CHEST)).create(Optional.empty());

        ACTIVE.set(true);
        try {
            CommonHooks.modifyLoot(SOURCE_ID, new ObjectArrayList<>(java.util.List.of(stack)), context);
        } finally {
            ACTIVE.set(false);
        }
    }
}