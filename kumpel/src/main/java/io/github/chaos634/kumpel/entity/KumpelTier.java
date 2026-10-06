package io.github.chaos634.kumpel.entity;

import net.minecraft.network.chat.Component;

/**
 * The five levels a Kumpel can reach. Each level makes it sturdier, faster and lets it sense ores further away.
 */
public enum KumpelTier {
	COPPER(1, "copper", 0, 20.0, 0.27, 8, 6),
	IRON(2, "iron", 60, 30.0, 0.28, 11, 8),
	GOLD(3, "gold", 180, 40.0, 0.29, 14, 10),
	DIAMOND(4, "diamond", 400, 50.0, 0.30, 17, 12),
	NETHERITE(5, "netherite", 800, 60.0, 0.31, 20, 14);

	private final int level;
	private final String name;
	private final int requiredExperience;
	private final double maxHealth;
	private final double movementSpeed;
	private final int senseRadius;
	private final int collectRadius;

	KumpelTier(int level, String name, int requiredExperience, double maxHealth, double movementSpeed, int senseRadius, int collectRadius) {
		this.level = level;
		this.name = name;
		this.requiredExperience = requiredExperience;
		this.maxHealth = maxHealth;
		this.movementSpeed = movementSpeed;
		this.senseRadius = senseRadius;
		this.collectRadius = collectRadius;
	}

	public static KumpelTier byLevel(int level) {
		KumpelTier[] tiers = values();
		return tiers[Math.clamp(level - 1, 0, tiers.length - 1)];
	}

	public static KumpelTier forExperience(int experience) {
		KumpelTier result = COPPER;
		for (KumpelTier tier : values()) {
			if (experience >= tier.requiredExperience) {
				result = tier;
			}
		}
		return result;
	}

	/** The next tier, or {@code null} at max level. */
	public KumpelTier next() {
		return this == NETHERITE ? null : values()[ordinal() + 1];
	}

	public int level() {
		return level;
	}

	public String textureName() {
		return name;
	}

	public Component displayName() {
		return Component.translatable("kumpel.tier." + name);
	}

	public int requiredExperience() {
		return requiredExperience;
	}

	public double maxHealth() {
		return maxHealth;
	}

	public double movementSpeed() {
		return movementSpeed;
	}

	public int senseRadius() {
		return senseRadius;
	}

	public int collectRadius() {
		return collectRadius;
	}
}
