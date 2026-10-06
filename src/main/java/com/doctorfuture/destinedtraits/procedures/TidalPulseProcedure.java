package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = "destinedtraits")
public final class TidalPulseProcedure {
	private static final ResourceLocation PERK = ResourceLocation.fromNamespaceAndPath("destinedtraits", "tidal_pulse");
	private static final int CHARGE_TICKS = 30;
	private static final int COOLDOWN_TICKS = 60;
	private static final int SPRINT_GRACE_TICKS = 20;
	private static final int SURFACE_SLIDE_TICKS = 10;
	private static final Map<Player, PlayerState> PLAYER_STATES = new WeakHashMap<>();

	private TidalPulseProcedure() {}

	private static final class PlayerState {
		private int sprintTicks;
		private int ticksSinceSprint;
		private int cooldownTicks;
		private boolean wasSneaking;
		private int surfaceSlideTicks;
		private boolean restoreCrouching;
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide()) return;

		PlayerState state = PLAYER_STATES.computeIfAbsent(player, ignored -> new PlayerState());
		if (state.surfaceSlideTicks > 0) {
			player.setPose(Pose.SWIMMING);
			if (--state.surfaceSlideTicks == 0) {
				player.setPose(state.restoreCrouching ? Pose.CROUCHING : Pose.STANDING);
			}
		}

		ItemStack perkArmor = getEquippedPerkArmor(player);
		boolean hasPerk = !perkArmor.isEmpty();
		boolean sneakPressed = player.isShiftKeyDown() && !state.wasSneaking;
		boolean sprintCharging = hasPerk && player.isSprinting();

		if (!hasPerk) {
			state.sprintTicks = 0;
		} else if (sneakPressed) {
			tryActivate(player, state, perkArmor);
		} else if (sprintCharging) {
			state.sprintTicks = Math.min(CHARGE_TICKS, state.sprintTicks + 1);
			state.ticksSinceSprint = 0;
		} else if (state.sprintTicks > 0 && ++state.ticksSinceSprint > SPRINT_GRACE_TICKS) {
			state.sprintTicks = 0;
		}

		if (state.cooldownTicks > 0) state.cooldownTicks--;
		state.wasSneaking = player.isShiftKeyDown();
	}

	public static void onConfiguredSneakPressed(Player player) {
		if (player.level().isClientSide()) return;
		PlayerState state = PLAYER_STATES.get(player);
		ItemStack perkArmor = getEquippedPerkArmor(player);
		if (state != null && !perkArmor.isEmpty()) tryActivate(player, state, perkArmor);
	}

	public static boolean hasTidalPulseArmor(Player player) {
		return !getEquippedPerkArmor(player).isEmpty();
	}

	private static void tryActivate(Player player, PlayerState state, ItemStack perkArmor) {
		if (state.cooldownTicks > 0) return;
		if (state.sprintTicks < CHARGE_TICKS || state.ticksSinceSprint > SPRINT_GRACE_TICKS) {
			return;
		}

		activateSlide(player, state, WeaponPerkUtils.isOriginEnhanced(perkArmor, PERK));
	}

	private static ItemStack getEquippedPerkArmor(Player player) {
		ItemStack firstMatch = ItemStack.EMPTY;
		for (ItemStack armor : player.getArmorSlots()) {
			if (WeaponPerkUtils.hasPerk(armor, PERK)) {
				if (WeaponPerkUtils.isOriginEnhanced(armor, PERK)) return armor;
				if (firstMatch.isEmpty()) firstMatch = armor;
			}
		}
		return firstMatch;
	}

	private static void activateSlide(Player player, PlayerState state, boolean enhanced) {
		if (!(player.level() instanceof ServerLevel level)) return;
		MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
		double strengthMultiplier = masterworks.strengthMultiplier();

		Vec3 motion = player.getDeltaMovement();
		boolean underwater = player.isUnderWater();
		Vec3 forward;
		if (underwater) {
			forward = player.getLookAngle().normalize();
		} else {
			forward = new Vec3(motion.x, 0.0, motion.z);
			if (forward.lengthSqr() < 0.0001) {
				Vec3 look = player.getLookAngle();
				forward = new Vec3(look.x, 0.0, look.z);
			}
			if (forward.lengthSqr() < 0.0001) return;
			forward = forward.normalize();
		}

		double boost = (enhanced ? 1.55 : 1.2) * strengthMultiplier;
		if (underwater) {
			player.setDeltaMovement(motion.scale(0.35).add(forward.scale(boost)));
		} else {
			state.restoreCrouching = player.isShiftKeyDown();
			state.surfaceSlideTicks = masterworks.scaleDuration(SURFACE_SLIDE_TICKS);
			player.setPose(Pose.SWIMMING);
			player.setDeltaMovement(motion.x * 0.35 + forward.x * boost,
					Math.max(motion.y, 0.12), motion.z * 0.35 + forward.z * boost);
		}
		player.hurtMarked = true;

		state.sprintTicks = 0;
		state.ticksSinceSprint = 0;
		state.cooldownTicks = masterworks.scaleEfficiencyCooldown(COOLDOWN_TICKS);

		double radius = (enhanced ? 6.5 : 5.0) * masterworks.rangeMultiplier();
		AABB area = player.getBoundingBox().inflate(radius, 2.0, radius);
		for (Entity target : level.getEntities(player, area, entity -> entity.isAlive())) {
			Vec3 fromPlayer = target.position().subtract(player.position());
			if (fromPlayer.lengthSqr() > radius * radius) continue;

			if (target instanceof ItemEntity item) {
				Vec3 pull = player.position().subtract(item.position()).normalize()
						.scale((enhanced ? 0.85 : 0.65) * strengthMultiplier);
				item.setDeltaMovement(item.getDeltaMovement().scale(0.35).add(pull.x, pull.y + 0.12, pull.z));
				item.setNoPickUpDelay();
			} else if (target instanceof LivingEntity livingTarget) {
				Vec3 away = fromPlayer.lengthSqr() < 0.0001 ? forward : fromPlayer.normalize();
				livingTarget.push(away.x * (enhanced ? 1.45 : 1.1) * strengthMultiplier,
						0.28 * strengthMultiplier, away.z * (enhanced ? 1.45 : 1.1) * strengthMultiplier);
				livingTarget.hurtMarked = true;
			}
		}

		level.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 0.15, player.getZ(),
				40, radius * 0.45, 0.15, radius * 0.45, 0.12);
		level.sendParticles(ParticleTypes.BUBBLE, player.getX(), player.getY() + 0.2, player.getZ(),
				24, radius * 0.35, 0.2, radius * 0.35, 0.08);
		level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS,
				1.2F, enhanced ? 0.75F : 0.9F);
	}
}