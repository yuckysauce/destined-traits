package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

public final class WeaponPerkUtils {
    private static final ResourceLocation PERKS_COMPONENT_ID = ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks");

    private WeaponPerkUtils() {}

    public static List<ResourceLocation> getPerks(ItemStack stack) {
        @SuppressWarnings("unchecked")
        DataComponentType<List<ResourceLocation>> component = (DataComponentType<List<ResourceLocation>>) (Object)
                BuiltInRegistries.DATA_COMPONENT_TYPE.get(PERKS_COMPONENT_ID);
        return component == null ? Collections.emptyList() : stack.getOrDefault(component, Collections.emptyList());
    }

    public static boolean hasPerk(ItemStack stack, ResourceLocation perk) {
        return getPerks(stack).contains(perk);
    }

    public static boolean isEnhanced(ItemStack stack, ResourceLocation perk) {
        List<ResourceLocation> perks = getPerks(stack);
        int index = perks.indexOf(perk);
        return isEnhancedAtIndex(stack, index);
    }

    public static boolean isEnhancedAtIndex(ItemStack stack, int index) {
        if (index < 0) return false;

        boolean hasOriginTrait = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getBoolean("has_origin_trait");
        int columnIndex = index - (hasOriginTrait ? 1 : 0);
        int tier = getTier(stack);
        return (columnIndex == 0 && tier >= 2) || (columnIndex == 1 && tier >= 3);
    }

    public static boolean isLeftColumnEnhanced(ItemStack stack, ResourceLocation perk) {
        return hasPerk(stack, perk) && getTier(stack) >= 2;
    }

    public static boolean isOriginEnhanced(ItemStack stack, ResourceLocation originTrait) {
        return hasPerk(stack, originTrait) && getTier(stack) >= 4;
    }

    public static int countOriginArmorPieces(LivingEntity entity, ResourceLocation originTrait) {
        int count = 0;
        for (ItemStack armor : entity.getArmorSlots()) {
            if (isArmorOriginTrait(armor, originTrait)) count++;
        }
        return Math.min(count, 4);
    }

    public static double getOriginArmorMultiplier(LivingEntity entity, ResourceLocation originTrait) {
        return countOriginArmorPieces(entity, originTrait) / 2.0;
    }

    public static boolean hasEnhancedOriginArmor(LivingEntity entity, ResourceLocation originTrait) {
        for (ItemStack armor : entity.getArmorSlots()) {
            if (isArmorOriginTrait(armor, originTrait) && isOriginEnhanced(armor, originTrait)) return true;
        }
        return false;
    }

    private static boolean isArmorOriginTrait(ItemStack stack, ResourceLocation originTrait) {
        if (stack.isEmpty()) return false;
        List<ResourceLocation> perks = getPerks(stack);
        if (perks.isEmpty() || !perks.get(0).equals(originTrait)) return false;
        net.minecraft.nbt.CompoundTag customData = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return customData.getBoolean("has_origin_trait") && !customData.getBoolean("is_exotic");
    }

    private static int getTier(ItemStack stack) {
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
    }
}