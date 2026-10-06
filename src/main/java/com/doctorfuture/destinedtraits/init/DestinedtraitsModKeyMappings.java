/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package com.doctorfuture.destinedtraits.init;

import org.lwjgl.glfw.GLFW;

import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

@EventBusSubscriber(Dist.CLIENT)
public class DestinedtraitsModKeyMappings {
	public static final KeyMapping INSPECT = new KeyMapping("key.destinedtraits.inspect", GLFW.GLFW_KEY_Z, "key.category.destinedtraits.inventory");

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(INSPECT);
	}

	@EventBusSubscriber(Dist.CLIENT)
	public static class KeyEventListener {
		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			if (Minecraft.getInstance().screen == null) {
			}
		}
	}
}