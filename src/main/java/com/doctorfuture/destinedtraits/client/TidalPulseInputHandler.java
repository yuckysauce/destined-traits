package com.doctorfuture.destinedtraits.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import com.doctorfuture.destinedtraits.network.TidalPulseSneakPayload;
import com.doctorfuture.destinedtraits.procedures.TidalPulseProcedure;

@EventBusSubscriber(modid = "destinedtraits", value = Dist.CLIENT)
public final class TidalPulseInputHandler {
	private static boolean wasSneakDown;

	private TidalPulseInputHandler() {}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		boolean sneakDown = minecraft.options.keyShift.isDown();
		boolean sneakPressed = sneakDown && !wasSneakDown;
		wasSneakDown = sneakDown;
		if (sneakPressed && minecraft.player != null && minecraft.screen == null
				&& TidalPulseProcedure.hasTidalPulseArmor(minecraft.player)) {
			PacketDistributor.sendToServer(new TidalPulseSneakPayload());
		}
	}
}