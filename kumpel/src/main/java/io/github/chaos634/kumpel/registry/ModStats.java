package io.github.chaos634.kumpel.registry;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

import io.github.chaos634.kumpel.Kumpel;
import io.github.chaos634.kumpel.entity.ShiftLog;

/**
 * Statistics in the player's statistics screen: everything a player's Kumpels count in their shift log,
 * added up over all of them, plus how many Kumpels the player has awakened.
 */
public final class ModStats {
	public static final Identifier KUMPELS_AWAKENED = register("kumpels_awakened");
	private static final Map<ShiftLog.Entry, Identifier> BY_LOG_ENTRY = new EnumMap<>(ShiftLog.Entry.class);

	static {
		for (ShiftLog.Entry entry : ShiftLog.Entry.values()) {
			BY_LOG_ENTRY.put(entry, register(entry.key()));
		}
	}

	private ModStats() {
	}

	private static Identifier register(String name) {
		Identifier id = Kumpel.id(name);
		Registry.register(BuiltInRegistries.CUSTOM_STAT, id, id);
		Stats.CUSTOM.get(id, StatFormatter.DEFAULT);
		return id;
	}

	/** The statistic a shift log entry counts towards. */
	public static Identifier of(ShiftLog.Entry entry) {
		return BY_LOG_ENTRY.get(entry);
	}

	public static void initialize() {
		// Loading the class registers the statistics.
	}
}
