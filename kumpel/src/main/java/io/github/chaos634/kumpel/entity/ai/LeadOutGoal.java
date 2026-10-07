package io.github.chaos634.kumpel.entity.ai;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import io.github.chaos634.kumpel.entity.ExitTrail;
import io.github.chaos634.kumpel.entity.KumpelEntity;

/**
 * Ausfahrt: walks the recorded trail back to the surface, a little ahead of the owner, waiting when they fall behind.
 * Glowing particles mark the next stretch of the way.
 */
public class LeadOutGoal extends Goal {
	private static final double REACHED_SQ = 1.6 * 1.6;
	private static final double WAIT_FOR_OWNER_SQ = 10.0 * 10.0;

	private final KumpelEntity kumpel;
	private final double speedModifier;
	private int index;
	private int repathCooldown;

	public LeadOutGoal(KumpelEntity kumpel, double speedModifier) {
		this.kumpel = kumpel;
		this.speedModifier = speedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return kumpel.isLeadingOut() && kumpel.isTame() && !kumpel.isOrderedToSit();
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public void start() {
		index = kumpel.getExitTrail().points().size() - 1;
		repathCooldown = 0;
	}

	@Override
	public void stop() {
		kumpel.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		ServerLevel level = (ServerLevel) kumpel.level();
		List<BlockPos> points = kumpel.getExitTrail().points();
		if (!(kumpel.getOwner() instanceof Player owner) || owner.level() != level) {
			kumpel.finishLeadingOut(false);
			return;
		}
		if (!ExitTrail.isUnderground(level, owner.blockPosition()) || points.isEmpty()) {
			kumpel.finishLeadingOut(true);
			return;
		}

		index = Math.min(index, points.size() - 1);
		BlockPos target = points.get(index);
		Vec3 center = Vec3.atBottomCenterOf(target);
		if (kumpel.distanceToSqr(center) < REACHED_SQ) {
			index--;
			if (index < 0) {
				kumpel.finishLeadingOut(true);
			}
			return;
		}

		if (kumpel.tickCount % 10 == 0) {
			for (int i = index; i >= Math.max(0, index - 3); i--) {
				BlockPos point = points.get(i);
				level.sendParticles(ParticleTypes.GLOW, point.getX() + 0.5, point.getY() + 0.6, point.getZ() + 0.5, 2, 0.15, 0.2, 0.15, 0.0);
			}
		}

		if (kumpel.distanceToSqr(owner) > WAIT_FOR_OWNER_SQ) {
			kumpel.getNavigation().stop();
			kumpel.getLookControl().setLookAt(owner, 30.0F, 30.0F);
			return;
		}

		if (--repathCooldown <= 0) {
			repathCooldown = adjustedTickDelay(10);
			kumpel.getNavigation().moveTo(center.x, center.y, center.z, speedModifier);
		}
	}
}
