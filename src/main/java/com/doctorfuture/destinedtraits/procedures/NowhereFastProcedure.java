package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

@EventBusSubscriber(modid = "destinedtraits")
public final class NowhereFastProcedure {
	private static final ResourceLocation PERK = ResourceLocation.fromNamespaceAndPath("destinedtraits", "nowhere_fast");
	private static final String MARKER = "destinedtraits_nowhere_fast";
	private static final String PEAK_Y = "destinedtraits_nowhere_fast_peak_y";
	private static final String START_X = "destinedtraits_nowhere_fast_start_x";
	private static final String START_Z = "destinedtraits_nowhere_fast_start_z";
	private static final String IMPACT_APPLIED = "destinedtraits_nowhere_fast_impact_applied";
	private static final double MAX_HORIZONTAL_RANGE = 12.0;

	private NowhereFastProcedure() {}

	@SubscribeEvent
	public static void onTridentThrown(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ThrownTrident trident)) return;

		ItemStack thrownItem = trident.getWeaponItem();
		if (!WeaponPerkUtils.hasPerk(thrownItem, PERK)) return;
		CustomData itemData = thrownItem.get(DataComponents.CUSTOM_DATA);
		if (itemData == null || !itemData.copyTag().getBoolean("is_exotic")) return;

		var data = trident.getPersistentData();
		data.putBoolean(MARKER, true);
		data.putDouble(PEAK_Y, trident.getY());
		data.putDouble(START_X, trident.getX());
		data.putDouble(START_Z, trident.getZ());
		trident.setDeltaMovement(trident.getDeltaMovement().scale(0.35));
		trident.hurtMarked = true;
	}

	@SubscribeEvent
	public static void onMarkedTridentTick(EntityTickEvent.Post event) {
		if (!(event.getEntity() instanceof ThrownTrident trident) || !trident.getPersistentData().getBoolean(MARKER)) return;

		var data = trident.getPersistentData();
		data.putDouble(PEAK_Y, Math.max(data.getDouble(PEAK_Y), trident.getY()));
		double dx = trident.getX() - data.getDouble(START_X);
		double dz = trident.getZ() - data.getDouble(START_Z);
		if (dx * dx + dz * dz > MAX_HORIZONTAL_RANGE * MAX_HORIZONTAL_RANGE) {
			trident.discard();
		}
	}

	@SubscribeEvent
	public static void onMarkedTridentDamage(LivingDamageEvent.Pre event) {
		DamageSource source = event.getSource();
		if (!(source.getDirectEntity() instanceof ThrownTrident trident)
				|| !trident.getPersistentData().getBoolean(MARKER)) return;

		float fallDistance = (float) Math.max(0.0, trident.getPersistentData().getDouble(PEAK_Y) - event.getEntity().getY());
		if (fallDistance <= 1.5F) return;
		MasterworkUtils.Stats masterworks = trident.getOwner() instanceof net.minecraft.world.entity.player.Player player
				? MasterworkUtils.getStats(player) : new MasterworkUtils.Stats(100.0, 100.0, 100.0, 100.0);

		float maceBonus;
		if (fallDistance <= 3.0F) {
			maceBonus = 4.0F * fallDistance;
		} else if (fallDistance <= 8.0F) {
			maceBonus = 12.0F + 2.0F * (fallDistance - 3.0F);
		} else {
			maceBonus = 22.0F + fallDistance - 8.0F;
		}
		event.setNewDamage(event.getNewDamage() + (float) (maceBonus * masterworks.strengthMultiplier()));
	}

	@SubscribeEvent
	public static void onMarkedTridentImpact(ProjectileImpactEvent event) {
		if (event.getProjectile().level().isClientSide() || !(event.getProjectile() instanceof ThrownTrident trident)) return;
		var data = trident.getPersistentData();
		if (!data.getBoolean(MARKER) || data.getBoolean(IMPACT_APPLIED)) return;
		data.putBoolean(IMPACT_APPLIED, true);
		if (!(trident.level() instanceof ServerLevel level)) return;

		Vec3 impact = event.getRayTraceResult().getLocation();
		float fallDistance = (float) Math.max(0.0, data.getDouble(PEAK_Y) - impact.y);
		float maceBonus = getMaceFallBonus(fallDistance);
		Entity directEntity = event.getRayTraceResult() instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
		MasterworkUtils.Stats masterworks = trident.getOwner() instanceof net.minecraft.world.entity.player.Player player
				? MasterworkUtils.getStats(player) : new MasterworkUtils.Stats(100.0, 100.0, 100.0, 100.0);
		if (directEntity instanceof LivingEntity livingTarget) {
			livingTarget.addEffect(new MobEffectInstance(
					com.doctorfuture.destinedtraits.init.DestinedtraitsModMobEffects.NOWHERE_FAST_EFFECT,
					masterworks.scaleDuration(60), 0, false, true));
		}

		boolean enhanced = WeaponPerkUtils.isOriginEnhanced(trident.getWeaponItem(), PERK);
		applyImpactSplash(level, impact, trident.getOwner(), directEntity, fallDistance, maceBonus, enhanced, masterworks);
	}

	private static float getMaceFallBonus(float fallDistance) {
		if (fallDistance <= 1.5F) return 0.0F;
		if (fallDistance <= 3.0F) return 4.0F * fallDistance;
		if (fallDistance <= 8.0F) return 12.0F + 2.0F * (fallDistance - 3.0F);
		return 22.0F + fallDistance - 8.0F;
	}

	private static void applyImpactSplash(ServerLevel level, Vec3 impact, Entity owner, Entity directEntity,
			float fallDistance, float maceBonus, boolean enhanced, MasterworkUtils.Stats masterworks) {
		double radius = Math.min(9.0, (enhanced ? 6.0 : 5.0) + Math.max(0.0, fallDistance - 1.5) * 0.25)
				* masterworks.rangeMultiplier();
		AABB area = new AABB(impact, impact).inflate(radius);
		for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, area,
				entity -> entity.isAlive() && entity != directEntity && entity != owner)) {
			double distance = Math.sqrt(nearby.distanceToSqr(impact));
			if (distance >= radius) continue;

			float falloff = (float) (1.0 - distance / radius);
			float splashDamage = (float) ((2.0F + maceBonus * 0.5F) * falloff * masterworks.strengthMultiplier());
			if (splashDamage <= 0.0F) continue;

			nearby.invulnerableTime = 0;
			nearby.hurt(nearby.damageSources().explosion(owner, owner), splashDamage);
		}
	}
}