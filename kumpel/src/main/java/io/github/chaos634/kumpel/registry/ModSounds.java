package io.github.chaos634.kumpel.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import io.github.chaos634.kumpel.Kumpel;

/** The mod's own sounds, synthesised for it (see {@code assets/kumpel/sounds.json}). */
public final class ModSounds {
	public static final SoundEvent WHISTLE_CALL = register("item.steiger_whistle.call");
	public static final SoundEvent WHISTLE_BREAK = register("item.steiger_whistle.break");
	public static final SoundEvent WHISTLE_ORDER = register("item.steiger_whistle.order");
	public static final SoundEvent KUMPEL_CHEER = register("entity.kumpel.cheer");

	private ModSounds() {
	}

	private static SoundEvent register(String name) {
		Identifier id = Kumpel.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void initialize() {
		// Loading the class registers the sounds.
	}
}
