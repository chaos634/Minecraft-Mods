package io.github.chaos634.kumpel.entity.ai;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.entity.behaviour.CoalDust;

/**
 * Waschkaue: a dusty Kumpel with nothing else to do walks to a water cauldron nearby and washes.
 */
public class WashGoal extends Goal {
	private static final int SEARCH_INTERVAL = 40;
	private static final int RANGE = 10;
	private static final double REACH_SQ = 2.0 * 2.0;
	private static final int GIVE_UP_TICKS = 300;

	private final KumpelEntity kumpel;
	private final double speedModifier;
	private BlockPos cauldron;
	private int searchCooldown;
	private int ticks;

	public WashGoal(KumpelEntity kumpel, double speedModifier) {
		this.kumpel = kumpel;
		this.speedModifier = speedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	private boolean wantsToWash() {
		return KumpelSettings.get().behaviour().coalDust && kumpel.isTame() && !kumpel.isOrderedToSit() && kumpel.getTunnel() == null
				&& kumpel.getDust() >= CoalDust.WASH_AT;
	}

	@Override
	public boolean canUse() {
		if (!wantsToWash() || --searchCooldown > 0) {
			return false;
		}

		searchCooldown = SEARCH_INTERVAL;
		cauldron = findCauldron();
		return cauldron != null;
	}

	@Override
	public boolean canContinueToUse() {
		return cauldron != null && ticks < GIVE_UP_TICKS && kumpel.getDust() > 0 && !kumpel.isOrderedToSit()
				&& kumpel.level().getBlockState(cauldron).is(Blocks.WATER_CAULDRON);
	}

	@Override
	public void start() {
		ticks = 0;
		moveToCauldron();
	}

	@Override
	public void stop() {
		cauldron = null;
		kumpel.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (cauldron == null) {
			return;
		}

		ticks++;
		Vec3 center = Vec3.atCenterOf(cauldron);
		kumpel.getLookControl().setLookAt(center.x, center.y, center.z);
		if (kumpel.distanceToSqr(center) <= REACH_SQ) {
			kumpel.getNavigation().stop();
			kumpel.washInCauldron((ServerLevel) kumpel.level(), cauldron);
			cauldron = null;
		} else if (ticks % 20 == 0) {
			moveToCauldron();
		}
	}

	private void moveToCauldron() {
		kumpel.getNavigation().moveTo(cauldron.getX() + 0.5, cauldron.getY(), cauldron.getZ() + 0.5, speedModifier);
	}

	/** The nearest water cauldron around the Kumpel. */
	private BlockPos findCauldron() {
		BlockPos origin = kumpel.blockPosition();
		BlockPos nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-RANGE, -3, -RANGE), origin.offset(RANGE, 3, RANGE))) {
			if (kumpel.level().getBlockState(pos).is(Blocks.WATER_CAULDRON)) {
				double distance = pos.distSqr(origin);
				if (distance < nearestDistance) {
					nearest = pos.immutable();
					nearestDistance = distance;
				}
			}
		}

		return nearest;
	}
}
