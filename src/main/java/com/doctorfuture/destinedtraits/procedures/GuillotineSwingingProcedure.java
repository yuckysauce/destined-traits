package com.doctorfuture.destinedtraits.procedures;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.common.NeoForge;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber
public class GuillotineSwingingProcedure {

    // Map to track active player dashes and their remaining ticks
    private static final Map<UUID, Integer> activeDashes = new ConcurrentHashMap<>();

    // Helper class to store the results of our weapon scan
    private static class PerkData {
        public boolean hasPerk = false;
        public boolean hasEnhanced = false;
    }

    // Helper method to scan the item for the Guillotine Swinging perk
    private static PerkData getGuillotineData(ItemStack item) {
        PerkData data = new PerkData();
        if (item.isEmpty()) return data;

        @SuppressWarnings("unchecked")
        DataComponentType<List<ResourceLocation>> perkComponent = 
            (DataComponentType<List<ResourceLocation>>) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));

        if (perkComponent != null) {
            List<ResourceLocation> perks = item.getOrDefault(perkComponent, Collections.emptyList());
            int index = perks.indexOf(ResourceLocation.fromNamespaceAndPath("destinedtraits", "guillotine_swinging"));
            
            if (index != -1) {
                data.hasPerk = true;
                int tier = item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
                
                if (WeaponPerkUtils.isOriginEnhanced(item, ResourceLocation.fromNamespaceAndPath("destinedtraits", "guillotine_swinging"))) {
                    data.hasEnhanced = true;
                }
            }
        }
        return data;
    }

    // =================================================================================
    // EVENT 1: KILL A MOB -> PROC GUILLOTINE SWINGING
    // =================================================================================
    @SubscribeEvent
    public static void onEntityKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        
        if (event.getSource().getEntity() instanceof Player player) {
            PerkData perkData = getGuillotineData(player.getMainHandItem());
            
            if (perkData.hasPerk) {
                MobEffect rawEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "guillotine_swinging_effect"));
                
                if (rawEffect != null) {
                    Holder<MobEffect> effectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(rawEffect);
                    
                    // Check if the player already has the buff before applying it
                    boolean alreadyHadBuff = player.hasEffect(effectHolder);
                    MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
                    
                    // Grant the buff for 10 seconds (200 ticks)
                    player.addEffect(new MobEffectInstance(effectHolder, masterworks.scaleDuration(200), perkData.hasEnhanced ? 1 : 0, false, true));
                    
                    // Only play the sound if this is a fresh application (not a refresh)
                    if (!alreadyHadBuff) {
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.2F);
                    }
                }
            }
        }
    }

    // =================================================================================
    // EVENT 2: RIGHT CLICK -> CONSUME BUFF AND DASH
    // =================================================================================
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack item = player.getItemInHand(event.getHand());
        PerkData perkData = getGuillotineData(item);
        
        if (!perkData.hasPerk) return;

        // 1. Prevent activation if the weapon's attack speed is on cooldown or the item itself is on cooldown
        if (player.getAttackStrengthScale(0.0f) < 1.0f || player.getCooldowns().isOnCooldown(item.getItem())) {
            return;
        }

        MobEffect rawEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("destinedtraits", "guillotine_swinging_effect"));
        
        if (rawEffect != null) {
            Holder<MobEffect> effectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(rawEffect);
            
            if (player.hasEffect(effectHolder)) {
                
                // 2. Behave as if the weapon was swung by resetting the attack cooldown timer natively
                player.resetAttackStrengthTicker();
                
                // Consume the effect (Server Side)
                if (!player.level().isClientSide()) {
                    player.removeEffect(effectHolder);
                    
                    // Initiate dash for 10 ticks (15 if enhanced)
                    activeDashes.put(player.getUUID(), MasterworkUtils.getStats(player).scaleDuration(perkData.hasEnhanced ? 15 : 10));
                    
                    ServerLevel serverLevel = (ServerLevel) player.level();
                    
                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PIGLIN_BRUTE_DEATH, SoundSource.PLAYERS, 1.0F, 1.0F);
                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1.0F, 0.7F);
                    
                    // 3. Damage the item by 3 durability (Using Minecraft 1.21.1 standard signature)
                    if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        item.hurtAndBreak(3, serverLevel, serverPlayer,
                                brokenItem -> serverPlayer.onEquippedItemBroken(brokenItem, event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND));
                    }
                }
                
                // Apply velocity (Run on both sides to prevent client rubberbanding)
                Vec3 look = player.getLookAngle();
                // Push the player aggressively forward and slightly upward
                player.setDeltaMovement(player.getDeltaMovement().add(look.x * 1.0, 0.3, look.z * 1.0));
                player.hurtMarked = true;
                
                player.swing(event.getHand(), true);
            }
        }
    }
    
    // =================================================================================
    // EVENT 3: DASH COLLISION -> ARMOR PIERCING DAMAGE
    // =================================================================================
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        
        UUID uuid = player.getUUID();
        
        if (activeDashes.containsKey(uuid)) {
            int ticksLeft = activeDashes.get(uuid);
            
            if (ticksLeft > 0) {
                activeDashes.put(uuid, ticksLeft - 1);
                ServerLevel serverLevel = (ServerLevel) player.level();
                
                // Spawn trail particles while dashing
                serverLevel.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
                
                // Create a hitbox slightly larger than the player to catch enemies during the charge
                MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(player);
                AABB hitbox = player.getBoundingBox().inflate(1.5 * masterworks.rangeMultiplier());
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, hitbox, e -> e.isAlive() && e != player);
                
                if (!targets.isEmpty()) {
                    // Calculate base damage based on the player's attack attribute
                    double baseDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE) * masterworks.strengthMultiplier();
                    
                    for (LivingEntity target : targets) {
                        // Rely on vanilla I-frames so we don't hit the same mob 10 times in one dash
                        if (target.invulnerableTime <= 10) {
                            
                            // 1. Manually fire a CriticalHitEvent so Forge mods register this as a crit
                            CriticalHitEvent critEvent = new CriticalHitEvent(player, target, 1.5F, true);
                            NeoForge.EVENT_BUS.post(critEvent);
                            
                            // 2. Use the event's damage multiplier
                            float finalDamage = (float) (baseDamage * critEvent.getDamageMultiplier()); 
                            
                            // 3. FIX FOR MCREATOR: Spoof the player's gravity state to trick MCreator's crit checks (Dragonfly)
                            float oldFallDist = player.fallDistance;
                            boolean oldOnGround = player.onGround();
                            
                            player.fallDistance = 1.0F; // Pretend we are falling
                            player.setOnGround(false);  // Pretend we are in the air
                            
                            // Indirect Magic damage natively bypasses armor in Minecraft while still attributing the kill to the player
                            DamageSource piercingSource = target.damageSources().indirectMagic(player, player);
                            boolean success = target.hurt(piercingSource, finalDamage);
                            
                            // Immediately restore the player's actual physical state
                            player.fallDistance = oldFallDist;
                            player.setOnGround(oldOnGround);
                            
                            if (success) {
                                // Knock the enemy aside
                                Vec3 knockback = player.getLookAngle().scale(0.8);
                                target.push(knockback.x, 0.4, knockback.z);
                                
                                // Heavy impact visuals
                                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                                serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
                                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(), 5, 0.3, 0.3, 0.3, 0.1);
                                
                                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.9F);
                            }
                        }
                    }
                }
            } else {
                // Dash duration finished, remove from queue
                activeDashes.remove(uuid);
            }
        }
    }
}