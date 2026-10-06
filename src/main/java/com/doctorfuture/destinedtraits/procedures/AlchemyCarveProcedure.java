package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.doctorfuture.destinedtraits.network.DestinedtraitsModVariables;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = "destinedtraits")
public final class AlchemyCarveProcedure {
    private static final ResourceLocation OLD_WOUNDS = ResourceLocation.fromNamespaceAndPath("destinedtraits", "the_old_wounds");
    private static final String EFFECTS_TAG = "DestinedTraitsAlchemyEffects";
    private static final String XP_CHARGE_TAG = "DestinedTraitsAlchemyXpCharge";
    private static final String POTION_MARKER = "destinedtraits_alchemy_carve";
    private static final String POTION_EFFECTS_TAG = "destinedtraits_alchemy_effects";
    private static final String POTION_ENHANCED_TAG = "destinedtraits_alchemy_enhanced";
    private static final String POTION_STRENGTH_TAG = "destinedtraits_alchemy_strength";
    private static final String POTION_RANGE_TAG = "destinedtraits_alchemy_range";
    private static final String POTION_DURATION_TAG = "destinedtraits_alchemy_duration";
    private static final int BREWED_EFFECT_DURATION = 160;
    private static final float MULTISHOT_SPREAD_DEGREES = 10.0F;

