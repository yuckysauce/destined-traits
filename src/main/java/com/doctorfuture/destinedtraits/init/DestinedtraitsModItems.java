/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.doctorfuture.destinedtraits.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

import net.minecraft.world.item.Item;

import com.doctorfuture.destinedtraits.item.ParacausalMoteItem;
import com.doctorfuture.destinedtraits.DestinedtraitsMod;

public class DestinedtraitsModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(DestinedtraitsMod.MODID);
	public static final DeferredItem<Item> PARACAUSAL_MOTE;
	static {
		PARACAUSAL_MOTE = REGISTRY.register("paracausal_mote", ParacausalMoteItem::new);
	}
	// Start of user code block custom items
	// End of user code block custom items
}