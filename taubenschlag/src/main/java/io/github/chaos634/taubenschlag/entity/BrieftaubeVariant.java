package io.github.chaos634.taubenschlag.entity;

import net.minecraft.util.RandomSource;

/** The plumage of a pigeon, named the way pigeon fanciers call them. */
public enum BrieftaubeVariant {
	/** Blue with two black bars on the wings, the most common. */
	BLAU("blau", 40),
	/** Blue, chequered all over the wings ("hammered"). */
	GEHAEMMERT("gehaemmert", 30),
	/** Red, a warm brown. */
	ROT("rot", 15),
	/** White. */
	WEISS("weiss", 15);

	private static final int TOTAL_WEIGHT = 100;

	private final String id;
	private final int weight;

	BrieftaubeVariant(String id, int weight) {
		this.id = id;
		this.weight = weight;
	}

	public String id() {
		return id;
	}

	public static BrieftaubeVariant byIndex(int index) {
		BrieftaubeVariant[] values = values();
		return values[Math.floorMod(index, values.length)];
	}

	/** A plumage as it turns up among wild pigeons. */
	public static BrieftaubeVariant random(RandomSource random) {
		int roll = random.nextInt(TOTAL_WEIGHT);
		for (BrieftaubeVariant variant : values()) {
			roll -= variant.weight;
			if (roll < 0) {
				return variant;
			}
		}

		return BLAU;
	}
}
