package io.github.chaos634.taubenschlag.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

import io.github.chaos634.taubenschlag.Taubenschlag;
import io.github.chaos634.taubenschlag.entity.BrieftaubeData;

public final class TaubenschlagComponents {
	/** The pigeon sitting in a travel basket. */
	public static final DataComponentType<BrieftaubeData> BRIEFTAUBE = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Taubenschlag.id("brieftaube"),
			DataComponentType.<BrieftaubeData>builder()
					.persistent(BrieftaubeData.CODEC)
					.networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(BrieftaubeData.CODEC))
					.build()
	);

	private TaubenschlagComponents() {
	}

	public static void initialize() {
		// Registers the components by loading this class.
	}
}
