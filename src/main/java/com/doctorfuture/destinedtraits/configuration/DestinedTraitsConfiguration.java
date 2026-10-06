package com.doctorfuture.destinedtraits.configuration;

import net.neoforged.neoforge.common.ModConfigSpec;

public class DestinedTraitsConfiguration {
	public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	public static final ModConfigSpec SPEC;
	public static final ModConfigSpec.ConfigValue<Double> EXOTICDROPRATE;
	public static final ModConfigSpec.ConfigValue<Double> TIERUPGRADECOST;
	public static final ModConfigSpec.ConfigValue<Double> XPPERKREQUIREMENT;
	public static final ModConfigSpec.IntValue XPMOTEREQUIREMENT;
	static {
		EXOTICDROPRATE = BUILDER.comment("Controls the percentage chance that a randomly rolled item will be replaced by a bespoke Exotic item.").define("exotic_drop_rate", (double) 0.05);
		TIERUPGRADECOST = BUILDER.comment("Controls the base XP cost per tier for using a Paracausal Mote.").define("tier_upgrade_cost", (double) 3.5);
		XPPERKREQUIREMENT = BUILDER.comment("Controls how much XP must be collected to activate certain perks.").define("xp_perk_requirement", (double) 42);
		XPMOTEREQUIREMENT = BUILDER.comment("Controls how much XP must be collected to earn a Paracausal Mote.").defineInRange("xp_mote_requirement", 1395, 1, 1000000);

		SPEC = BUILDER.build();
	}

}