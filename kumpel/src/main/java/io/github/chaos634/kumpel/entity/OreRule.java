package io.github.chaos634.kumpel.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An ore a Kumpel can sense: a block tag or a single block, the level needed to sense it,
 * how valuable it is (the Kumpel always points at the most valuable one) and the pitch of its chime.
 */
public record OreRule(TagKey<Block> tag, Identifier block, int level, int value, float pitch) {
	public boolean matches(BlockState state) {
		if (tag != null && state.is(tag)) {
			return true;
		}

		return block != null && block.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
	}

	/** {@code #c:ores/coal} or {@code minecraft:ancient_debris}, for messages and JEI/REI. */
	public String describe() {
		return tag != null ? "#" + tag.location() : String.valueOf(block);
	}
}
