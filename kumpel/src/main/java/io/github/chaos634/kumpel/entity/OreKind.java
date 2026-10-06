package io.github.chaos634.kumpel.entity;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;

/**
 * Ores a Kumpel can sense, ordered from least to most valuable.
 * Uses the conventional {@code c:ores/*} tags, so ores from other mods are found too.
 */
public enum OreKind {
	COAL(ConventionalBlockTags.COAL_ORES, 1, 0.7F),
	COPPER(ConventionalBlockTags.COPPER_ORES, 1, 0.8F),
	QUARTZ(ConventionalBlockTags.QUARTZ_ORES, 2, 0.9F),
	IRON(ConventionalBlockTags.IRON_ORES, 2, 1.0F),
	REDSTONE(ConventionalBlockTags.REDSTONE_ORES, 2, 1.05F),
	LAPIS(ConventionalBlockTags.LAPIS_ORES, 3, 1.1F),
	GOLD(ConventionalBlockTags.GOLD_ORES, 3, 1.2F),
	EMERALD(ConventionalBlockTags.EMERALD_ORES, 4, 1.4F),
	DIAMOND(ConventionalBlockTags.DIAMOND_ORES, 4, 1.6F),
	ANCIENT_DEBRIS(ConventionalBlockTags.NETHERITE_SCRAP_ORES, 5, 1.9F);

	private final TagKey<Block> tag;
	private final int minLevel;
	private final float pitch;

	OreKind(TagKey<Block> tag, int minLevel, float pitch) {
		this.tag = tag;
		this.minLevel = minLevel;
		this.pitch = pitch;
	}

	/**
	 * Returns the most valuable ore kind this block belongs to, or {@code null} if it is no ore.
	 */
	public static OreKind of(BlockState state) {
		if (!state.is(ConventionalBlockTags.ORES) && !state.is(ConventionalBlockTags.NETHERITE_SCRAP_ORES)) {
			return null;
		}

		OreKind[] kinds = values();
		for (int i = kinds.length - 1; i >= 0; i--) {
			if (state.is(kinds[i].tag)) {
				return kinds[i];
			}
		}

		return null;
	}

	/** The Kumpel level needed to sense this ore. */
	public int minLevel() {
		return minLevel;
	}

	/** Pitch of the chime played when this ore is sensed; rarer ores ring higher. */
	public float pitch() {
		return pitch;
	}
}
