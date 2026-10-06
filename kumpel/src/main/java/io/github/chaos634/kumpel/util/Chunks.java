package io.github.chaos634.kumpel.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;

public final class Chunks {
	private Chunks() {
	}

	/**
	 * Whether the chunk is loaded, without loading it. ({@code hasChunkAt} is deprecated, but still the way to ask a level this.)
	 */
	@SuppressWarnings("deprecation")
	public static boolean isLoaded(ServerLevel level, int chunkX, int chunkZ) {
		return level.hasChunkAt(new BlockPos(SectionPos.sectionToBlockCoord(chunkX), 0, SectionPos.sectionToBlockCoord(chunkZ)));
	}
}
