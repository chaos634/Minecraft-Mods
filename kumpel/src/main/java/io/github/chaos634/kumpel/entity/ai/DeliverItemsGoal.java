package io.github.chaos634.kumpel.entity.ai;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import io.github.chaos634.kumpel.entity.KumpelEntity;

/**
 * Brings collected items to the Kumpel's storage chest, if it has one, or else back to the owner,
 * once the Kumpel is done collecting (or its pockets are full).
 */
public class DeliverItemsGoal extends Goal {
	private static final double MAX_DISTANCE_SQ = 32.0 * 32.0;
	private static final double HAND_OVER_DISTANCE_SQ = 2.5 * 2.5;
	private static final double STORAGE_REACH_SQ = 2.5 * 2.5;
	private static final int STORAGE_GIVE_UP_TICKS = 300;

	private final KumpelEntity kumpel;
	private final double speedModifier;
	private Player owner;
	private BlockPos storage;
	private int repathCooldown;
	private int ticks;

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

		storage = kumpel.usableStorage();
		owner = kumpel.getOwner() instanceof Player player ? player : null;
		return storage != null || isValidOwner();
	}

	@Override
	public boolean canContinueToUse() {
		if (kumpel.isOrderedToSit() || !kumpel.hasItemsToDeliver()) {
			return false;
		}

		return storage != null ? storage.equals(kumpel.usableStorage()) : isValidOwner();
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
		ticks = 0;
	}

	@Override
	public void stop() {
		owner = null;
		storage = null;
		kumpel.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		ticks++;
		if (storage != null) {
			tickTowardsStorage();
		} else {
			tickTowardsOwner();
		}
	}

	private void tickTowardsStorage() {
		Vec3 center = Vec3.atCenterOf(storage);
		kumpel.getLookControl().setLookAt(center.x, center.y, center.z);

		if (kumpel.distanceToSqr(center) <= STORAGE_REACH_SQ) {
			kumpel.getNavigation().stop();
			kumpel.deliverItemsToStorage((ServerLevel) kumpel.level());
			return;
		}

		if (ticks > STORAGE_GIVE_UP_TICKS) {
			// Can't get there: take the loot to the owner for a while instead.
			kumpel.storageUnreachable();
			return;
		}

		if (--repathCooldown <= 0) {
			repathCooldown = adjustedTickDelay(10);
			kumpel.getNavigation().moveTo(center.x, storage.getY(), center.z, speedModifier);
		}
	}

	private void tickTowardsOwner() {
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
