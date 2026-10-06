package io.github.chaos634.kumpel.entity;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * One level a Kumpel can reach. The list of levels comes from the config, see {@link io.github.chaos634.kumpel.config.KumpelSettings}.
 */
public record KumpelTier(
		int level,
		String name,
		int requiredExperience,
		double maxHealth,
		double movementSpeed,
		int senseRadius,
		int collectRadius,
		int pocketRows,
		boolean fireImmune,
		Identifier texture
) {
	public static final int MAX_POCKET_ROWS = 6;

	public int pocketSlots() {
		return pocketRows * 9;
	}

	public Component displayName() {
		String fallback = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
		return Component.translatableWithFallback("kumpel.tier." + name, fallback);
	}
}
