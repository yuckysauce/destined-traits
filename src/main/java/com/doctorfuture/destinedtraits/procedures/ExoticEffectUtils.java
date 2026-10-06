package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class ExoticEffectUtils {
    private ExoticEffectUtils() {}

    public static boolean hasArmorPerk(Player player, ResourceLocation perk) {
        for (ItemStack armor : player.getArmorSlots()) {
            if (WeaponPerkUtils.hasPerk(armor, perk)) return true;
        }
        return false;
    }

    public static boolean hasNearbyRedstoneSource(ServerLevel level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -radius; z <= radius; z++) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    var state = level.getBlockState(pos);
                    if (state.is(net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK)
                            || state.is(net.minecraft.world.level.block.Blocks.REDSTONE_TORCH)
                            || state.is(net.minecraft.world.level.block.Blocks.REDSTONE_WALL_TORCH)) return true;
                }
            }
        }
        return false;
    }

    public static void launchAway(LivingEntity target, Vec3 center, double horizontal, double vertical) {
        double dx = target.getX() - center.x;
        double dz = target.getZ() - center.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.001) length = 1.0;
        target.setDeltaMovement(target.getDeltaMovement().add(dx / length * horizontal, vertical, dz / length * horizontal));
        target.hasImpulse = true;
    }
}