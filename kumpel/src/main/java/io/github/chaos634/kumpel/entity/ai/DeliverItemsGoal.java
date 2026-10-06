package io.github.chaos634.kumpel.entity.ai;

import java.util.EnumSet;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import io.github.chaos634.kumpel.entity.KumpelEntity;

/**
 * Brings collected items back to the owner once the Kumpel is done collecting (or its pockets are full).
 */
public class DeliverItemsGoal extends Goal {
	private static final double MAX_DISTANCE_SQ = 32.0 * 32.0;
	private static final double HAND_OVER_DISTANCE_SQ = 2.5 * 2.5;

	private final KumpelEntity kumpel;
	private final double speedModifier;
	private Player owner;
	private int repathCooldown;

	public DeliverItemsGoal(KumpelEntity kumpel, double speedModifier) {
		this.kumpel = kumpel;
		this.speedModifier = speedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (kumpel.isOrderedToSit() || !kumpel.wantsToDeliver()) {
			return false;
		}

		owner = kumpel.getOwner() instanceof Player player ? player : null;
		return isValidOwner();
	}

	@Override
	public boolean canContinueToUse() {
		return !kumpel.isOrderedToSit() && kumpel.hasItemsToDeliver() && isValidOwner();
	}

	private boolean isValidOwner() {
		return owner != null
				&& owner.isAlive()
				&& !owner.isSpectator()
				&& owner.level() == kumpel.level()
				&& kumpel.distanceToSqr(owner) < MAX_DISTANCE_SQ;
	}

	@Override
	public void start() {
		repathCooldown = 0;
	}

	@Override
	public void stop() {
		owner = null;
		kumpel.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		kumpel.getLookControl().setLookAt(owner, 30.0F, 30.0F);

		if (kumpel.distanceToSqr(owner) <= HAND_OVER_DISTANCE_SQ) {
			kumpel.getNavigation().stop();
			kumpel.deliverItemsTo(owner);
			return;
		}

		if (--repathCooldown <= 0) {
			repathCooldown = adjustedTickDelay(10);
			kumpel.getNavigation().moveTo(owner, speedModifier);
		}
	}
}
