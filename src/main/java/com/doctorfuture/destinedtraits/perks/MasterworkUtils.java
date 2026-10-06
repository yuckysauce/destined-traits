package com.doctorfuture.destinedtraits.perks;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import com.doctorfuture.destinedtraits.init.ModRegistries;
import com.doctorfuture.destinedtraits.procedures.WeaponPerkUtils;

import java.util.ArrayList;
import java.util.List;

public final class MasterworkUtils {
    private static final ResourceLocation LEGACY_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("destinedtraits", "masterwork");
    public static final String PENDING_MASTERWORK_ID = "destinedtraits_pending_masterwork_id";
    public static final String PENDING_MASTERWORK_SELECTION = "destinedtraits_pending_masterwork_selection";
    public static final List<String> STATS = List.of("strength", "range", "duration", "efficiency");

    public record Stats(double strength, double range, double duration, double efficiency) {
        public double strengthMultiplier() { return strength / 100.0; }
        public double rangeMultiplier() { return range / 100.0; }
        public double durationMultiplier() { return duration / 100.0; }
        public double efficiencyMultiplier() { return efficiency / 100.0; }

        public int scaleStrength(int value) { return scale(value, strength); }
        public float scaleStrength(float value) { return (float) (value * strengthMultiplier()); }
        public double scaleStrength(double value) { return value * strengthMultiplier(); }
        public double scaleRange(double value) { return value * rangeMultiplier(); }
        public int scaleDuration(int ticks) { return scale(ticks, duration); }
        public int scaleEfficiencyCost(int cost) { return Math.max(1, scale(cost, 10000.0 / Math.max(1.0, efficiency))); }
        public int scaleEfficiencyCooldown(int ticks) { return Math.max(1, scale(ticks, 10000.0 / Math.max(1.0, efficiency))); }

        private static int scale(int value, double stat) {
            return (int) Math.round(value * stat / 100.0);
        }
    }

    private MasterworkUtils() {}

    public static boolean applyRandom(ItemStack stack, RandomSource random) {
        if (stack.isEmpty()) return false;
        int positiveIndex = random.nextInt(STATS.size());
        int negativeIndex = random.nextInt(STATS.size() - 1);
        if (negativeIndex >= positiveIndex) negativeIndex++;
        return apply(stack, STATS.get(positiveIndex), STATS.get(negativeIndex));
    }

    public static boolean reroll(ItemStack stack, RandomSource random) {
        if (stack.isEmpty()) return false;
        String oldBonus = stack.get(ModRegistries.MASTERWORK_COMPONENT);
        String oldPenalty = stack.get(ModRegistries.MASTERWORK_PENALTY_COMPONENT);
        if (!STATS.contains(oldBonus) || !STATS.contains(oldPenalty) || oldBonus.equals(oldPenalty)) return false;

        List<String> newPairs = new ArrayList<>();
        for (String bonus : STATS) {
            for (String penalty : STATS) {
                if (!bonus.equals(penalty) && !(bonus.equals(oldBonus) && penalty.equals(oldPenalty))) {
                    newPairs.add(bonus);
                    newPairs.add(penalty);
                }
            }
        }

        int pairIndex = random.nextInt(newPairs.size() / 2) * 2;
        return apply(stack, newPairs.get(pairIndex), newPairs.get(pairIndex + 1));
    }

    public static boolean apply(ItemStack stack, String positiveStat, String negativeStat) {
        if (stack.isEmpty() || !STATS.contains(positiveStat) || !STATS.contains(negativeStat)
                || positiveStat.equals(negativeStat)) return false;

        removeLegacyAttributeMasterwork(stack);
        stack.set(ModRegistries.MASTERWORK_COMPONENT, positiveStat);
        stack.set(ModRegistries.MASTERWORK_PENALTY_COMPONENT, negativeStat);
        stack.update(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY, customData -> customData.update(tag -> {
            tag.remove(PENDING_MASTERWORK_ID);
            tag.remove(PENDING_MASTERWORK_SELECTION);
        }));
        return true;
    }

