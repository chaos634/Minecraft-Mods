package io.github.chaos634.taubenschlag.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import io.github.chaos634.taubenschlag.Taubenschlag;

/** The mod's own sounds, synthesised for it (see {@code assets/taubenschlag/sounds.json}). */
public final class TaubenschlagSounds {
	public static final SoundEvent GURREN = register("brieftaube.gurren");
	public static final SoundEvent AUFLASSEN = register("brieftaube.auflassen");

	private TaubenschlagSounds() {
	}

	private static SoundEvent register(String name) {
		Identifier id = Taubenschlag.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void initialize() {
		// Loading the class registers the sounds.
	}
}
