package com.doctorfuture.destinedtraits.procedures;

import com.doctorfuture.destinedtraits.perks.MasterworkUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

@EventBusSubscriber(modid = "destinedtraits")
public final class CleanLandingProcedure {
    private static final ResourceLocation PERK = ResourceLocation.fromNamespaceAndPath("destinedtraits", "clean_landing");
    private static final String MOONSHOT_PEARL = "destinedtraits_clean_landing_pearl";

    private CleanLandingProcedure() {}

    @SubscribeEvent
    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)) return;

        if (event.getEntity() instanceof ThrownEnderpearl pearl && pearl.getOwner() instanceof Player owner
            && hasCleanLandingCrossbow(owner)) {
            pearl.getPersistentData().putBoolean(MOONSHOT_PEARL, true);
            return;
        }

        if (!(event.getEntity() instanceof AbstractArrow arrow) || !(arrow.getOwner() instanceof Player player)) return;
        var weapon = getCleanLandingCrossbow(player);
        if (weapon.isEmpty()) return;

        boolean multishot = getMultishotLevel(player, weapon) > 0;
        if (multishot && arrow.getDeltaMovement().normalize().dot(player.getLookAngle()) < 0.995) return;

        ThrownEnderpearl pearl = new ThrownEnderpearl(level, player);
        pearl.setPos(arrow.position());
        pearl.setDeltaMovement(arrow.getDeltaMovement());
        pearl.setYRot(arrow.getYRot());
        pearl.setXRot(arrow.getXRot());
        pearl.getPersistentData().putBoolean(MOONSHOT_PEARL, true);
        arrow.discard();
        event.setCanceled(true);
        level.addFreshEntity(pearl);
    }

    @SubscribeEvent
    public static void onPearlImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownEnderpearl pearl)
                || !pearl.getPersistentData().getBoolean(MOONSHOT_PEARL)
                || !(pearl.level() instanceof ServerLevel level)) return;
        Vec3 impact = event.getRayTraceResult().getLocation();
        if (pearl.getOwner() instanceof Player player) {
            applyImpactBlast(player, level, impact);
            teleportOwner(player, event.getRayTraceResult(), impact);
        }
        pearl.discard();
        event.setCanceled(true);
    }

    private static void teleportOwner(Player player, HitResult hit, Vec3 impact) {
        Vec3 target = impact;
        if (hit instanceof BlockHitResult blockHit) {
            var normal = blockHit.getDirection().getNormal();
            target = impact.add(normal.getX() * 0.5, normal.getY() < 0 ? -player.getBbHeight() : 0.0, normal.getZ() * 0.5);
        }
        player.teleportTo(target.x, target.y, target.z);
        player.fallDistance = 0.0F;
    }

        private static int getMultishotLevel(Player player, net.minecraft.world.item.ItemStack weapon) {
        var multishot = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.MULTISHOT);
        return weapon.getEnchantmentLevel(multishot);
    }

        private static net.minecraft.world.item.ItemStack getCleanLandingCrossbow(Player player) {
        if (WeaponPerkUtils.hasPerk(player.getMainHandItem(), PERK)) return player.getMainHandItem();
        if (WeaponPerkUtils.hasPerk(player.getOffhandItem(), PERK)) return player.getOffhandItem();
        return net.minecraft.world.item.ItemStack.EMPTY;
        }

    private static boolean hasCleanLandingCrossbow(Player player) {
        return WeaponPerkUtils.hasPerk(player.getMainHandItem(), PERK)
            || WeaponPerkUtils.hasPerk(player.getOffhandItem(), PERK);
    }

    private static void applyImpactBlast(Player player, ServerLevel level, Vec3 impact) {
        MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
        double radius = 5.0 * masterworks.rangeMultiplier();
        AABB area = new AABB(impact, impact).inflate(radius);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area,
                entity -> entity.isAlive() && entity.distanceToSqr(impact) <= radius * radius)) {
            ExoticEffectUtils.launchAway(mob, impact, 1.4, 0.55);
            mob.hurt(player.damageSources().playerAttack(player), (float) (2.0 * masterworks.strengthMultiplier()));
        }
        level.sendParticles(ParticleTypes.PORTAL, impact.x, impact.y, impact.z, 45, 1.5, 0.8, 1.5, 0.25);
        level.playSound(null, net.minecraft.core.BlockPos.containing(impact), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.9F, 0.8F);
    }

}