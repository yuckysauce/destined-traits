package com.doctorfuture.destinedtraits.procedures;

import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import com.doctorfuture.destinedtraits.perks.MasterworkUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@EventBusSubscriber
public class SoulOfTheAncientsProcedure {

    // --- Custom Delay Queue System ---
    private static class DelayedTask {
        public int ticks;
        public Runnable action;
        
        public DelayedTask(int ticks, Runnable action) {
            this.ticks = ticks;
            this.action = action;
        }
    }

    private static final List<DelayedTask> delayedTasks = new CopyOnWriteArrayList<>();

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (DelayedTask task : delayedTasks) {
            task.ticks--;
            if (task.ticks <= 0) {
                task.action.run();
                delayedTasks.remove(task);
            }
        }
    }
    // ---------------------------------

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        net.minecraft.world.item.ItemStack itemstack = event.getItemStack();
        
        if (itemstack.getOrDefault((net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks")), java.util.Collections.emptyList())
                .contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "soul_of_the_ancients"))) {
            
            event.addModifier(
                ALObjects.Attributes.DRAW_SPEED, 
                new AttributeModifier(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "soul_of_the_ancients_draw_debuff"),
                    -0.6, 
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ),
                EquipmentSlotGroup.HAND
            );
        }
    }

    @SubscribeEvent
    public static void onProjectileFired(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
            if (projectile.getOwner() instanceof net.minecraft.world.entity.player.Player player) {
                // Pass the projectile object into the execute method
                execute(event, event.getLevel(), player, projectile);
            }
        }
    }

    public static void execute(LevelAccessor world, Entity entity) {
        execute(null, world, entity, null);
    }

    private static void execute(@Nullable Event event, LevelAccessor world, Entity entity, @Nullable Projectile projectile) {
        if (entity == null)
            return;
            
        if (!world.isClientSide() && world instanceof net.minecraft.server.level.ServerLevel _serverLevel) {
            if (entity.isAlive() && entity instanceof net.minecraft.world.entity.player.Player _player) {
                
                net.minecraft.world.item.ItemStack mainHand = _player.getMainHandItem();
                net.minecraft.world.item.ItemStack offHand = _player.getOffhandItem();
                net.minecraft.world.item.ItemStack activeStack = net.minecraft.world.item.ItemStack.EMPTY;
                
                net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>> perksComp = 
                        (net.minecraft.core.component.DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>) (Object) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
                        .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "perks"));
                
                if (mainHand.getOrDefault(perksComp, java.util.Collections.emptyList()).contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "soul_of_the_ancients"))) {
                    activeStack = mainHand;
                } else if (offHand.getOrDefault(perksComp, java.util.Collections.emptyList()).contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "soul_of_the_ancients"))) {
                    activeStack = offHand;
                }

                if (!activeStack.isEmpty()) {
                    if (projectile instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow
                            && isMultishotSideArrow(_player, activeStack, arrow)) return;
                    MasterworkUtils.Stats masterworks = MasterworkUtils.getStats(_player);
                    double tempDamage = 19.0;
                    boolean isEnhanced = false;
                    java.util.List<net.minecraft.resources.ResourceLocation> perks = activeStack.getOrDefault(perksComp, java.util.Collections.emptyList());
                    int index = perks.indexOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "soul_of_the_ancients"));
                    
                    if (index != -1) {
                        int tier = activeStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("tier");
                        if (WeaponPerkUtils.isOriginEnhanced(activeStack, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("destinedtraits", "soul_of_the_ancients"))) {
                            tempDamage = 23.0; 
                            isEnhanced = true;
                        }
                    }
                    
                    double rangedMultiplier = 1.0;
                    AttributeInstance arrowDamageAttr = _player.getAttribute(ALObjects.Attributes.ARROW_DAMAGE);
                    if (arrowDamageAttr != null) {
                        rangedMultiplier = arrowDamageAttr.getValue();
                    }
                    
                    final double finalDamage = tempDamage * rangedMultiplier * masterworks.strengthMultiplier();
                    
                    // 1. Play the Sonic Charge sound immediately
                    _serverLevel.playSound(null, _player.blockPosition(), net.minecraft.sounds.SoundEvents.WARDEN_DEATH, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
                    
                    // 2. Calculate trajectory and spawn particles IMMEDIATELY
                    Vec3 start = _player.getEyePosition();
                    
                    Vec3 dir;
                    if (projectile != null) {
                        dir = projectile.getDeltaMovement().normalize();
                    } else {
                        dir = _player.getLookAngle().normalize();
                    }
                    
                    // Dynamic distance based on enhanced status
                    double distance = (isEnhanced ? 20.0 : 15.0) * masterworks.rangeMultiplier();
                    Vec3 end = start.add(dir.scale(distance)); 
                    Vec3 step = dir.scale(1.5); 
                    
                    // Draw the visual particle beam
                    Vec3 currentPos = start;
                    for (double d = 0; d < distance; d += 1.5) {
                        currentPos = currentPos.add(step);
                        _serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SONIC_BOOM, currentPos.x, currentPos.y, currentPos.z, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                    
                    // 3. Schedule the Boom sound and Damage exactly 9 ticks later
                    delayedTasks.add(new DelayedTask(9, () -> {
                        // Ensure the player hasn't logged off or died
                        if (!_player.isAlive() || _player.isRemoved()) return;

                        // Play the Sonic Boom sound
                        _serverLevel.playSound(null, _player.blockPosition(), net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
                        
                        // AoE Damage
                        AABB searchBox = new AABB(start, end).inflate(1.5 * masterworks.rangeMultiplier());
                        List<Entity> hitEntities = _serverLevel.getEntities(_player, searchBox);
                        
                        for (Entity targetEntity : hitEntities) {
                            if (targetEntity instanceof net.minecraft.world.entity.LivingEntity _livingTarget) {
                                Optional<Vec3> clipResult = _livingTarget.getBoundingBox().inflate(0.5 * masterworks.rangeMultiplier()).clip(start, end);
                                
                                if (clipResult.isPresent()) {
                                    
                                    // === MANUALLY FIRE THE PROJECTILE IMPACT EVENT ===
                                    if (projectile != null) {
                                        // Update the ghost projectile's position to the exact hit coordinates so 
                                        // Explosive Head knows where to place the visual explosion particles
                                        projectile.setPos(_livingTarget.getX(), _livingTarget.getY(0.5), _livingTarget.getZ());
                                        
                                        EntityHitResult hitResult = new EntityHitResult(_livingTarget);
                                        net.neoforged.neoforge.event.entity.ProjectileImpactEvent impactEvent = new net.neoforged.neoforge.event.entity.ProjectileImpactEvent(projectile, hitResult);
                                        
                                        // Post it to trick other scripts into triggering
                                        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(impactEvent);
                                    }
                                    // =================================================
                                    
                                    // DETERMINE ARROW DAMAGE SOURCE TO TRIGGER ON-HIT MODIFIERS
                                    DamageSource dmgSource;
                                    if (projectile instanceof net.minecraft.world.entity.projectile.AbstractArrow _arrow) {
                                        dmgSource = _livingTarget.damageSources().arrow(_arrow, _player);
                                    } else if (projectile != null) {
                                        dmgSource = _livingTarget.damageSources().thrown(projectile, _player);
                                    } else {
                                        dmgSource = _livingTarget.damageSources().playerAttack(_player);
                                    }
                                    
                                    // Hit them with the new source!
                                    _livingTarget.hurt(dmgSource, (float) finalDamage);
                                    
                                    Vec3 knockback = dir.scale(1.5 * masterworks.strengthMultiplier());
                                    _livingTarget.push(knockback.x, knockback.y + 0.5, knockback.z);
                                }
                            }
                        }
                    }));
                    
                    // 4. Schedule 3 to 5 Warden Tendril Clicks roughly 8 ticks after the Boom (tick 17+)
                    RandomSource random = _serverLevel.getRandom();
                    int clickCount = 3 + random.nextInt(3); 
                    
                    for (int i = 0; i < clickCount; i++) {
                        int tickOffset = 9 + (i * 2) + random.nextInt(3);
                        double randomDist = random.nextDouble() * distance;
                        Vec3 soundPos = start.add(dir.scale(randomDist));
                        
                        delayedTasks.add(new DelayedTask(tickOffset, () -> {
                            if (_player.isAlive() && !_player.isRemoved()) {
                                float pitch = 0.8F + (random.nextFloat() * 0.4F);
                                _serverLevel.playSound(null, BlockPos.containing(soundPos.x, soundPos.y, soundPos.z), net.minecraft.sounds.SoundEvents.SCULK_SHRIEKER_SHRIEK, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, pitch);
                            }
                        }));
                    }
                    
                    // 5. Cancel the arrow spawning
                    if (event instanceof EntityJoinLevelEvent joinEvent) {
                        if (joinEvent instanceof net.neoforged.bus.api.ICancellableEvent _cancellable) {
                            _cancellable.setCanceled(true);
                        }
                    }
                }
            }
        }
    }

    private static boolean isMultishotSideArrow(net.minecraft.world.entity.player.Player player,
            net.minecraft.world.item.ItemStack weapon, net.minecraft.world.entity.projectile.AbstractArrow arrow) {
        var multishot = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.MULTISHOT);
        return weapon.getEnchantmentLevel(multishot) > 0
                && arrow.getDeltaMovement().normalize().dot(player.getLookAngle().normalize()) < 0.995;
    }
}