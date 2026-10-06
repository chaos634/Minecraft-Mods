package io.github.chaos634.kumpel.compat.rei;

import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;

import io.github.chaos634.kumpel.Kumpel;

/** Registers how Kumpel's REI displays are saved and synced. Only loaded when REI is installed. */
public class KumpelReiCommonPlugin implements REICommonPlugin {
	@Override
	public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
		registry.register(Kumpel.id("feeding"), FeedingDisplay.SERIALIZER);
		registry.register(Kumpel.id("ore_sensing"), OreSensingDisplay.SERIALIZER);
	}
}
