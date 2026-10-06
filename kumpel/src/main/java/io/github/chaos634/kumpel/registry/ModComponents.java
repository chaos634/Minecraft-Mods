package io.github.chaos634.kumpel.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

import io.github.chaos634.kumpel.Kumpel;
import io.github.chaos634.kumpel.item.KumpelSoul;

public final class ModComponents {
	public static final DataComponentType<KumpelSoul> SOUL = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Kumpel.id("soul"),
			DataComponentType.<KumpelSoul>builder().persistent(KumpelSoul.CODEC).networkSynchronized(KumpelSoul.STREAM_CODEC).build()
	);

	private ModComponents() {
	}

	public static void initialize() {
		// Registers the components by loading this class.
	}
}
