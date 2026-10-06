package io.github.chaos634.kumpel.entity.behaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

import io.github.chaos634.kumpel.registry.ModItems;
import io.github.chaos634.kumpel.util.Chunks;

/**
 * Steigerlied: while a jukebox plays nearby, the Kumpel dances; to the Kumpelkapelle's record it also sings along.
 */
public final class Steigerlied {
	private static final int RANGE = 8;

	private Steigerlied() {
	}

	/** Whether the jukebox there plays the Kumpelkapelle's own record. */
	public static boolean playsKumpelkapelle(ServerLevel level, BlockPos jukebox) {
		return level.getBlockEntity(jukebox) instanceof JukeboxBlockEntity box && box.getTheItem().is(ModItems.MUSIC_DISC_GLUECK_AUF);
	}

	/** The nearest jukebox within range that is playing a song, or {@code null}. */
	public static BlockPos findPlayingJukebox(ServerLevel level, BlockPos center) {
		BlockPos nearest = null;
		double nearestDistance = Double.MAX_VALUE;

		int minX = SectionPos.blockToSectionCoord(center.getX() - RANGE);
		int maxX = SectionPos.blockToSectionCoord(center.getX() + RANGE);
		int minZ = SectionPos.blockToSectionCoord(center.getZ() - RANGE);
		int maxZ = SectionPos.blockToSectionCoord(center.getZ() + RANGE);

		// Looking through the block entities of the nearby chunks is much cheaper than checking every block.
		for (int chunkX = minX; chunkX <= maxX; chunkX++) {
			for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
				if (!Chunks.isLoaded(level, chunkX, chunkZ)) {
					continue;
				}

				for (BlockEntity blockEntity : level.getChunk(chunkX, chunkZ).getBlockEntities().values()) {
					if (!(blockEntity instanceof JukeboxBlockEntity jukebox) || !jukebox.getSongPlayer().isPlaying()) {
						continue;
					}

					double distance = blockEntity.getBlockPos().distSqr(center);
					if (distance <= RANGE * RANGE && distance < nearestDistance) {
						nearest = blockEntity.getBlockPos();
						nearestDistance = distance;
					}
				}
			}
		}

		return nearest;
	}
}
