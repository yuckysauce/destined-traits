package com.doctorfuture.destinedtraits.procedures;

import com.doctorfuture.destinedtraits.configuration.DestinedTraitsConfiguration;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

@EventBusSubscriber(modid = "destinedtraits")
public final class UnwelcomeProcedure {
    private static final ResourceLocation PERK = ResourceLocation.fromNamespaceAndPath("destinedtraits", "unwelcome");
    private static final String STORED_XP = "destinedtraits_unwelcome_xp";

    private UnwelcomeProcedure() {}

    @SubscribeEvent
    public static void onExperiencePickup(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && hasShieldPerk(player)) {
            player.getPersistentData().putInt(STORED_XP,
                    player.getPersistentData().getInt(STORED_XP) + event.getOrb().getValue());
        }
    }

    @SubscribeEvent
    public static void onAttackWhileBlocking(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !player.isBlocking() || !hasShieldPerk(player)) return;
        int requirement = Math.max(1, DestinedTraitsConfiguration.XPPERKREQUIREMENT.get().intValue());
        var data = player.getPersistentData();
        int charge = data.getInt(STORED_XP);
        if (charge < requirement) return;
        data.putInt(STORED_XP, charge - requirement);
        sandBlast(player, event.getTarget() instanceof LivingEntity target ? target : player);
    }

    private static boolean hasShieldPerk(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return (main.getItem() instanceof ShieldItem && WeaponPerkUtils.hasPerk(main, PERK))
                || (off.getItem() instanceof ShieldItem && WeaponPerkUtils.hasPerk(off, PERK));
    }

    private static void sandBlast(Player player, LivingEntity center) {
        if (!(player.level() instanceof ServerLevel level)) return;
        AABB area = center.getBoundingBox().inflate(7.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity instanceof Enemy && entity.isAlive())) {
            ExoticEffectUtils.launchAway(target, center.position(), 1.25, 0.55);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, true, true));
        }
        level.sendParticles(ParticleTypes.CLOUD, center.getX(), center.getY() + 0.2, center.getZ(),
                45, 2.5, 0.25, 2.5, 0.08);
        level.playSound(null, center.blockPosition(), SoundEvents.SAND_PLACE,
                SoundSource.PLAYERS, 1.0F, 0.7F);
    }
}