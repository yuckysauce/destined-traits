package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = "destinedtraits")
public final class SoulfireEminenceProcedure {
	private static final ResourceLocation PERK = ResourceLocation.fromNamespaceAndPath("destinedtraits", "soulfire_eminence");
	private static final int TICKS_PER_SECOND = 20;
	private static final int BURST_SIZE = 3;
	private static final int BURST_SHOT_GAP_TICKS = 4;
	private static final int BURST_INTERVAL_TICKS = 30;
	private static final int MAX_WISP_DURATION_TICKS = 60 * TICKS_PER_SECOND;
	private static final double TARGET_RANGE = 9.0;
	private static final Map<UUID, WispState> WISPS = new ConcurrentHashMap<>();

	private SoulfireEminenceProcedure() {}

	private static final class WispState {
		private int remainingTicks;
		private int shotCooldown;
		private int age;
		private int shotsInBurst;
		private boolean enhanced;

		private WispState(int durationTicks, boolean enhanced) {
			this.remainingTicks = durationTicks;
			this.enhanced = enhanced;
		}
	}

	@SubscribeEvent
	public static void onFoodConsumed(LivingEntityUseItemEvent.Finish event) {
		if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
		ItemStack soulfireArmor = getSoulfireArmor(player);
		if (soulfireArmor.isEmpty()) return;
		boolean enhanced = WeaponPerkUtils.isOriginEnhanced(soulfireArmor, PERK);
		MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);

		FoodProperties food = event.getItem().get(net.minecraft.core.component.DataComponents.FOOD);
		if (food == null) return;

		double conversionRate = enhanced ? 1.5 : 1.0;
	int duration = masterworks.scaleDuration((int) Math.round((food.nutrition() + food.saturation()) * TICKS_PER_SECOND * conversionRate * 0.5));
		duration = Math.clamp(duration, 2 * TICKS_PER_SECOND, MAX_WISP_DURATION_TICKS);
		int addedDuration = duration;
		boolean enhancedWisp = enhanced;
		WISPS.compute(player.getUUID(), (uuid, current) -> {
			if (current == null) return new WispState(addedDuration, enhancedWisp);
			current.remainingTicks = Math.min(MAX_WISP_DURATION_TICKS, current.remainingTicks + addedDuration);
			current.enhanced |= enhancedWisp;
			return current;
		});
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide() || !(player.level() instanceof ServerLevel level)) return;

		WispState wisp = WISPS.get(player.getUUID());
		if (wisp == null) return;
		if (getSoulfireArmor(player).isEmpty()) {
			WISPS.remove(player.getUUID());
			return;
		}
		if (--wisp.remainingTicks <= 0) {
			WISPS.remove(player.getUUID());
			return;
		}

		wisp.age++;
		if (wisp.shotCooldown > 0) wisp.shotCooldown--;
		Vec3 wispPosition = getWispPosition(player, wisp.age);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, wispPosition.x, wispPosition.y, wispPosition.z,
				2, 0.07, 0.07, 0.07, 0.005);
		if ((wisp.age & 1) == 0) {
			level.sendParticles(ParticleTypes.SOUL, wispPosition.x, wispPosition.y, wispPosition.z,
					1, 0.02, 0.02, 0.02, 0.0);
		}

		if (wisp.shotCooldown == 0) {
			MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
			LivingEntity target = findVisibleHostile(player, level, wispPosition, TARGET_RANGE * masterworks.rangeMultiplier());
			if (target != null) {
				fireBeam(player, level, wispPosition, target,
						(float) ((wisp.enhanced ? 2.0F : 1.0F) * masterworks.strengthMultiplier()));
				wisp.shotsInBurst++;
				if (wisp.shotsInBurst < BURST_SIZE) {
					wisp.shotCooldown = BURST_SHOT_GAP_TICKS;
				} else {
					wisp.shotsInBurst = 0;
					wisp.shotCooldown = BURST_INTERVAL_TICKS;
				}
			}
		}
	}

	private static ItemStack getSoulfireArmor(Player player) {
		for (ItemStack armor : player.getArmorSlots()) {
			if (WeaponPerkUtils.hasPerk(armor, PERK)) return armor;
		}
		return ItemStack.EMPTY;
	}

	private static Vec3 getWispPosition(Player player, int age) {
		double angle = age * 0.08 + player.getId();
		return player.position().add(Math.cos(angle) * 0.55, player.getBbHeight() * 0.72 + Math.sin(angle * 0.5) * 0.12,
				Math.sin(angle) * 0.55);
	}

	private static LivingEntity findVisibleHostile(Player owner, ServerLevel level, Vec3 origin, double targetRange) {
		AABB searchArea = new AABB(origin, origin).inflate(targetRange);
		LivingEntity nearest = null;
		double nearestDistance = targetRange * targetRange;
		for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, searchArea,
				entity -> entity != owner && entity.isAlive() && entity instanceof Enemy)) {
			Vec3 targetPoint = candidate.getBoundingBox().getCenter();
			double distance = origin.distanceToSqr(targetPoint);
			if (distance >= nearestDistance) continue;

			BlockHitResult obstruction = level.clip(new ClipContext(origin, targetPoint, ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE, owner));
			if (obstruction.getType() == HitResult.Type.MISS || origin.distanceToSqr(obstruction.getLocation()) >= distance) {
				nearest = candidate;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	private static void fireBeam(Player owner, ServerLevel level, Vec3 origin, LivingEntity target, float damage) {
		Vec3 endpoint = target.getBoundingBox().getCenter();
		Vec3 beam = endpoint.subtract(origin);
		int particleCount = Math.max(2, (int) (beam.length() * 5.0));
		for (int index = 0; index <= particleCount; index++) {
			Vec3 point = origin.add(beam.scale((double) index / particleCount));
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, point.x, point.y, point.z, 1, 0.015, 0.015, 0.015, 0.0);
		}

		target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 4 * TICKS_PER_SECOND));
		target.invulnerableTime = 0;
		target.hurt(owner.damageSources().onFire(), damage);
		level.sendParticles(ParticleTypes.FLAME, endpoint.x, endpoint.y, endpoint.z, 8, 0.15, 0.2, 0.15, 0.025);
		level.playSound(null, target.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.45F, 1.6F);
	}
}