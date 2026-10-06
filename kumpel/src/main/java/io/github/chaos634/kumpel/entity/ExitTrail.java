package io.github.chaos634.kumpel.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Ausfahrt: the way the owner walked underground, from the last place with open sky. Recorded every few blocks;
 * when the owner walks back along it, the loop is cut off, so the trail always leads the shortest known way out.
 */
public class ExitTrail {
	private static final int MAX_POINTS = 256;
	private static final double STEP_SQ = 4.0 * 4.0;
	private static final double LOOP_SQ = 3.0 * 3.0;

	private final List<BlockPos> points = new ArrayList<>();
	private ResourceKey<Level> dimension;
	private BlockPos lastSurface;

	/** Called now and then with where the owner is. */
	public void record(Level level, BlockPos ownerPos, boolean underground) {
		if (level.dimension() != dimension) {
			points.clear();
			lastSurface = null;
			dimension = level.dimension();
		}

		if (!underground) {
			points.clear();
			lastSurface = ownerPos.immutable();
			return;
		}

		if (points.isEmpty()) {
			if (lastSurface == null) {
				// Never saw the owner up top, so the Kumpel can't know the way.
				return;
			}
			points.add(lastSurface);
		}

		if (points.getLast().distSqr(ownerPos) < STEP_SQ) {
			return;
		}

		for (int i = 0; i < points.size() - 1; i++) {
			if (points.get(i).distSqr(ownerPos) < LOOP_SQ) {
				points.subList(i + 1, points.size()).clear();
				break;
			}
		}

		points.add(ownerPos.immutable());
		if (points.size() > MAX_POINTS) {
			thinOut();
		}
	}

	/** Keeps every other point (but always the first and the last), so a long trail still fits. */
	private void thinOut() {
		List<BlockPos> kept = new ArrayList<>();
		for (int i = 0; i < points.size(); i++) {
			if (i % 2 == 0 || i == points.size() - 1) {
				kept.add(points.get(i));
			}
		}
		points.clear();
		points.addAll(kept);
	}

	/** From the way out (index 0) to the deepest point. */
	public List<BlockPos> points() {
		return points;
	}

	public boolean knowsTheWay() {
		return points.size() >= 2;
	}

	public void clear() {
		points.clear();
	}
}