    public static Stats getStats(Player player) {
        double strength = 100.0;
        double range = 100.0;
        double duration = 100.0;
        double efficiency = 100.0;
        List<ItemStack> equipped = new ArrayList<>();
        player.getArmorSlots().forEach(equipped::add);
        equipped.add(player.getMainHandItem());
        equipped.add(player.getOffhandItem());

        for (ItemStack stack : equipped) {
            String positive = stack.get(ModRegistries.MASTERWORK_COMPONENT);
            String negative = stack.get(ModRegistries.MASTERWORK_PENALTY_COMPONENT);
            if (positive != null && STATS.contains(positive)) {
                switch (positive) {
                    case "strength" -> strength += 25.0;
                    case "range" -> range += 25.0;
                    case "duration" -> duration += 25.0;
                    case "efficiency" -> efficiency += 25.0;
                }
            }
            if (negative != null && STATS.contains(negative)) {
                switch (negative) {
                    case "strength" -> strength -= 15.0;
                    case "range" -> range -= 15.0;
                    case "duration" -> duration -= 15.0;
                    case "efficiency" -> efficiency -= 15.0;
                }
            }
        }

        return new Stats(Math.max(0.0, strength), Math.max(0.0, range), Math.max(0.0, duration), Math.max(0.0, efficiency));
    }

    public static void applyScaledEffectModifiers(Player player) {
        Stats stats = getStats(player);
        for (MobEffectInstance effectInstance : player.getActiveEffects()) {
            ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effectInstance.getEffect().value());
            if (effectId == null || !"destinedtraits".equals(effectId.getNamespace()) || !hasCorrespondingPerkEquipped(player, effectId)) continue;
            applyScaledEffectModifiers(player, effectInstance, stats);
        }
    }

    private static boolean hasCorrespondingPerkEquipped(Player player, ResourceLocation effectId) {
        String path = effectId.getPath();
        if (path.endsWith("_active_effect")) {
            path = path.substring(0, path.length() - "_active_effect".length());
        } else if (path.endsWith("_debuff")) {
            path = path.substring(0, path.length() - "_debuff".length());
        } else if (path.endsWith("_effect")) {
            path = path.substring(0, path.length() - "_effect".length());
        } else {
            return false;
        }

        ResourceLocation perk = ResourceLocation.fromNamespaceAndPath(effectId.getNamespace(), path);
        if (WeaponPerkUtils.hasPerk(player.getMainHandItem(), perk)
                || WeaponPerkUtils.hasPerk(player.getOffhandItem(), perk)) return true;
        for (ItemStack armor : player.getArmorSlots()) {
            if (WeaponPerkUtils.hasPerk(armor, perk)) return true;
        }
        return false;
    }

    public static void applyScaledEffectModifiers(LivingEntity target, MobEffectInstance effectInstance, Stats stats) {
        effectInstance.getEffect().value().createModifiers(effectInstance.getAmplifier(), (attribute, modifier) -> {
            AttributeInstance instance = target.getAttribute(attribute);
            if (instance == null) return;

            double multiplier = attribute.equals(Attributes.ENTITY_INTERACTION_RANGE)
                    || attribute.equals(Attributes.BLOCK_INTERACTION_RANGE)
                    ? stats.rangeMultiplier() : stats.strengthMultiplier();
            AttributeModifier scaled = new AttributeModifier(modifier.id(), modifier.amount() * multiplier, modifier.operation());
            AttributeModifier current = instance.getModifier(modifier.id());
            if (current == null || Double.compare(current.amount(), scaled.amount()) != 0
                    || current.operation() != scaled.operation()) {
                instance.addOrUpdateTransientModifier(scaled);
            }
        });
    }

    private static void removeLegacyAttributeMasterwork(ItemStack stack) {
        ItemAttributeModifiers current = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        if (current.modifiers().isEmpty()) return;

        List<ItemAttributeModifiers.Entry> kept = new ArrayList<>();
        for (ItemAttributeModifiers.Entry entry : current.modifiers()) {
            if (!entry.modifier().id().equals(LEGACY_MODIFIER_ID)) kept.add(entry);
        }
        if (kept.size() != current.modifiers().size()) {
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(kept, current.showInTooltip()));
        }
    }
}
