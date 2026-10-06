package com.doctorfuture.destinedtraits.procedures;

import com.doctorfuture.destinedtraits.perks.MasterworkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = "destinedtraits")
public final class NorthStarProcedure {
    private static final ResourceLocation PERK = ResourceLocation.fromNamespaceAndPath("destinedtraits", "north_star");
    private static final String CHARGE_START = "destinedtraits_north_star_charge_start";
    private static final int BASE_CHARGE_TICKS = 40;
    private static final int MAX_CHARGE_TICKS = 100;
    private static final Map<LivingEntity, FreezeDot> FREEZE_DOTS = new HashMap<>();

    private NorthStarProcedure() {}

    public static void handleUseInput(Player player, boolean held) {
        if (!(player instanceof ServerPlayer) || player.level().isClientSide()) return;
        var data = player.getPersistentData();
        if (held) {
            if (WeaponPerkUtils.hasPerk(player.getMainHandItem(), PERK)) {
                data.putLong(CHARGE_START, player.level().getGameTime());
            }
            return;
        }

        long start = data.getLong(CHARGE_START);
        data.remove(CHARGE_START);
        if (start > 0) releaseSlam((ServerPlayer) player, (int) (player.level().getGameTime() - start));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long chargeStart = player.getPersistentData().getLong(CHARGE_START);
        if (chargeStart > 0 && player.tickCount % 5 == 0) {
            int elapsed = (int) (player.level().getGameTime() - chargeStart);
            int percent = Math.min(100, elapsed);
            int requiredCharge = MasterworkUtils.getStats(player).scaleEfficiencyCooldown(BASE_CHARGE_TICKS);
                String status = elapsed >= requiredCharge ? "Slam ready" : "Charging";
                player.displayClientMessage(Component.literal("North Star: " + status + " (" + percent + "%)"), true);
        }
        var iterator = FREEZE_DOTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LivingEntity, FreezeDot> entry = iterator.next();
            LivingEntity target = entry.getKey();
            FreezeDot dot = entry.getValue();
            if (target.isRemoved() || !target.isAlive() || dot.ticksLeft <= 0) {
                iterator.remove();
                continue;
            }
            if (dot.owner != player) continue;
            dot.ticksLeft--;
            if (++dot.pulseTicks >= 20) {
                dot.pulseTicks = 0;
                double freezePercent = Math.clamp(target.getTicksFrozen() / 140.0, 0.0, 1.0);
                float damage = (float) ((1.0 + 4.0 * freezePercent) * MasterworkUtils.getStats(player).strengthMultiplier());
                target.hurt(player.damageSources().freeze(), damage);
            }
        }
    }

    private static void releaseSlam(ServerPlayer player, int heldTicks) {
        if (!WeaponPerkUtils.hasPerk(player.getMainHandItem(), PERK)) return;
        MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
        int requiredCharge = masterworks.scaleEfficiencyCooldown(BASE_CHARGE_TICKS);
        if (heldTicks < requiredCharge) {
            player.displayClientMessage(Component.literal("North Star: Charge longer to prepare the slam"), true);
            return;
        }

        double charge = Math.min(1.0, (double) heldTicks / Math.max(requiredCharge, MAX_CHARGE_TICKS));
        double radius = (4.0 + charge * 3.0) * masterworks.rangeMultiplier();
        float damage = (float) ((7.0 + charge * 8.0) * masterworks.strengthMultiplier());
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(18.0));
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        int targetX = net.minecraft.util.Mth.floor(player.getX() + player.getLookAngle().x * 5.0);
        int targetZ = net.minecraft.util.Mth.floor(player.getZ() + player.getLookAngle().z * 5.0);
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);
        BlockPos slamPos = hit.getType() == HitResult.Type.BLOCK
                ? hit.getBlockPos() : new BlockPos(targetX, groundY - 1, targetZ);
        Vec3 center = Vec3.atCenterOf(slamPos);

        AABB area = new AABB(center, center).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive() && entity instanceof Enemy
                        && entity.distanceToSqr(center) <= radius * radius)) {
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), 600));
            target.hurt(player.damageSources().freeze(), damage);
            FREEZE_DOTS.put(target, new FreezeDot(player, 160));
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.15, center.z,
                70, radius * 0.45, 0.35, radius * 0.45, 0.03);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z,
                30, radius * 0.4, 0.1, radius * 0.4, 0.08);
        level.playSound(null, slamPos, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.65F);
    }

    private static final class FreezeDot {
        private final Player owner;
        private int ticksLeft;
        private int pulseTicks;

        private FreezeDot(Player owner, int ticksLeft) {
            this.owner = owner;
            this.ticksLeft = ticksLeft;
        }
    }
}