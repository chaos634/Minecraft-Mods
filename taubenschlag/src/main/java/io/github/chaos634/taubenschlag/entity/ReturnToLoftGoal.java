package io.github.chaos634.taubenschlag.entity;

import java.util.EnumSet;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** A pigeon with a loft does not stray far from it: when it has wandered off, it flies back. */
public class ReturnToLoftGoal extends Goal {
	private static final double CLOSE_ENOUGH_SQ = 3.0 * 3.0;
	/** After a failed or finished return, wait this long before trying again. */
	private static final int COOLDOWN_TICKS = 40;

	private final BrieftaubeEntity pigeon;
	private final double speedModifier;
	private int cooldown;

	public ReturnToLoftGoal(BrieftaubeEntity pigeon, double speedModifier) {
		this.pigeon = pigeon;
		this.speedModifier = speedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	private Vec3 target() {
		GlobalPos home = pigeon.getHome();
		if (home == null || !home.dimension().equals(pigeon.level().dimension()) || pigeon.isOrderedToSit() || pigeon.isLeashed() || pigeon.isPassenger()) {
			return null;
		}

		return Vec3.atBottomCenterOf(home.pos().above());
	}

	@Override
	public boolean canUse() {
		if (cooldown > 0) {
			cooldown--;
			return false;
		}

		Vec3 target = target();
		return target != null && pigeon.distanceToSqr(target) > BrieftaubeEntity.LOFT_RANGE * BrieftaubeEntity.LOFT_RANGE
				&& pigeon.distanceToSqr(target) <= BrieftaubeEntity.HOMESICK_RANGE * BrieftaubeEntity.HOMESICK_RANGE;
	}

	@Override
	public boolean canContinueToUse() {
		Vec3 target = target();
		return target != null && !pigeon.getNavigation().isDone() && pigeon.distanceToSqr(target) > CLOSE_ENOUGH_SQ;
	}

	@Override
	public void start() {
		Vec3 target = target();
		if (target != null) {
			pigeon.getNavigation().moveTo(target.x, target.y + 0.5, target.z, speedModifier);
		}
	}

	@Override
	public void stop() {
		cooldown = COOLDOWN_TICKS;
	}
}
