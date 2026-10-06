package io.github.chaos634.kumpel.entity.ai;

import java.util.EnumSet;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

import io.github.chaos634.kumpel.entity.KumpelEntity;

/**
 * Walks to dropped items near the Kumpel and its owner and picks them up.
 */
public class CollectItemsGoal extends Goal {
	private static final int GIVE_UP_AFTER_TICKS = 200;
	private static final double PICKUP_DISTANCE_SQ = 1.6 * 1.6;

	private final KumpelEntity kumpel;
	private final double speedModifier;
	private ItemEntity target;
	private int ticksRunning;
	private int repathCooldown;
	private int searchCooldown;

	public CollectItemsGoal(KumpelEntity kumpel, double speedModifier) {
		this.kumpel = kumpel;
		this.speedModifier = speedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!kumpel.isTame() || kumpel.isOrderedToSit() || kumpel.isInventoryFull()) {
			return false;
		}

		if (--searchCooldown > 0) {
			return false;
		}

		searchCooldown = reducedTickDelay(10);
		target = kumpel.findCollectableItem();
		return target != null;
	}

	@Override
	public boolean canContinueToUse() {
		return target != null
				&& ticksRunning < GIVE_UP_AFTER_TICKS
				&& !kumpel.isOrderedToSit()
				&& kumpel.canCollect(target, kumpel.getOwner());
	}

	@Override
	public void start() {
		ticksRunning = 0;
		repathCooldown = 0;
	}

	@Override
	public void stop() {
		if (target != null && ticksRunning >= GIVE_UP_AFTER_TICKS) {
			// Probably unreachable, don't try again for a while.
			kumpel.ignoreItem(target);
		}

		target = null;
		kumpel.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		ticksRunning++;
		kumpel.getLookControl().setLookAt(target, 30.0F, 30.0F);

		if (kumpel.distanceToSqr(target) <= PICKUP_DISTANCE_SQ) {
			kumpel.collect(target);
			target = null;
			return;
		}

		if (--repathCooldown <= 0) {
			repathCooldown = adjustedTickDelay(10);
			kumpel.getNavigation().moveTo(target, speedModifier);
		}
	}
}