    private AlchemyCarveProcedure() {}

    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob target) || !target.isAlive()) return;

        ItemStack weapon = player.getMainHandItem();
        if (!WeaponPerkUtils.hasPerk(weapon, OLD_WOUNDS)) return;

        List<MobEffectInstance> activeEffects = target.getActiveEffects().stream()
                .filter(effect -> BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()) != null)
                .toList();
        if (activeEffects.isEmpty()) return;

        CompoundTag data = player.getPersistentData();
        ListTag collected = data.getList(EFFECTS_TAG, Tag.TAG_COMPOUND);

        for (MobEffectInstance effect : activeEffects) {
            ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
            if (effectId == null) continue;
            CompoundTag stored = findEffect(collected, effectId);
            if (stored == null) {
                stored = new CompoundTag();
                stored.putString("id", effectId.toString());
                stored.putInt("amplifier", effect.getAmplifier());
                collected.add(stored);
            } else {
                stored.putInt("amplifier", Math.max(stored.getInt("amplifier"), effect.getAmplifier()));
            }
        }

        data.put(EFFECTS_TAG, collected);
        player.displayClientMessage(buildCounterMessage(collected), true);
    }

    @SubscribeEvent
    public static void onXpPickup(PlayerXpEvent.PickupXp event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !hasOldWoundsEquipped(player)) return;

        CompoundTag data = player.getPersistentData();
        int requiredXp = MasterworkUtils.getStats(player).scaleEfficiencyCost(
            com.doctorfuture.destinedtraits.configuration.DestinedTraitsConfiguration.XPPERKREQUIREMENT.get().intValue());
        int charge = Math.min(requiredXp, data.getInt(XP_CHARGE_TAG) + event.getOrb().getValue());
        data.putInt(XP_CHARGE_TAG, charge);
        if (charge >= requiredXp) {
            player.displayClientMessage(Component.literal("Alchemy Carve is ready"), true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag data = player.getPersistentData();
        if (!data.contains(EFFECTS_TAG, Tag.TAG_LIST) || data.getList(EFFECTS_TAG, Tag.TAG_COMPOUND).isEmpty()) return;

        if (!player.getData(DestinedtraitsModVariables.PLAYER_VARIABLES).in_combat_bool) {
            data.remove(EFFECTS_TAG);
            player.displayClientMessage(Component.literal("Alchemy Carve effects faded"), true);
        }
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) return;
        ItemStack weapon = player.getItemInHand(event.getHand());
        if (!WeaponPerkUtils.hasPerk(weapon, OLD_WOUNDS)) return;

        CompoundTag playerData = player.getPersistentData();
        MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
        int requiredXp = masterworks.scaleEfficiencyCost(
            com.doctorfuture.destinedtraits.configuration.DestinedTraitsConfiguration.XPPERKREQUIREMENT.get().intValue());
        if (playerData.getInt(XP_CHARGE_TAG) < requiredXp) {
            player.displayClientMessage(Component.literal("Alchemy Carve requires " + requiredXp + " XP"), true);
            return;
        }

        ListTag collected = playerData.getList(EFFECTS_TAG, Tag.TAG_COMPOUND);
        List<MobEffectInstance> effects = readEffects(collected);
        if (effects.isEmpty()) {
            player.displayClientMessage(Component.literal("Alchemy Carve has no effects to brew"), true);
            return;
        }

        boolean enhanced = WeaponPerkUtils.isOriginEnhanced(weapon, OLD_WOUNDS);
        ItemStack potionStack = Items.SPLASH_POTION.getDefaultInstance();
        potionStack.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
        CustomData customData = potionStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        potionStack.set(DataComponents.CUSTOM_DATA, customData.update(tag -> {
            tag.putBoolean(POTION_MARKER, true);
            tag.putBoolean(POTION_ENHANCED_TAG, enhanced);
            tag.putDouble(POTION_STRENGTH_TAG, masterworks.strength());
            tag.putDouble(POTION_RANGE_TAG, masterworks.range());
            tag.putDouble(POTION_DURATION_TAG, masterworks.duration());
            tag.put(POTION_EFFECTS_TAG, collected.copy());
        }));

        for (float yawOffset : new float[] { -MULTISHOT_SPREAD_DEGREES, 0.0F, MULTISHOT_SPREAD_DEGREES }) {
            ThrownPotion potion = new ThrownPotion(player.level(), player);
            potion.setItem(potionStack.copy());
            potion.shootFromRotation(player, player.getXRot(), player.getYRot() + yawOffset, -20.0F, 0.5F, 1.0F);
            player.level().addFreshEntity(potion);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F, 0.8F);

        playerData.putInt(XP_CHARGE_TAG, 0);
        player.swing(event.getHand(), true);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPotionImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownPotion potion) || potion.level().isClientSide()) return;
        ItemStack item = potion.getItem();
        CustomData customData = item.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        CompoundTag tag = customData.copyTag();
        if (!tag.getBoolean(POTION_MARKER) || !(potion.level() instanceof ServerLevel level)) return;

        List<MobEffectInstance> effects = readEffects(tag.getList(POTION_EFFECTS_TAG, Tag.TAG_COMPOUND));
        boolean enhanced = tag.getBoolean(POTION_ENHANCED_TAG);
        MasterworkUtils.Stats masterworks = new MasterworkUtils.Stats(
            tag.contains(POTION_STRENGTH_TAG) ? tag.getDouble(POTION_STRENGTH_TAG) : 100.0,
            tag.contains(POTION_RANGE_TAG) ? tag.getDouble(POTION_RANGE_TAG) : 100.0,
            tag.contains(POTION_DURATION_TAG) ? tag.getDouble(POTION_DURATION_TAG) : 100.0,
            100.0);
        float impactDamage = (float) (getTotalEffectLevel(effects, enhanced) * masterworks.strengthMultiplier());
        Vec3 impact = event.getRayTraceResult().getLocation();
        double splashRadius = 4.0D * masterworks.rangeMultiplier();
        AABB area = new AABB(impact, impact).inflate(splashRadius);
        Entity owner = potion.getOwner();

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (target == owner || target.distanceToSqr(impact) > splashRadius * splashRadius) continue;
            boolean targetIsPlayer = target instanceof Player;
            if (!targetIsPlayer) {
                target.hurt(target.damageSources().indirectMagic(potion, owner), impactDamage);
            }
            for (MobEffectInstance effect : effects) {
                MobEffectCategory category = effect.getEffect().value().getCategory();
                if (targetIsPlayer && category != MobEffectCategory.BENEFICIAL) continue;
                if (!targetIsPlayer && category != MobEffectCategory.HARMFUL) continue;

                int amplifier = effect.getAmplifier() + (enhanced ? 1 : 0);
                int duration = Math.max(20, masterworks.scaleDuration(BREWED_EFFECT_DURATION / 4));
                MobEffectInstance appliedEffect = new MobEffectInstance(effect.getEffect(), duration, amplifier, false, true);
                target.addEffect(appliedEffect);
                MasterworkUtils.applyScaledEffectModifiers(target, appliedEffect, masterworks);
            }
        }

        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, 0.9F);
        potion.discard();
        event.setCanceled(true);
    }

    private static float getTotalEffectLevel(List<MobEffectInstance> effects, boolean enhanced) {
        int totalLevel = 0;
        for (MobEffectInstance effect : effects) {
            totalLevel += effect.getAmplifier() + 1 + (enhanced ? 1 : 0);
        }
        return Math.max(1.0F, totalLevel);
    }

    private static boolean hasOldWoundsEquipped(Player player) {
        if (WeaponPerkUtils.hasPerk(player.getMainHandItem(), OLD_WOUNDS)
                || WeaponPerkUtils.hasPerk(player.getOffhandItem(), OLD_WOUNDS)) return true;
        for (ItemStack armor : player.getArmorSlots()) {
            if (WeaponPerkUtils.hasPerk(armor, OLD_WOUNDS)) return true;
        }
        return false;
    }

    private static CompoundTag findEffect(ListTag effects, ResourceLocation effectId) {
        for (int i = 0; i < effects.size(); i++) {
            CompoundTag entry = effects.getCompound(i);
            if (effectId.toString().equals(entry.getString("id"))) return entry;
        }
        return null;
    }

    private static List<MobEffectInstance> readEffects(ListTag storedEffects) {
        List<MobEffectInstance> effects = new ArrayList<>();
        for (int i = 0; i < storedEffects.size(); i++) {
            CompoundTag stored = storedEffects.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(stored.getString("id"));
            Holder<MobEffect> effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
            if (effect == null) continue;
            effects.add(new MobEffectInstance(effect, BREWED_EFFECT_DURATION, stored.getInt("amplifier"), false, true));
        }
        return effects;
    }

    private static Component buildCounterMessage(ListTag collected) {
        MutableComponent message = Component.literal("Alchemy Carve [" + collected.size() + "]: ");
        for (int i = 0; i < collected.size(); i++) {
            CompoundTag stored = collected.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(stored.getString("id"));
            Holder<MobEffect> effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
            if (effect == null) continue;
            if (i > 0) message.append(Component.literal(", "));
            message.append(effect.value().getDisplayName()).append(Component.literal(" " + (stored.getInt("amplifier") + 1)));
        }
        return message;
    }
}
