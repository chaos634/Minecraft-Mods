package io.github.chaos634.kumpel.entity.ai;

import java.util.EnumSet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import io.github.chaos634.kumpel.entity.KumpelEntity;

/**
 * Grubenwehr: picks a monster to fight. That is one that hurt the owner, one the owner just hit, or one that
 * is about to go for the owner. Whether the Kumpel wants to fight it at all is up to {@link KumpelEntity#wantsToAttack}.
 */
public class DefendOwnerGoal extends Goal {
	private static final double THREAT_RANGE = 12.0;
	private static final double GIVE_UP_DISTANCE_SQ = 24.0 * 24.0;

	private final KumpelEntity kumpel;
	private LivingEntity threat;
	private int cooldown;

	public DefendOwnerGoal(KumpelEntity kumpel) {
		this.kumpel = kumpel;
		this.setFlags(EnumSet.of(Goal.Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		if (--cooldown > 0 || !kumpel.isTame() || kumpel.isOrderedToSit()) {
			return false;
		}

		cooldown = reducedTickDelay(10);
		LivingEntity owner = kumpel.getOwner();
		if (owner == null) {
			return false;
		}

		threat = findThreat(owner);
		return threat != null;
	}

	@Override
	public boolean canContinueToUse() {
		return threat != null
				&& threat.isAlive()
				&& kumpel.getTarget() == threat
				&& !kumpel.isOrderedToSit()
				&& kumpel.distanceToSqr(threat) < GIVE_UP_DISTANCE_SQ;
	}

	@Override
	public void start() {
		kumpel.setTarget(threat);
	}

	@Override
	public void stop() {
		if (kumpel.getTarget() == threat) {
			kumpel.setTarget(null);
		}
		threat = null;
	}

	private LivingEntity findThreat(LivingEntity owner) {
		LivingEntity best = null;
		double bestDistance = Double.MAX_VALUE;

		for (LivingEntity candidate : new LivingEntity[] {owner.getLastHurtByMob(), owner.getLastHurtMob()}) {
			if (isThreat(candidate, owner)) {
				double distance = candidate.distanceToSqr(owner);
				if (distance < bestDistance) {
					best = candidate;
					bestDistance = distance;
				}
			}
		}

		for (Mob mob : kumpel.level().getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(THREAT_RANGE),
				mob -> mob.getTarget() == owner)) {
			if (isThreat(mob, owner)) {
				double distance = mob.distanceToSqr(owner);
				if (distance < bestDistance) {
					best = mob;
					bestDistance = distance;
				}
			}
		}

		return best;
	}

	private boolean isThreat(LivingEntity candidate, LivingEntity owner) {
		return candidate != null
				&& candidate.isAlive()
				&& candidate.level() == kumpel.level()
				&& candidate.distanceToSqr(owner) < THREAT_RANGE * THREAT_RANGE
				&& kumpel.wantsToAttack(candidate, owner);
	}
}
