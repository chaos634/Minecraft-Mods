package io.github.chaos634.kumpel.util;

import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Finds blocks in a sphere quickly: chunk sections whose palette holds no matching block state are skipped
 * without looking at a single block, and unloaded chunks are never loaded.
 */
public final class BlockScanner {
	@FunctionalInterface
	public interface Visitor {
		void visit(BlockPos pos, BlockState state, int distanceSq);
	}

	private BlockScanner() {
	}

	/** Calls the visitor for every block within {@code radius} of {@code center} whose state matches the filter. */
	public static void scanSphere(ServerLevel level, BlockPos center, int radius, Predicate<BlockState> filter, Visitor visitor) {
		int radiusSq = radius * radius;
		int minChunkX = SectionPos.blockToSectionCoord(center.getX() - radius);
		int maxChunkX = SectionPos.blockToSectionCoord(center.getX() + radius);
		int minChunkZ = SectionPos.blockToSectionCoord(center.getZ() - radius);
		int maxChunkZ = SectionPos.blockToSectionCoord(center.getZ() + radius);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				if (!Chunks.isLoaded(level, chunkX, chunkZ)) {
					continue;
				}

				LevelChunk chunk = level.getChunk(chunkX, chunkZ);
				for (int index = 0; index < level.getSectionsCount(); index++) {
					int sectionMinY = SectionPos.sectionToBlockCoord(level.getSectionYFromSectionIndex(index));
					if (sectionMinY + 15 < center.getY() - radius || sectionMinY > center.getY() + radius) {
						continue;
					}

					LevelChunkSection section = chunk.getSection(index);
					if (section.hasOnlyAir() || !section.maybeHas(filter)) {
						continue;
					}

					int minX = Math.max(SectionPos.sectionToBlockCoord(chunkX), center.getX() - radius);
					int maxX = Math.min(SectionPos.sectionToBlockCoord(chunkX) + 15, center.getX() + radius);
					int minY = Math.max(sectionMinY, center.getY() - radius);
					int maxY = Math.min(sectionMinY + 15, center.getY() + radius);
					int minZ = Math.max(SectionPos.sectionToBlockCoord(chunkZ), center.getZ() - radius);
					int maxZ = Math.min(SectionPos.sectionToBlockCoord(chunkZ) + 15, center.getZ() + radius);

					for (int x = minX; x <= maxX; x++) {
						for (int y = minY; y <= maxY; y++) {
							for (int z = minZ; z <= maxZ; z++) {
								int dx = x - center.getX();
								int dy = y - center.getY();
								int dz = z - center.getZ();
								int distanceSq = dx * dx + dy * dy + dz * dz;
								if (distanceSq > radiusSq) {
									continue;
								}

								BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
								if (filter.test(state)) {
									visitor.visit(cursor.set(x, y, z), state, distanceSq);
								}
							}
						}
					}
				}
			}
		}
	}
}
