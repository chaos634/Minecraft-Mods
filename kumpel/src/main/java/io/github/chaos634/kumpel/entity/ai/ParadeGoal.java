package io.github.chaos634.kumpel.entity.ai;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.entity.behaviour.Bergparade;

/**
 * Bergparade: marches in single file behind the Kumpel (or owner) in front of it.
 */
public class ParadeGoal extends Goal {
	/** Distance kept to the one in front. */
	private static final double SPACING = 1.5;
	private static final double CLOSE_ENOUGH_SQ = 0.6 * 0.6;
	private static final double TELEPORT_SQ = 20.0 * 20.0;
	private static final int REORDER_INTERVAL = 10;

	private final KumpelEntity kumpel;
	private final double speedModifier;
	private LivingEntity leader;
	private int reorderCooldown;

	public ParadeGoal(KumpelEntity kumpel, double speedModifier) {
		this.kumpel = kumpel;
		this.speedModifier = speedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	private Player marchingOwner() {
		if (Bergparade.canMarch(kumpel) && kumpel.getOwner() instanceof Player owner && owner.level() == kumpel.level()
				&& kumpel.distanceToSqr(owner) < Bergparade.RANGE * Bergparade.RANGE && Bergparade.isLeading(owner)) {
			return owner;
		}

		return null;
	}

	@Override
	public boolean canUse() {
		return marchingOwner() != null;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public void start() {
		reorderCooldown = 0;
		leader = null;
	}

	@Override
	public void stop() {
		leader = null;
		kumpel.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		Player owner = marchingOwner();
		if (owner == null) {
			return;
		}

		if (--reorderCooldown <= 0 || leader == null || !leader.isAlive()) {
			reorderCooldown = REORDER_INTERVAL;
			List<KumpelEntity> column = Bergparade.column((ServerLevel) kumpel.level(), owner);
			leader = Bergparade.leaderOf(kumpel, column, owner);
			LivingEntity aheadOfLeader = leader instanceof KumpelEntity marcher ? Bergparade.leaderOf(marcher, column, owner) : null;
			if (leader == owner) {
				kumpel.headParade(owner, column.size());
			}
			Vec3 target = placeBehind(leader, aheadOfLeader);
			if (kumpel.distanceToSqr(target) > TELEPORT_SQ) {
				kumpel.snapTo(target.x, target.y, target.z, leader.getYRot(), 0.0F);
				kumpel.getNavigation().stop();
			} else if (kumpel.distanceToSqr(target) > CLOSE_ENOUGH_SQ) {
				kumpel.getNavigation().moveTo(target.x, target.y, target.z, speedModifier);
			} else {
				kumpel.getNavigation().stop();
			}
		}

		kumpel.getLookControl().setLookAt(leader, 30.0F, 30.0F);
	}

	/**
	 * The spot right behind the one in front: behind the owner's back at the head of the column, and further down the line
	 * (from the one in front of the leader through the leader) everywhere else, so the column stays a line.
	 */
	private static Vec3 placeBehind(LivingEntity leader, LivingEntity aheadOfLeader) {
		Vec3 back = Vec3.directionFromRotation(0.0F, leader.getYRot()).scale(-1.0);
		if (aheadOfLeader != null) {
			Vec3 line = leader.position().subtract(aheadOfLeader.position()).multiply(1.0, 0.0, 1.0);
			if (line.lengthSqr() > 0.01) {
				back = line.normalize();
			}
		}
		return leader.position().add(back.scale(SPACING));
	}
}
