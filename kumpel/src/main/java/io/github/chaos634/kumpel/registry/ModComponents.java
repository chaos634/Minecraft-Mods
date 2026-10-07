package io.github.chaos634.kumpel.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;

import io.github.chaos634.kumpel.Kumpel;
import io.github.chaos634.kumpel.bau.Bauplan;
import io.github.chaos634.kumpel.item.KumpelSoul;

public final class ModComponents {
	public static final DataComponentType<KumpelSoul> SOUL = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Kumpel.id("soul"),
			DataComponentType.<KumpelSoul>builder().persistent(KumpelSoul.CODEC).networkSynchronized(KumpelSoul.STREAM_CODEC).build()
	);

	/** Which building a Bauplan shows. */
	public static final DataComponentType<ResourceKey<Bauplan>> BAUPLAN = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Kumpel.id("bauplan"),
			DataComponentType.<ResourceKey<Bauplan>>builder()
					.persistent(ResourceKey.codec(Bauplan.REGISTRY))
					.networkSynchronized(ResourceKey.streamCodec(Bauplan.REGISTRY))
					.build()
	);

	private ModComponents() {
	}

	public static void initialize() {
		// Registers the components by loading this class.
	}
}
